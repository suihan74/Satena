package com.suihan74.satena.scenes.browser

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.suihan74.utilities.extensions.withSafety
import java.io.ByteArrayInputStream

class BrowserWebViewClient(
    private val activity: BrowserActivity,
    private val viewModel: BrowserViewModel
) : WebViewClient() {

    private val emptyResourceRequest : WebResourceResponse by lazy {
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
    }

    /** ページに遷移するか否かを決定する */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        return when (val scheme = uri.scheme) {
            "https", "http" ->
                super.shouldOverrideUrlLoading(view, request)

            "intent", "android-app" -> {
                handleIntentScheme(scheme, uri)
                true
            }

            else -> {
                handleOtherSchemes(uri)
                true
            }
        }
    }

    /** intentスキームのURIを処理する */
    private fun handleIntentScheme(scheme: String, uri: Uri) {
        try {
            val intentScheme =
                if (scheme == "intent") Intent.URI_INTENT_SCHEME
                else Intent.URI_ANDROID_APP_SCHEME
            val intent = Intent.parseUri(uri.toString(), intentScheme).withSafety()
            activity.startActivity(intent)
        }
        catch (e: Throwable) {
            Log.e("WebViewClient", Log.getStackTraceString(e))
        }
    }

    /** ネットワークアドレスでもなく、intentでもないアドレスを処理する */
    private fun handleOtherSchemes(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_DEFAULT, uri).withSafety()
            activity.startActivity(intent)
        }
        catch (e: Throwable) {
            Log.e("WebViewClient", Log.getStackTraceString(e))
        }
    }

    // ------ //

    /** WebView内でのページ遷移をViewModelに伝える */
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        url ?: return
        super.onPageStarted(view, url, favicon)
        viewModel.onPageStarted(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) {
            viewModel.onPageFinished(view, url)
        }
    }

    /** URLブロック対象のリソースを読み込まないようにする */
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        val blocked =
            viewModel.useUrlBlocking.value == true &&
            viewModel.browserRepo.blockUrlsRegex.containsMatchIn(url)

        val result =
            if (!blocked) super.shouldInterceptRequest(view, request)
            else emptyResourceRequest

        if (blocked) {
            viewModel.addResource(url, blocked = true)
        }

        return result
    }

    /** ページ中のリソースURLをすべて記録する */
    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
        if (url != null) {
            viewModel.addResource(url, blocked = false)
        }
    }
}
