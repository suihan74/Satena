package com.suihan74.utilities

interface PermissionRequestable {
    fun onRequestPermissionsResult(pairs: List<Pair<String, Int>>)
}
