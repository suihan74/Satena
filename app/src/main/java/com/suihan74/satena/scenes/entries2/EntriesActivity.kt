package com.suihan74.satena.scenes.entries2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityEntries2Binding
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.activity_entries2.*

class EntriesActivity : AppCompatActivity() {
    private lateinit var viewModel : EntriesViewModel
    private lateinit var binding : ActivityEntries2Binding

    private val isDrawerOpened : Boolean
        get() = drawer_layout.isDrawerOpen(categories_list)

    private var isFABMenuOpened : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        setTheme(
            if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
            else R.style.AppTheme_Light
        )

        viewModel =
            if (savedInstanceState == null) {
                val factory = EntriesViewModel.Factory(
                    EntriesRepository(
                        client = HatenaClient,
                        accountLoader = AccountLoader(
                            this,
                            HatenaClient,
                            MastodonClientHolder
                        ),
                        prefs = prefs,
                        historyPrefs = SafeSharedPreferences.create(this)
                    )
                )
                ViewModelProviders.of(this, factory)[EntriesViewModel::class.java]
            }
            else {
                ViewModelProviders.of(this)[EntriesViewModel::class.java]
            }.apply {
                initialize { e ->
                    showToast(R.string.msg_auth_failed)
                    Log.e("error", Log.getStackTraceString(e))
                }
            }

        binding = DataBindingUtil.setContentView<ActivityEntries2Binding>(
            this,
            R.layout.activity_entries2
        ).apply {
            lifecycleOwner = this@EntriesActivity
            vm = viewModel
        }

