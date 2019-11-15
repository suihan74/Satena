package com.suihan74.satena.scenes.entries.pages

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.MaintenanceEntry
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.MaintenanceEntriesAdapter
import com.suihan74.utilities.CoroutineScopeFragment
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.showToast
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MaintenanceInformationFragment : CoroutineScopeFragment() {

    @Parcelize
    data class Members (
        var entries: List<MaintenanceEntry>? = null
    ) : Parcelable

    private var members =
        Members()

    override val title: String
        get() = SatenaApplication.instance.getString(R.string.category_maintenance)

    val currentCategory = Category.Maintenance

    companion object {
        fun createInstance() =
            MaintenanceInformationFragment()

        private const val BUNDLE_MEMBERS = "members"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(BUNDLE_MEMBERS, members)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.run {
            members = getParcelable(BUNDLE_MEMBERS) ?: return@run
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_entries_tab, container, false)
        val activity = activity as ActivityBase

        // エントリーリストの設定
        val entriesList = root.findViewById<RecyclerView>(R.id.entries_list).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = null

            val dividerItemDecoration = DividerItemDecorator(
                ContextCompat.getDrawable(context, R.drawable.recycler_view_item_divider)!!)
            addItemDecoration(dividerItemDecoration)
        }

        launch(Dispatchers.Main) {
            activity.showProgressBar()

            try {
                val entries = HatenaClient.getMaintenanceEntriesAsync().await()
                entriesList.adapter =
                    MaintenanceEntriesAdapter(entries)
            }
            catch (e: Exception) {
                Log.e("FetchingFailure", Log.getStackTraceString(e))
                activity.showToast("障害情報の取得に失敗")
            }
            finally {
                activity.hideProgressBar()
            }
        }

        // スワイプ更新機能の設定
        root.findViewById<SwipeRefreshLayout>(R.id.entries_swipe_layout).apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(ContextCompat.getColor(activity, R.color.colorPrimary))
            setOnRefreshListener {
                launch(Dispatchers.Main) {
                    try {
                    }
                    catch (e: Exception) {
                        Log.e("FetchingFailure", Log.getStackTraceString(e))
                        activity.showToast("更新失敗")
                    }
                    finally {
                        this@swipeLayout.isRefreshing = false
                    }
                }
            }
        }

        return root
    }

}
