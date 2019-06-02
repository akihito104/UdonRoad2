package com.freshdigitable.udonroad2.timeline.bindingadapter

import androidx.databinding.BindingAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

@BindingAdapter("isVisible")
fun bindFabVisible(v: FloatingActionButton, isVisible: Boolean?) {
    if (isVisible == true) {
        v.show()
    } else {
        v.hide()
    }
}
