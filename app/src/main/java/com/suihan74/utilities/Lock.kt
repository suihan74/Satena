package com.suihan74.utilities

// TODO: coroutinesと併用すると問題があるので`Mutex`ロックに置き換える
inline fun <T,U> lock(obj: T, action: (T) -> U) : U {
    var result : U
    synchronized(obj as Any) {
        result = action(obj)
    }
    return result
}
