package com.suihan74.satena.scenes.post

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBookmarkPostBinding
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.utilities.extensions.hideSoftInputMethod

/**
 * 投稿ダイアログ本体
 *
 * 配置先のActivityはBookmarkPostViewModelOwnerである必要がある
 */
class BookmarkPostFragment : Fragment() {
    companion object {
        fun createInstance() = BookmarkPostFragment()
    }

    // ------ //

    private val browserActivity : BrowserActivity?
        get() = requireActivity() as? BrowserActivity

    private val browserViewModel : BrowserViewModel?
        get() = browserActivity?.viewModel

    private val bookmarkPostActivity : BookmarkPostActivity?
        get() = requireActivity() as? BookmarkPostActivity

    // ------ //

    private val viewModel : BookmarkPostViewModel
        get() = (requireActivity() as BookmarkPostViewModelOwner).bookmarkPostViewModel

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        browserViewModel?.let { vm ->
            initializeForBrowser(vm.bookmarksRepo, savedInstanceState != null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentBookmarkPostBinding>(
            inflater,
            R.layout.fragment_bookmark_post,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }

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

        // タグリストを初期化
        setupTagsList(binding)

        return binding.root
    }

    /** アプリ内ブラウザで使用されている場合の接続処理 */
    private fun initializeForBrowser(bookmarksRepo: BookmarksRepository, restored: Boolean) {
        // 画面復元時の初回呼び出しで実行されないようにする
        var skipOnRestored = restored

        // WebViewがページ遷移したらエントリ情報をリロードする
        bookmarksRepo.entry.observe(requireActivity()) {
            lifecycleScope.launchWhenResumed {
                if (it == null) return@launchWhenResumed
                if (skipOnRestored) {
                    skipOnRestored = false
                    return@launchWhenResumed
                }

                val userSignedIn = bookmarksRepo.userSignedIn
                val userComment =
                    if (userSignedIn == null) null
                    else it.bookmarkedData?.commentRaw

                viewModel.comment.value = userComment.orEmpty()
                viewModel.initialize(requireContext(), entry = it)
            }
        }
    }

    /** ブクマを投稿する */
    private fun postBookmark() {
        activity?.hideSoftInputMethod()
        viewModel.postBookmark(requireContext(), childFragmentManager)
    }

    /** タグリストを初期化 */
    private fun setupTagsList(binding: FragmentBookmarkPostBinding) {
        val comment = binding.comment
        val tagsList = binding.tagsList

        val adapter = viewModel.createTagsListAdapter(requireContext(), viewLifecycleOwner, comment)
        // タッチイベントを他に伝播させない
        // このフラグメントが属するタブやドロワのタッチ処理を防止して
        // タグリストのスクロールだけを行うようにする
        adapter.setOnItemTouchListener {
            tagsList.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        tagsList.layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.HORIZONTAL
        }

        tagsList.adapter = adapter
    }
}
