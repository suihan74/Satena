package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityEntries2Binding
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences
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
                        prefs = prefs,
                        historyPrefs = SafeSharedPreferences.create(this)
                    )
                )
                ViewModelProviders.of(this, factory)[EntriesViewModel::class.java]
            }
            else {
                ViewModelProviders.of(this)[EntriesViewModel::class.java]
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

        // FABメニュー表示ボタン
        entries_menu_button.setOnClickListener {
            if (isFABMenuOpened) {
                closeFABMenu()
            }
            else {
                openFABMenu()
            }
        }
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
            if (!viewModel.onBackPressed()) {
                super.onBackPressed()
            }
        }
    }

    private fun openFABMenuAnimation(layoutId: Int, descId: Int, dimenId: Int) {
        val layout = findViewById<View>(layoutId)
        val desc = findViewById<View>(descId)

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

    private fun closeFABMenuAnimation(layoutId: Int, descId: Int) {
        val layout = findViewById<View>(layoutId)
        val desc = findViewById<View>(descId)

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


    private fun openFABMenu() {
        if (isFABMenuOpened) return

        isFABMenuOpened = true

        if (viewModel.isFABMenuBackgroundActive) {
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            entries_menu_background_guard_full.visibility = View.VISIBLE
        }

        val clickGuard = findViewById<View>(R.id.entries_menu_background_guard)
        clickGuard.visibility = View.VISIBLE

        if (HatenaClient.signedIn()/* && currentFragment !is NoticesFragment*/) {
            openFABMenuAnimation(
                R.id.entries_menu_notices_layout,
                R.id.entries_menu_notices_desc,
                R.dimen.dp_238
            )
        }
        openFABMenuAnimation(
            R.id.entries_menu_categories_layout,
            R.id.entries_menu_categories_desc,
            R.dimen.dp_180
        )
        openFABMenuAnimation(
            R.id.entries_menu_my_bookmarks_layout,
            R.id.entries_menu_my_bookmarks_desc,
            R.dimen.dp_122
        )
        openFABMenuAnimation(
            R.id.entries_menu_settings_layout,
            R.id.entries_menu_preferences_desc,
            R.dimen.dp_64
        )

        val menuButton = findViewById<FloatingActionButton>(R.id.entries_menu_button)
        menuButton.setImageResource(R.drawable.ic_baseline_close)
    }

    private fun closeFABMenu() {
        if (!isFABMenuOpened) return

        isFABMenuOpened = false
        entries_menu_background_guard_full.visibility = View.GONE
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        val clickGuard = findViewById<View>(R.id.entries_menu_background_guard)
        clickGuard.visibility = View.GONE

        closeFABMenuAnimation(
            R.id.entries_menu_notices_layout,
            R.id.entries_menu_notices_desc
        )
        closeFABMenuAnimation(
            R.id.entries_menu_categories_layout,
            R.id.entries_menu_categories_desc
        )
        closeFABMenuAnimation(
            R.id.entries_menu_my_bookmarks_layout,
            R.id.entries_menu_my_bookmarks_desc
        )
        closeFABMenuAnimation(
            R.id.entries_menu_settings_layout,
            R.id.entries_menu_preferences_desc
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
