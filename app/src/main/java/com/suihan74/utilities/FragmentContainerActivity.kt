package com.suihan74.utilities

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

abstract class FragmentContainerActivity : AppCompatActivity(), FragmentContainer {

    open fun onFragmentShown(fragment: Fragment) {
    }

    fun showFragment(fragment: Fragment, backStackLabel: String? = null) {
        supportFragmentManager.beginTransaction().apply {
            val current = currentFragment
            if (current != null) {
                hide(current)
            }
            add(containerId, fragment)
            addToBackStack(backStackLabel)
            commitAllowingStateLoss()
        }

        onFragmentShown(fragment)
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            val current = currentFragment
            if (current == null) {
                addToBackStack(null)
            }
            replace(containerId, fragment)
            commitAllowingStateLoss()
        }

        onFragmentShown(fragment)
    }

    fun popFragment() {
        supportFragmentManager.popBackStackImmediate()
        currentFragment?.let {
            it.onStart()
            it.onResume()
            onFragmentShown(it)
        }
    }

    fun popFragment(backStackLabel: String?, flag: Int = 0) {
        supportFragmentManager.popBackStackImmediate(backStackLabel, flag)
        currentFragment?.let {
            it.onStart()
            it.onResume()
            onFragmentShown(it)
        }
    }

    fun isFragmentShowed() = currentFragment != null
    fun isFragmentShowed(fragment: Fragment) = currentFragment == fragment

    override val currentFragment : Fragment?
        get() = supportFragmentManager.findFragmentById(containerId)

    inline fun <reified T> getStackedFragment() =
        supportFragmentManager.fragments.lastOrNull { it is T } as? T

    inline fun <reified T> getStackedFragment(predicate: (T)->Boolean) =
        supportFragmentManager.fragments.lastOrNull { it is T && predicate(it) } as? T

    override fun onBackPressed() = onBackPressed(null)

    private fun backActivity() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
            overridePendingTransition(0, android.R.anim.slide_out_right)
        }
        else {
            popFragment()
//            super.onBackPressed()
        }
    }

    fun onBackPressed(alternativeAction: (()->Boolean)?) {
        try {
            val fragment = currentFragment
            if (fragment is BackPressable) {
                if (fragment.onBackPressed()) {
                    return
                }
            }

            val acted = alternativeAction?.invoke() ?: false

            if (!acted) {
                backActivity()
            }
        }
        catch (e: Exception) {
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


inline fun <reified T> FragmentManager.get() : T? =
    this.fragments.lastOrNull { it is T } as? T

inline fun <reified T> FragmentManager.get(predicate: (Fragment) -> Boolean) : T? =
    this.fragments.lastOrNull(predicate) as? T