        // カテゴリリスト初期化
        categories_list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = object : CategoriesAdapter() {
                override fun onItemClicked(category: Category) {
                    viewModel.currentCategory.value = category
                    drawer_layout.closeDrawers()
                }
            }
        }

        // --- Event listeners ---

        // FABメニュー表示ボタン
        entries_menu_button.setOnClickListener {
            if (isFABMenuOpened) {
                closeFABMenu()
            }
            else {
                openFABMenu()
            }
        }

        // カテゴリリスト表示ボタン
        entries_menu_categories_button.setOnClickListener {
            closeFABMenu()
            drawer_layout.openDrawer(categories_list)
        }

        // サインイン/マイブックマークボタン
        entries_menu_my_bookmarks_button.setOnClickListener {
            closeFABMenu()
            if (viewModel.signedIn.value == true) {
                // マイブックマークを表示
                viewModel.currentCategory.value = Category.MyBookmarks
            }
            else {
                // サインイン画面に遷移
                val intent = Intent(this, HatenaAuthenticationActivity::class.java)
                startActivity(intent)
            }
        }

        // 設定画面表示ボタン
        entries_menu_preferences_button.setOnClickListener {
            closeFABMenu()
            val intent = Intent(this, PreferencesActivity::class.java)
            startActivity(intent)
        }

        // --- Observers ---

        var isActionBarInitialized = false
        // コンテンツ部分のフラグメントを設定
        viewModel.currentCategory.observe(this, Observer {
            if (!isActionBarInitialized) {
                // アクションバー設定
                setSupportActionBar(toolbar)
                isActionBarInitialized = true
            }

            val fragment =
                if (it.singleColumns) SingleTabEntriesFragment.createInstance()
                else TwinTabsEntriesFragment.createInstance()

            var transaction = supportFragmentManager.beginTransaction()
                .replace(R.id.main_layout, fragment)

            if (supportFragmentManager.fragments.isNotEmpty()) {
                transaction = transaction.addToBackStack(null)
            }

            transaction.commit()
        })

        viewModel.signedIn.observe(this, Observer {
            if (it) {
                entries_menu_notices_button.show()
            }
            else {
                entries_menu_notices_button.hide()
            }
            entries_menu_notices_desc.visibility = it.toVisibility()
        })
    }

    /** 戻るボタンの挙動 */
    override fun onBackPressed() {
        if (isDrawerOpened) {
            drawer_layout.closeDrawer(categories_list)
        }
        else if (isFABMenuOpened) {
            closeFABMenu()
        }
        else {
            super.onBackPressed()
        }
    }

    /** FABメニュー各項目のオープン時移動アニメーション */
    private fun openFABMenuAnimation(layout: View, desc: View, dimenId: Int) {
        layout.visibility = View.VISIBLE
        layout.animate()
            .withEndAction {
                desc.animate()
                    .translationXBy(100f)
                    .translationX(0f)
                    .alphaBy(0.0f)
                    .alpha(1.0f)
                    .duration = 100
            }
            .translationY(-resources.getDimension(dimenId))
            .alphaBy(0.0f)
            .alpha(1.0f)
            .duration = 100
    }

    /** FABメニュー各項目のクローズ時移動アニメーション */
    private fun closeFABMenuAnimation(layout: View, desc: View) {
        if (layout.visibility != View.VISIBLE) return

        desc.animate()
            .withEndAction {
                layout.animate()
                    .withEndAction {
                        layout.visibility = View.INVISIBLE
                    }
                    .translationY(0f)
                    .alphaBy(1.0f)
                    .alpha(0.0f)
                    .duration = 100
            }
            .translationX(100f)
            .alphaBy(1.0f)
            .alpha(0.0f)
            .duration = 100
    }


    /** FABメニューを開く */
    private fun openFABMenu() {
        if (isFABMenuOpened) return

        isFABMenuOpened = true

        if (viewModel.isFABMenuBackgroundActive) {
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            entries_menu_background_guard_full.visibility = View.VISIBLE
        }

        entries_menu_background_guard.visibility = View.VISIBLE

//        if (viewModel.signedIn.value == true && viewModel.currentCategory.value != Category.Notices) {
            openFABMenuAnimation(
                entries_menu_notices_layout,
                entries_menu_notices_desc,
                R.dimen.dp_238
            )
//        }
        openFABMenuAnimation(
            entries_menu_categories_layout,
            entries_menu_categories_desc,
            R.dimen.dp_180
        )
        openFABMenuAnimation(
            entries_menu_my_bookmarks_layout,
            entries_menu_my_bookmarks_desc,
            R.dimen.dp_122
        )
        openFABMenuAnimation(
            entries_menu_settings_layout,
            entries_menu_preferences_desc,
            R.dimen.dp_64
        )

        entries_menu_button.setImageResource(R.drawable.ic_baseline_close)
    }

    /** FABメニューを閉じる */
    private fun closeFABMenu() {
        if (!isFABMenuOpened) return

        isFABMenuOpened = false
        entries_menu_background_guard_full.visibility = View.GONE
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        entries_menu_background_guard.visibility = View.GONE

        closeFABMenuAnimation(
            entries_menu_notices_layout,
            entries_menu_notices_desc
        )
        closeFABMenuAnimation(
            entries_menu_categories_layout,
            entries_menu_categories_desc
        )
        closeFABMenuAnimation(
            entries_menu_my_bookmarks_layout,
            entries_menu_my_bookmarks_desc
        )
        closeFABMenuAnimation(
            entries_menu_settings_layout,
            entries_menu_preferences_desc
        )

        entries_menu_button.setImageResource(R.drawable.ic_baseline_menu_white)
    }
}

@BindingAdapter("bind:items")
fun RecyclerView.setItems(categories: Array<Category>?) {
    if (categories == null) return

    val adapter = this.adapter as CategoriesAdapter
    adapter.submitList(categories.toList())
}

@BindingAdapter("bind:src")
fun FloatingActionButton.setSrc(resId: Int?) {
    if (resId == null) return

    try {
        setImageResource(resId)
    }
    catch (e: Throwable) {
        Log.e("resource error", Log.getStackTraceString(e))
    }
}
