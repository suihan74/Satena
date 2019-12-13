package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.utilities.lock
import com.suihan74.utilities.showToast
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class TagUserDialogFragment : AlertDialogFragment(), CoroutineScope {
    private val mJob: Job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = mJob

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }

    interface Listener: AlertDialogFragment.Listener {
        suspend fun onCompleteTaggedUser(userName: String, dialog: TagUserDialogFragment) : Boolean
    }

    /** 入力されたユーザーが存在するか */
    private var mIsUserExisted = false

    /** ユーザーの存在確認時に通信する */
    private lateinit var client: OkHttpClient

    private lateinit var content: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        content = inflater.inflate(R.layout.fragment_dialog_tagged_user, null)

        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        val listener = parentFragment as? Listener ?: activity as? Listener

        client = OkHttpClient()
            .newBuilder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .build()

        content.findViewById<EditText>(R.id.user_name).apply {
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val userName = text.toString()
                    updateUserExistence(userName)
                }
            })
        }

        val builder = createBuilder(arguments!!, savedInstanceState).apply {
            setView(content)
            setMessage("ユーザーを追加")
            setPositiveButton(R.string.dialog_register, null)
            setNegativeButton(R.string.dialog_cancel, null)
        }

        return builder.show().apply {
            getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val nameEditor = content.findViewById<EditText>(R.id.user_name)
                val userName = nameEditor.text?.toString() ?: ""

                if (userName.isBlank()) {
                    context.showToast("はてなIDを入力してください")
                    return@setOnClickListener
                }

                // ユーザーが存在しない
                lock (this@TagUserDialogFragment) {
                    if (!mIsUserExisted) {
                        context.showToast("ユーザーが見つかりません")
                        return@setOnClickListener
                    }
                }

                launch(Dispatchers.Main) {
                    if (listener?.onCompleteTaggedUser(
                            userName,
                            this@TagUserDialogFragment
                        ) != false
                    ) {
                        dismiss()
                    }
                }
            }
        }
    }

    /**
     * 入力されたユーザーIDの存在確認
     */
    private fun updateUserExistence(userName: String) = launch(Dispatchers.IO) {
        val url = "https://b.hatena.ne.jp/$userName/"
        val icon = content.findViewById<ImageView>(R.id.user_icon)

        try {
            val request = Request.Builder()
                .url(url)
                .build()
            val call = client.newCall(request)
            call.execute().use { response ->
                lock(this@TagUserDialogFragment) {
                    mIsUserExisted = 200 == response.code
                }
            }
        }
        catch (e: Exception) {
            mIsUserExisted = false
        }

        withContext(Dispatchers.Main) {
            if (mIsUserExisted) {
                val iconUrl = HatenaClient.getUserIconUrl(userName)
                Glide.with(content)
                    .load(iconUrl)
                    .into(icon)
            }
            else {
                Glide.with(content)
                    .clear(icon)
            }
        }
    }

    class Builder(themeResId: Int) : AlertDialogFragment.Builder(themeResId) {
        override fun create(): AlertDialogFragment =
            TagUserDialogFragment().apply {
                this.arguments = this@Builder.arguments
            }
    }
}
