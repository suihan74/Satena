package com.suihan74.satena.scenes.post

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.ConnectionFailureException
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TootVisibility
import com.suihan74.satena.models.misskey.NoteVisibility
import com.suihan74.satena.scenes.post.dialog.AddingTagDialog
import com.suihan74.satena.scenes.post.dialog.ConfirmPostBookmarkDialog
import com.suihan74.satena.scenes.post.exceptions.CommentTooLongException
import com.suihan74.satena.scenes.post.exceptions.MultiplePostException
import com.suihan74.satena.scenes.post.exceptions.TagAlreadyExistsException
import com.suihan74.satena.scenes.post.exceptions.TooManyTagsException
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarkPostViewModel(
    val repository: BookmarkPostRepository
) : ViewModel() {

    /** ダイアログ用テーマ */
    val themeId : Int = repository.themeId

    val closeOnTouchOutside : Boolean =
        repository.closeOnTouchOutside

    val entry = repository.entry

    val private = repository.private

    val postTwitter = repository.postTwitter

    val postMisskey = repository.postMisskey

    val postMastodon = repository.postMastodon

    val postFacebook = repository.postFacebook

    val share = repository.share

    /** コメント長 */
    val commentLength = MutableLiveData(0)

    val comment = MutableLiveData<String>("").also {
        // コメントが変化したら文字数を計算し直す
        it.observeForever { comment ->
            commentLength.value = BookmarkPostRepository.calcCommentLength(comment)
        }
    }

    /** Twitterアカウントが紐づいているか否か */
    val signedInTwitter = repository.signedInTwitter

    /** Mastodonアカウントが紐づいているか否か */
    val signedInMastodon = repository.signedInMastodon

    /** Misskeyアカウントが紐づいているか否か */
    val signedInMisskey = repository.signedInMisskey

    /** Facebookアカウントが紐づいているか否か */
    val signedInFacebook = repository.signedInFacebook

    /**
     * 読み込み・投稿処理中の操作防止のために使用
     */
    val nowLoading = MutableLiveData<Boolean>(false)

    /**
     * エントリ情報を上部に表示する
     */
    val displayEntryTitle = MutableLiveData<Boolean>(false)

    /**
     * タグ入力ダイアログを最初から最大展開する
     */
    val expandAddingTagsDialogByDefault = repository.expandAddingTagsDialogByDefault

    // ------ //

    /** 投稿完了時の追加処理 */
    private var onPostSuccess : OnSuccess<BookmarkResult>? = null

    /** 初期化失敗時の追加処理 */
    private var onInitializeFailure : OnError? = null

    /** 投稿完了時の追加処理をセットする */
    fun setOnPostSuccessListener(l : OnSuccess<BookmarkResult>?) {
        onPostSuccess = l
    }

    /** 初期化失敗時の追加処理をセットする */
    fun setOnInitializeFailureListener(l : OnError? = null) {
        onInitializeFailure = l
    }

    // ------ //

    /** 編集中のデータを取得する */
    val editData: BookmarkEditData
        get() = BookmarkEditData(
            entry = entry.value,
            comment = comment.value.orEmpty(),
            private = private.value ?: false,
            postTwitter = postTwitter.value ?: false,
            postMastodon = postMastodon.value ?: false,
            postMisskey = postMisskey.value ?: false,
            postFacebook = postFacebook.value ?: false,
            share = share.value ?: false
        )

    /** 編集データを再現する */
    @MainThread
    fun restore(editData : BookmarkEditData) {
        editData.entry?.let { e ->
            repository.entry.value = e
        }
        comment.value = editData.comment
        private.value = editData.private
        postTwitter.value = editData.postTwitter
        postMastodon.value = editData.postMastodon
        postMisskey.value = editData.postMisskey
        postFacebook.value = editData.postFacebook
        share.value = editData.share
    }

    /** 初期化 */
    fun initialize(
        context: Context,
        entry: Entry,
        editData: BookmarkEditData? = null
    ) = initializeImpl(context, editData) {
        repository.initialize(entry)
    }

    /** 初期化 */
    fun initialize(
        context: Context,
        url: String,
        editData: BookmarkEditData? = null
    ) = initializeImpl(context, editData) {
        repository.initialize(url)
        displayEntryTitle.value = true
    }

    private fun initializeImpl(
        context: Context,
        editData: BookmarkEditData? = null,
        initEntryAction: suspend ()->Unit
    ) = viewModelScope.launch(Dispatchers.Main) {
        nowLoading.value = true

        val result = runCatching {
            initEntryAction()
        }

        when (val e = result.exceptionOrNull()) {
            is AccountLoader.HatenaSignInException -> {
                context.showToast(R.string.msg_hatena_sign_in_failed)
                onInitializeFailure?.invoke(e)
                return@launch
            }

            is AccountLoader.MastodonSignInException -> {
                context.showToast(R.string.msg_auth_mastodon_failed)
            }

            is AccountLoader.MisskeySignInException -> {
                context.showToast(R.string.msg_auth_misskey_failed)
            }

            is ConnectionFailureException -> {
                context.showToast(R.string.msg_post_bookmark_failed)
                onInitializeFailure?.invoke(e)
                return@launch
            }
        }

        if (editData != null) {
            restore(editData)
        }
        else {
            comment.value = entry.value?.bookmarkedData?.commentRaw.orEmpty()
        }

        nowLoading.value = false

        val loadTagsResult = runCatching {
            repository.loadTags()
        }

        if (loadTagsResult.isFailure) {
            context.showToast(R.string.msg_fetch_tags_failed)
        }
    }

    // ------ //

    /** 投稿をキャンセルして閉じたときの処理 */
    fun onCancel() {
        repository.saveStates()
    }

    // ------ //

    /**
     * ブクマを投稿する
     *
     * 必要ならダイアログを開いて投稿するか確認する
     */
    fun postBookmark(
        context: Context,
        fragmentManager: FragmentManager
    ) = viewModelScope.launch(Dispatchers.Main) {
        if (repository.useConfirmDialog) {
            val dialog = ConfirmPostBookmarkDialog.createInstance(
                user = repository.userName,
                comment = comment.value.orEmpty()
            )

            dialog.setOnApproveListener {
                viewModelScope.launch(Dispatchers.Main) {
                    postBookmarkImpl(context)
                }
            }

            dialog.setOnCancelListener {
                nowLoading.value = false
            }

            dialog.showAllowingStateLoss(fragmentManager)
        }
        else {
            postBookmarkImpl(context)
        }
    }

    private suspend fun postBookmarkImpl(context: Context) = withContext(Dispatchers.Main) {
        nowLoading.value = true

        runCatching {
            repository.postBookmark(context, editData)
        }.onSuccess { (bookmarkResult, mstdnException, misskeyException) ->
            when {
                mstdnException != null -> context.showToast(R.string.msg_post_mastodon_failed)
                misskeyException != null -> context.showToast(R.string.msg_post_misskey_failed)
                else -> context.showToast(R.string.msg_post_bookmark_succeeded)
            }
            onPostSuccess?.invoke(bookmarkResult)
        }.onFailure { e ->
            when (e) {
                is MultiplePostException ->
                    context.showToast(R.string.msg_multiple_post)

                is CommentTooLongException ->
                    context.showToast(
                        R.string.msg_comment_too_long,
                        e.limitLength
                    )

                is TooManyTagsException ->
                    context.showToast(
                        R.string.msg_post_too_many_tags,
                        e.limitCount
                    )

                else -> {
                    context.showToast(R.string.msg_post_bookmark_failed)
                    Log.e("postBookmark", Log.getStackTraceString(e))
                }
            }
        }

        nowLoading.value = false
    }

    // ------ //

    private fun createMaintainSelectionTextWatcher(comment: EditText) : TextWatcher {
        return object : TextWatcher {
            private var before: Int = 0
            private var countDiff: Int = 0
            private var tagsEnd: Int = 0

            override fun afterTextChanged(s: Editable?) {
                val after = comment.selectionStart
                if (after == 0) {
                    val selecting =
                        if (before < tagsEnd) repository.getTagsEnd(s)
                        else before + countDiff
                    comment.setSelection(selecting)
                }
                comment.removeTextChangedListener(this)
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                this.tagsEnd = repository.getTagsEnd(s)
                this.before = comment.selectionStart
                this.countDiff = after - count
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        }
    }

    /**
     * 指定タグをON/OFFする
     */
    @MainThread
    private fun toggleTag(context: Context, tag: String, comment: EditText) {
        val maintainSelectionTextWatcher = createMaintainSelectionTextWatcher(comment)

        try {
            comment.addTextChangedListener(maintainSelectionTextWatcher)
            val commentText = this.comment.value.orEmpty()
            this.comment.value = repository.toggleCommentTag(commentText, tag)
        }
        catch (e: TooManyTagsException) {
            context.showToast(R.string.msg_post_too_many_tags, e.limitCount)
            comment.removeTextChangedListener(maintainSelectionTextWatcher)
        }
    }

    /**
     * 指定タグを挿入する（入力済みのコメントに含まれない場合のみ）
     */
    @MainThread
    private fun addTag(context: Context, tag: String, comment: EditText) {
        val maintainSelectionTextWatcher = createMaintainSelectionTextWatcher(comment)

        try {
            val commentText = this.comment.value.orEmpty()
            comment.addTextChangedListener(maintainSelectionTextWatcher)
            this.comment.value = repository.insertTagToComment(commentText, tag)
            context.showToast(R.string.msg_post_tag_added, tag)
        }
        catch (e: TagAlreadyExistsException) {
            comment.removeTextChangedListener(maintainSelectionTextWatcher)
            context.showToast(R.string.msg_post_tag_already_exists, e.tag)
        }
        catch (e: TooManyTagsException) {
            comment.removeTextChangedListener(maintainSelectionTextWatcher)
            context.showToast(R.string.msg_post_too_many_tags, e.limitCount)
        }
    }

    /** タグリストを作成する */
    fun createTagsListAdapter(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        fragmentManager: FragmentManager,
        comment: EditText,
    ) : TagsListAdapter {
        val adapter = TagsListAdapter()
        adapter.setOnItemClickedListener { tag ->
            toggleTag(context, tag, comment)
        }
        adapter.setOnItemLongClickedListener {
            openChangingTagsListOrderDialog(fragmentManager)
        }

        // 使ったことがあるタグを入力するボタンを表示する
        repository.tags.removeObservers(lifecycleOwner)
        repository.tags.observe(lifecycleOwner) {
            adapter.setTags(
                it.map { t -> t.text }
            )
        }

        // タグリストの並び順が変更されたら表示を更新する
        repository.tagsListOrder.observe(lifecycleOwner) {
            viewModelScope.launch {
                repository.updateTagsList()
            }
        }

        return adapter
    }

    // ------ //

    /** タグ入力ダイアログを開く */
    fun openNewTagDialog(context: Context, commentEditText: EditText, fragmentManager: FragmentManager) {
        AddingTagDialog.createInstance()
            .setOnAddingTagListener { tag ->
                if (tag.isNotBlank()) {
                    addTag(context, tag, commentEditText)
                }
            }
            .show(fragmentManager, null)
    }

    // ------ //

    /**
     * Mastodon投稿の公開範囲を選択するダイアログを開く
     */
    fun openTootVisibilitySettingDialog(
        context: Context,
        fragmentManager: FragmentManager
    ) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.MASTODON_POST_VISIBILITY
        val items = TootVisibility.values()
        val labels = items.map { it.textId }
        val initialSelected = prefs.getInt(key)

        AlertDialogFragment.Builder()
            .setTitle(R.string.pref_accounts_mastodon_status_visibility_desc)
            .setSingleChoiceItems(labels, initialSelected) { f, which ->
                SafeSharedPreferences.create<PreferenceKey>(f.context)
                    .edit {
                        putInt(key, which)
                    }
            }
            .setNegativeButton(R.string.dialog_cancel)
            .dismissOnClickItem(true)
            .create()
            .show(fragmentManager, null)
    }

    /**
     * Misskey投稿の公開範囲を選択するダイアログを開く
     */
    fun openNoteVisibilitySettingDialog(
        context: Context,
        fragmentManager: FragmentManager
    ) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.MISSKEY_POST_VISIBILITY
        val items = NoteVisibility.values()
        val labels = items.map { it.textId }
        val initialSelected = prefs.getInt(key)

        AlertDialogFragment.Builder()
            .setTitle(R.string.pref_accounts_misskey_status_visibility_desc)
            .setSingleChoiceItems(labels, initialSelected) { f, which ->
                SafeSharedPreferences.create<PreferenceKey>(f.context)
                    .edit {
                        putInt(key, which)
                    }
            }
            .setNegativeButton(R.string.dialog_cancel)
            .dismissOnClickItem(true)
            .create()
            .show(fragmentManager, null)
    }

    /** タグリストの並び順を変更するダイアログを表示する */
    private fun openChangingTagsListOrderDialog(fragmentManager: FragmentManager) {
        val items = TagsListOrder.values()
        val labelIds = items.map { it.textId }
        val selectedItem = items.indexOf(repository.tagsListOrder.value)

        AlertDialogFragment.Builder()
            .setTitle(R.string.pref_post_bookmarks_tags_list_order_desc)
            .setSingleChoiceItems(labelIds, selectedItem) { _, which ->
                repository.tagsListOrder.value = items[which]
            }
            .setNegativeButton(R.string.dialog_cancel)
            .create()
            .show(fragmentManager, null)
    }
}
