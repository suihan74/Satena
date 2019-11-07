package com.suihan74.utilities

import android.os.Bundle
import androidx.fragment.app.Fragment
import java.util.*

abstract class FragmentBase : Fragment() {
    open val title : String = ""
    open val subtitle : String? = null
    open val isToolbarVisible : Boolean = true
    open val isSearchViewVisible : Boolean = false

    companion object {
        private const val BUNDLE_FRAGMENT_ID = "com.suihan74.utilities.FragmentBase.fragmentId"
    }

    private lateinit var fragmentId: String

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_FRAGMENT_ID, fragmentId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            fragmentId = UUID.randomUUID().toString()
        }
        else {
            fragmentId = savedInstanceState.getString(BUNDLE_FRAGMENT_ID)!!
        }
    }
}


