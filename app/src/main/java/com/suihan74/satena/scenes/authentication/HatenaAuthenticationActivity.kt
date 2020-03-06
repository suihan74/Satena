package com.suihan74.satena.scenes.authentication

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import androidx.core.content.ContextCompat
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.activity_hatena_authentication.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HatenaAuthenticationActivity : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hatena_authentication)

        // ログイン
        auth_button.setOnClickListener {
            val name = user_name.text.toString()
            val password = password.text.toString()

            if (name.isBlank() || password.length < 5) {
                showToast("ユーザー名とパスワードを入力してください")
            }
            else {
                launch {
                    signIn(name, password)
                }
            }
        }

        // 新規登録
        sign_up_text_view.apply {
            movementMethod = LinkMovementMethod.getInstance()
            setLinkTextColor(ContextCompat.getColor(this@HatenaAuthenticationActivity, R.color.colorPrimary))
        }
    }

    private suspend fun signIn(name: String, password: String) = withContext(Dispatchers.Main) {
        try {
            val account = HatenaClient.signInAsync(name, password).await()

            showToast("id:${account.name} でログインしました")

            AccountLoader(
                applicationContext,
                HatenaClient,
                MastodonClientHolder
            ).saveHatenaAccount(name, password)
            SatenaApplication.instance.startNotificationService()

            // 前の画面に戻る
            finish()
        }
        catch (e: Exception) {
            Log.d("FailedToSignIn", e.message)
            showToast("ログイン失敗")
        }
    }
}
