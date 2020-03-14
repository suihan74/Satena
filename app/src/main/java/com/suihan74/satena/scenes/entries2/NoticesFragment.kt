package com.suihan74.satena.scenes.entries2

import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.entries2.dialog.NoticeMenuDialog
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments

class NoticesFragment : EntriesTabFragmentBase() {
    companion object {
        fun createInstance(fragmentViewModelKey: String) = NoticesFragment().withArguments {
            putString(ARG_FRAGMENT_VIEW_MODEL_KEY, fragmentViewModelKey)
            putEnum(ARG_CATEGORY, Category.Notices)
        }
    }

    /** tag for NoticeMenuDialog */
    private val DIALOG_NOTICE_MENU by lazy { "DIALOG_NOTICE_MENU" }

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
                val dialog = NoticeMenuDialog.createInstance(notice)
                dialog.show(childFragmentManager, DIALOG_NOTICE_MENU)
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
                viewModel.refresh(onErrorRefreshEntries).invokeOnCompletion {
                    this.isRefreshing = false
                }
            }
        }
    }

    /** スターが付けられたときの通知をクリックしたときの処理 */
    private fun onClickedForStar(notice: Notice) {
        val intent = Intent(requireContext(), BookmarksActivity::class.java).apply {
            putExtra(BookmarksActivity.EXTRA_ENTRY_ID, notice.eid)
            putExtra(
                BookmarksActivity.EXTRA_TARGET_USER,
                notice.user
            )
        }
        startActivity(intent)
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
