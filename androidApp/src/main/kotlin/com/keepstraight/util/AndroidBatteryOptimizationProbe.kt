package com.keepstraight.util

import android.content.Context
import com.keepstraight.shared.platform.BatteryOptimizationProbe

class AndroidBatteryOptimizationProbe(
    private val context: Context,
) : BatteryOptimizationProbe {
    override fun isOptimizationRequired(): Boolean =
        !BatteryOptimizationChecker.isIgnoringBatteryOptimizations(context)
}
