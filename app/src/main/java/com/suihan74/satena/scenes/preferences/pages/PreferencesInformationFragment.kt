package com.suihan74.satena.scenes.preferences.pages

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesInformationBinding
import com.suihan74.satena.dialogs.ReleaseNotesDialogFragment
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.extensions.setHtml
import com.suihan74.utilities.showAllowingStateLoss
import org.threeten.bp.LocalDateTime

class PreferencesInformationFragment : PreferencesFragmentBase()
{
    companion object {
        fun createInstance() = PreferencesInformationFragment()

        private const val DIALOG_RELEASE_NOTES = "DIALOG_RELEASE_NOTES"
    }

    // ------ //

    private val preferencesActivity : PreferencesActivity
        get() = requireActivity() as PreferencesActivity

    // ------ //

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
            preferencesActivity.openSaveSettingsDialog()
        }

        // ファイルから設定を復元
        binding.loadSettingsButton.setOnClickListener {
            preferencesActivity.openLoadSettingsDialog()
        }

        return binding.root
    }


}
