package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.scenes.preferences.userTag.TaggedUsersListFragment
import com.suihan74.utilities.lock
import com.suihan74.utilities.showToast
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class TaggedUserDialogFragment : DialogFragment(), CoroutineScope {
    private val mJob: Job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = mJob

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }

    private lateinit var mPositiveAction : ((TaggedUsersListFragment, String)->Boolean)
    private var mIsUserExisted = false

    companion object {
        fun createInstance(positiveAction: ((TaggedUsersListFragment, String)->Boolean)) = TaggedUserDialogFragment().apply {
            mPositiveAction = positiveAction
        }

        private var savedPositiveAction : ((TaggedUsersListFragment, String)->Boolean)? = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedPositiveAction = mPositiveAction
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_tagged_user, null)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        savedInstanceState?.run {
            if (savedPositiveAction != null) {
                mPositiveAction = savedPositiveAction!!
                savedPositiveAction = null
            }
        }

        val client = OkHttpClient()
            .newBuilder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .build()

        val icon = content.findViewById<ImageView>(R.id.user_icon)

        val name = content.findViewById<EditText>(R.id.user_name).apply {
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val text = text.toString()
                    val url = "https://b.hatena.ne.jp/$text"

                    launch {
                        try {
                            val request = Request.Builder()
                                .url(url)
                                .build()
                            val call = client.newCall(request)
                            call.execute().use { response ->
                                lock(this@TaggedUserDialogFragment) {
                                    this@TaggedUserDialogFragment.mIsUserExisted =
                                        200 == response.code
                                }
                            }
                        }
                        catch (e: Exception) {
                            this@TaggedUserDialogFragment.mIsUserExisted = false
                        }

                        if (this@TaggedUserDialogFragment.mIsUserExisted) {
                            val iconUrl = HatenaClient.getUserIconUrl(text)
                            withContext(Dispatchers.Main) {
                                Glide.with(content)
                                    .load(iconUrl)
                                    .into(icon)
                            }
                        }
                        else {
                            withContext(Dispatchers.Main) {
                                Glide.with(content)
                                    .clear(icon)
                            }
                        }
                    }
                }
            })
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setView(content)
            .setMessage("ユーザーを追加")
            .setPositiveButton("登録", null)
            .setNegativeButton("Cancel", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val nameEditor = content.findViewById<EditText>(R.id.user_name)

                    if (nameEditor.text.isNullOrBlank()) {
                        context.showToast("はてなIDを入力してください")
                        return@setOnClickListener
                    }

                    // ユーザーが存在しない
                    lock (this@TaggedUserDialogFragment) {
                        if (!mIsUserExisted) {
                            context.showToast("ユーザーが見つかりません")
                            return@setOnClickListener
                        }
                    }

                    val fragment = fragmentManager?.fragments?.lastOrNull { it is TaggedUsersListFragment } as? TaggedUsersListFragment
                    if (fragment != null && mPositiveAction.invoke(fragment, nameEditor.text.toString())) {
                        this.dismiss()
                    }
                }
            }
    }
}
