package com.suihan74.satena.scenes.bookmarks2.detail

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.NetworkReceiver
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentBookmarkDetailBinding
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks2.dialog.PostStarDialog
import com.suihan74.satena.scenes.browser.bookmarks.StarExhaustedException
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarkDetailFragment : Fragment() {
    companion object {
        fun createInstance(bookmark: Bookmark) = BookmarkDetailFragment().withArguments {
            putObject(ARG_BOOKMARK, bookmark)
        }

        // argument keys
        private const val ARG_BOOKMARK = "ARG_BOOKMARK"

        // dialog tags
        private const val DIALOG_CONFIRM_POST_STAR = "DIALOG_CONFIRM_POST_STAR"
    }

    // ------ //

    private val activityViewModel: BookmarksViewModel
        get() = (requireActivity() as BookmarksActivity).viewModel

    val viewModel: BookmarkDetailViewModel by lazyProvideViewModel {
        val bookmark = requireArguments().getObject<Bookmark>(ARG_BOOKMARK)!!
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        BookmarkDetailViewModel(activityViewModel.repository, prefs, bookmark).also {
            initializeViewModel(it)
        }
    }

    private fun initializeViewModel(vm: BookmarkDetailViewModel) {
        // スターロード失敗時の挙動
        vm.setOnLoadedStarsFailureListener { e ->
            activity?.showToast(R.string.msg_update_stars_failed)
            Log.e("UserStars", Log.getStackTraceString(e))
        }
        // スター付与完了時の挙動を設定
        vm.setOnCompletedPostStarListener {
            activity?.showToast(R.string.msg_post_star_succeeded, viewModel.bookmark.user)
        }
        vm.setOnPostStarFailureListener { color, throwable ->
            when (throwable) {
                is StarExhaustedException -> {
                    activity?.showToast(R.string.msg_no_color_stars, color)
                }
                else -> {
                    activity?.showToast(R.string.msg_post_star_failed, viewModel.bookmark.user)
                }
            }
            Log.e("PostStar", Log.getStackTraceString(throwable))
        }

        vm.init()
    }

    private val bookmarksActivity
        get() = requireActivity() as BookmarksActivity

    /** この画面で表示しているブックマーク */
    val bookmark
        get() = viewModel.bookmark

    // ------ //

    private var _binding : FragmentBookmarkDetailBinding? = null
    private val binding
        get() = _binding!!

    // ------ //


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 画面遷移アニメーション
        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this._binding = FragmentBookmarkDetailBinding.inflate(inflater, container, false)

        // 対象ブクマ表示部初期化
        initializeBookmarkArea(binding)

        // タブの設定
        val tabAdapter = DetailTabAdapter(this).apply {
            setOnDataSetChangedListener {
                // アイコンを設定
                val context = requireContext()
                repeat(count) { i ->
                    binding.tabLayout.getTabAt(i)?.let { tab ->
                        tab.icon = ContextCompat.getDrawable(context, getPageTitleIcon(i))?.also {
                            val color = context.getThemeColor(
                                if (i == binding.tabLayout.selectedTabPosition) R.attr.tabSelectedTextColor
                                else R.attr.tabTextColor
                            )
                            DrawableCompat.setColorFilter(it, color)
                        }
                    }
                }
            }
        }

        binding.tabPager.adapter = tabAdapter
        binding.tabLayout.apply {
            setupWithViewPager(binding.tabPager)

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val icon = tab?.icon ?: return
                    DrawableCompat.setColorFilter(icon, requireContext().getThemeColor(R.attr.tabSelectedTextColor))
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    val icon = tab?.icon ?: return
                    DrawableCompat.setColorFilter(icon, requireContext().getThemeColor(R.attr.tabTextColor))
                }
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    tabAdapter.findFragment(binding.tabPager, tab!!.position).alsoAs<ScrollableToTop> {
                        it.scrollToTop()
                    }
                }
            })

            tabAdapter.notifyDataSetChanged()
        }

        // スター付与ボタンの表示状態を切り替える
        binding.showStarsButton.run {
            setOnClickListener {
                lifecycleScope.launch(Dispatchers.Default) {
                    viewModel.userStars.onLoaded {
                        // 所持スターがロードされていない場合、再度ロード処理
                        viewModel.loadUserStars()
                    }
                    viewModel.starsMenuOpened.postValue(viewModel.starsMenuOpened.value != true)
                }
            }
            hide()
        }

        // スター付与ボタン各色
        binding.yellowStarButton.setOnClickListener { postStar(StarColor.Yellow) }
        binding.redStarButton.setOnClickListener { postStar(StarColor.Red) }
        binding.greenStarButton.setOnClickListener { postStar(StarColor.Green) }
        binding.blueStarButton.setOnClickListener { postStar(StarColor.Blue) }
        binding.purpleStarButton.setOnClickListener { postStar(StarColor.Purple) }

        // 非表示ユーザーリストが更新されたら各リストを更新する
        activityViewModel.ignoredUsers.observe(viewLifecycleOwner) {
            viewModel.loadMentions()
            viewModel.starsToUser.notifyReload()
            viewModel.starsAll.notifyReload()
        }

        // スターメニューボタンの状態を監視
        viewModel.starsMenuOpened.observe(viewLifecycleOwner) {
            if (it) {
                openStarMenu()
            }
            else {
                closeStarMenu()
            }
        }

        // サインイン状態でブコメがあればスターを付けられるようにする
        activityViewModel.signedIn.observe(viewLifecycleOwner) {
            if (it && bookmark.comment.isNotBlank()) {
                binding.showStarsButton.show()
            }
            else {
                binding.showStarsButton.hide()
            }
        }

        // 所持スター情報を監視
        viewModel.userStars.observe(viewLifecycleOwner) {
            binding.redStarsCount.text = it.red.toString()
            binding.greenStarsCount.text = it.green.toString()
            binding.blueStarsCount.text = it.blue.toString()
            binding.purpleStarsCount.text = it.purple.toString()
        }

        // コメント引用を監視
        viewModel.quote.observe(viewLifecycleOwner) { comment ->
            binding.quoteTextView.run {
                text = getString(R.string.bookmark_detail_quote_comment, comment)
                visibility = (!comment.isNullOrBlank()).toVisibility()
            }
        }

        // タブタイトルに各タブのアイテム数を表示する
        viewModel.starsToUser.observe(viewLifecycleOwner) {
            val idx = getTabIndex<StarsToUserFragment>(tabAdapter) ?: return@observe
            val tab = binding.tabLayout.getTabAt(idx)
            tab?.text = String.format("(%d) %s", it?.totalStarsCount ?: 0, tabAdapter.getPageTitle(idx))
        }

        viewModel.starsFromUser.observe(viewLifecycleOwner) {
            val idx = getTabIndex<StarsFromUserFragment>(tabAdapter) ?: return@observe
            val tab = binding.tabLayout.getTabAt(idx)
            tab?.text = String.format("(%d) %s", it.sumBy { s -> s.star?.count ?: 0 }, tabAdapter.getPageTitle(idx))
        }

        viewModel.mentionsToUser.observe(viewLifecycleOwner) {
            val idx = getTabIndex<MentionToUserFragment>(tabAdapter) ?: return@observe
            val tab = binding.tabLayout.getTabAt(idx)
            tab?.text = String.format("(%d) %s", it.size, tabAdapter.getPageTitle(idx))
        }

        viewModel.mentionsFromUser.observe(viewLifecycleOwner) {
            val idx = getTabIndex<MentionFromUserFragment>(tabAdapter) ?: return@observe
            val tab = binding.tabLayout.getTabAt(idx)
            tab?.text = String.format("(%d) %s", it.size, tabAdapter.getPageTitle(idx))
        }

        // 接続状態を監視する
        var isNetworkReceiverInitialized = false
        val networkReceiver = SatenaApplication.instance.networkReceiver
        networkReceiver.state.observe(viewLifecycleOwner) { state ->
            if (!isNetworkReceiverInitialized) {
                isNetworkReceiverInitialized = true
                return@observe
            }

            if (state == NetworkReceiver.State.CONNECTED) {
                viewModel.viewModelScope.launch {
                    try {
                        viewModel.starsToUser.update()
                        viewModel.starsAll.update()
                    }
                    catch (e: Throwable) {
                        withContext(Dispatchers.Main) {
                            context?.showToast(R.string.msg_update_stars_failed)
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // 戻るボタンを監視
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (viewModel.starsMenuOpened.value == true) {
                viewModel.starsMenuOpened.postValue(false)
            }
            else {
                bookmarksActivity.onBackPressedCallback.handleOnBackPressed()
            }
        }
    }

    private inline fun <reified T> getTabIndex(tabAdapter: DetailTabAdapter) =
        (0 until tabAdapter.count).firstOrNull { i ->
            tabAdapter.findFragment(binding.tabPager, i) is T
        }

    /** 対象ブクマ表示部を初期化 */
    private fun initializeBookmarkArea(binding: FragmentBookmarkDetailBinding) {
        val bookmark = viewModel.bookmark
        val analyzedComment = BookmarkCommentDecorator.convert(bookmark.comment)
        binding.comment.run {
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
                        activityViewModel.onLinkClicked(bookmarksActivity, link)
                    }
                    else {
                        analyzedComment.entryIds
                            .firstOrNull { eid -> link.contains(eid.toString()) }
                            ?.let { eid ->
                                activityViewModel.onEntryIdClicked(bookmarksActivity, eid)
                            }
                    }
                }

                override fun onLongPressed(link: String) {
                    if (link.startsWith("http")) {
                        activityViewModel.onLinkLongClicked(bookmarksActivity, link)
                    }
                }
            }
        }

        Glide.with(requireContext())
            .load(bookmark.userIconUrl)
            .into(binding.userIcon)

        // タグ部分
        binding.tagsLayout.apply {
            if (bookmark.tags.isNullOrEmpty()) {
                visibility = View.GONE
            }
            else {
                visibility = View.VISIBLE
                binding.tags.apply {
                    text = BookmarkCommentDecorator.makeClickableTagsText(bookmark.tags) { tag ->
                        val intent = Intent(context, EntriesActivity::class.java).apply {
                            putExtra(EntriesActivity.EXTRA_SEARCH_TAG, tag)
                        }
                        startActivity(intent)
                    }
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }

        // メニューボタン
        binding.menuButton.setOnClickListener {
            activityViewModel.openBookmarkMenuDialog(bookmarksActivity, viewModel.bookmark)
        }

        // 非表示ユーザーマーク
        activityViewModel.ignoredUsers.observe(viewLifecycleOwner) {
            binding.ignoredUserMark.visibility = it.contains(bookmark.user).toVisibility()
        }

        // ユーザータグ情報の変更を監視
        activityViewModel.taggedUsers.observe(viewLifecycleOwner) {
            initializeUserNameAndUserTags(binding)
        }
    }

    /** ユーザー名・ユーザータグ表示を初期化 */
    private fun initializeUserNameAndUserTags(binding: FragmentBookmarkDetailBinding) {
        binding.userName.run {
            val bookmark = viewModel.bookmark
            val user = bookmark.user
            val tags = activityViewModel.taggedUsers.value?.firstOrNull { it.user.name == user }?.tags?.sortedBy { it.id }
            text =
                if (tags.isNullOrEmpty()) {
                    bookmark.user
                }
                else {
                    val tagsText = tags.joinToString(",") { it.name }
                    val st = "$user _$tagsText"
                    val size = context.sp2px(12)

                    SpannableString(st).apply {
                        val tagTextColor = context.getThemeColor(R.attr.tagTextColor)
                        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_user_tag, null)!!.apply {
                            setBounds(0, 0, lineHeight, lineHeight)
                            setTint(tagTextColor)
                        }
                        val pos = user.length + 1
                        setSpan(
                            ImageSpan(drawable),
                            pos, pos + 1,
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(
                            TextAppearanceSpan(null, Typeface.NORMAL, size, ColorStateList.valueOf(tagTextColor), null),
                            user.length + 1, st.length,
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
        }
    }

    /** スターをつける（必要なら確認ダイアログを表示） */
    private fun postStar(color: StarColor) {
        if (!viewModel.checkStarCount(color)) return

        val quote = viewModel.quote.value

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val showDialog = prefs.getBoolean(PreferenceKey.USING_POST_STAR_DIALOG)
        if (showDialog) {
            viewModel.viewModelScope.launch(Dispatchers.Main) {
                val dialog = PostStarDialog.createInstance(bookmark, color, quote ?: "")
                dialog.showAllowingStateLoss(childFragmentManager, DIALOG_CONFIRM_POST_STAR)
                dialog.setOnPostStar { (_, starColor, _) ->
                    viewModel.postStar(starColor, quote)
                }
            }
        }
        else {
            viewModel.postStar(color, quote)
        }
    }

    // --- スターボタンの表示切替アニメーション --- //

    private fun showStarButton(layoutId: Int, counterId: Int, dimenId: Int) =
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> showStarButtonLandscape(layoutId, counterId, dimenId)
            else -> showStarButtonPortrait(layoutId, counterId, dimenId)
        }

    private fun showStarButtonPortrait(layoutId: Int, counterId: Int, dimenId: Int) {
        val view = requireView()
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
        val view = requireView()
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
        val view = requireView()
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
        val view = requireView()
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

        binding.showStarsButton.setImageResource(R.drawable.ic_baseline_close)
    }

    private fun closeStarMenu() {
        hideStarButton(R.id.purple_star_layout, R.id.purple_stars_count)
        hideStarButton(R.id.blue_star_layout, R.id.blue_stars_count)
        hideStarButton(R.id.red_star_layout, R.id.red_stars_count)
        hideStarButton(R.id.green_star_layout, R.id.green_stars_count)
        hideStarButton(R.id.yellow_star_layout, R.id.yellow_stars_count)

        binding.showStarsButton.setImageResource(R.drawable.ic_add_star_filled)
    }
}
