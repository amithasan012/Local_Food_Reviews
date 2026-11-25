package com.localfoodreviews.app.util

import android.view.View
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.textValue(): String = editText?.text?.toString()?.trim().orEmpty()

fun View.show(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}
