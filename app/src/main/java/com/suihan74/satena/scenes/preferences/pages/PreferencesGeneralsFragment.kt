package com.suihan74.satena.scenes.preferences.pages

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.InverseBindingAdapter
import androidx.lifecycle.observe
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesGeneralsBinding
import com.suihan74.satena.dialogs.NumberPickerDialog
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.PreferencesTabMode
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.putObjectExtra
import com.suihan74.utilities.lazyProvideViewModel
import com.suihan74.utilities.showAllowingStateLoss

class PreferencesGeneralsFragment : PreferencesFragmentBase() {
    companion object {
        fun createInstance() = PreferencesGeneralsFragment()
    }

    // ------ //

    object BindingAdapters {
        @JvmStatic
        @InverseBindingAdapter(attribute = "drawerGravity", event = "android:checkedAttrChanged")
        fun inverseSetDrawerGravityToggle(view: ToggleButton): Int {
            return if (view.isChecked) Gravity.LEFT else Gravity.RIGHT
        }

        @JvmStatic
        @BindingAdapter("drawerGravity")
        fun setDrawerGravityToggle(view: ToggleButton, gravity: Int) {
            val new = gravity == Gravity.LEFT
            if (view.isChecked != new) {
                view.isChecked = new
            }
        }
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        PreferencesGeneralsViewModel(requireContext(), prefs)
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentPreferencesGeneralsBinding>(
            inflater,
            R.layout.fragment_preferences_generals,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        // ダークテーマか否か
        val initialTheme = viewModel.darkTheme.value
        viewModel.darkTheme.observe(viewLifecycleOwner) {
            // ユーザーの操作によって値が変更された場合のみテーマの再設定とアプリ再起動を行う
            // この判別をしないと無限ループする
            if (initialTheme == it) return@observe

            // 再起動
            val intent = Intent(activity, PreferencesActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION

                putObjectExtra(
                    PreferencesActivity.EXTRA_CURRENT_TAB,
                    PreferencesTabMode.GENERALS
                )
                putExtra(PreferencesActivity.EXTRA_THEME_CHANGED, true)
            }
            startActivity(intent)
        }

        // ダイアログのテーマを選択
        binding.dialogThemeButton.setOnClickListener {
            viewModel.openDialogThemeSelectionDialog(childFragmentManager)
        }

        // バックグラウンドで通知を確認する
        var called = false // TODO: 応急
        viewModel.checkNotices.observe(viewLifecycleOwner) {
            if (!called) {
                called = true
                return@observe
            }
            val context = requireContext()
            if (it) SatenaApplication.instance.startCheckingNotificationsWorker(context)
            else SatenaApplication.instance.stopCheckingNotificationsWorker(context)
        }

        // 通知確認の間隔
        binding.buttonCheckingNoticesInterval.setOnClickListener {
            val dialog = NumberPickerDialog.createInstance(
                min = 15,
                max = 180,
                default = viewModel.checkNoticesInterval.value!!.toInt(),
                titleId = R.string.pref_generals_checking_notices_intervals_desc,
                messageId = R.string.pref_generals_notices_intervals_dialog_msg
            ) { value ->
                viewModel.checkNoticesInterval.value = value.toLong()
                SatenaApplication.instance.startCheckingNotificationsWorker(requireContext(), forceReplace = true)
            }
            dialog.showAllowingStateLoss(childFragmentManager)
        }

        // アプリ内アップデート通知の対象を選択
        binding.buttonAppUpdateNoticeMode.setOnClickListener {
            viewModel.openAppUpdateNoticeModeSelectionDialog(childFragmentManager)
        }

        // 画像キャッシュを削除する
        binding.clearImageCacheButton.setOnClickListener {
            viewModel.openClearImageCacheConfirmDialog(childFragmentManager)
        }

        return binding.root
    }
}
