package com.suihan74.satena.scenes.entries.pages

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Spinner
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.SingleTabEntriesFragmentBase
import com.suihan74.satena.scenes.entries.initialize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserEntriesFragment : SingleTabEntriesFragmentBase() {

    private var mUser : String? = null
    private val user: String
        get() = mUser ?: arguments!!.getString(ARG_KEY_USER)!!

    private var mTag : String? = null
    private var mTags : ArrayList<String>? = null

    override val title: String
        get() = "${user}のブックマーク"

    override val subtitle: String?
        get() = mTag

    override val currentCategory = Category.User

    companion object {
        fun createInstance(user: String) = UserEntriesFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_KEY_USER, user)
            }
        }

        private const val ARG_KEY_USER = "user"
        private const val BUNDLE_TAG = "tag"
        private const val BUNDLE_TAGS = "tags"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putString(BUNDLE_TAG, mTag)
            putStringArrayList(BUNDLE_TAGS, mTags)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments!!.let {
            mUser = it.getString(ARG_KEY_USER)!!
        }

        savedInstanceState?.let {
            mTag = it.getString(BUNDLE_TAG)
            mTags = it.getStringArrayList(BUNDLE_TAGS)
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        refreshEntries()
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        launch(Dispatchers.Main) {
            try {
                val activity = activity as ActivityBase

                var updatable = true

                if (mTags == null) {
                    val tags = HatenaClient.getUserTagsAsync(user).await()
                    mTags = ArrayList(tags.map { it.text })
                }
                else {
                    updatable = false
                }

                inflater.inflate(R.menu.spinner_issues, menu)
                (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
                    val items = mTags as List<String>

                    initialize(activity, items, R.drawable.spinner_allow_tags, "${user}が使用しているタグ") { position ->
                        if (updatable) {
                            val prevTag = mTag
                            mTag = if (position == null) null else items[position]

                            if (prevTag != mTag) {
                                activity.updateToolbar()
                                refreshEntries()
                            }
                        }
                        updatable = true
                    }
                }
            }
            catch (e: Exception) {
                Log.e("UserEntriesFragment", "failed to create the options menu")
            }
        }
    }

    override fun refreshEntries() =
        super.refreshEntries("${user}のブックマークエントリの取得失敗") { offset ->
            HatenaClient.getUserEntriesAsync(user, of = offset, tag = mTag)
        }
}
