package com.suihan74.satena.scenes.preferences.pages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesGeneralsBinding
import com.suihan74.satena.dialogs.NumberPickerDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.PreferencesTabMode
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.android.synthetic.main.fragment_preferences_generals.view.*

class PreferencesGeneralsFragment :
    PreferencesFragmentBase(),
    NumberPickerDialogFragment.Listener
{
    companion object {
        fun createInstance() = PreferencesGeneralsFragment()
    }

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
        viewModel.darkTheme.observe(viewLifecycleOwner, Observer {
            // ユーザーの操作によって値が変更された場合のみテーマの再設定とアプリ再起動を行う
            // この判別をしないと無限ループする
            if (initialTheme == it) return@Observer

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
        })

        // バックグラウンドで通知を確認する
        viewModel.checkNotices.observe(viewLifecycleOwner, Observer {
            if (it) SatenaApplication.instance.startNotificationService()
            else SatenaApplication.instance.stopNotificationService()
        })

        // 通知確認の間隔
        view.button_checking_notices_interval.setOnClickListener {
            NumberPickerDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_generals_notices_intervals_dialog_title)
                .setMessage(R.string.pref_generals_checking_notices_intervals_desc)
                .setMinValue(1)
                .setMaxValue(180)
                .setDefaultValue(viewModel.checkNoticesInterval.value!!.toInt())
                .show(childFragmentManager, "notices_intervals_picker")
        }

        return view
    }

    override fun onCompleteNumberPicker(value: Int, dialog: NumberPickerDialogFragment) {
        viewModel.checkNoticesInterval.value = value.toLong()
    }
}
