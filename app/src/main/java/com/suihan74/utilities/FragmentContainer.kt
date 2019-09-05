package com.suihan74.utilities

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager

interface FragmentContainer {
    val containerId: Int

    val currentFragment: Fragment?
}
