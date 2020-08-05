package com.suihan74.satena.models

import android.graphics.Typeface

/** フォント設定 */
data class FontSettings (
    /** フォントファミリ */
    val fontFamily : String,

    /** サイズ(sp) */
    val size : Float,

    /** フォント参照方法 */
    var _fontType: FontType? = FontType.LOGICAL,

    /** 太字 */
    val bold : Boolean = false,

    /** 斜体 */
    val italic : Boolean = false
) {
    enum class FontType {
        /** 論理フォント */
        LOGICAL,
        /** "/system/fonts"から読み込む */
        @Deprecated("use LOGICAL")
        SYSTEM_FILE,
        /** 外部から読み込む */
        FILE
    }

    /** テキスト化した情報 */
    val information : String
        get() = "$fontFamily, ${size}sp"

    /** 装飾 */
    val style : Int
        get() = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

    var fontType : FontType
        get() {
            if (_fontType == null) {
                _fontType = FontType.LOGICAL
            }
            return _fontType!!
        }
        set(value) {
            _fontType = value
        }

    /** Viewに適用するためのデータ */
    val typeface : Typeface
        get() {
            if (_typeface == null) {
                _typeface = when (fontType) {
                    FontType.LOGICAL -> Typeface.create(fontFamily, style)
                    FontType.SYSTEM_FILE -> Typeface.createFromFile("/system/fonts/${fontFamily}.ttf")
                    FontType.FILE -> Typeface.createFromFile(fontFamily)
                }
            }
            return _typeface!!
        }
    private var _typeface : Typeface? = null

    override fun toString(): String = information
}
