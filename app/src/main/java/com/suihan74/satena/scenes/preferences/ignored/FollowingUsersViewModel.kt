package com.suihan74.satena.scenes.preferences.ignored

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    /** (フィルタ前の)非表示ユーザーリスト */
    private val allUsers = MutableLiveData<List<String>>()

    /** (フィルタ済みの)非表示ユーザーリスト */
    val users by lazy {
        MutableLiveData<List<String>>()
    }

    /** 検索テキスト */
    val filterText = MutableLiveData("")

    /** 検索テキスト表示状態 */
    val isFilterTextVisible by lazy {
        MutableLiveData(false)
    }

    // ------ //

    init {
        allUsers.observeForever {
            createUsersList()
        }
        filterText.observeForever {
            createUsersList()
        }

        viewModelScope.launch {
            loadList()
        }
    }

    // ------ //

    /** 画面表示用の非表示ユーザーリストを生成する */
    private fun createUsersList() = viewModelScope.launch(Dispatchers.IO) {
        val rawList = allUsers.value ?: emptyList()
        val filterText = filterText.value

        val list =
            if (filterText.isNullOrBlank()) rawList
            else rawList.filter { it.contains(filterText) }

        users.postValue(list)
    }

    /** リストを再読み込みする */
    suspend fun loadList() {
        withContext(Dispatchers.Default) {
            runCatching {
                allUsers.postValue(repository.getFollowings())
            }
        }
    }

    // ------ //

    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** 項目に対するメニューを開く */
    fun openMenuDialog(
        activity: Activity,
        user: String,
        fragmentManager: FragmentManager
    ) {
        val items = listOf(
            R.string.pref_ignored_users_show_entries to { showEntries(activity, user) },
            R.string.pref_ignored_users_unignore to { unIgnoreUser(activity, user) }
        )

        val dialog = AlertDialogFragment.Builder()
            .setTitle("id:$user")
            .setNegativeButton(R.string.dialog_cancel)
            .setItems(items.map { it.first }) { _, which ->
                items[which].second.invoke()
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

    private fun unIgnoreUser(
        context: Context,
        user: String
    ) = viewModelScope.launch(Dispatchers.Main) {
        val result = runCatching {
            repository.unIgnoreUser(user)
        }

        if (result.isSuccess) {
            context.showToast(
                R.string.msg_unignore_user_succeeded,
                user
            )
        }
        else {
            context.showToast(
                R.string.msg_unignore_user_failed,
                user
            )
        }
    }
}
