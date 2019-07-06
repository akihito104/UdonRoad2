package com.freshdigitable.udonroad2.timeline.bindingadapter

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("visibility_goneIfNullOrEmpty")
fun bindVisibilityGoneIfNullOrEmpty(v: TextView, text: String?) {
    if (text.isNullOrEmpty()) {
        v.visibility = View.GONE
    } else {
        v.visibility = View.VISIBLE
    }
}
