package com.suihan74.utilities.exceptions

/** 操作失敗時の(汎用的な)例外 */
class TaskFailureException(
    message: String = "",
    cause: Throwable? = null
) : Throwable(message, cause)
