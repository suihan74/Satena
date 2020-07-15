package com.suihan74.satena.scenes.tools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.suihan74.hatenaLib.EntriesType
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.modifySpecificUrlsWithConnection
import com.suihan74.satena.modifySpecificUrlsWithoutConnection
import com.suihan74.utilities.openUrlExcludeApplication
import com.suihan74.utilities.showToast
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

    private suspend fun redirect(srcUrl: String) = withContext(Dispatchers.Main) {
        try {
            val srcUri = Uri.parse(srcUrl)
            if (srcUri.scheme != "http" && srcUri.scheme != "https") {
                throw RuntimeException("passed text is not a URL")
            }

            val commentPageUrl = modifyUrl(srcUrl)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(HatenaClient.getCommentPageUrlFromEntryUrl(commentPageUrl))
            )
            startActivity(intent.openUrlExcludeApplication(baseContext))
        }
        catch (e: Throwable) {
            Log.w("OpenBookmarksPage", e)
            showToast("Failure")
        }
        finally {
            finish()
        }
    }

    private suspend fun modifyUrl(srcUrl: String) : String = withContext(Dispatchers.IO) {
        val modifiedUrl = modifySpecificUrlsWithoutConnection(srcUrl)!!
        val searchResult = HatenaClient.searchEntriesAsync(modifiedUrl, SearchType.Text, EntriesType.Recent).await()

        if (searchResult.any { it.url == modifiedUrl }) modifiedUrl
        else modifySpecificUrlsWithConnection(modifiedUrl)!!
    }
}
