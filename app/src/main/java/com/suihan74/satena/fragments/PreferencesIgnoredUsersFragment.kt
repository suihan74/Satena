package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.adapters.IgnoredUsersAdapter
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PreferencesIgnoredUsersFragment : CoroutineScopeFragment(), BackPressable {
    private lateinit var mRoot : View
    private var mIgnoredUsersAdapter: IgnoredUsersAdapter? = null

    companion object {
        fun createInstance() = PreferencesIgnoredUsersFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_ignored_users, container, false)
        mRoot = root

        // ID検索テキストボックス
        val searchEditText = root.findViewById<EditText>(R.id.search_text).apply {
            visibility = if (mIgnoredUsersAdapter?.searchText.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    mIgnoredUsersAdapter!!.searchText = text.toString()
                }
            })
        }

        // ID検索ボタン
        root.findViewById<FloatingActionButton>(R.id.search_button).setOnClickListener {
            when(searchEditText.visibility) {
                View.VISIBLE -> {
                    searchEditText.visibility = View.INVISIBLE
                    searchEditText.text.clear()
                }

                else -> {
                    searchEditText.visibility = View.VISIBLE
                }
            }
        }

        // ユーザーリスト
        mIgnoredUsersAdapter = object : IgnoredUsersAdapter(emptyList()) {
            val items = arrayListOf<Pair<String, (String)->Unit>>(
                "ブクマ済みのエントリ一覧を見る" to { user ->
                    val fragment = UserEntriesFragment.createInstance(user)
                    (activity as FragmentContainerActivity).showFragment(fragment, null)
                },
                "非表示を解除する" to { user ->
                    launch(Dispatchers.Main) {
                        try {
                            HatenaClient.unignoreUserAsync(user).await()
                            mIgnoredUsersAdapter!!.removeUser(user)
                        }
                        catch (e: Exception) {
                            Log.d("FailedToUnignoreUser", Log.getStackTraceString(e))
                        }
                    }
                }
            )

            override fun onItemClicked(user: String) {
                AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                    .setTitle("id:$user")
                    .setNegativeButton("Cancel", null)
                    .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                        items[which].second(user)
                    }
                    .show()
            }
        }

        root.findViewById<RecyclerView>(R.id.ignored_users_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                R.drawable.recycler_view_item_divider
            )!!)
            adapter = mIgnoredUsersAdapter
            layoutManager = LinearLayoutManager(context!!)
            addItemDecoration(dividerItemDecoration)
            setHasFixedSize(true)
        }

        // スワイプ更新機能の設定
        root.findViewById<SwipeRefreshLayout>(R.id.swipe_layout).apply {
            val swipeLayout = this
            setProgressBackgroundColorSchemeColor(activity!!.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(activity!!.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                launch(Dispatchers.Main) {
                    refresh()
                    swipeLayout.isRefreshing = false
                }
            }
        }

        // 非表示ユーザー取得
        root.findViewById<ProgressBar>(R.id.detail_progress_bar).visibility = View.VISIBLE

        launch { refresh() }

        return root
    }

    private suspend fun refresh() = withContext(Dispatchers.Main) {
        try {
            val ignoredUsers = HatenaClient.getIgnoredUsersAsync(forciblyUpdate = true).await()
            mIgnoredUsersAdapter!!.setUsers(ignoredUsers)
        }
        catch (e: Exception) {
            activity?.showToast("非表示ユーザーリスト更新失敗")
            Log.d("FailedToUpdateIgnores", Log.getStackTraceString(e))
        }
        finally {
            mRoot.findViewById<ProgressBar>(R.id.detail_progress_bar).visibility = View.INVISIBLE
        }
    }

    override fun onBackPressed(): Boolean {
        val searchEditText = mRoot.findViewById<EditText>(R.id.search_text)
        return when(searchEditText.visibility) {
            View.VISIBLE -> {
                searchEditText.visibility = View.INVISIBLE
                searchEditText.text.clear()
                true
            }

            else -> {
                false
            }
        }
    }
}
