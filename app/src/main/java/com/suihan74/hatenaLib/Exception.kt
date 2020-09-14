package com.suihan74.hatenaLib

/** はてなへのサインイン失敗 */
class SignInFailureException : RuntimeException {
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String? = null, cause: Throwable? = null) : super(message, cause)
}

/** はてなスターへのサインイン失敗 */
class SignInStarFailureException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

/** 非表示ユーザーリスト更新失敗 */
class FetchIgnoredUsersFailureException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

/** 404 Not Found */
class NotFoundException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

/** timeout */
class TimeoutException : RuntimeException {
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String? = null, cause: Throwable? = null) : super(message, cause)
}

/** ユーザーが所持するスター数の取得に失敗 */
class FetchUserStarsException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

/** (理由を問わない)通信失敗 */
class ConnectionFailureException: RuntimeException {
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String? = null, cause: Throwable? = null) : super(message, cause)
}
