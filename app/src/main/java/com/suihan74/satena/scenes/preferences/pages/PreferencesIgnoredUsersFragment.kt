package com.suihan74.satena.scenes.preferences.pages

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.ignored.IgnoredUsersAdapter
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.fragment_preferences_ignored_users.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PreferencesIgnoredUsersFragment : PreferencesFragmentBase(), BackPressable, AlertDialogFragment.Listener {
    private var mIgnoredUsersAdapter: IgnoredUsersAdapter? = null

    private var mDialogMenuItems : List<Pair<String, (user: String)->Unit>>? = null

    companion object {
        fun createInstance() =
            PreferencesIgnoredUsersFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_ignored_users, container, false)

        // ID検索テキストボックス
        val searchEditText = root.search_text.apply {
            visibility = (!mIgnoredUsersAdapter?.searchText.isNullOrEmpty()).toVisibility(View.INVISIBLE)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    mIgnoredUsersAdapter!!.searchText = text.toString()
                }
            })
        }

        // ID検索ボタン
        root.search_button.setOnClickListener {
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

        // 各アイテムに対するメニュー項目
        mDialogMenuItems = arrayListOf(
            getString(R.string.pref_ignored_users_show_entries) to { user ->
                val intent = Intent(context, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_DISPLAY_USER, user)
                }
                context?.startActivity(intent)
            },

            getString(R.string.pref_ignored_users_unignore) to { user ->
                launch(Dispatchers.Main) {
                    try {
                        HatenaClient.unignoreUserAsync(user).await()
                        mIgnoredUsersAdapter!!.removeUser(user)
                    }
                    catch (e: Exception) {
                        Log.d("unIgnoreFailure", Log.getStackTraceString(e))
                    }
                }
            }
        )

        // ユーザーリスト
        mIgnoredUsersAdapter = object : IgnoredUsersAdapter(emptyList()) {
            override fun onItemClicked(user: String) {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle("id:$user")
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(mDialogMenuItems!!.map { it.first })
                    .setAdditionalData("user", user)
                    .show(childFragmentManager, "menu_dialog")
            }
        }

        val activity = requireActivity()
        val context = requireContext()

        root.ignored_users_list.apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context,
                R.drawable.recycler_view_item_divider
            )!!)
            adapter = mIgnoredUsersAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(dividerItemDecoration)
            setHasFixedSize(true)
        }

        // スワイプ更新機能の設定
        root.swipe_layout.apply {
            val swipeLayout = this
            setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(activity.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                launch(Dispatchers.Main) {
                    refresh()
                    swipeLayout.isRefreshing = false
                }
            }
        }

        // 非表示ユーザー取得
        root.detail_progress_bar.visibility = View.VISIBLE

        launch { refresh() }

        return root
    }

    private suspend fun refresh() = withContext(Dispatchers.Main) {
        try {
            val ignoredUsers = HatenaClient.getIgnoredUsersAsync(forciblyUpdate = true).await()
            mIgnoredUsersAdapter!!.setUsers(ignoredUsers)
        }
        catch (e: Exception) {
            activity?.showToast(R.string.msg_pref_ignored_users_update_failed)
            Log.d("FailedToUpdateIgnores", Log.getStackTraceString(e))
        }
        finally {
            view?.detail_progress_bar?.visibility = View.INVISIBLE
        }
    }

    override fun onBackPressed(): Boolean {
        val searchEditText = view?.search_text ?: return false
        return when(searchEditText.visibility) {
            View.VISIBLE -> {
                searchEditText.visibility = View.INVISIBLE
                searchEditText.text.clear()
                true
            }

            else -> false
        }
    }

    override fun onSelectItem(dialog: AlertDialogFragment, which: Int) {
        val items = mDialogMenuItems ?: return
        val user = dialog.getAdditionalData<String>("user") ?: return
        items[which].second.invoke(user)
    }
}
