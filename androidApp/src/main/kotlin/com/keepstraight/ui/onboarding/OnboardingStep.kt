package com.keepstraight.ui.onboarding

enum class OnboardingStep {
    WELCOME,
    PAIR,
    NOTIFICATIONS,
    BATTERY,
    WATCH_PERMISSIONS,
    CALIBRATE,
    SENSITIVITY,
    ;

    fun next(): OnboardingStep? = entries.getOrNull(ordinal + 1)

    fun previous(): OnboardingStep? = entries.getOrNull(ordinal - 1)
}
