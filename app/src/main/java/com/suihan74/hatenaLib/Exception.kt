package com.suihan74.hatenaLib

/** はてなへのサインイン失敗 */
class SignInFailureException : RuntimeException {
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String? = null, cause: Throwable? = null) : super(message, cause)
}

/** はてなスターへのサインイン失敗 */
class SignInStarFailureException : RuntimeException {
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String? = null, cause: Throwable? = null) : super(message, cause)
}

/** 非表示ユーザーリスト更新失敗 */
class FetchIgnoredUsersFailureException : RuntimeException {
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String? = null, cause: Throwable? = null) : super(message, cause)
}
