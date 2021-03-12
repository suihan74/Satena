package com.suihan74.satena.scenes.preferences

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemPreferencesMenuBinding
import com.suihan74.satena.scenes.preferences.pages.*
import com.suihan74.utilities.Listener

enum class PreferencesTabMode(
    val int : Int,
    @StringRes val titleId : Int = 0,
    @DrawableRes val iconId : Int = 0,
    val createFragment : () -> Fragment = { Fragment() }
) {
    // 環状スクロールできるように細工
    DUMMY_HEAD(0),

    INFORMATION(1,
        R.string.pref_title_information,
        R.drawable.ic_baseline_info,
        { InformationFragment() }
    ),

    ACCOUNT(2,
        R.string.pref_title_account,
        R.drawable.ic_preferences_accounts,
        { AccountFragment() }
    ),

    GENERALS(3,
        R.string.pref_title_generals,
        R.drawable.ic_preferences_generals,
        { GeneralFragment() }
    ),

    ENTRIES(4,
        R.string.pref_title_entries,
        R.drawable.ic_preferences_entries,
        { EntryFragment() }
    ),

    BOOKMARKS(5,
        R.string.pref_title_bookmarks,
        R.drawable.ic_preferences_bookmarks,
        { BookmarkFragment() }
    ),

    BROWSER(6,
        R.string.pref_title_browser,
        R.drawable.ic_world,
        { BrowserFragment() }
    ),

    FAVORITE_SITES(7,
        R.string.category_favorite_sites,
        R.drawable.ic_star,
        { FavoriteSitesFragment.createInstance() }),

    IGNORED_ENTRIES(8,
        R.string.pref_title_ignored_entries,
        R.drawable.ic_preferences_filters,
        { IgnoredEntriesFragment.createInstance() }),

    IGNORED_USERS(9,
        R.string.pref_title_ignored_users,
        R.drawable.ic_preferences_ignored_users,
        { IgnoredUsersFragment.createInstance() }),

    USER_TAGS(10,
        R.string.pref_title_user_tags,
        R.drawable.ic_preferences_user_tags,
        { UserTagsFragment.createInstance() }),

    DUMMY_TAIL(11);

    companion object {
        fun fromId(i: Int) = values().firstOrNull { it.int == i } ?: INFORMATION
    }
}

// ------ //

/**
 * ViewPager2用のページアダプタ
 */
class PreferencesTabAdapter(private val activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun createFragment(position: Int): Fragment =
        PreferencesTabMode.fromId(position).createFragment.invoke()

    fun getIconId(fixedPosition: Int) =
        PreferencesTabMode.fromId(fixedPosition + 1).iconId

    fun getIndexFromIconId(iconId : Int) =
        PreferencesTabMode.values().firstOrNull { iconId == getIconId(it.int - 1) }?.int ?: 0

    override fun getItemCount(): Int = PreferencesTabMode.values().size
    fun getActualCount() = itemCount - 2

    fun findFragment(position: Int) : Fragment? = activity.supportFragmentManager.findFragmentByTag("f$position")
}

// ------ //

/**
 * メニューアイコン表示用のリストアダプタ
 */
class PreferencesMenuAdapter(
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<PreferencesMenuAdapter.ViewHolder>() {

    private val items = PreferencesTabMode.values()
        .filter { it.iconId != 0 }
        .map { Item(it, MutableLiveData(false)) }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListviewItemPreferencesMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).also {
            it.lifecycleOwner = lifecycleOwner
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.item = items[position]

        val root = holder.binding.root
        root.setOnClickListener {
            onClick?.invoke(items[position].tab)
        }
        TooltipCompat.setTooltipText(
            root,
            root.context.getText(items[position].tab.titleId)
        )
    }

    // ------ //

    fun selectTab(tab: PreferencesTabMode) {
        items.forEach {
            it.selected.value = it.tab == tab
        }
    }

    // ------ //

    private var onClick :Listener<PreferencesTabMode>? = null

    fun setOnClickListener(listener: Listener<PreferencesTabMode>?) {
        onClick = listener
    }

    // ------ //

    class ViewHolder(
        val binding: ListviewItemPreferencesMenuBinding
    ) : RecyclerView.ViewHolder(binding.root)

    data class Item(
        val tab : PreferencesTabMode,
        val selected : MutableLiveData<Boolean>
    )
}
