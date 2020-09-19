package com.suihan74.satena.scenes.tools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.extensions.createIntentWithoutThisApplication
import com.suihan74.utilities.extensions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** webブラウザでブクマページを開く共有機能のためのダミーアクティビティ */
class OpenBookmarksPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val srcUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        GlobalScope.launch {
            redirect(srcUrl)
        }
    }

    private suspend fun redirect(srcUrl: String?) = withContext(Dispatchers.Main) {
        try {
            if (srcUrl == null || !(srcUrl.startsWith("http://") || srcUrl.startsWith("https://"))) {
                throw InvalidUrlException(srcUrl)
            }

            val commentPageUrl = modifySpecificUrls(srcUrl)!!
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(HatenaClient.getCommentPageUrlFromEntryUrl(commentPageUrl))
            )
            startActivity(intent.createIntentWithoutThisApplication(baseContext))
        }
        catch (e: InvalidUrlException) {
            showToast(R.string.invalid_url_error)
        }
        catch (e: Throwable) {
            Log.w("OpenBookmarksPage", e)
            showToast("Failure")
        }
        finally {
            finish()
        }
    }
}
