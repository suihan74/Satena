package com.suihan74.utilities

import android.view.View

fun Boolean?.toVisibility(defaultInvisible: Int = View.GONE) =
    if (this == true) View.VISIBLE
    else defaultInvisible
