package com.suihan74.satena.scenes.preferences.pages

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.PreferencesMigration
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.ReleaseNotesDialogFragment
import com.suihan74.satena.models.AppDatabase
import com.suihan74.satena.models.EntriesHistoryKey
import com.suihan74.satena.models.NoticesKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.RestartActivity
import com.suihan74.utilities.setHtml
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.fragment_preferences_information.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime

class PreferencesInformationFragment : PreferencesFragmentBase()
{
    companion object {
        fun createInstance() = PreferencesInformationFragment()

        private const val WRITE_REQUEST_CODE = 42
        private const val READ_REQUEST_CODE = 43
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_information, container, false)
        val activity = requireActivity() as PreferencesActivity
        val context = requireContext()

        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName

        // アプリバージョン
        root.app_version.text = String.format("version: %s", versionName)

        // コピーライト
        root.copyright.run {
            setHtml(getString(R.string.copyright))
            movementMethod = LinkMovementMethod.getInstance()
        }

        // 更新履歴ダイアログ
        root.show_release_notes_button.setOnClickListener {
            val dialog = ReleaseNotesDialogFragment.createInstance()
            dialog.show(parentFragmentManager, "release_notes")
        }

        // ライセンス表示アクティビティ
        root.show_licenses_button.setOnClickListener {
            val intent = Intent(activity, OssLicensesMenuActivity::class.java).apply {
                putExtra("title", "Licenses")
            }
            startActivity(intent)
        }

        // ファイルに設定を出力
        root.save_settings_button.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, "${LocalDateTime.now()}.satena-settings")
            }
            startActivityForResult(intent, WRITE_REQUEST_CODE)
        }

        // ファイルから設定を復元
        root.load_settings_button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            }
            startActivityForResult(intent, READ_REQUEST_CODE)
        }

        return root
    }

    private fun savePreferencesToFile(targetUri: Uri) {
        val activity = activity as? ActivityBase
        activity?.showProgressBar()

        launch(Dispatchers.Main) {
            val context = SatenaApplication.instance.applicationContext
            try {
                PreferencesMigration.Output(context).run {
                    addPreference<PreferenceKey>()
                    addPreference<NoticesKey>()
                    addPreference<EntriesHistoryKey>()
                    addDatabase<AppDatabase>(SatenaApplication.APP_DATABASE_FILE_NAME)
                    addDatabase<AppDatabase>(SatenaApplication.APP_DATABASE_FILE_NAME + "-shm")
                    addDatabase<AppDatabase>(SatenaApplication.APP_DATABASE_FILE_NAME + "-wal")
                    write(targetUri)
                }

                context.showToast(R.string.msg_pref_information_save_succeeded, targetUri.path!!)
            }
            catch (e: Throwable) {
                Log.e("SavingSettings", e.message ?: "")
                context.showToast(R.string.msg_pref_information_save_failed)
            }
            finally {
                activity?.hideProgressBar()
            }
        }
    }

    private fun loadPreferencesFromFile(targetUri: Uri) {
        val activity = activity as? ActivityBase
        activity?.showProgressBar()

        launch(Dispatchers.Main) {
            val context = SatenaApplication.instance.applicationContext
            try {
                PreferencesMigration.Input(context)
                    .read(targetUri)

                context.showToast(R.string.msg_pref_information_load_succeeded, targetUri.path!!)

                // アプリを再起動
                val intent = RestartActivity.createIntent(context)
                context.startActivity(intent)
            }
            catch (e: Throwable) {
                val msg = e.message ?: ""
                Log.e("LoadingSettings", msg)
                if (e is IllegalStateException) {
                    context.showToast("${getString(R.string.msg_pref_information_load_failed)}\n${msg}")
                }
                else {
                    context.showToast(R.string.msg_pref_information_load_failed)
                }
            }
            finally {
                activity?.hideProgressBar()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val targetUri = data?.data

        if (resultCode != Activity.RESULT_OK || targetUri == null) {
            Log.d("FilePick", "canceled")
            return
        }

        when (requestCode) {
            WRITE_REQUEST_CODE -> {
                Log.d("SaveSettings", targetUri.path ?: "")
                savePreferencesToFile(targetUri)
            }

            READ_REQUEST_CODE -> {
                Log.d("LoadSettings", targetUri.path ?: "")
                loadPreferencesFromFile(targetUri)
            }
        }
    }
}
