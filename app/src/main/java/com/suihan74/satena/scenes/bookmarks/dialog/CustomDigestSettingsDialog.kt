package com.suihan74.satena.scenes.bookmarks.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogCustomDigestSettingsBinding
import com.suihan74.satena.dialogs.NumberPickerDialog
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.utilities.Listener
import com.suihan74.utilities.lazyProvideViewModel

/**
 * ブクマリストのダイジェスト抽出方法の設定ダイアログ
 */
class CustomDigestSettingsDialog : BottomSheetDialogFragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDialogCustomDigestSettingsBinding.inflate(localLayoutInflater()).also {
            it.vm = viewModel.apply { initialize(bookmarksViewModel, childFragmentManager) }
            it.lifecycleOwner = this
        }

        binding.okButton.setOnClickListener {
            viewModel.finish(bookmarksViewModel, this)
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        return binding.root
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
        val maxNumOfElements = MutableLiveData<Int>()
        val starsCountThreshold = MutableLiveData<Int>()

        var onComplete : Listener<DialogFragment>? = null

        private var fragmentManager : FragmentManager? = null
        private var initialized : Boolean = false

        // ------ //

        fun initialize(
            bookmarksViewModel: BookmarksViewModel,
            fragmentManager: FragmentManager
        ) {
            this.fragmentManager = fragmentManager
            if (initialized) {
                return
            }
            val repo = bookmarksViewModel.repository
            useCustomDigest.value = repo.useCustomDigest.value
            ignoreStarsByIgnoredUsers.value = repo.ignoreStarsByIgnoredUsers.value
            deduplicateStars.value = repo.deduplicateStars.value
            maxNumOfElements.value = repo.maxNumOfElements.value
            starsCountThreshold.value = repo.starsCountThreshold.value
            initialized = true
        }

        fun finish(bookmarksViewModel: BookmarksViewModel, fragment: DialogFragment) {
            val repo = bookmarksViewModel.repository
            repo.useCustomDigest.value = useCustomDigest.value
            repo.ignoreStarsByIgnoredUsers.value = ignoreStarsByIgnoredUsers.value
            repo.deduplicateStars.value = deduplicateStars.value
            repo.maxNumOfElements.value = maxNumOfElements.value
            repo.starsCountThreshold.value = starsCountThreshold.value
            onComplete?.invoke(fragment)
        }

        // ------ //

        fun openMaxNumOfElementsPickerDialog() {
            fragmentManager?.let { fm ->
                NumberPickerDialog.createInstance(
                    min = 1, max = 30, default = maxNumOfElements.value ?: 10,
                    titleId = R.string.digest_bookmarks_max_num_of_elements_picker_title
                ).setOnCompleteListener { num ->
                    maxNumOfElements.value = num
                }.show(fm, null)
            }
        }

        fun openStarsCountThresholdPickerDialog() {
            fragmentManager?.let { fm ->
                NumberPickerDialog.createInstance(
                    min = 0, max = 100, default = starsCountThreshold.value ?: 1,
                    titleId = R.string.digest_bookmarks_stars_count_threshold_picker_title
                ).setOnCompleteListener { num ->
                    starsCountThreshold.value = num
                }.show(fm, null)
            }
        }
    }
}