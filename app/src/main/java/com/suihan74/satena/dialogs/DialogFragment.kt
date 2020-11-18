package com.suihan74.satena.dialogs

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.suihan74.satena.models.DialogThemeSetting
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

/**
 * テーマ設定を反映したダイアログを作成する
 */
fun DialogFragment.createBuilder(
    context: Context,
    @StyleRes styleId: Int? = null
) : AlertDialog.Builder =
    AlertDialog.Builder(context, styleId ?: let {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val dialogThemeSetting = DialogThemeSetting.fromId(
            prefs.getInt(PreferenceKey.DIALOG_THEME)
        )
        dialogThemeSetting.themeId
    })

/**
 * テーマ設定を反映した`AlertDialog.Builder`を作成する
 */
fun DialogFragment.createBuilder() : AlertDialog.Builder =
    createBuilder(requireContext())

/**
 * ダイアログ用のテーマを適用した`Context`を取得する
 */
fun DialogFragment.themeWrappedContext(@StyleRes styleId: Int? = null) : Context =
    ContextThemeWrapper(requireContext(), styleId ?: let {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val dialogThemeSetting = DialogThemeSetting.fromId(
            prefs.getInt(PreferenceKey.DIALOG_THEME)
        )

        if (dialogThemeSetting == DialogThemeSetting.APP) {
            if (prefs.getBoolean(PreferenceKey.DARK_THEME)) DialogThemeSetting.DARK.themeId
            else DialogThemeSetting.LIGHT.themeId
        }
        else dialogThemeSetting.themeId
    })

/**
 * テーマ設定を反映したView作成用の`LayoutInflater`を取得する
 */
fun DialogFragment.localLayoutInflater(@StyleRes styleId: Int? = null) : LayoutInflater =
    LayoutInflater.from(themeWrappedContext(styleId))
