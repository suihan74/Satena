package com.suihan74.satena.scenes.preferences.ignored

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.suihan74.hatenaLib.Follower
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FollowingUsersViewModel(
    private val repository: UserRelationRepository
) : ViewModel() {

    /** リスト表示対象がフォローorフォロワーどちらか */
    enum class Mode {
        FOLLOWINGS,
        FOLLOWERS
    }

    /** (フィルタ前の)フォローリスト */
    private val allFollowings = MutableLiveData<List<String>>()

    /** (フィルタ前の)フォロワーリスト */
    private val allFollowers = MutableLiveData<List<Follower>>()

    /** (フィルタ済みの)非表示ユーザーリスト */
    val users by lazy {
        MutableLiveData<List<String>>()
    }

    /** リスト表示対象がフォローorフォロワーどちらか */
    var mode = Mode.FOLLOWINGS
        private set

    /** 表示対象リスト更新時に表示用リストに内容反映するオブザーバ */
    private val listObserver = Observer<List<*>> {
        createUsersList()
    }

    /** 検索テキスト */
    val filterText = MutableLiveData("")

    /** 検索テキスト表示状態 */
    val isFilterTextVisible by lazy {
        MutableLiveData(false)
    }

    // ------ //

    init {
        filterText.observeForever {
            createUsersList()
        }
        viewModelScope.launch {
            loadList(refreshAll = true)
            createUsersList()
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.Main) {
            runCatching {
                allFollowings.value = emptyList()
                allFollowers.value = emptyList()
            }
        }
    }

    // ------ //

    /** リスト表示対象を切替え */
    fun setMode(mode: Mode, lifecycleOwner: LifecycleOwner) {
        this.mode = mode
        allFollowings.removeObserver(listObserver)
        allFollowers.removeObserver(listObserver)
        when (mode) {
            Mode.FOLLOWINGS -> allFollowings.observe(lifecycleOwner, listObserver)
            Mode.FOLLOWERS -> allFollowers.observe(lifecycleOwner, listObserver)
        }
        if (this.mode == mode) return
        createUsersList()
    }

    /** 画面表示用の非表示ユーザーリストを生成する */
    private fun createUsersList() = viewModelScope.launch(Dispatchers.Default) {
        val rawList = when(mode) {
            Mode.FOLLOWINGS -> allFollowings.value
            Mode.FOLLOWERS -> allFollowers.value?.map { it.name }
        } ?: emptyList()

        val filterText = filterText.value

        val list =
            if (filterText.isNullOrBlank()) rawList
            else rawList.filter { it.contains(filterText) }

        users.postValue(list)
    }

    /** フォローリストを再読み込みする */
    private suspend fun loadFollowings() {
        withContext(Dispatchers.Default) {
            runCatching {
                allFollowings.postValue(repository.getFollowings())
            }
        }
    }

    /** フォロワーリストを再読み込みする */
    private suspend fun loadFollowers() {
        withContext(Dispatchers.Default) {
            runCatching {
                allFollowers.postValue(repository.getFollowers())
            }
        }
    }

    /** リストを再読み込みする */
    suspend fun loadList(refreshAll : Boolean = false) {
        if (refreshAll) {
            loadFollowings()
            loadFollowers()
        }
        else when(mode) {
            Mode.FOLLOWINGS -> loadFollowings()
            Mode.FOLLOWERS -> loadFollowers()
        }
    }

    // ------ //

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** 項目に対するメニューを開く */
    @OptIn(ExperimentalStdlibApi::class)
    fun openMenuDialog(
        user: String,
        fragmentManager: FragmentManager
    ) {
        val items = buildList<Pair<Int,(DialogFragment)->Unit>> {
            add(R.string.pref_ignored_users_show_entries to { f -> showEntries(f.requireActivity(), user) })

            if (allFollowings.value?.contains(user) == true) {
                add(R.string.pref_followings_menu_unfollow to { f -> unFollowUser(f.requireActivity(), user) })
            }
            else {
                add(R.string.pref_followings_menu_follow to { f -> followUser(f.requireActivity(), user) })
            }
        }

        val dialog = AlertDialogFragment.Builder()
            .setTitle("id:$user")
            .setNegativeButton(R.string.dialog_cancel)
            .setItems(items.map { it.first }) { f, which ->
                items[which].second.invoke(f)
            }
            .create()

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_MENU)
    }

    private fun showEntries(activity: Activity, user: String) {
        val intent = Intent(activity, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_USER, user)
        }
        activity.startActivity(intent)
    }

    private fun followUser(
        activity: FragmentActivity,
        user: String
    ) = activity.lifecycleScope.launch(Dispatchers.Default) {
        val result = runCatching {
            repository.followUser(user)
        }

        if (result.isSuccess) {
            allFollowings.value = allFollowings.value?.plus(user)
            activity.showToast(R.string.msg_follow_user_succeeded, user)
        }
        else {
            activity.showToast(R.string.msg_follow_user_failed, user)
        }
    }

    private fun unFollowUser(
        activity: FragmentActivity,
        user: String
    ) = activity.lifecycleScope.launch(Dispatchers.Default) {
        val result = runCatching {
            repository.unFollowUser(user)
        }

        if (result.isSuccess) {
            allFollowings.value = allFollowings.value?.filterNot { it == user }
            activity.showToast(R.string.msg_unfollow_user_succeeded, user)
        }
        else {
            activity.showToast(R.string.msg_unfollow_user_failed, user)
        }
    }
}
