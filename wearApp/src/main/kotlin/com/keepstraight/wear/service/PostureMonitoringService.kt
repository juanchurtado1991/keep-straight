package com.keepstraight.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.keepstraight.wear.KeepStraightWearApp
import com.keepstraight.wear.MainActivity
import com.keepstraight.wear.R
import com.keepstraight.wear.alerts.AlertDispatcher
import com.keepstraight.wear.sensors.OffBodyDetector
import com.keepstraight.shared.presentation.MonitoringState
import com.keepstraight.wear.sync.ConnectionRetryManager
import com.keepstraight.wear.sync.PendingSyncQueue
import com.keepstraight.wear.sync.WearMessageSender
import com.ghost.serialization.Ghost
import com.keepstraight.shared.model.PostureEvent
import com.keepstraight.shared.sync.SyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PostureMonitoringService : Service(), SensorEventListener {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var sensorManager: SensorManager
    private lateinit var offBodyDetector: OffBodyDetector
    private lateinit var alertDispatcher: AlertDispatcher
    private lateinit var pendingSyncQueue: PendingSyncQueue
    private lateinit var connectionRetryManager: ConnectionRetryManager
    private lateinit var messageSender: WearMessageSender

    private var accelerometer: Sensor? = null
    private var stepCounter: Sensor? = null

    private var latestAx = 0f
    private var latestAy = 0f
    private var latestAz = 0f
    private var latestStepCount = 0
    private var sensorsRegistered = false
    private var wasOffWrist = false

    private val sampleRunnable = object : Runnable {
        override fun run() {
            processCurrentSample()
            handler.postDelayed(this, SAMPLE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        sensorManager = getSystemService(SensorManager::class.java)
        offBodyDetector = OffBodyDetector(this)
        alertDispatcher = AlertDispatcher(this)
        pendingSyncQueue = (application as KeepStraightWearApp).pendingSyncQueue
        connectionRetryManager = ConnectionRetryManager(
            context = this,
            onRetry = { attemptSync() },
            onRetryExhausted = { onRetryExhausted() },
        )
        messageSender = WearMessageSender(this)

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        wireSessionCallbacks()
        createNotificationChannel()
        offBodyDetector.register()
    }

    private fun wireSessionCallbacks() {
        val session = (application as KeepStraightWearApp).monitoringSession
        session.onAlert = { alertDispatcher.dispatchAlert(session.getAlertPreferences()) }
        session.onPostureEvent = { event ->
            scope.launch { sendPostureEvent(event) }
        }
        session.onSyncRequested = {
            scope.launch { trySendPendingEvents() }
        }
        session.onConnectionRetry = {
            scope.launch { trySendPendingEvents() }
        }
        session.onConnectionRetryExhausted = {
            session.setPhoneDisconnectedPaused()
        }
        session.onStateChanged = {
            handler.post { updateNotification() }
        }
        session.onEnsureSensors = {
            handler.post {
                wasOffWrist = false
                registerMotionSensors()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isMonitoringEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        registerSensors()
        handler.removeCallbacks(sampleRunnable)
        handler.post(sampleRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(sampleRunnable)
        unregisterSensors()
        offBodyDetector.unregister()
        connectionRetryManager.cancelRetryCycle()
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                latestAx = event.values[0]
                latestAy = event.values[1]
                latestAz = event.values[2]
                offBodyDetector.onAccelerometerSample(
                    ax = latestAx,
                    ay = latestAy,
                    az = latestAz,
                    stepCount = latestStepCount,
                )
            }
            Sensor.TYPE_STEP_COUNTER -> {
                latestStepCount = event.values[0].toInt()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun processCurrentSample() {
        val app = application as KeepStraightWearApp
        val offWrist = offBodyDetector.isOffWrist()

        val calibrating = app.monitoringSession.isCalibrating.value

        // Keep sensors alive while calibrating so capture always gets IMU samples.
        if (!calibrating && offWrist && !wasOffWrist && offBodyDetector.hasOffBodySensor) {
            unregisterMotionSensors()
        } else if (calibrating || (!offWrist && wasOffWrist)) {
            registerMotionSensors()
        }
        if (!calibrating) {
            wasOffWrist = offWrist
        }

        app.monitoringSession.processSample(
            ax = latestAx,
            ay = latestAy,
            az = latestAz,
            // Treat as on-wrist during calibration so capture is not blocked by off-body glitches.
            stepCount = latestStepCount,
            offWrist = if (calibrating) false else offWrist,
        )
    }

    private fun registerSensors() {
        registerMotionSensors()
    }

    private fun registerMotionSensors() {
        if (sensorsRegistered) return
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepCounter?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorsRegistered = true
    }

    private fun unregisterMotionSensors() {
        if (!sensorsRegistered) return
        accelerometer?.let { sensorManager.unregisterListener(this, it) }
        stepCounter?.let { sensorManager.unregisterListener(this, it) }
        sensorsRegistered = false
    }

    private fun unregisterSensors() {
        unregisterMotionSensors()
    }

    private suspend fun trySendPendingEvents() {
        val batchBytes = pendingSyncQueue.encodeBatchBytes() ?: return
        val sent = messageSender.sendToPhone(SyncPaths.EVENTS_BATCH, batchBytes)
        if (sent) {
            pendingSyncQueue.clear()
            connectionRetryManager.cancelRetryCycle()
            (application as KeepStraightWearApp).monitoringSession.setPhoneRetryActive(false)
        } else {
            startConnectionRetryIfNeeded()
        }
    }

    private suspend fun sendPostureEvent(event: PostureEvent) {
        val bytes = Ghost.encodeToBytes(event)
        val sent = messageSender.sendToPhone(SyncPaths.EVENTS, bytes)
        if (!sent) {
            pendingSyncQueue.enqueue(event)
            startConnectionRetryIfNeeded()
        }
    }

    private fun attemptSync() {
        scope.launch { trySendPendingEvents() }
    }

    private fun onRetryExhausted() {
        (application as KeepStraightWearApp).monitoringSession.setPhoneDisconnectedPaused()
    }

    private fun startConnectionRetryIfNeeded() {
        if (!connectionRetryManager.isRetryActive()) {
            connectionRetryManager.startRetryCycle()
            (application as KeepStraightWearApp).monitoringSession.setPhoneRetryActive(true)
        }
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val state = (application as KeepStraightWearApp).monitoringSession.monitoringState.value
        val contentText = when (state) {
            MonitoringState.ACTIVE -> getString(R.string.notification_monitoring_active)
            MonitoringState.NOT_WORN -> getString(R.string.status_not_worn)
            MonitoringState.PHONE_DISCONNECTED_PAUSED -> getString(R.string.status_phone_paused)
            MonitoringState.PHONE_RETRY -> getString(R.string.status_phone_retry)
            else -> (application as KeepStraightWearApp).monitoringSession.statusText.value
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(launchIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "posture_monitoring"
        private const val NOTIFICATION_ID = 1
        private const val SAMPLE_INTERVAL_MS = 500L

        private var instance: PostureMonitoringService? = null

        const val PREFS_NAME = "keepstraight_wear"
        const val KEY_MONITORING_ENABLED = "monitoring_enabled"

        fun getPendingSyncQueue(context: Context): PendingSyncQueue? {
            return (context.applicationContext as? KeepStraightWearApp)?.pendingSyncQueue
        }

        fun handleRetryAlarm(context: Context) {
            instance?.connectionRetryManager?.handleRetryAlarm()
                ?: run {
                    val manager = ConnectionRetryManager(
                        context = context,
                        onRetry = { getPendingSyncQueue(context)?.let { /* no-op without service */ } },
                        onRetryExhausted = {
                            (context.applicationContext as KeepStraightWearApp)
                                .monitoringSession.setPhoneDisconnectedPaused()
                        },
                    )
                    manager.handleRetryAlarm()
                }
        }

        fun cancelRetryCycle(context: Context) {
            instance?.connectionRetryManager?.cancelRetryCycle()
        }

        fun isMonitoringEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_MONITORING_ENABLED, false)
        }

        fun setMonitoringEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MONITORING_ENABLED, enabled)
                .apply()
        }

        fun start(context: Context) {
            val intent = Intent(context, PostureMonitoringService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PostureMonitoringService::class.java))
        }
    }
}
