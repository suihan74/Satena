package com.suihan74.satena.scenes.entries2.pages

import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.satena.scenes.entries2.NoticesAdapter
import com.suihan74.satena.scenes.entries2.dialog.NoticeMenuDialog
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.extensions.putEnum
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.showAllowingStateLoss

class NoticesTabFragment : EntriesTabFragmentBase() {
    companion object {
        fun createInstance(fragmentViewModelKey: String) = NoticesTabFragment().withArguments {
            putString(ARG_FRAGMENT_VIEW_MODEL_KEY, fragmentViewModelKey)
            putEnum(ARG_CATEGORY, Category.Notices)
        }

        /** NoticeMenuDialog用のタグ */
        private const val DIALOG_NOTICE_MENU = "DIALOG_NOTICE_MENU"
    }

    override fun initializeRecyclerView(
        entriesList: RecyclerView,
        swipeLayout: SwipeRefreshLayout
    ) {
        val context = requireContext()

        // エントリリスト用のアダプタ
        val noticesAdapter = NoticesAdapter().apply {
            setOnItemClickedListener { notice ->
                when (notice.verb) {
                    Notice.VERB_STAR -> onClickedForStar(notice)
                    Notice.VERB_ADD_FAVORITE -> onClickedForFavorite(notice)
                    Notice.VERB_BOOKMARK -> onClickedForBookmark(notice)
                    else -> onClickedForUnknown(notice)
                }
            }

            setOnItemLongClickedListener { notice ->
                NoticeMenuDialog.createInstance(notice).also { dialog ->
                    dialog.setOnDeleteNoticeListener {
                        viewModel.deleteNotice(it, onErrorRefreshEntries)
                    }
                    dialog.showAllowingStateLoss(childFragmentManager, DIALOG_NOTICE_MENU)
                }
                true
            }
        }

        // エントリリストの設定
        entriesList.adapter = noticesAdapter

        // 引っ張って更新
        swipeLayout.apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(context.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                viewModel.reloadLists(
                    onError = onErrorRefreshEntries,
                    onFinally = { this.isRefreshing = false }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        entriesList.adapter.alsoAs<NoticesAdapter> { adapter ->
            adapter.onResume()
        }
    }

    /** スターが付けられたときの通知をクリックしたときの処理 */
    private fun onClickedForStar(notice: Notice) {
        runCatching {
            Intent(requireContext(), BookmarksActivity::class.java).apply {
                putExtra(BookmarksActivity.EXTRA_ENTRY_ID, notice.eid)
                putExtra(
                    BookmarksActivity.EXTRA_TARGET_USER,
                    notice.user
                )
            }
        }.onSuccess { intent ->
            startActivity(intent)
        }.onFailure {
            requireContext().startInnerBrowser(url = notice.link)
        }
    }

    /** お気に入りユーザーに追加されたときの通知をクリックしたときの処理 */
    private fun onClickedForFavorite(notice: Notice) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
        startActivity(intent)
    }

    /** ユーザーのコンテンツがブクマされたときの通知をクリックしたときの処理 */
    private fun onClickedForBookmark(notice: Notice) {
        val baseUrl = "${HatenaClient.B_BASE_URL}/entry?url="
        if (notice.link.startsWith(baseUrl)) {
            val url = Uri.decode(notice.link.substring(baseUrl.length))
            val intent = Intent(requireContext(), BookmarksActivity::class.java).apply {
                putExtra(BookmarksActivity.EXTRA_ENTRY_URL, url)
            }
            startActivity(intent)
        }
        else {
            onClickedForUnknown(notice)
        }
    }

    /** 種別が判別できない通知をクリックしたときの処理 */
    private fun onClickedForUnknown(notice: Notice) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.link))
        startActivity(intent)
    }
}
