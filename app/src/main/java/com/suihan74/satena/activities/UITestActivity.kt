package com.suihan74.satena.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.suihan74.satena.R

class UITestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ui_test)

        findViewById<EditText>(R.id.edit_text).apply {
            // 注: XML側に書くと折り返さない一行での表示になる
            setHorizontallyScrolling(false)
            // 注: XML側に書くと縦幅が一行が表示される分だけになる
            maxLines = Int.MAX_VALUE

            // DONEボタンとかSEARCHボタンとかが押された時の処理
            setOnEditorActionListener { _, action, _ ->
                when (action) {
                    EditorInfo.IME_ACTION_DONE -> {
                        /* DONEボタン押したときの処理 */
                        true
                    }
                    else -> false
                }
            }
        }
    }
}
