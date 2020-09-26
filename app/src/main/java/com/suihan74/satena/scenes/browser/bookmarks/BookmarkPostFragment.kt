package com.suihan74.satena.scenes.browser.bookmarks

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityBookmarkPost2Binding
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.satena.scenes.post2.BookmarkPostViewModel
import com.suihan74.satena.scenes.post2.TagsListAdapter
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.getThemeColor
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.provideViewModel

class BookmarkPostFragment : Fragment() {
    companion object {
        fun createInstance() = BookmarkPostFragment()

        const val VIEW_MODEL_BOOKMARK_POST = "VIEW_MODEL_BOOKMARK_POST"
    }

    private val browserActivity
        get() = requireActivity() as BrowserActivity

    private val activityViewModel : BrowserViewModel
        get() = browserActivity.viewModel

    private val viewModel : BookmarkPostViewModel by lazy {
        // オーナーをアクティビティにすることで、タブ切り替えでリロードされないようにする
        provideViewModel(requireActivity(), VIEW_MODEL_BOOKMARK_POST) {
            val context = requireContext()
            BookmarkPostViewModel(
                HatenaClient,
                AccountLoader(
                    context = context,
                    client = HatenaClient,
                    mastodonClientHolder = MastodonClientHolder
                ),
                SafeSharedPreferences.create(context)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<ActivityBookmarkPost2Binding>(
            inflater,
            R.layout.activity_bookmark_post_2,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        binding.bookmarkPostLayout.setBackgroundColor(
            browserActivity.getThemeColor(R.attr.tabBackground)
        )

        binding.postButton.setOnClickListener {
            postBookmark()
        }

        binding.comment.let { comment ->
            // 注: XML側に書くと折り返さない一行での表示になる
            comment.setHorizontallyScrolling(false)
            // 注: XML側に書くと表示部分の縦幅が一行分だけになる
            comment.maxLines = Int.MAX_VALUE

            comment.setOnFocusChangeListener { view, b ->
                if (!b) {
                    activity?.hideSoftInputMethod(binding.bookmarkPostLayout)
                }
            }

            // DONEボタンとかSEARCHボタンとかが押された時の処理
            comment.setOnEditorActionListener { _, action, _ ->
                when (action) {
                    EditorInfo.IME_ACTION_DONE -> {
                        postBookmark()
                        true
                    }
                    else -> false
                }
            }

            if (savedInstanceState == null) {
                comment.addTextChangedListener(object : TextWatcher {
                    var initialized = false
                    override fun afterTextChanged(s: Editable?) {
                        if (!initialized) {
                            comment.setSelection(s?.length ?: 0)
                            initialized = true
                        }
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
        }

        // WebViewがページ遷移したらエントリ情報をリロードする
        activityViewModel.bookmarksEntry.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            val userSignedIn = activityViewModel.browserRepo.userSignedIn
            val userComment =
                if (userSignedIn == null) null
                else it.bookmarks.firstOrNull { b -> b.user == userSignedIn }

            if (viewModel.comment.value.isNullOrBlank()) {
                viewModel.comment.value = userComment?.comment ?: ""
            }

            viewModel.init(it.url, viewModel.comment.value)
        }

        // タグリストを初期化
        setupTagsList(binding)

        return binding.root
    }

    /** ブクマを投稿する */
    private fun postBookmark() {
        browserActivity.hideSoftInputMethod()
        viewModel.postBookmark(
            childFragmentManager,
            onSuccess = {
                activity?.showToast(R.string.msg_post_bookmark_succeeded)
            },
            onError = { e ->
                activity?.showToast(R.string.msg_post_bookmark_failed)
                Log.e("PostBookmark", Log.getStackTraceString(e))
            }
        )
    }

    /** タグリストを初期化 */
    private fun setupTagsList(binding: ActivityBookmarkPost2Binding) {
        val comment = binding.comment
        val tagsList = binding.tagsList

        tagsList.layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.HORIZONTAL
        }
        tagsList.adapter = TagsListAdapter().also { adapter ->
            adapter.setOnItemClickedListener { tag ->
                var watcher: TextWatcher? = null
                try {
                    watcher = object : TextWatcher {
                        private var before: Int = 0
                        private var countDiff: Int = 0
                        private var tagsEnd: Int = 0

                        override fun afterTextChanged(s: Editable?) {
                            val after = comment.selectionStart
                            if (after == 0) {
                                val selecting =
                                    if (before < tagsEnd) viewModel.getTagsEnd(s)
                                    else before + countDiff
                                comment.setSelection(selecting)
                            }
                            comment.removeTextChangedListener(watcher)
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                            this.tagsEnd = viewModel.getTagsEnd(s)
                            this.before = comment.selectionStart
                            this.countDiff = after - count
                        }
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    }

                    comment.addTextChangedListener(watcher)
                    viewModel.toggleTag(tag)

                }
                catch (e: BookmarkPostViewModel.TooManyTagsException) {
                    activity?.showToast(R.string.msg_post_too_many_tags)
                    comment.removeTextChangedListener(watcher)
                }
            }

            // タッチイベントを他に伝播させない
            // このフラグメントが属するタブやドロワのタッチ処理を防止して
            // タグリストのスクロールだけを行うようにする
            adapter.setOnItemTouchListener {
                tagsList.parent.requestDisallowInterceptTouchEvent(true)
                false
            }

            // 使ったことがあるタグを入力するボタンを表示する
            viewModel.tags.observe(viewLifecycleOwner) {
                adapter.setTags(
                    it.map { t -> t.text }
                )
            }
        }
    }
}
