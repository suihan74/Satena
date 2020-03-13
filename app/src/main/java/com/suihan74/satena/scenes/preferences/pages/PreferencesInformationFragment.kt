package com.suihan74.satena.scenes.preferences.pages

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
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.PreferencesMigration
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.FilePickerDialog
import com.suihan74.satena.dialogs.ReleaseNotesDialogFragment
import com.suihan74.satena.models.AppDatabase
import com.suihan74.satena.models.EntriesHistoryKey
import com.suihan74.satena.models.NoticesKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import java.io.File

class PreferencesInformationFragment :
    PreferencesFragmentBase(),
    PermissionRequestable,
    FilePickerDialog.Listener
{
    companion object {
        fun createInstance() =
            PreferencesInformationFragment()
    }

    enum class FilePickerMode {
        SAVE,
        LOAD
    }

    private var mFilePickerMode: FilePickerMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_information, container, false)
        val activity = requireActivity() as PreferencesActivity
        val context = requireContext()

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

        // 更新履歴ダイアログ
        root.findViewById<Button>(R.id.show_release_notes_button).setOnClickListener {
            val dialog = ReleaseNotesDialogFragment.createInstance()
            dialog.show(parentFragmentManager, "release_notes")
        }

        // ライセンス表示アクティビティ
        root.findViewById<Button>(R.id.show_licenses_button).setOnClickListener {
            val intent = Intent(activity, OssLicensesMenuActivity::class.java)
            intent.putExtra("title", "Licenses")
            startActivity(intent)
        }

        // ファイルに設定を出力
        root.findViewById<Button>(R.id.save_settings_button).setOnClickListener {
            mFilePickerMode =
                FilePickerMode.SAVE

            val rp = RuntimePermission(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
            rp.request(activity)
        }

        // ファイルから設定を復元
        root.findViewById<Button>(R.id.load_settings_button).setOnClickListener {
            mFilePickerMode =
                FilePickerMode.LOAD

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
            try {
                when (mFilePickerMode) {
                    FilePickerMode.SAVE -> {
                        FilePickerDialog.Builder(R.style.AlertDialogStyle)
                            .setDirectoryOnly(true)
                            .setTitle(R.string.dialog_title_pref_information_save_settings)
                            .show(childFragmentManager, "save_dialog")
                    }

                    FilePickerMode.LOAD -> {
                        FilePickerDialog.Builder(R.style.AlertDialogStyle)
                            .setDirectoryOnly(false)
                            .setTitle(R.string.dialog_title_pref_information_load_settings)
                            .show(childFragmentManager, "load_dialog")
                    }
                }
            }
            catch (e: Exception) {
                Log.e("filePicker", e.message)
            }
        }
        else {
            activity?.showToast(R.string.msg_pref_information_save_load_permission_failed)
        }
    }

    private fun savePreferencesToFile(file: File) {
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
                    write(file)
                }

                context.showToast(R.string.msg_pref_information_save_succeeded, file.absolutePath)
            }
            catch (e: Exception) {
                Log.e("SavingSettings", e.message)
                context.showToast(R.string.msg_pref_information_save_failed)
            }
            finally {
                activity?.hideProgressBar()
            }
        }
    }

    private fun loadPreferencesFromFile(file: File) {
        val activity = activity as? ActivityBase
        activity?.showProgressBar()

        launch(Dispatchers.Main) {
            val context = SatenaApplication.instance.applicationContext
            try {
                PreferencesMigration.Input(context)
                    .read(file)

                context.showToast(R.string.msg_pref_information_load_succeeded, file.absolutePath)

                // アプリを再起動
                val intent = RestartActivity.createIntent(context)
                context.startActivity(intent)

                /*
                val intent = Intent(context, PreferencesActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    putExtra(
                        PreferencesActivity.EXTRA_CURRENT_TAB,
                        PreferencesTabMode.INFORMATION
                    )
                    putExtra(PreferencesActivity.EXTRA_THEME_CHANGED, true)
                    putExtra(PreferencesActivity.EXTRA_RELOAD_ALL_PREFERENCES, true)
                }
                startActivity(intent)
                 */
            }
            catch (e: Exception) {
                Log.e("LoadingSettings", e.message)
                if (e is IllegalStateException) {
                    context.showToast("${getString(R.string.msg_pref_information_load_failed)}\n${e.message}")
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

    override fun onOpen(file: File, dialog: FilePickerDialog) {
        when (dialog.tag) {
            "save_dialog" -> {
                Log.d("FilePickerDialog", file.absolutePath)
                val target = File(file, "${LocalDateTime.now()}.satena-settings")
                savePreferencesToFile(target)
            }

            "load_dialog" -> {
                Log.d("FilePickerDialog", file.absolutePath)
                loadPreferencesFromFile(file)
            }
        }
    }
}
