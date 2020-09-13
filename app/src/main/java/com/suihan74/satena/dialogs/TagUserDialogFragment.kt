package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.fragment_dialog_tagged_user.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TagUserDialogFragment : DialogFragment() {
    companion object {
        fun createInstance() = TagUserDialogFragment()
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[DialogViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_tagged_user, null)

        content.user_name.apply {
            setText(viewModel.userName)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val userName = text.toString()
                    viewModel.updateUserExistence(userName)
                }
            })
        }

        // 入力したIDに対応するユーザーアイコンを表示する
        viewModel.userIconUrl.observe(requireActivity(), Observer {
            val context = requireContext()
            if (it == null) {
                Glide.with(context)
                    .clear(content.user_icon)
            }
            else {
                Glide.with(context)
                    .load(Uri.parse(it))
                    .into(content.user_icon)
            }
        })

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.pref_user_tags_add_user_dialog_title)
            .setView(content)
            .setPositiveButton(R.string.dialog_register, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
            .apply {
                // IME表示を維持するための設定
                window?.run {
                    clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
                requireActivity().showSoftInputMethod(
                    content.user_name,
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                )

                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val context = requireContext()
                    viewModel.invokeOnComplete(this@TagUserDialogFragment) { e -> when(e) {
                        is EmptyUserNameException -> context.showToast(R.string.msg_add_user_dialog_empty_username)
                        is UserNotFoundException -> context.showToast(R.string.msg_add_user_dialog_user_not_found)
                    } }
                }
            }
    }

    suspend fun setOnCompleteListener(listener: SuspendSwitcher<String>?) = whenStarted {
        viewModel.onComplete = listener
    }

    // ------ //

    class EmptyUserNameException : RuntimeException()
    class UserNotFoundException : RuntimeException()

    // ------ //

    class DialogViewModel : ViewModel() {
        private val client = OkHttpClient()
            .newBuilder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .build()

        var userName: String = ""

        var isUserExisted: Boolean = false
            get() = lock(this) { field }
            private set(value) {
                lock(this) {
                    field = value
                }
            }

        val userIconUrl by lazy {
            MutableLiveData<String?>(null)
        }

        /**  */
        var onComplete: SuspendSwitcher<String>? = null

        fun invokeOnComplete(
            dialog: DialogFragment,
            onError: OnError?
        ) = viewModelScope.launch(Dispatchers.Main) {
            if (userName.isBlank()) {
                onError?.invoke(EmptyUserNameException())
                return@launch
            }

            // ユーザーが存在しない
            if (!isUserExisted) {
                onError?.invoke(UserNotFoundException())
                return@launch
            }

            if (false != onComplete?.invoke(userName)) {
                dialog.dismiss()
            }
        }

        /**
         * 入力されたユーザーIDの存在確認
         */
        fun updateUserExistence(userName: String) = viewModelScope.launch(Dispatchers.Default) {
            val url = "https://b.hatena.ne.jp/$userName/"
            this@DialogViewModel.userName = userName

            try {
                val request = Request.Builder()
                    .url(url)
                    .build()
                val call = client.newCall(request)
                call.execute().use { response ->
                    isUserExisted = 200 == response.code
                }
            }
            catch (e: Throwable) {
                isUserExisted = false
            }

            userIconUrl.postValue(
                if (isUserExisted) HatenaClient.getUserIconUrl(userName)
                else null
            )
        }
    }
}
