package com.suihan74.satena.scenes.entries.pages

import android.net.Uri
import android.os.Bundle
import android.view.*
import com.suihan74.HatenaLib.EntriesType
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.AlertDialogListener
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.scenes.entries.SingleTabEntriesFragmentBase

class SiteEntriesFragment : SingleTabEntriesFragmentBase(), AlertDialogListener {
    /** 表示順 */
    private enum class EntriesOrder(
        val description: String,
        val entriesType: EntriesType,
        val allMode: Boolean = false
    ) {
        Hot("人気", EntriesType.Hot),
        Recent("新着", EntriesType.Recent),
        All("すべて", EntriesType.Recent, allMode = true)
    }

    private lateinit var mRoot : View
    private var mEntriesOrderMenuItem : MenuItem? = null
    private lateinit var mRootUrl : String
    private var mEntriesOrder : EntriesOrder = EntriesOrder.Recent

    override val title : String
        get() = "「${Regex("""https?://(.+)/$""").find(mRootUrl)?.groupValues?.get(1) ?: Uri.parse(mRootUrl).host}」のエントリ"

    override val currentCategory = Category.Site

    /** 次に読み込むページ */
    private var nextPage = 1

    companion object {
        fun createInstance(rootUrl: String) = SiteEntriesFragment().apply {
            mRootUrl = rootUrl
        }

        private const val BUNDLE_QUERY = "mRootUrl"
        private const val BUNDLE_ENTRIES_TYPE = "mEntriesType"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_QUERY, mRootUrl)
        outState.putInt(BUNDLE_ENTRIES_TYPE, mEntriesOrder.ordinal)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRoot = super.onCreateView(inflater, container, savedInstanceState)!!

        savedInstanceState?.let {
            mRootUrl = it.getString(BUNDLE_QUERY)!!
            mEntriesOrder = EntriesOrder.values()[it.getInt(BUNDLE_ENTRIES_TYPE)]
        }

        setHasOptionsMenu(true)

        return mRoot
    }

    override fun onResume() {
        super.onResume()

        if (entriesCount == 0) {
            refreshEntries()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.site_entries, menu)

        val activity = activity as? EntriesActivity
        activity?.expandAppBar()

        mEntriesOrderMenuItem = menu.findItem(R.id.entries_order)?.apply {
            title = mEntriesOrder.description
            setOnMenuItemClickListener {
                val items = EntriesOrder.values().map { it.description }.toTypedArray()

                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.desc_site_entries_order)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setSingleChoiceItems(items, EntriesOrder.values().indexOf(mEntriesOrder))
                    .show(childFragmentManager, "entries_order_dialog")

                return@setOnMenuItemClickListener true
            }
        }
    }

    override fun refreshEntries() {
        (activity as? EntriesActivity)?.updateToolbar()
        super.refreshEntries("エントリ取得失敗") { offset ->
            val page = if (offset == null) 1 else nextPage
            HatenaClient.getEntriesAsync(mRootUrl, mEntriesOrder.entriesType, allMode = mEntriesOrder.allMode, page = page).apply {
                invokeOnCompletion {
                    nextPage = page + 1
                }
            }
        }
    }

    override fun onSingleChoiceItem(dialog: AlertDialogFragment, which: Int) {
        val newEntriesOrder = EntriesOrder.values()[which]
        mEntriesOrderMenuItem?.title = newEntriesOrder.description
        if (newEntriesOrder != mEntriesOrder) {
            mEntriesOrder = newEntriesOrder
            refreshEntries()
        }
        else {
            mEntriesOrder = newEntriesOrder
        }
        dialog.dismiss()
    }
}
