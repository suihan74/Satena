package com.suihan74.satena.scenes.entries2.dialog

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentDialogBrowserShortcutBinding
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSiteMenuDialog
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSiteRegistrationDialog
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesAdapter
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.ExpandableBottomSheetDialogFragment
import com.suihan74.utilities.exceptions.EmptyException
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.extensions.showSoftInputMethod
import com.suihan74.utilities.lazyProvideViewModel
import com.suihan74.utilities.showAllowingStateLoss

/**
 * エントリ画面からブラウザを開く際の追加機能ショートカット
 */
class BrowserShortcutDialog : ExpandableBottomSheetDialogFragment() {
    companion object {
        fun createInstance() = BrowserShortcutDialog()
    }

    // ------ //

    val viewModel by lazyProvideViewModel {
        DialogViewModel(SatenaApplication.instance.favoriteSitesRepository)
    }

    // ------ //

    private var _binding : FragmentDialogBrowserShortcutBinding? = null
    private val binding get() = _binding!!

    override val hiddenTopView: View
        get() = binding.favoriteSitesHeader

    override val expandBottomSheetByDefault = false

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogBrowserShortcutBinding.inflate(
            layoutInflater,
            container,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = this
        }

        viewModel.fragment = this

        binding.openButton.setOnClickListener {
            runCatching {
                viewModel.openBrowser(requireActivity())
                dismissAllowingStateLoss()
            }
        }

        binding.searchButton.setOnClickListener {
            dismissAfterOpenBrowser()
        }

        binding.searchText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    dismissAfterOpenBrowser()
                    true
                }
                else -> false
            }
        }

        binding.favoriteSitesList.adapter = FavoriteSitesAdapter(viewLifecycleOwner).also { adapter ->
            adapter.setOnClickItemListener {
                val url = it.site?.url ?: return@setOnClickItemListener
                dismissAfterOpenBrowser(url)
            }

            adapter.setOnLongLickItemListener {
                val site = it.site ?: return@setOnLongLickItemListener
                viewModel.openFavoriteSiteMenuDialog(site, parentFragmentManager)
            }
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        activity?.hideSoftInputMethod()
        super.onDismiss(dialog)
    }


    // ------ //

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        // ダイアログのサイズ変更したときIMEの高さの分だけダイアログがずれるので、IMEを一度閉じて開き直す
        when (newState) {
            BottomSheetBehavior.STATE_EXPANDED,
            BottomSheetBehavior.STATE_COLLAPSED -> {
                val editTextFocused = binding.searchText.hasFocus()
                binding.searchText.hideSoftInputMethod(binding.root)
                if (editTextFocused) {
                    dialog?.showSoftInputMethod(requireActivity(), binding.searchText)
                }
            }
            else -> {}
        }
    }

    // ------ //

    private fun dismissAfterOpenBrowser(query: String? = null) {
        val activity = requireActivity()
        try {
            activity.hideSoftInputMethod()
            viewModel.openBrowserWithQuery(activity, query)
            dismissAllowingStateLoss()
        }
        catch (e: EmptyException) {
            activity.showToast(R.string.msg_dialog_browser_shortcut_empty_search_query)
        }
    }

    // ------ //

    class DialogViewModel(
        private val favoriteSitesRepo : FavoriteSitesRepository
    ) : ViewModel() {

        var fragment: BrowserShortcutDialog? = null

        /** 検索クエリ入力欄の内容 */
        val searchQuery = MutableLiveData<String>()

        /** お気に入りサイトリスト */
        val favoriteSites : LiveData<List<FavoriteSite>> by lazy { _favoriteSites }
        private val _favoriteSites = favoriteSitesRepo.favoriteSites

        // ------ //

        /**
         * ブラウザを開いて初期ページに遷移する
         */
        fun openBrowser(activity: Activity) {
            activity.startInnerBrowser()
        }

        /**
         * ブラウザを開いて検索orページ遷移する
         *
         * @throws EmptyException
         */
        fun openBrowserWithQuery(activity: Activity, query: String? = null) {
            val q = query ?: searchQuery.value
            if (q.isNullOrBlank()) {
                throw EmptyException()
            }
            activity.startInnerBrowser(q)
        }

        /**
         * お気に入りサイトのメニューダイアログを表示する
         */
        fun openFavoriteSiteMenuDialog(site: FavoriteSite, fragmentManager: FragmentManager) {
            val dialog = FavoriteSiteMenuDialog.createInstance(site)

            dialog.setOnOpenListener { value, f ->
                runCatching {
                    openBrowserWithQuery(f.requireActivity(), value.url)
                    fragment?.dismissAllowingStateLoss()
                }
            }

            dialog.setOnOpenEntriesListener { value, f ->
                runCatching {
                    val activity = f.requireActivity()
                    openEntries(activity, value)
                    fragment?.dismissAllowingStateLoss()
                }
            }

            dialog.setOnModifyListener { value, f ->
                runCatching {
                    openFavoriteSiteModificationDialog(value, f.parentFragmentManager)
                    // ボトムシートは閉じない
                }
            }

            dialog.setOnDeleteListener { value, f ->
                deleteFavoriteSite(f.requireActivity(), value)
                // ボトムシートは閉じない
            }

            dialog.showAllowingStateLoss(fragmentManager)
        }

        /**
         * 選択したお気に入り項目のエントリ一覧を開く
         */
        private fun openEntries(activity: Activity, site: FavoriteSite) {
            when (activity) {
                is EntriesActivity -> {
                    activity.showSiteEntries(site.url)
                }

                else -> {
                    val intent = Intent(activity, EntriesActivity::class.java).also {
                        it.putExtra(EntriesActivity.EXTRA_SITE_URL, site.url)
                    }
                    activity.startActivity(intent)
                }
            }
        }

        /**
         * 選択したお気に入り項目を修正する
         */
        private fun openFavoriteSiteModificationDialog(site: FavoriteSite, fragmentManager: FragmentManager) {
            val dialog = FavoriteSiteRegistrationDialog.createModificationInstance(site)

            dialog.setDuplicationChecker { item ->
                item.url != site.url && favoriteSitesRepo.contains(item.url)
            }

            dialog.setOnModifyListener { result ->
                _favoriteSites.value = _favoriteSites.value.orEmpty()
                    .map {
                        if (it.url == result.url) result
                        else it
                    }
            }

            dialog.showAllowingStateLoss(fragmentManager)
        }

        /**
         * 選択したお気に入り項目を削除する
         */
        private fun deleteFavoriteSite(context: Context, site: FavoriteSite) {
            val result = runCatching {
                favoriteSitesRepo.unfavoritePage(site)
            }

            if (result.isSuccess) {
                context.showToast(R.string.unfavorite_site_succeeded)
            }
            else {
                context.showToast(R.string.unfavorite_site_failed)
                Log.w("unfavoriteSite", Log.getStackTraceString(result.exceptionOrNull()))
            }
        }
    }
}
