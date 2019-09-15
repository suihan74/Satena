package com.suihan74.satena.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.activities.PreferencesActivity
import com.suihan74.satena.adapters.tabs.PreferencesTabMode
import com.suihan74.satena.dialogs.NumberPickerDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class PreferencesGeneralsFragment : Fragment() {
    companion object {
        fun createInstance() = PreferencesGeneralsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_generals, container, false)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context!!)

        // ダークテーマか否か
        view.findViewById<ToggleButton>(R.id.preferences_generals_theme).apply {
            val key = PreferenceKey.DARK_THEME
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }

                SatenaApplication.instance.setTheme(if (isChecked) R.style.AppTheme_Dark else R.style.AppTheme_Light)

                val intent = Intent(activity, PreferencesActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                    putExtra(PreferencesActivity.EXTRA_CURRENT_TAB, PreferencesTabMode.GENERALS)
                    putExtra(PreferencesActivity.EXTRA_THEME_CHANGED, true)
                }
                startActivity(intent)
            }
        }

        // 終了確認ダイアログを表示する
        view.findViewById<ToggleButton>(R.id.preferences_generals_using_termination_dialog).apply {
            val key = PreferenceKey.USING_TERMINATION_DIALOG
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // 通知既読をアプリから更新する
        view.findViewById<ToggleButton>(R.id.preferences_generals_notices_last_seen_updatable).apply {
            val key = PreferenceKey.NOTICES_LAST_SEEN_UPDATABLE
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // バックグラウンドで通知を確認する
        view.findViewById<ToggleButton>(R.id.preferences_generals_background_checking_notices).apply {
            val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }

                if (isChecked) {
                    SatenaApplication.instance.startNotificationService()
                }
                else {
                    SatenaApplication.instance.stopNotificationService()
                }
            }
        }

        // バックグラウンドで通知を確認する間隔
        view.findViewById<Button>(R.id.preferences_generals_checking_notices_interval).apply {
            val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS
            val value = prefs.get<Long>(key)
            text = String.format("%d分", value)

            setOnClickListener {
                val currentValue = prefs.get<Long>(key)
                val dialog = NumberPickerDialogFragment.createInstance(
                    title = "通知確認間隔を設定",
                    message = "1分～180分で指定できます",
                    minValue = 1,
                    maxValue = 180,
                    defaultValue = currentValue.toInt()
                ) { value ->
                    prefs.edit {
                        putLong(key, value.toLong())
                    }
                    text = String.format("%d分", prefs.get(key))
                }
                dialog.show(fragmentManager, "dialog")
            }
        }

        return view
    }
}
