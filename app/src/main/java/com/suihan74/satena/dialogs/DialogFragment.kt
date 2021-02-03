package com.suihan74.satena.dialogs

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.suihan74.satena.models.DialogThemeSetting
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.utilities.SafeSharedPreferences

/**
 * ダイアログに対して行う必要がある追加のデフォルト設定を施すための`AlertDialog.Builder`
 */
class DialogFragmentContentBuilder(context: Context, @StyleRes themeId: Int)
    : AlertDialog.Builder(context, themeId)
{
    /** ダイアログ外側をタッチで閉じる */
    var canceledOnTouchOutside : Boolean =
        SafeSharedPreferences.create<PreferenceKey>(context)
            .getBoolean(PreferenceKey.CLOSE_DIALOG_ON_TOUCH_OUTSIDE)

    override fun create(): AlertDialog {
        return super.create().also { dialog ->
            dialog.setCanceledOnTouchOutside(canceledOnTouchOutside)
        }
    }
}

/**
 * テーマ設定を反映したダイアログを作成する
 */
fun DialogFragment.createBuilder(
    context: Context,
    @StyleRes themeId: Int? = null
) = DialogFragmentContentBuilder(
    context,
    themeId ?: let {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val dialogThemeSetting = DialogThemeSetting.fromId(
            prefs.getInt(PreferenceKey.DIALOG_THEME)
        )
        dialogThemeSetting.themeId
    }
)

/**
 * テーマ設定を反映した`AlertDialog.Builder`を作成する
 */
fun DialogFragment.createBuilder() : DialogFragmentContentBuilder =
    createBuilder(requireContext())

/**
 * ダイアログ用のテーマを適用した`Context`を取得する
 */
fun DialogFragment.themeWrappedContext(@StyleRes themeId: Int? = null) : Context =
    ContextThemeWrapper(requireContext(), themeId ?: let {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val dialogThemeSetting = DialogThemeSetting.fromId(
            prefs.getInt(PreferenceKey.DIALOG_THEME)
        )

        if (dialogThemeSetting == DialogThemeSetting.APP) {
            when (prefs.getInt(PreferenceKey.THEME)) {
                Theme.LIGHT.id -> DialogThemeSetting.LIGHT.themeId
                else -> DialogThemeSetting.DARK.themeId
            }
        }
        else dialogThemeSetting.themeId
    })

/**
 * テーマ設定を反映したView作成用の`LayoutInflater`を取得する
 */
fun DialogFragment.localLayoutInflater(@StyleRes themeId: Int? = null) : LayoutInflater =
    LayoutInflater.from(themeWrappedContext(themeId))
