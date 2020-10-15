package com.suihan74.utilities

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

abstract class FragmentContainerActivity : AppCompatActivity(), FragmentContainer {

    open fun onFragmentShown(fragment: Fragment) {
    }

    fun showFragment(fragment: Fragment, backStackLabel: String? = null) : Fragment {
        fragment.retainInstance = false

        supportFragmentManager.beginTransaction().apply {
            addToBackStack(backStackLabel)
            replace(containerId, fragment, backStackLabel)
            commitAllowingStateLoss()
        }

        onFragmentShown(fragment)

        return fragment
    }

    fun replaceFragment(fragment: Fragment) : Fragment {
        fragment.retainInstance = false

        supportFragmentManager.beginTransaction().apply {
            val current = currentFragment
            if (current == null) {
                addToBackStack(null)
            }
            replace(containerId, fragment, null)
            commitAllowingStateLoss()
        }

        onFragmentShown(fragment)

        return fragment
    }

    fun popFragment() {
        supportFragmentManager.popBackStackImmediate()
        currentFragment?.let {
            onFragmentShown(it)
        }
    }

    fun popFragment(backStackLabel: String?, flag: Int = 0) {
        supportFragmentManager.popBackStackImmediate(backStackLabel, flag)
        currentFragment?.let {
            onFragmentShown(it)
        }
    }

    fun isFragmentShowed() = currentFragment != null
    fun isFragmentShowed(fragment: Fragment) = currentFragment == fragment

    override val currentFragment : Fragment?
        get() = supportFragmentManager.findFragmentById(containerId)

    inline fun <reified T> getStackedFragment(tag: String) =
        supportFragmentManager.findFragmentByTag(tag) as? T

    override fun onBackPressed() = onBackPressed(null)

    private fun backActivity() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
            overridePendingTransition(0, android.R.anim.slide_out_right)
        }
        else {
            popFragment()
        }
    }

    fun onBackPressed(alternativeAction: (()->Boolean)?) {
        try {
            if (onBackPressedDispatcher.hasEnabledCallbacks()) {
                onBackPressedDispatcher.onBackPressed()
                return
            }

            val acted = alternativeAction?.invoke() ?: false

            if (!acted) {
                backActivity()
            }
        }
        catch (e: Throwable) {
            Log.e("onBackPressed", Log.getStackTraceString(e))
            backActivity()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == RuntimePermission.REQUEST_PERMISSION) {
            val pairs = permissions.zip(grantResults.toTypedArray())
            onRequestPermissionsResult(pairs)
        }
    }

    open fun onRequestPermissionsResult(
        pairs: List<Pair<String, Int>>
    ) {
        val fragment = currentFragment
        if (fragment is PermissionRequestable) {
            fragment.onRequestPermissionsResult(pairs)
        }
    }
}

