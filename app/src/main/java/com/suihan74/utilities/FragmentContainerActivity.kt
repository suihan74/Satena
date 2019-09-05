package com.suihan74.utilities

import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

abstract class FragmentContainerActivity : AppCompatActivity(), FragmentContainer {

    fun showFragment(fragment: Fragment, backStackLabel: String?) {
        supportFragmentManager.beginTransaction().apply {
            addToBackStack(backStackLabel)
            replace(containerId, fragment)
            commit()
        }
    }

    fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(containerId, fragment)
            commit()
        }
    }

    fun popFragment() =
        supportFragmentManager.popBackStack()

    fun popFragment(backStackLabel: String?, flag: Int = 0) =
        supportFragmentManager.popBackStack(backStackLabel, flag)

    fun isFragmentShowed() = currentFragment != null
    fun isFragmentShowed(fragment: Fragment) = currentFragment == fragment

    override val currentFragment : Fragment?
        get() = supportFragmentManager.findFragmentById(containerId)

    override fun onBackPressed() {
        val fragment = currentFragment
        if (fragment is BackPressable) {
            if (fragment.onBackPressed()) { return }
        }
        super.onBackPressed()
    }

    fun onBackPressed(alternativeAction: ()->Boolean) {
        val fragment = currentFragment
        if (fragment is BackPressable) {
            if (fragment.onBackPressed()) { return }
        }
        if (!alternativeAction()) {
            super.onBackPressed()
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
