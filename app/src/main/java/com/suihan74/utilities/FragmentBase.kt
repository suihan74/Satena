package com.suihan74.utilities

import androidx.fragment.app.Fragment

abstract class FragmentBase : Fragment() {
    open val title : String = ""
    open val isToolbarVisible : Boolean = true
    open val isSearchViewVisible : Boolean = false
}
