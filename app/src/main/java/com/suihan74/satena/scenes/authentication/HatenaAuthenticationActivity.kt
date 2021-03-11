package com.suihan74.satena.scenes.authentication

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityHatenaAuthenticationBinding
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HatenaAuthenticationActivity : AppCompatActivity() {
    companion object {
        /** 初回起動時の呼び出しかを判別する */
        const val EXTRA_FIRST_LAUNCH = "EXTRA_FIRST_LAUNCH"

        val REQUEST_CODE
            get() = hashCode() and 0x0000ffff
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHatenaAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ログイン
        binding.authButton.setOnClickListener {
            val name = binding.userName.text.toString()
            val password = binding.password.text.toString()

            if (name.isBlank() || password.length < 5) {
                showToast(R.string.msg_hatena_sign_in_info_is_blank)
            }
            else {
                GlobalScope.launch {
                    signIn(name, password)
                }
            }
        }

        // 新規登録
        binding.signUpTextView.apply {
            movementMethod = LinkMovementMethod.getInstance()
            setLinkTextColor(ContextCompat.getColor(this@HatenaAuthenticationActivity, R.color.colorPrimary))
        }
    }

    override fun onBackPressed() {
        finish(RESULT_CANCELED)
    }

    fun finish(result: Int) {
        if (intent.getBooleanExtra(EXTRA_FIRST_LAUNCH, false)) {
            val intent = Intent(this, EntriesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
        else {
            setResult(result)
            super.finish()
        }
    }

    private suspend fun signIn(name: String, password: String) = withContext(Dispatchers.Main) {
        val app = SatenaApplication.instance
        try {
            val account = app.accountLoader.signInHatena(name, password)

            app.showToast(R.string.msg_hatena_sign_in_succeeded, account.name)

            app.startCheckingNotificationsWorker(this@HatenaAuthenticationActivity)

            // 前の画面に戻る
            lifecycleScope.launch {
                finish(RESULT_OK)
            }
        }
        catch (e: Throwable) {
            Log.d("Hatena", e.message ?: "")
            app.showToast(R.string.msg_hatena_sign_in_failed)
        }

        // 現在のアカウントがある場合、ログイン状態を復元する
        try {
            app.accountLoader.signInHatenaAsync()
        }
        catch (e: Throwable) {
            Log.e("Hatena", e.message ?: "")
        }
    }
}
