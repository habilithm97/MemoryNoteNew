package com.example.memorynotenew.common

import androidx.annotation.StringRes
import com.example.memorynotenew.R

enum class PasswordString(@StringRes val resId: Int) {
    CONFIRM(R.string.password_confirm),
    RE_ENTER(R.string.password_reenter)
}