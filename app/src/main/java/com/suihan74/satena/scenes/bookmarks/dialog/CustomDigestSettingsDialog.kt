package com.suihan74.satena.scenes.bookmarks.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.utilities.Listener
import com.suihan74.utilities.lazyProvideViewModel

/**
 * ブクマリストのダイジェスト抽出方法の設定ダイアログ
 */
class CustomDigestSettingsDialog : DialogFragment() {

    companion object {
        fun createInstance() = CustomDigestSettingsDialog()
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        DialogViewModel()
    }

    private val bookmarksViewModel
        get() = (requireActivity() as BookmarksActivity).bookmarksViewModel

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel.initialize(bookmarksViewModel)
        val items = viewModel.items()
        val labels = items.map { getString(it.first) }.toTypedArray()
        val initialStates = items.map { it.second == true }.toBooleanArray()

        return createBuilder()
            .setTitle(R.string.digest_bookmarks_settings_dialog_title)
            .setMultiChoiceItems(labels, initialStates) { _, which, b ->
                items[which].third(b)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.finish(bookmarksViewModel, this)
                dismiss()
            }
            .create()
    }

    // ------ //

    fun setOnCompleteListener(listener: Listener<DialogFragment>?) : CustomDigestSettingsDialog {
        lifecycleScope.launchWhenCreated {
            viewModel.onComplete = listener
        }
        return this
    }

    // ------ //

    class DialogViewModel : ViewModel() {
        val useCustomDigest = MutableLiveData<Boolean>()
        val ignoreStarsByIgnoredUsers = MutableLiveData<Boolean>()
        val deduplicateStars = MutableLiveData<Boolean>()

        var onComplete : Listener<DialogFragment>? = null

        // ------ //

        fun initialize(
            bookmarksViewModel: BookmarksViewModel
        ) {
            val repo = bookmarksViewModel.repository
            useCustomDigest.value = repo.useCustomDigest.value
            ignoreStarsByIgnoredUsers.value = repo.ignoreStarsByIgnoredUsers.value
            deduplicateStars.value = repo.deduplicateStars.value
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun items() = buildList<Triple<Int, Boolean?, (Boolean)->Unit>> {
            add(Triple(R.string.digest_bookmarks_use_custom_digest_desc, useCustomDigest.value, { b -> useCustomDigest.value = b }))
            add(Triple(R.string.digest_bookmarks_ignore_users_desc, ignoreStarsByIgnoredUsers.value, { b -> ignoreStarsByIgnoredUsers.value = b }))
            add(Triple(R.string.digest_bookmarks_deduplicate_stars_desc, deduplicateStars.value, { b -> deduplicateStars.value = b }))
        }

        fun finish(bookmarksViewModel: BookmarksViewModel, fragment: DialogFragment) {
            val repo = bookmarksViewModel.repository
            repo.useCustomDigest.value = useCustomDigest.value
            repo.ignoreStarsByIgnoredUsers.value = ignoreStarsByIgnoredUsers.value
            repo.deduplicateStars.value = deduplicateStars.value
            onComplete?.invoke(fragment)
        }
    }
}
