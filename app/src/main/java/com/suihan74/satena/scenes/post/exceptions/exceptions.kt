package com.suihan74.satena.scenes.post.exceptions

/** サインインできない */
class NotSignedInException : Throwable("not signed in")

/** タグリスト読み込み失敗 */
class LoadingTagsFailureException(cause: Throwable? = null) : Throwable("loading tags is failed.", cause)

/** コメント長すぎ例外 */
class CommentTooLongException(val limitLength: Int) : Throwable("the comment is too long to post.")

/** 使用タグ数が制限を超える例外 */
class TooManyTagsException(val limitCount: Int) : Throwable("too many tags (more than 10)")

/** 多重投稿例外 */
class MultiplePostException : Throwable("multiple post")

/** Mastodonへの投稿に失敗 */
class PostingMastodonFailureException(cause: Throwable? = null) : Throwable(cause = cause)
