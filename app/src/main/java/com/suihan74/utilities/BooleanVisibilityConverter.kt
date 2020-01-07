package com.suihan74.utilities

import android.view.View

fun Boolean.toVisibility(defaultInvisible: Int = View.GONE) =
    if (this)
        View.VISIBLE
    else
        defaultInvisible
