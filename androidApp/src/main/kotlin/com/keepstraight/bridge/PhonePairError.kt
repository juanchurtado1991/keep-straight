package com.keepstraight.bridge

import androidx.annotation.StringRes
import com.keepstraight.R

enum class PhonePairError(@StringRes val messageRes: Int) {
    NO_WIFI(R.string.phone_pair_no_wifi),
    DESKTOP_UNREACHABLE(R.string.phone_pair_desktop_unreachable),
    DESKTOP_REJECTED(R.string.phone_pair_desktop_rejected),
}

class PhonePairException(
    val error: PhonePairError,
    detail: String? = null,
) : Exception(detail)
