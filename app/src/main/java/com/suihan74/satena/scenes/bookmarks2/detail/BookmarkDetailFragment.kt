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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks2.dialog.BookmarkMenuDialog
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.fragment_bookmark_detail.view.*

class BookmarkDetailFragment : Fragment() {
    private lateinit var activityViewModel: BookmarksViewModel
    private lateinit var viewModel: BookmarkDetailViewModel

    private val bookmarksActivity
        get() = activity as? BookmarksActivity

    companion object {
        fun createInstance(bookmark: Bookmark) = BookmarkDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_BOOKMARK, bookmark)
            }
        }

        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProviders.of(requireActivity())[BookmarksViewModel::class.java]

        val bookmark = requireArguments().getSerializable(ARG_BOOKMARK) as Bookmark
        val factory = BookmarkDetailViewModel.Factory(activityViewModel.repository, bookmark)
        viewModel = ViewModelProviders.of(this, factory)[BookmarkDetailViewModel::class.java]
        viewModel.init { e ->
            context?.showToast(R.string.msg_get_stars_report_failed)
            Log.e("userStars", Log.getStackTraceString(e))
        }

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
        val tabAdapter = DetailTabAdapter(this)
        view.tab_pager.adapter = tabAdapter
        view.tab_layout.apply {
            setupWithViewPager(view.tab_pager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                }
                override fun onTabUnselected(p0: TabLayout.Tab?) {
                }
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    (tabAdapter.findFragment(view.tab_pager, tab!!.position) as? ScrollableToTop)?.scrollToTop()
                }
            })
        }

        // スター付与ボタン
        view.show_stars_button.setOnClickListener {
            viewModel.starsMenuOpened.postValue(viewModel.starsMenuOpened.value != true)
        }

        // ユーザータグ情報の変更を監視
        activityViewModel.taggedUsers.observe(this, Observer {
            initializeUserNameAndUserTags(view)
        })

        // スターメニューボタンの状態を監視
        viewModel.starsMenuOpened.observe(this, Observer {
            if (it) {
                openStarMenu()
            }
            else {
                closeStarMenu()
            }
        })

        // 所持スター情報を監視
        viewModel.userStars.observe(this, Observer {
            view.red_stars_count.text = it.red.toString()
            view.green_stars_count.text = it.green.toString()
            view.blue_stars_count.text = it.blue.toString()
            view.purple_stars_count.text = it.purple.toString()
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

    /** 対象ブクマ表示部を初期化 */
    private fun initializeBookmarkArea(view: View) {
        viewModel.bookmark.also {
            val analyzedComment = BookmarkCommentDecorator.convert(it.comment)
            view.comment.let { comment ->
                comment.text = analyzedComment.comment

                // 選択テキストを画面下部で強調表示，スターを付ける際に引用文とする
                comment.customSelectionActionModeCallback = object : ActionMode.Callback {
                    override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?) : Boolean {
                        view.quote_text_view.apply {
                            val selectedText = comment.text.substring(comment.selectionStart, comment.selectionEnd)
                            text = String.format("\"%s\"", selectedText)
                            visibility = View.VISIBLE
                        }
                        return false
                    }
                    override fun onDestroyActionMode(p0: ActionMode?) {
                        view.quote_text_view.apply {
                            text = ""
                            visibility = View.INVISIBLE
                        }
                    }
                    override fun onCreateActionMode(p0: ActionMode?, p1: Menu?) = true
                    override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?) = false
                }
            }

            Glide.with(requireContext())
                .load(it.userIconUrl)
                .into(view.user_icon)

            // タグ部分
            view.tags_layout.apply {
                if (it.tags.isNullOrEmpty()) {
                    visibility = View.GONE
                }
                else {
                    visibility = View.VISIBLE
                    view.tags.apply {
                        text = BookmarkCommentDecorator.makeClickableTagsText(it.tags) { tag ->
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
                dialog.show(childFragmentManager, "bookmark_dialog")
            }
        }
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
}
