package com.suihan74.satena.fragments

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.suihan74.satena.PreferencesMigrator
import com.suihan74.satena.R
import com.suihan74.satena.activities.ActivityBase
import com.suihan74.satena.activities.PreferencesActivity
import com.suihan74.satena.adapters.tabs.PreferencesTabMode
import com.suihan74.satena.dialogs.FilePickerDialog
import com.suihan74.satena.models.IgnoredEntriesKey
import com.suihan74.satena.models.NoticesKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.UserTagsKey
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import java.io.File

class PreferencesInformationFragment : CoroutineScopeFragment(), PermissionRequestable {
    companion object {
        fun createInstance() = PreferencesInformationFragment()
    }

    enum class FilePickerMode {
        SAVE,
        LOAD
    }

    private var mFilePickerMode: FilePickerMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_information, container, false)
        val activity = activity as PreferencesActivity
        val context = context!!

        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName

        // アプリバージョン
        root.findViewById<TextView>(R.id.app_version).text =
            String.format("version: %s", versionName)

        // コピーライト
        val copyrightStr = getString(R.string.copyright)
        root.findViewById<TextView>(R.id.copyright).apply {
            setHtml(copyrightStr)
            movementMethod = LinkMovementMethod.getInstance()
        }

        // ライセンス表示アクティビティ
        root.findViewById<Button>(R.id.show_licenses_button).setOnClickListener {
            val intent = Intent(activity, OssLicensesMenuActivity::class.java)
            intent.putExtra("title", "Licenses")
            startActivity(intent)
        }

        // ファイルに設定を出力
        root.findViewById<Button>(R.id.save_settings_button).setOnClickListener {
            mFilePickerMode = FilePickerMode.SAVE

            val rp = RuntimePermission(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
            rp.request(activity)
        }

        // ファイルから設定を復元
        root.findViewById<Button>(R.id.load_settings_button).setOnClickListener {
            mFilePickerMode = FilePickerMode.LOAD

            val rp = RuntimePermission(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
            rp.request(activity)
        }

        return root
    }

    override fun onRequestPermissionsResult(pairs: List<Pair<String, Int>>) {
        val granted = pairs.all { p -> p.second == RuntimePermission.PERMISSION_GRANTED }

        if (granted) {
            val dialog = when (mFilePickerMode) {
                FilePickerMode.SAVE -> {
                    FilePickerDialog.createInstance("出力先") { dir ->
                        Log.d("FilePickerDialog", dir.absolutePath)
                        val file = File(dir, "${LocalDateTime.now()}.satena-settings")
                        savePreferencesToFile(file)

                    }.apply {
                        directoryOnly = true
                    }
                }

                FilePickerMode.LOAD -> {
                    FilePickerDialog.createInstance("設定ファイルを選択") { file ->
                        Log.d("FilePickerDialog", file.absolutePath)
                        loadPreferencesFromFile(file)

                    }.apply {
                        directoryOnly = false
                    }
                }

                else -> return
            }

            dialog.show(fragmentManager!!, "FilePicker")
        }
        else {
            activity!!.showToast("ファイルを入出力するための権限がありません")
        }
    }

    private fun savePreferencesToFile(file: File) {
        val activity = activity!! as ActivityBase
        activity.showProgressBar()

        launch(Dispatchers.Main) {
            try {
                PreferencesMigrator.Output(activity).run {
                    addPreference<PreferenceKey>()
                    addPreference<IgnoredEntriesKey>()
                    addPreference<NoticesKey>()
                    addPreference<UserTagsKey>()
                    write(file)
                }

                activity.showToast("設定を${file.absolutePath}に保存しました")
            }
            catch (e: Exception) {
                Log.e("SavingSettings", e.message)
                activity.showToast("設定の保存に失敗しました")
            }
            finally {
                activity.hideProgressBar()
            }
        }
    }

    private fun loadPreferencesFromFile(file: File) {
        val activity = activity!! as ActivityBase
        activity.showProgressBar()

        launch(Dispatchers.Main) {
            try {
                PreferencesMigrator.Input(activity)
                    .read(file)

                activity.showToast("設定を${file.absolutePath}から読み込みました")

                val intent = Intent(activity, PreferencesActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    putExtra(PreferencesActivity.EXTRA_CURRENT_TAB, PreferencesTabMode.INFORMATION)
                    putExtra(PreferencesActivity.EXTRA_THEME_CHANGED, true)
                    putExtra(PreferencesActivity.EXTRA_RELOAD_ALL_PREFERENCES, true)
                }
                startActivity(intent)
            }
            catch (e: Exception) {
                Log.e("LoadingSettings", e.message)
                if (e is IllegalStateException) {
                    activity.showToast("設定の読み込みに失敗しました\n${e.message}")
                }
                else {
                    activity.showToast("設定の読み込みに失敗しました")
                }
            }
            finally {
                activity.hideProgressBar()
            }
        }
    }
}
