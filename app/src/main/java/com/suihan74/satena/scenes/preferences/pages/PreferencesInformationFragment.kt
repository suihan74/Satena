package com.suihan74.satena.scenes.preferences.pages

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.PreferencesMigration
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesInformationBinding
import com.suihan74.satena.dialogs.ReleaseNotesDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.tools.RestartActivity
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.setHtml
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime

class PreferencesInformationFragment : PreferencesFragmentBase()
{
    companion object {
        fun createInstance() = PreferencesInformationFragment()

        private const val DIALOG_RELEASE_NOTES = "DIALOG_RELEASE_NOTES"

        enum class RequestCode {
            WRITE,
            READ
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val activity = requireActivity() as PreferencesActivity
        val binding = FragmentPreferencesInformationBinding.inflate(inflater, container, false)

        // アプリバージョン
        binding.appVersion.text = String.format(
            "version: %s",
            SatenaApplication.instance.versionName
        )

        // コピーライト
        binding.copyright.run {
            val startYear = 2019
            val yearStr = LocalDateTime.now().year.let {
                if (it <= startYear) "$startYear-"
                else "$startYear-$it"
            }
            setHtml(getString(R.string.copyright, yearStr))
            movementMethod = LinkMovementMethod.getInstance()
        }

        // 更新履歴ダイアログ
        binding.showReleaseNotesButton.setOnClickListener {
            val dialog = ReleaseNotesDialogFragment.createInstance()
            dialog.showAllowingStateLoss(parentFragmentManager, DIALOG_RELEASE_NOTES)
        }

        // ライセンス表示アクティビティ
        binding.showLicensesButton.setOnClickListener {
            val intent = Intent(activity, OssLicensesMenuActivity::class.java).apply {
                putExtra("title", "Licenses")
            }
            startActivity(intent)
        }

        // ファイルに設定を出力
        binding.saveSettingsButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, "${LocalDateTime.now()}.satena-settings")
            }
            startActivityForResult(intent, RequestCode.WRITE.ordinal)
        }

        // ファイルから設定を復元
        binding.loadSettingsButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            }
            startActivityForResult(intent, RequestCode.READ.ordinal)
        }

        return binding.root
    }

    // ------ //

    class Credentials private constructor(
        val deviceId : String?,
        val hatenaRk : String?,
        val hatenaUser : String?,
        val hatenaPass : String?,
        val mstdnToken : String?
    ) {
        companion object {
            /**
             * 保存対象に含めないユーザー情報などを抽出する
             */
            suspend fun extract(context: Context) : Credentials {
                val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                val credentials = Credentials(
                    deviceId = prefs.getString(PreferenceKey.ID),
                    hatenaRk = prefs.getString(PreferenceKey.HATENA_RK),
                    hatenaUser = prefs.getString(PreferenceKey.HATENA_USER_NAME),
                    hatenaPass = prefs.getString(PreferenceKey.HATENA_PASSWORD),
                    mstdnToken = prefs.getString(PreferenceKey.MASTODON_ACCESS_TOKEN)
                )
                prefs.editSync {
                    remove(PreferenceKey.ID)
                    remove(PreferenceKey.HATENA_RK)
                    remove(PreferenceKey.HATENA_USER_NAME)
                    remove(PreferenceKey.HATENA_PASSWORD)
                    remove(PreferenceKey.MASTODON_ACCESS_TOKEN)
                }
                return credentials
            }
        }

        /**
         * 抽出したデータを`SafeSharedPreferences`に再登録する
         */
        suspend fun restore(context: Context) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            prefs.editSync {
                putString(PreferenceKey.ID, deviceId)
                putString(PreferenceKey.HATENA_RK, hatenaRk)
                putString(PreferenceKey.HATENA_USER_NAME, hatenaUser)
                putString(PreferenceKey.HATENA_PASSWORD, hatenaPass)
                putString(PreferenceKey.MASTODON_ACCESS_TOKEN, mstdnToken)
            }
        }
    }

    // ------ //

    private fun savePreferencesToFile(targetUri: Uri) {
        val activity = activity as? ActivityBase
        activity?.showProgressBar()

        lifecycleScope.launch(Dispatchers.Default) {
            val context = SatenaApplication.instance.applicationContext
            val credentials = Credentials.extract(context)

            try {
                PreferencesMigration.Output(context).run {
                    addPreference<PreferenceKey>()
                    addPreference<NoticesKey>()
                    addPreference<EntriesHistoryKey>()
                    addPreference<BrowserSettingsKey>()
                    addPreference<FavoriteSitesKey>()
                    addDatabase<AppDatabase>(SatenaApplication.APP_DATABASE_FILE_NAME)
                    addDatabase<AppDatabase>(SatenaApplication.APP_DATABASE_FILE_NAME + "-shm")
                    addDatabase<AppDatabase>(SatenaApplication.APP_DATABASE_FILE_NAME + "-wal")
                    write(targetUri)
                }

                withContext(Dispatchers.Main) {
                    context.showToast(R.string.msg_pref_information_save_succeeded, targetUri.path!!)
                }
            }
            catch (e: Throwable) {
                Log.e("SavingSettings", e.message ?: "")
                withContext(Dispatchers.Main) {
                    context.showToast(R.string.msg_pref_information_save_failed)
                }
            }
            finally {
                credentials.restore(context)
                withContext(Dispatchers.Main) {
                    activity?.hideProgressBar()
                }
            }
        }
    }

    private fun loadPreferencesFromFile(targetUri: Uri) {
        val activity = activity as? ActivityBase
        activity?.showProgressBar()

        lifecycleScope.launch(Dispatchers.Default) {
            val context = SatenaApplication.instance.applicationContext
            val credentials = Credentials.extract(context)

            try {
                PreferencesMigration.Input(context)
                    .read(targetUri)

                withContext(Dispatchers.Main) {
                    context.showToast(R.string.msg_pref_information_load_succeeded, targetUri.path!!)

                    // アプリを再起動
                    val intent = RestartActivity.createIntent(context)
                    context.startActivity(intent)
                }
            }
            catch (e: PreferencesMigration.MigrationFailureException) {
                val msg = e.message ?: ""
                Log.e("LoadingSettings", msg)
                withContext(Dispatchers.Main) {
                    if (e.cause is IllegalStateException) {
                        context.showToast("${getString(R.string.msg_pref_information_load_failed)}\n${msg}")
                    }
                    else {
                        context.showToast(R.string.msg_pref_information_load_failed)
                    }
                }
            }
            finally {
                credentials.restore(context)
                withContext(Dispatchers.Main) {
                    activity?.hideProgressBar()
                }
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
            RequestCode.WRITE.ordinal -> {
                Log.d("SaveSettings", targetUri.path ?: "")
                savePreferencesToFile(targetUri)
            }

            RequestCode.READ.ordinal -> {
                Log.d("LoadSettings", targetUri.path ?: "")
                loadPreferencesFromFile(targetUri)
            }
        }
    }
}
