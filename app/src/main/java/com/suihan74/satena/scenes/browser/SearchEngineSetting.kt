package com.suihan74.satena.scenes.browser

/**
 * 検索エンジン設定
 */
data class SearchEngineSetting (
    /** 識別用のタイトル */
    val title : String,

    /** クエリURL */
    val query : String
) {
    /** プリセット項目 */
    enum class Presets(
        val setting : SearchEngineSetting
    ) {
        Google(SearchEngineSetting(
            "Google",
            "https://www.google.com/search?q=%s"
        )),

        Yahoo(SearchEngineSetting(
            "Yahoo",
            "https://search.yahoo.co.jp/search?p=%s"
        )),

        Bing(SearchEngineSetting(
            "Bing",
            "https://www.bing.com/search?q=%s"
        )),

        Hatena(SearchEngineSetting(
            "はてな",
            "https://www.hatena.ne.jp/o/search/top?q=%s"
        )),

        HatenaBookmark(SearchEngineSetting(
            "はてなブックマーク",
            "https://b.hatena.ne.jp/search/text?q=%s"
        )),

        Twitter(SearchEngineSetting(
            "Twitter",
            "https://twitter.com/search?q=%s&src=typed_query"
        )),

        GoogleTranslateEnToJa(SearchEngineSetting(
            "Google翻訳(英→日)",
            "https://translate.google.co.jp/?hl=ja#op=translate&sl=en&tl=ja&text=%s"
        )),

        GoogleTranslateJaToEn(SearchEngineSetting(
            "Google翻訳(日→英)",
            "https://translate.google.co.jp/?hl=ja#op=translate&sl=ja&tl=en&text=%s"
        ))
    }
}
