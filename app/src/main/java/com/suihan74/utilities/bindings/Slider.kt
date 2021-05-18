package com.suihan74.utilities.bindings

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.slider.Slider

object SliderBindingAdapters {
    @JvmStatic
    @BindingAdapter("android:value")
    fun bindValue(slider: Slider, value: Float?) {
        (value ?: slider.valueFrom).let { next ->
            if (slider.value != next) {
                slider.value = next
            }
        }
    }

    @JvmStatic
    @InverseBindingAdapter(attribute = "android:value")
    fun bindValueInverse(slider: Slider) : Float = slider.value

    @JvmStatic
    @BindingAdapter("android:valueAttrChanged")
    fun bindListeners(slider: Slider, listener: InverseBindingListener?) {
        slider.addOnChangeListener { _, _, _ ->
            listener?.onChange()
        }
    }
}
