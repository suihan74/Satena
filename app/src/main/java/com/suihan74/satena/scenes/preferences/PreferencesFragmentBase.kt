package com.suihan74.satena.scenes.preferences

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import com.suihan74.utilities.CoroutineScopeFragment

// ViewPagerでページ切替時にツールバーメニュー項目が残ったままになるのを防ぐ
abstract class PreferencesFragmentBase : CoroutineScopeFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
}
