package com.suihan74.satena.scenes.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import com.suihan74.satena.scenes.splash.SplashActivity

class RestartActivity : Activity() {
    companion object {
        const val EXTRA_MAIN_PID = "RestartActivity.EXTRA_MAIN_PID"

        fun createIntent(context: Context) = Intent().apply {
            setClass(context, RestartActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_MAIN_PID, Process.myPid())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // メインプロセス終了
        val mainPid = intent.getIntExtra(EXTRA_MAIN_PID, -1)
        Process.killProcess(mainPid)

        // メインアクティビティ再起動
        val restartIntent = Intent(applicationContext, SplashActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        applicationContext.startActivity(restartIntent)

        // RestartActivity終了
        finish()
        Process.killProcess(Process.myPid())
    }
}
