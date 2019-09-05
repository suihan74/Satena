package com.suihan74.utilities

import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

class RuntimePermission {
    companion object {
        const val REQUEST_PERMISSION = 1000
        const val PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED
        const val PERMISSION_DENIED = PackageManager.PERMISSION_DENIED
    }

    private val permissions: Array<String>

    constructor(permission: String) {
        permissions = arrayOf(permission)
    }

    constructor(permissions: Array<String>) {
        this.permissions = permissions.distinct().toTypedArray()
    }

    constructor(permissions: List<String>) {
        this.permissions = permissions.distinct().toTypedArray()
    }

    fun request(activity: FragmentContainerActivity) {
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions(activity, permissions)
        }
        else {
            sendResults(activity, permissions, PERMISSION_GRANTED)
        }
    }

    private fun sendResults(activity: FragmentContainerActivity, permissions: Array<String>, result: Int) =
        activity.onRequestPermissionsResult(permissions.map { Pair(it, result) })

    private fun checkPermissions(activity: FragmentContainerActivity, permissions: Array<String>) {
        val notGrantedPermissions = permissions.filterNot {
            PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, it)
        }

        if (notGrantedPermissions.isEmpty()) {
            sendResults(activity, permissions, PERMISSION_GRANTED)
        }
        else {
            requestPermissions(activity, notGrantedPermissions.toTypedArray())
        }
    }

    private fun requestPermissions(activity: FragmentContainerActivity, permissions: Array<String>) {
        val flag = permissions.fold(true) { b, it ->
            b and ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }

        ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSION)

/*        if (flag) {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSION)
        }
        else {
            sendResults(activity, permissions, PERMISSION_DENIED)
        }
 */
    }
}
