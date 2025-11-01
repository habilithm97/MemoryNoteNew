package com.example.memorynotenew.common

import androidx.annotation.StringRes
import com.example.memorynotenew.R

enum class PasswordString(@StringRes val resId: Int) {
    NEW(R.string.enter_new_password),
    CONFIRM(R.string.confirm_password),
    RE_ENTER(R.string.reenter_password),
    ENTER(R.string.enter_password)
}