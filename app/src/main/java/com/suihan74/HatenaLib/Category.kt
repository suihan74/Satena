package com.suihan74.HatenaLib

enum class Category(
    val int: Int,
    val code: String
) {
    All(0, "315767106563433873"),
    Social(1, "301816409282464093"),
    Economics(2, "300989576564947867"),
    Life(3, "244148959988020477"),
    Knowledge(4, "315890158150969179"),
    It(5, "261248828312298389"),
    Entertainment(6, "302115476501939948"),
    Game(7, "297347994088281699"),
    Fun(8, "302115476506048236");
//    CurrentEvents(9, "83497569613451046"),

    companion object {
        fun fromInt(i: Int) : Category = values().getOrNull(i) ?: All
    }
}
