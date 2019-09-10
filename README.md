# Satena

## ファイルの場所など
* Kotlinコード
  * [com.suihan74.*](https://github.com/suihan74/Satena/tree/master/app/src/main/java/com/suihan74)
    * [はてなAPI](https://github.com/suihan74/Satena/tree/master/app/src/main/java/com/suihan74/HatenaLib)
    * [アプリ部分](https://github.com/suihan74/Satena/tree/master/app/src/main/java/com/suihan74/satena)
    * [ユーティリティ・拡張](https://github.com/suihan74/Satena/tree/master/app/src/main/java/com/suihan74/utilities)
* 画面レイアウト
  * [res/layout](https://github.com/suihan74/Satena/tree/master/app/src/main/res/layout)
  * [res/layout-land](https://github.com/suihan74/Satena/tree/master/app/src/main/res/layout-land)
* ビュー中の文字列・スタイルなどのリソース
  * [res/values](https://github.com/suihan74/Satena/tree/master/app/src/main/res/values)
* ベクタイメージ
  * [res/drawable](https://github.com/suihan74/Satena/tree/master/app/src/main/res/drawable)
  
## ざっくり説明
* [Activity](https://github.com/suihan74/Satena/tree/master/app/src/main/java/com/suihan74/satena/activities)には表示コンテンツである[Fragment](https://github.com/suihan74/Satena/tree/master/app/src/main/java/com/suihan74/satena/fragments)とプログレスバー（とロード中クリック防止の半透明黒背景）を配置
* 〇〇Fragmentが実際に表示されているコンテンツ
* ××TabFragmentはさらにその内側でタブとして表示されている子コンテンツ
* TabFragment，リスト表示される内容は[Adapter](https://github.com/suihan74/Satena/tree/master/app/src/main/java/com/suihan74/satena/adapters)により管理
  * Adapter内部のViewHolderがリストの各要素

* Activity直下のFragment遷移に関する処理は[utilities.FragmentContainerActivity](https://github.com/suihan74/Satena/blob/master/app/src/main/java/com/suihan74/utilities/FragmentContainerActivity.kt)にまとめてある
  * さらにそれを継承した[activities.ActivityBase](https://github.com/suihan74/Satena/blob/master/app/src/main/java/com/suihan74/satena/activities/ActivityBase.kt)にプログレスバー周りの処理をまとめて，各Activityで継承して使用している

* はてなとの通信は[HatenaLib.HatenaClient](https://github.com/suihan74/Satena/blob/master/app/src/main/java/com/suihan74/HatenaLib/HatenaClient.kt)を介して行う．HatenaLib/以下にある他のファイルは大体そのレスポンスデータ
