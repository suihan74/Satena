package com.suihan74.satena.scenes.browser.bookmarks

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityBookmarkPost2Binding
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.satena.scenes.post2.BookmarkPostViewModel
import com.suihan74.utilities.*

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
            viewModel.init(it.url, viewModel.comment.value)
        }

        // 各トグルボタンをONにしたときにメッセージを表示する
        viewModel.postMastodon.observe(viewLifecycleOwner){
            if (it == null) return@observe
            if (it) context?.showToast(R.string.hint_mastodon_toggle)
        }

        viewModel.postTwitter.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            if (it) context?.showToast(R.string.hint_twitter_toggle)
        }

        viewModel.postFacebook.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            if (it) context?.showToast(R.string.hint_facebook_toggle)
        }

        viewModel.isPrivate.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            if (it) context?.showToast(R.string.hint_private_toggle)
        }

        return binding.root
    }

    private fun postBookmark() {
        browserActivity.hideSoftInputMethod()
        viewModel.postBookmark(
            childFragmentManager,
        )
    }
}
