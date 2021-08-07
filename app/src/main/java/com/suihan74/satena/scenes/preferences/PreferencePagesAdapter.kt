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

enum class PreferencesTab(
    @StringRes val titleId : Int = 0,
    @DrawableRes val iconId : Int = 0,
    val createFragment : () -> Fragment = { Fragment() }
) {
    // 環状スクロールできるように細工
    DUMMY_HEAD,

    INFORMATION(
        R.string.pref_title_information,
        R.drawable.ic_baseline_info,
        { InformationFragment() }
    ),

    ACCOUNT(
        R.string.pref_title_account,
        R.drawable.ic_preferences_accounts,
        { AccountFragment() }
    ),

    GENERALS(
        R.string.pref_title_generals,
        R.drawable.ic_preferences_generals,
        { GeneralFragment() }
    ),

    ENTRIES(
        R.string.pref_title_entries,
        R.drawable.ic_preferences_entries,
        { EntryFragment() }
    ),

    BOOKMARKS(
        R.string.pref_title_bookmarks,
        R.drawable.ic_preferences_bookmarks,
        { BookmarkFragment() }
    ),

    BROWSER(
        R.string.pref_title_browser,
        R.drawable.ic_world,
        { BrowserFragment() }
    ),

    FAVORITE_SITES(
        R.string.category_favorite_sites,
        R.drawable.ic_star,
        { FavoriteSitesFragment.createInstance() }
    ),

    IGNORED_ENTRIES(
        R.string.pref_title_ignored_entries,
        R.drawable.ic_preferences_filters,
        { IgnoredEntriesFragment2.createInstance() }
    ),

    IGNORED_USERS(
        R.string.pref_title_ignored_users,
        R.drawable.ic_preferences_ignored_users,
        { IgnoredUsersFragment.createInstance() }
    ),

    FOLLOWED_USERS(
        R.string.pref_title_followings,
        R.drawable.ic_category_followings,
        { FollowingUsersFragment.createInstance() }
    ),

    USER_TAGS(
        R.string.pref_title_user_tags,
        R.drawable.ic_preferences_user_tags,
        { UserTagsFragment.createInstance() }
    ),

    DUMMY_TAIL;

    companion object {
        fun fromOrdinal(idx: Int) = values().getOrElse(idx) { INFORMATION }
    }
}

// ------ //

/**
 * ViewPager2用のページアダプタ
 */
class PreferencesTabAdapter(private val activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun createFragment(position: Int): Fragment =
        PreferencesTab.fromOrdinal(position).createFragment.invoke()

    fun getIconId(fixedPosition: Int) =
        PreferencesTab.fromOrdinal(fixedPosition + 1).iconId

    fun getIndexFromIconId(iconId : Int) =
        PreferencesTab.values().firstOrNull { iconId == getIconId(it.ordinal - 1) }?.ordinal ?: 0

    override fun getItemCount(): Int = PreferencesTab.values().size
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

    private val items = PreferencesTab.values()
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

    fun selectTab(tab: PreferencesTab) {
        items.forEach {
            it.selected.value = it.tab == tab
        }
    }

    // ------ //

    private var onClick :Listener<PreferencesTab>? = null

    fun setOnClickListener(listener: Listener<PreferencesTab>?) {
        onClick = listener
    }

    // ------ //

    class ViewHolder(
        val binding: ListviewItemPreferencesMenuBinding
    ) : RecyclerView.ViewHolder(binding.root)

    data class Item(
        val tab : PreferencesTab,
        val selected : MutableLiveData<Boolean>
    )
}
