package com.suihan74.satena.scenes.bookmarks.detail

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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.StarColor
import com.suihan74.HatenaLib.UserColorStarsCount
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.BookmarkDialog
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.BookmarksFragment
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.showCustomTabsIntent
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class BookmarkDetailFragment :
        CoroutineScopeFragment(),
        BackPressable,
        BookmarkDialog.Listener,
        UserTagDialogFragment.Listener
{
    private lateinit var mView : View
    private lateinit var mBookmark : Bookmark

    private lateinit var mColorStars : UserColorStarsCount

    private val mIsStarMenuOpened : Boolean
        get() = mView.findViewById<View>(R.id.yellow_star_layout).alpha > 0f

    private val bookmarksFragment : BookmarksFragment?
        get() {
            val activity = activity as? BookmarksActivity ?: throw IllegalStateException("BookmarksDetailFragment has created from an invalid activity")
            return activity.bookmarksFragment
        }

    val bookmark
        get() = mBookmark

    override val isToolbarVisible: Boolean = false

    override val fragmentManagerForDialog: FragmentManager
        get() = childFragmentManager

    companion object {
        fun createInstance(b: Bookmark) = BookmarkDetailFragment().apply {
            mBookmark = b
        }

        private const val BUNDLE_BOOKMARK = "mBookmark"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putSerializable(BUNDLE_BOOKMARK, mBookmark)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(GravityCompat.END))

        savedInstanceState?.let {
            mBookmark = it.getSerializable(BUNDLE_BOOKMARK) as Bookmark
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bookmark_detail, container, false)
        mView = view

        val activity = activity as? BookmarksActivity ?: throw IllegalStateException("BookmarksDetailFragment has created from an invalid activity")

        val icon = view.findViewById<ImageView>(R.id.user_icon)

        // ユーザー名/ユーザータグの表示を初期化
        updateUserTags()

        view.findViewById<View>(R.id.tags_layout).apply {
            if (mBookmark.tags.isNullOrEmpty()) {
                visibility = View.GONE
            }
            else {
                visibility = View.VISIBLE
                mView.findViewById<TextView>(R.id.tags).apply {
                    text = BookmarkCommentDecorator.makeClickableTagsText(mBookmark.tags) { tag ->
                        val intent = Intent(
                            SatenaApplication.instance,
                            EntriesActivity::class.java
                        ).apply {
                            putExtra(EntriesActivity.EXTRA_DISPLAY_TAG, tag)
                        }
                        startActivity(intent)
                    }
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }

        // ブクマに対するメニューを表示
        view.findViewById<ImageButton>(R.id.menu_button).setOnClickListener {
            BookmarkDialog.Builder(
                    bookmark,
                    bookmarksFragment!!.entry)
                .build()
                .show(childFragmentManager, "dialog")
        }

        val starMenuButton = view.findViewById<FloatingActionButton>(R.id.show_stars_button)
        if (HatenaClient.signedIn()) {
            starMenuButton.setOnClickListener {
                starMenuButton.show()
                if (mIsStarMenuOpened) {
                    closeStarMenu()
                } else {
                    openStarMenu()
                }
            }

            view.findViewById<FloatingActionButton>(R.id.yellow_star_button).setOnClickListener {
                postStar(StarColor.Yellow, Int.MAX_VALUE)
            }
            view.findViewById<FloatingActionButton>(R.id.red_star_button).setOnClickListener {
                postStar(StarColor.Red, mColorStars.red)
            }
            view.findViewById<FloatingActionButton>(R.id.green_star_button).setOnClickListener {
                postStar(StarColor.Green, mColorStars.green)
            }
            view.findViewById<FloatingActionButton>(R.id.blue_star_button).setOnClickListener {
                postStar(StarColor.Blue, mColorStars.blue)
            }
            view.findViewById<FloatingActionButton>(R.id.purple_star_button).setOnClickListener {
                postStar(StarColor.Purple, mColorStars.purple)
            }

            val showStarsButton = mView.findViewById<View>(R.id.show_stars_button).apply {
                visibility = View.GONE
            }

            if (HatenaClient.signedIn()) {
                launch(Dispatchers.Main) {
                    try {
                        mColorStars = HatenaClient.getMyColorStarsAsync().await()
                    }
                    catch (e: Exception) {
                        mColorStars = UserColorStarsCount(red = 0, green = 0, blue = 0, purple = 0)
                        Log.d("failedToFetchStars", Log.getStackTraceString(e))
                        activity.showToast("所持スター数の取得に失敗しました")
                    }
                    finally {
                        mView.apply {
                            findViewById<TextView>(R.id.red_stars_count).text = mColorStars.red.toString()
                            findViewById<TextView>(R.id.green_stars_count).text = mColorStars.green.toString()
                            findViewById<TextView>(R.id.blue_stars_count).text = mColorStars.blue.toString()
                            findViewById<TextView>(R.id.purple_stars_count).text = mColorStars.purple.toString()
                        }
                        showStarsButton.visibility = View.VISIBLE
                    }
                }
            }
        }
        else {
            starMenuButton.hide()
        }

        val analyzed = BookmarkCommentDecorator.convert(mBookmark.comment)

        view.findViewById<TextView>(R.id.comment).apply {
            text = analyzed.comment
            val comment = this

            // 選択テキストを画面下部で強調表示，スターを付ける際に引用文とする
            customSelectionActionModeCallback = object : ActionMode.Callback {
                private val quote = view.findViewById<TextView>(R.id.quote_text_view)

                override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?) : Boolean {
                    quote.apply {
                        val selectedText = comment.text.substring(comment.selectionStart, comment.selectionEnd)
                        text = String.format("\"%s\"", selectedText)
                        visibility = View.VISIBLE
                    }
                    return false
                }
                override fun onDestroyActionMode(p0: ActionMode?) {
                    quote.apply {
                        text = ""
                        visibility = View.INVISIBLE
                    }
                }

                override fun onCreateActionMode(p0: ActionMode?, p1: Menu?) = true
                override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?) = false
            }

            movementMethod = LinkMovementMethod.getInstance()

            /*
            val linkMovementMethod = object : MutableLinkMovementMethod() {
                override fun onSinglePressed(link: String) {
                    if (link.startsWith("http")) {
                        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                        val act = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION))
                        TappedActionLauncher.launch(context, act, link)
                    }
                    else {
                        val eid = analyzed.entryIds.firstOrNull { link.contains(it.toString()) }
                        if (eid != null) {
                            this@BookmarkDetailFragment.launch(Dispatchers.Main) {
                                val entryUrl = HatenaClient.getEntryUrlFromIdAsync(eid).await()
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entryUrl))
                                context.startActivity(intent)
                            }
                        }
                    }
                }

                override fun onLongPressed(link: String) {
                    if (link.startsWith("http")) {
                        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                        val act = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION))
                        TappedActionLauncher.launch(context, act, link)
                    }
                }
            }

            setOnTouchListener { view, event ->
                val textView = view as TextView
                return@setOnTouchListener linkMovementMethod.onTouchEvent(
                    textView,
                    SpannableString(textView.text),
                    event)
            }*/
        }

        Glide.with(view)
            .load(mBookmark.userIconUrl)
            .into(icon)

        return view
    }

    override fun onResume() {
        super.onResume()

        val tabLayout = mView.findViewById<TabLayout>(R.id.tab_layout)
        val tabPager = mView.findViewById<ViewPager>(R.id.tab_pager)

        val adapter = StarsTabAdapter(
            tabPager,
            this@BookmarkDetailFragment,
            mBookmark
        )
        tabPager.adapter = adapter

        tabLayout.run {
            setupWithViewPager(tabPager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(p0: TabLayout.Tab?) {}
                override fun onTabUnselected(p0: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    if (tab == null) return

                    when (val fragment = adapter.findFragment(tab.position)) {
                        is StarsTabFragment -> fragment.scrollToTop()
                        is MentionedBookmarksTabFragment -> fragment.scrollToTop()
                    }
                }
            })
        }

        val task = bookmarksFragment!!.getFetchStarsTask(mBookmark.user)
        if (task?.isActive == true) {
            launch(Dispatchers.Main) {
                val progressBar = mView.findViewById<ProgressBar>(R.id.detail_progress_bar).apply {
                    visibility = View.VISIBLE
                }

                try {
                    task.await()
                    if (isStateSaved) {
                        return@launch
                    }

                    if (bookmarksFragment != null) {
                        adapter.updateTabs(bookmarksFragment!!)
                    }
                }
                catch (e: Exception) {
                    Log.d("FailedToFetchStars", e.message)
                }
                finally {
                    progressBar.visibility = View.GONE
                }
            }
        }
        else {
            adapter.updateTabs(bookmarksFragment!!)
        }
    }

    private fun updateUserTags() {
        mView.findViewById<TextView>(R.id.user_name).run {
            val user = mBookmark.user
            val tags = bookmarksFragment?.taggedUsers?.firstOrNull { it.user.name == user }?.tags?.sortedBy { it.id }
            text =
                if (tags.isNullOrEmpty()) {
                    mBookmark.user
                }
                else {
                    val tagsText = tags.joinToString(",") { it.name }
                    val tagColor = resources.getColor(R.color.tagColor, null)
                    val st = "${mBookmark.user} _$tagsText"
                    val density = resources.displayMetrics.scaledDensity
                    val size = (13 * density).toInt()

                    SpannableString(st).apply {
                        val drawable = resources.getDrawable(R.drawable.ic_user_tag, null).apply {
                            setBounds(0, 0, lineHeight, lineHeight)
                            setTint(tagColor)
                        }
                        val pos = mBookmark.user.length + 1
                        setSpan(ImageSpan(drawable), pos, pos + 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(TextAppearanceSpan(null, Typeface.DEFAULT.style, size, ColorStateList.valueOf(tagColor), null),
                            mBookmark.user.length + 1, st.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
        }
    }

    private fun showStarButton(layoutId: Int, counterId: Int, dimenId: Int) =
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> showStarButtonLandscape(layoutId, counterId, dimenId)
            else -> showStarButtonPortrait(layoutId, counterId, dimenId)
        }

    private fun showStarButtonPortrait(layoutId: Int, counterId: Int, dimenId: Int) {
        val layout = mView.findViewById<View>(layoutId)
        val counter = mView.findViewById<View>(counterId)
        val pos = context!!.resources.getDimension(dimenId)

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
        val layout = mView.findViewById<View>(layoutId)
        val counter = mView.findViewById<View>(counterId)
        val pos = context!!.resources.getDimension(dimenId)

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
        val layout = mView.findViewById<View>(layoutId)
        val counter = mView.findViewById<View>(counterId)

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
        val layout = mView.findViewById<View>(layoutId)
        val counter = mView.findViewById<View>(counterId)

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

        mView.findViewById<FloatingActionButton>(R.id.show_stars_button).setImageResource(R.drawable.ic_baseline_close)
    }

    private fun closeStarMenu() {
        hideStarButton(R.id.purple_star_layout, R.id.purple_stars_count)
        hideStarButton(R.id.blue_star_layout, R.id.blue_stars_count)
        hideStarButton(R.id.red_star_layout, R.id.red_stars_count)
        hideStarButton(R.id.green_star_layout, R.id.green_stars_count)
        hideStarButton(R.id.yellow_star_layout, R.id.yellow_stars_count)

        mView.findViewById<FloatingActionButton>(R.id.show_stars_button).setImageResource(R.drawable.ic_star)
    }

    private fun postStar(color: StarColor, count: Int) {
        if (color == StarColor.Yellow || count > 0) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context!!)
            val usingDialog = prefs.getBoolean(PreferenceKey.USING_POST_STAR_DIALOG)
            if (usingDialog) {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.confirm_dialog_title_simple)
                    .setMessage(getString(R.string.msg_post_star_dialog, color.name.toUpperCase(Locale.ROOT)))
                    .setPositiveButton(R.string.dialog_ok)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setAdditionalData("color", color)
                    .setAdditionalData("count", count)
                    .show(childFragmentManager, "post_star_dialog")
            }
            else {
                postStarImpl(color, count)
            }
        }
        else {
            context?.showToast(R.string.msg_no_color_stars, color.name.toUpperCase(Locale.ROOT))
        }
    }

    private fun postStarImpl(color: StarColor, count: Int) {
        if (color == StarColor.Yellow || count > 0) {
            launch(Dispatchers.Main) {
                try {
                    val quote = mView.findViewById<TextView>(R.id.quote_text_view).text.toString()

                    HatenaClient.postStarAsync(
                        url = mBookmark.getBookmarkUrl(bookmarksFragment!!.bookmarksEntry!!),
                        color = color,
                        quote = quote
                    ).await()

                    bookmarksFragment!!.updateStar(mBookmark)

                    val tabPager = mView.findViewById<ViewPager>(R.id.tab_pager)
                    tabPager.adapter = StarsTabAdapter(
                        tabPager,
                        this@BookmarkDetailFragment,
                        mBookmark
                    ).apply {
                        updateTabs(bookmarksFragment!!)
                    }

                    context?.showToast(R.string.msg_post_star_succeeded, mBookmark.user)
                }
                catch (e: Exception) {
                    context?.showToast(R.string.msg_post_star_failed, mBookmark.user)
                    Log.d("failedToPostStar", Log.getStackTraceString(e))
                }
            }
        }
        else {
            context?.showToast(R.string.msg_no_color_stars, color.name.toUpperCase(Locale.ROOT))
        }
    }

    suspend fun updateStars() = withContext(Dispatchers.Main) {
        bookmarksFragment!!.updateStar(mBookmark)

        val tabPager = mView.findViewById<ViewPager>(R.id.tab_pager)
        tabPager.adapter = StarsTabAdapter(
            tabPager,
            this@BookmarkDetailFragment,
            mBookmark
        ).apply {
            updateTabs(bookmarksFragment!!)
        }
    }

    override fun onBackPressed(): Boolean =
        if (mIsStarMenuOpened) {
            closeStarMenu()
            true
        }
        else {
            val tabPager = mView.findViewById<ViewPager>(R.id.tab_pager)
            (tabPager.adapter as? StarsTabAdapter)?.clearTabs()
            tabPager.adapter = null
            false
        }

    override fun onClickPositiveButton(dialog: AlertDialogFragment) {
        when (dialog.tag) {
            "user_tag_dialog" ->
                BookmarkDialog.Listener.onCompleteSelectTags(activity as BookmarksActivity, this, dialog)

            "post_star_dialog" -> {
                val color = dialog.getAdditionalData<StarColor>("color")!!
                val count = dialog.getAdditionalData<Int>("count")!!
                postStarImpl(color, count)
            }
        }
    }

    override fun onClickNeutralButton(dialog: AlertDialogFragment) {
        when (dialog.tag) {
            "user_tag_dialog" ->
                BookmarkDialog.Listener.onCreateNewTag(this, dialog)
        }
    }

    override fun onRemoveBookmark(bookmark: Bookmark) {
        onBackPressed()
    }

    override fun onTagUser(bookmark: Bookmark) {
        updateUserTags()
    }

    override fun onSelectUrl(url: String) {
        context?.showCustomTabsIntent(url)
    }

    override suspend fun onCompletedEditTagName(tagName: String, dialog: UserTagDialogFragment): Boolean =
        BookmarkDialog.Listener.onCompleteCreateTag(tagName, activity as BookmarksActivity, dialog)
}
