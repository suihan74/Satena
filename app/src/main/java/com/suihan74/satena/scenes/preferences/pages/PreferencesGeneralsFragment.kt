package com.suihan74.satena.scenes.preferences.pages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesGeneralsBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.NumberPickerDialogFragment
import com.suihan74.satena.models.AppUpdateNoticeMode
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.PreferencesTabMode
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.android.synthetic.main.fragment_preferences_generals.view.*

class PreferencesGeneralsFragment :
    PreferencesFragmentBase(),
    AlertDialogFragment.Listener,
    NumberPickerDialogFragment.Listener
{
    companion object {
        fun createInstance() = PreferencesGeneralsFragment()
    }

    private val DIALOG_CHECKING_NOTICES_INTERVAL by lazy { "DIALOG_CHECKING_NOTICES_INTERVAL" }
    private val DIALOG_APP_UPDATE_NOTICE_MODE by lazy { "DIALOG_APP_UPDATE_NOTICE_MODE" }

    private lateinit var viewModel: PreferencesGeneralsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val factory = PreferencesGeneralsViewModel.Factory(prefs)
        viewModel = ViewModelProvider(this, factory)[PreferencesGeneralsViewModel::class.java]

        val binding = DataBindingUtil.inflate<FragmentPreferencesGeneralsBinding>(
            inflater,
            R.layout.fragment_preferences_generals,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }
        val view = binding.root

        // ダークテーマか否か
        val initialTheme = prefs.getBoolean(PreferenceKey.DARK_THEME)
        viewModel.darkTheme.observe(viewLifecycleOwner) {
            // ユーザーの操作によって値が変更された場合のみテーマの再設定とアプリ再起動を行う
            // この判別をしないと無限ループする
            if (initialTheme == it) return@observe

            // 再起動
            val intent = Intent(activity, PreferencesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                putExtra(
                    PreferencesActivity.EXTRA_CURRENT_TAB,
                    PreferencesTabMode.GENERALS
                )
                putExtra(PreferencesActivity.EXTRA_THEME_CHANGED, true)
            }
            startActivity(intent)
        }

        // バックグラウンドで通知を確認する
        viewModel.checkNotices.observe(viewLifecycleOwner) {
            if (it) SatenaApplication.instance.startNotificationService()
            else SatenaApplication.instance.stopNotificationService()
        }

        // 通知確認の間隔
        view.button_checking_notices_interval.setOnClickListener {
            NumberPickerDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_generals_notices_intervals_dialog_title)
                .setMessage(R.string.pref_generals_checking_notices_intervals_desc)
                .setMinValue(1)
                .setMaxValue(180)
                .setDefaultValue(viewModel.checkNoticesInterval.value!!.toInt())
                .showAllowingStateLoss(childFragmentManager, DIALOG_CHECKING_NOTICES_INTERVAL)
        }

        // アプリ内アップデート通知の対象を選択
        view.button_app_update_notice_mode.setOnClickListener {
            val currentValue = viewModel.appUpdateNoticeMode.value ?: AppUpdateNoticeMode.FIX
            val currentIdx = AppUpdateNoticeMode.values().indexOf(currentValue)
            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_generals_app_update_notice_mode_desc)
                .setSingleChoiceItems(AppUpdateNoticeMode.values().map { getString(it.textId) }.toTypedArray(), currentIdx)
                .setNegativeButton(R.string.dialog_cancel)
                .show(childFragmentManager, DIALOG_APP_UPDATE_NOTICE_MODE)
        }

        return view
    }

    override fun onCompleteNumberPicker(value: Int, dialog: NumberPickerDialogFragment) {
        viewModel.checkNoticesInterval.value = value.toLong()
    }

    override fun onSingleChoiceItem(dialog: AlertDialogFragment, which: Int) {
        when (dialog.tag) {
            DIALOG_APP_UPDATE_NOTICE_MODE -> {
                viewModel.appUpdateNoticeMode.value = AppUpdateNoticeMode.values()[which]
            }
        }
        dialog.dismissAllowingStateLoss()
    }
}
