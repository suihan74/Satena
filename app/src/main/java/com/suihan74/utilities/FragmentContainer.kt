package com.suihan74.utilities

import androidx.fragment.app.Fragment

interface FragmentContainer {
    val containerId: Int
    val currentFragment: Fragment?
}
