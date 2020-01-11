package com.suihan74.satena.scenes.bookmarks2.detail

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.text.style.TextAppearanceSpan
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks2.dialog.BookmarkMenuDialog
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.fragment_bookmark_detail.view.*

class BookmarkDetailFragment :
    Fragment(),
    AlertDialogFragment.Listener
{
    private lateinit var activityViewModel: BookmarksViewModel
    lateinit var viewModel: BookmarkDetailViewModel

    private val bookmarksActivity
        get() = activity as? BookmarksActivity

    /** この画面で表示しているブックマーク */
    val bookmark
        get() = viewModel.bookmark

    companion object {
        fun createInstance(bookmark: Bookmark) = BookmarkDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_BOOKMARK, bookmark)
            }
        }

        // argument keys
        private const val ARG_BOOKMARK = "ARG_BOOKMARK"

        // dialog tags
        private const val DIALOG_BOOKMARK_MENU = "DIALOG_BOOKMARK_MENU"
        private const val DIALOG_CONFIRM_POST_STAR = "DIALOG_CONFIRM_POST_STAR"
        private const val DIALOG_DATA_STAR_COLOR = "DIALOG_CONFIRM_POST_STAR.STAR_COLOR"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProviders.of(requireActivity())[BookmarksViewModel::class.java]

        val bookmark = requireArguments().getSerializable(ARG_BOOKMARK) as Bookmark
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val factory = BookmarkDetailViewModel.Factory(activityViewModel.repository, prefs, bookmark)
        viewModel = ViewModelProviders.of(this, factory)[BookmarkDetailViewModel::class.java].apply {
            // スターロード失敗時の挙動
            setOnLoadedStarsFailureListener { e ->
                requireActivity().showToast(R.string.msg_update_stars_failed)
                Log.e("UserStars", Log.getStackTraceString(e))
            }
            // スター付与完了時の挙動を設定
            setOnCompletedPostStarListener {
                requireActivity().showToast(R.string.msg_post_star_succeeded, viewModel.bookmark.user)
            }
            setOnPostStarFailureListener { color, throwable ->
                when (throwable) {
                    is BookmarkDetailViewModel.StarExhaustedException -> {
                        requireActivity().showToast(R.string.msg_no_color_stars, color)
                    }
                    else -> {
                        requireActivity().showToast(R.string.msg_post_star_failed, viewModel.bookmark.user)
                    }
                }
                Log.e("PostStar", Log.getStackTraceString(throwable))
            }
        }

        viewModel.init()

        // 非表示ユーザーリストが更新されたら各リストを更新する
        activityViewModel.ignoredUsers.observe(this, Observer {
            viewModel.loadMentions()
            viewModel.starsToUser.notifyReload()
            viewModel.starsAll.notifyReload()
        })

        // 画面遷移アニメーション
        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(GravityCompat.END))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmark_detail, container, false)

        // 対象ブクマ表示部初期化
        initializeBookmarkArea(view)

        // タブの設定
        val tabAdapter = DetailTabAdapter(this).apply {
            setOnDataSetChangedListener {
                // アイコンを設定
                (0 until count).forEach { i ->
                    val context = requireContext()
                    val tab = view.tab_layout.getTabAt(i) ?: return@forEach
                    tab.icon = context.getDrawable(getPageTitleIcon(i))?.apply {
                        setColorFilter(
                            context.getThemeColor(
                                if (i == view.tab_layout.selectedTabPosition) R.attr.tabSelectedTextColor
                                else R.attr.tabTextColor
                            ),
                            PorterDuff.Mode.SRC_IN
                        )
                    }
                }
            }
        }

        view.tab_pager.adapter = tabAdapter
        view.tab_layout.apply {
            setupWithViewPager(view.tab_pager)

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.icon?.setColorFilter(
                        requireContext().getThemeColor(R.attr.tabSelectedTextColor),
                        PorterDuff.Mode.SRC_IN
                    )
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tab?.icon?.setColorFilter(
                        requireContext().getThemeColor(R.attr.tabTextColor),
                        PorterDuff.Mode.SRC_IN
                    )
                }
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    (tabAdapter.findFragment(view.tab_pager, tab!!.position) as? ScrollableToTop)?.scrollToTop()
                }
            })

            tabAdapter.notifyDataSetChanged()
        }

        // スター付与ボタンの表示状態を切り替える
        view.show_stars_button.run {
            setOnClickListener {
                viewModel.starsMenuOpened.postValue(viewModel.starsMenuOpened.value != true)
            }
            hide()
        }

        // スター付与ボタン各色
        view.yellow_star_button.setOnClickListener { postStar(StarColor.Yellow) }
        view.red_star_button.setOnClickListener { postStar(StarColor.Red) }
        view.green_star_button.setOnClickListener { postStar(StarColor.Green) }
        view.blue_star_button.setOnClickListener { postStar(StarColor.Blue) }
        view.purple_star_button.setOnClickListener { postStar(StarColor.Purple) }


        // スターメニューボタンの状態を監視
        viewModel.starsMenuOpened.observe(this, Observer {
            if (it) {
                openStarMenu()
            }
            else {
                closeStarMenu()
            }
        })

        // サインイン状態でブコメがあればスターを付けられるようにする
        activityViewModel.signedIn.observe(this, Observer {
            if (it && bookmark.comment.isNotBlank()) {
                view.show_stars_button.show()
            }
            else {
                view.show_stars_button.hide()
            }
        })

        // 所持スター情報を監視
        viewModel.userStars.observe(this, Observer {
            view.red_stars_count.text = it.red.toString()
            view.green_stars_count.text = it.green.toString()
            view.blue_stars_count.text = it.blue.toString()
            view.purple_stars_count.text = it.purple.toString()
        })

        // コメント引用を監視
        viewModel.quote.observe(this, Observer { comment ->
            view.quote_text_view.run {
                text = getString(R.string.bookmark_detail_quote_comment, comment)
                visibility = (!comment.isNullOrBlank()).toVisibility()
            }
        })

        viewModel.starsToUser.observe(this, Observer {
            val idx = getTabIndex<StarsToUserFragment>(view, tabAdapter) ?: return@Observer
            val tab = view.tab_layout.getTabAt(idx)
            tab?.text = String.format("%s (%d)", tabAdapter.getPageTitle(idx), it?.totalStarsCount ?: 0)
        })

        viewModel.starsFromUser.observe(this, Observer {
            val idx = getTabIndex<StarsFromUserFragment>(view, tabAdapter) ?: return@Observer
            val tab = view.tab_layout.getTabAt(idx)
            tab?.text = String.format("%s (%d)", tabAdapter.getPageTitle(idx), it.sumBy { s -> s.star?.count ?: 0 })
        })

        viewModel.mentionsToUser.observe(this, Observer {
            val idx = getTabIndex<MentionToUserFragment>(view, tabAdapter) ?: return@Observer
            val tab = view.tab_layout.getTabAt(idx)
            tab?.text = String.format("%s (%d)", tabAdapter.getPageTitle(idx), it.size)
        })

        viewModel.mentionsFromUser.observe(this, Observer {
            val idx = getTabIndex<MentionFromUserFragment>(view, tabAdapter) ?: return@Observer
            val tab = view.tab_layout.getTabAt(idx)
            tab?.text = String.format("%s (%d)", tabAdapter.getPageTitle(idx), it.size)
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        // 戻るボタンを監視
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (viewModel.starsMenuOpened.value == true) {
                viewModel.starsMenuOpened.postValue(false)
            }
            else {
                bookmarksActivity?.onBackPressedCallback?.handleOnBackPressed()
            }
        }
    }

    private inline fun <reified T> getTabIndex(view: View, tabAdapter: DetailTabAdapter) =
        (0 until tabAdapter.count).firstOrNull { i ->
            tabAdapter.findFragment(view.tab_pager, i) is T
        }

    /** 対象ブクマ表示部を初期化 */
    private fun initializeBookmarkArea(view: View) {
        val bookmark = viewModel.bookmark
        val analyzedComment = BookmarkCommentDecorator.convert(bookmark.comment)
        view.comment.run {
            text = analyzedComment.comment

            visibility = analyzedComment.comment.isNotBlank().toVisibility()

            // 選択テキストを画面下部で強調表示，スターを付ける際に引用文とする
            customSelectionActionModeCallback = object : ActionMode.Callback {
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) : Boolean {
                    try {
                        val selectedText = text.substring(selectionStart, selectionEnd)
                        viewModel.quote.value = selectedText
                    }
                    catch (e: Throwable) {
                        viewModel.quote.value = null
                    }
                    return false
                }
                override fun onDestroyActionMode(mode: ActionMode?) {
                    viewModel.quote.value = null
                }
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = true
                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
            }

            // テキスト中のリンクを処理
            movementMethod = object : MutableLinkMovementMethod2() {
                override fun onSinglePressed(link: String) {
                    if (link.startsWith("http")) {
                        bookmarksActivity?.onLinkClicked(link)
                    }
                    else {
                        analyzedComment.entryIds
                            .firstOrNull { eid -> link.contains(eid.toString()) }
                            ?.let { eid ->
                                bookmarksActivity?.onEntryIdClicked(eid)
                            }
                    }
                }

                override fun onLongPressed(link: String) {
                    if (link.startsWith("http")) {
                        bookmarksActivity?.onLinkLongClicked(link)
                    }
                }
            }
        }

        Glide.with(requireContext())
            .load(bookmark.userIconUrl)
            .into(view.user_icon)

        // タグ部分
        view.tags_layout.apply {
            if (bookmark.tags.isNullOrEmpty()) {
                visibility = View.GONE
            }
            else {
                visibility = View.VISIBLE
                view.tags.apply {
                    text = BookmarkCommentDecorator.makeClickableTagsText(bookmark.tags) { tag ->
                        val intent = Intent(context, EntriesActivity::class.java).apply {
                            putExtra(EntriesActivity.EXTRA_DISPLAY_TAG, tag)
                        }
                        startActivity(intent)
                    }
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }

        // メニューボタン
        view.menu_button.setOnClickListener {
            val dialog = BookmarkMenuDialog.createInstance(viewModel.bookmark)
            dialog.show(childFragmentManager, DIALOG_BOOKMARK_MENU)
        }

        // 非表示ユーザーマーク
        activityViewModel.ignoredUsers.observe(this, Observer {
            view.ignored_user_mark.visibility = it.contains(bookmark.user).toVisibility()
        })

        // ユーザータグ情報の変更を監視
        activityViewModel.taggedUsers.observe(this, Observer {
            initializeUserNameAndUserTags(view)
        })
    }

    /** ユーザー名・ユーザータグ表示を初期化 */
    private fun initializeUserNameAndUserTags(view: View) {
        view.user_name.run {
            val bookmark = viewModel.bookmark
            val user = bookmark.user
            val tags = activityViewModel.taggedUsers.value?.firstOrNull { it.user.name == user }?.tags?.sortedBy { it.id }
            text =
                if (tags.isNullOrEmpty()) {
                    bookmark.user
                }
                else {
                    val tagsText = tags.joinToString(",") { it.name }
                    val tagColor = resources.getColor(R.color.tagColor, null)
                    val st = "$user _$tagsText"
                    val density = resources.displayMetrics.scaledDensity
                    val size = (13 * density).toInt()

                    SpannableString(st).apply {
                        val drawable = resources.getDrawable(R.drawable.ic_user_tag, null).apply {
                            setBounds(0, 0, lineHeight, lineHeight)
                            setTint(tagColor)
                        }
                        val pos = user.length + 1
                        setSpan(
                            ImageSpan(drawable),
                            pos, pos + 1,
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(
                            TextAppearanceSpan(null, Typeface.DEFAULT.style, size, ColorStateList.valueOf(tagColor), null),
                            user.length + 1, st.length,
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
        }
    }

    /** スターをつける（必要なら確認ダイアログを表示） */
    private fun postStar(color: StarColor) {
        if (!viewModel.checkStarCount(color)) return

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val showDialog = prefs.getBoolean(PreferenceKey.USING_POST_STAR_DIALOG)
        if (showDialog) {
            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.confirm_dialog_title_simple)
                .setIcon(R.drawable.ic_baseline_help)
                .setMessage(getString(R.string.msg_post_star_dialog, color.name))
                .setPositiveButton(R.string.dialog_ok)
                .setNegativeButton(R.string.dialog_cancel)
                .setAdditionalData(DIALOG_DATA_STAR_COLOR, color)
                .show(childFragmentManager, DIALOG_CONFIRM_POST_STAR)
        }
        else {
            viewModel.postStar(color)
        }
    }

    // --- スターボタンの表示切替アニメーション --- //

    private fun showStarButton(layoutId: Int, counterId: Int, dimenId: Int) =
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> showStarButtonLandscape(layoutId, counterId, dimenId)
            else -> showStarButtonPortrait(layoutId, counterId, dimenId)
        }

    private fun showStarButtonPortrait(layoutId: Int, counterId: Int, dimenId: Int) {
        val view = view!!
        val layout = view.findViewById<View>(layoutId)
        val counter = view.findViewById<View>(counterId)
        val pos = requireContext().resources.getDimension(dimenId)

        layout.animate()
            .withEndAction {
                counter.animate()
                    .translationXBy(100f)
                    .translationX(0f)
                    .alphaBy(0.0f)
                    .alpha(1.0f)
                    .duration = 100
            }
            .translationYBy(0f)
            .translationY(-pos)
            .alphaBy(0f)
            .alpha(1f)
            .duration = 100
    }

    private fun showStarButtonLandscape(layoutId: Int, counterId: Int, dimenId: Int) {
        val view = view!!
        val layout = view.findViewById<View>(layoutId)
        val counter = view.findViewById<View>(counterId)
        val pos = requireContext().resources.getDimension(dimenId)

        layout.animate()
            .withEndAction {
                counter.animate()
                    .translationYBy(50f)
                    .translationY(0f)
                    .alphaBy(0.0f)
                    .alpha(1.0f)
                    .duration = 100
            }
            .translationXBy(0f)
            .translationX(-pos)
            .alphaBy(0f)
            .alpha(1f)
            .duration = 100
    }


    private fun hideStarButton(layoutId: Int, counterId: Int) =
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> hideStarButtonLandscape(layoutId, counterId)
            else -> hideStarButtonPortrait(layoutId, counterId)
        }

    private fun hideStarButtonPortrait(layoutId: Int, counterId: Int) {
        val view = view!!
        val layout = view.findViewById<View>(layoutId)
        val counter = view.findViewById<View>(counterId)

        counter.animate()
            .withEndAction {
                layout.animate()
                    .translationY(0f)
                    .alpha(0f)
                    .duration = 100
            }
            .translationX(100f)
            .alphaBy(1.0f)
            .alpha(0.0f)
            .duration = 100
    }

    private fun hideStarButtonLandscape(layoutId: Int, counterId: Int) {
        val view = view!!
        val layout = view.findViewById<View>(layoutId)
        val counter = view.findViewById<View>(counterId)

        counter.animate()
            .withEndAction {
                layout.animate()
                    .translationX(0f)
                    .alpha(0f)
                    .duration = 100
            }
            .translationY(50f)
            .alphaBy(1.0f)
            .alpha(0.0f)
            .duration = 100
    }

    private fun openStarMenu() {
        showStarButton(
            R.id.purple_star_layout,
            R.id.purple_stars_count,
            R.dimen.purple_star_position
        )
        showStarButton(
            R.id.blue_star_layout,
            R.id.blue_stars_count,
            R.dimen.blue_star_position
        )
        showStarButton(
            R.id.red_star_layout,
            R.id.red_stars_count,
            R.dimen.red_star_position
        )
        showStarButton(
            R.id.green_star_layout,
            R.id.green_stars_count,
            R.dimen.green_star_position
        )
        showStarButton(
            R.id.yellow_star_layout,
            R.id.yellow_stars_count,
            R.dimen.yellow_star_position
        )

        view!!.show_stars_button.setImageResource(R.drawable.ic_baseline_close)
    }

    private fun closeStarMenu() {
        hideStarButton(R.id.purple_star_layout, R.id.purple_stars_count)
        hideStarButton(R.id.blue_star_layout, R.id.blue_stars_count)
        hideStarButton(R.id.red_star_layout, R.id.red_stars_count)
        hideStarButton(R.id.green_star_layout, R.id.green_stars_count)
        hideStarButton(R.id.yellow_star_layout, R.id.yellow_stars_count)

        view!!.show_stars_button.setImageResource(R.drawable.ic_star)
    }


    // --- post star dialog --- //

    override fun onClickPositiveButton(dialog: AlertDialogFragment) {
        when (dialog.tag) {
            DIALOG_CONFIRM_POST_STAR -> {
                val color = dialog.getAdditionalData<StarColor>(DIALOG_DATA_STAR_COLOR)!!
                viewModel.postStar(color)
            }
        }
    }
}
