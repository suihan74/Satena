package com.suihan74.utilities

inline fun <T,U> lock(obj: T, action: (T) -> U) : U {
    var result : U
    synchronized(obj as Any) {
        result = action(obj)
    }
    return result
}
