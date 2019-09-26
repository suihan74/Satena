package com.suihan74.HatenaLib

enum class Category(
    val int: Int,
    val requireSignedIn: Boolean = false,
    val singleColumns : Boolean = false
) {
    All(0),
    Social(1),
    Economics(2),
    Life(3),
    Knowledge(4),
    It(5),
    Entertainment(6),
    Game(7),
    Fun(8),
//    CurrentEvents(9),

    MyHotEntries(9, true, true),
    MyBookmarks(10, true),
    MyTags(11, true, true),

    Search(12, singleColumns = true);

    companion object {
        fun fromInt(i: Int) : Category = values().getOrNull(i) ?: All
        fun valuesWithSignedIn() = values()
        fun valuesWithoutSignedIn() = values().filterNot { it.requireSignedIn }.toTypedArray()
    }

    override fun toString() : String = when (this) {
        All -> "315767106563433873"
        Social -> "301816409282464093"
        Economics -> "300989576564947867"
        Life -> "244148959988020477"
        Knowledge -> "315890158150969179"
        It -> "261248828312298389"
        Entertainment -> "302115476501939948"
        Game -> "297347994088281699"
        Fun -> "302115476506048236"
//        CurrentEvents -> "83497569613451046"
        MyHotEntries -> ""
        MyBookmarks -> ""
        MyTags -> ""
        Search -> ""
    }
}
