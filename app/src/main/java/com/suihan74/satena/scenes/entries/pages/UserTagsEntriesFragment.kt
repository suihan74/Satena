package com.suihan74.satena.scenes.entries.pages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.HatenaLib.Tag
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries.MultipurposeSingleTabEntriesFragment
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserTagsEntriesFragment : MultipurposeSingleTabEntriesFragment() {
    private lateinit var mUser : String
    private var mTags : List<Tag> = emptyList()
    private var mSelectedTag : Tag? = null

    override val title: String
        get() = String.format("%sのタグ", mUser)

    companion object {
        fun createInstance(user: String) = UserTagsEntriesFragment().apply {
            mUser = user
        }

        private const val BUNDLE_USER = "mUser"
        private const val BUNDLE_TAGS = "mTags"
        private const val BUNDLE_SELECTED_TAG = "mSelectedTag"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_USER, mUser)
        outState.putSerializable(BUNDLE_TAGS, ArrayList(mTags))
        outState.putSerializable(BUNDLE_SELECTED_TAG, mSelectedTag)
    }

    override fun onRestoreSaveInstanceState(savedInstanceState: Bundle) {
        super.onViewStateRestored(savedInstanceState)
        mUser = savedInstanceState.getString(BUNDLE_USER)!!
        @Suppress("UNCHECKED_CAST")
        mTags = savedInstanceState.getSerializable(BUNDLE_TAGS) as List<Tag>
        mSelectedTag = savedInstanceState.getSerializable(BUNDLE_SELECTED_TAG) as Tag?

        if (mSelectedTag != null) {
            refreshEntries()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        val spinner = showSpinner()
        launch(Dispatchers.Main) {
            initializeSpinnerItems(spinner)
        }
        return root
    }

    private suspend fun initializeSpinnerItems(spinner: Spinner) = withContext(Dispatchers.Main) {
        if (mTags.isEmpty()) {
            showProgressBar()
            try {
                mTags = HatenaClient.getUserTagsAsync(mUser).await()
            }
            catch(e: Exception) {
                Log.e("InitializeSpinner", e.message)
                activity?.showToast("タグリストの取得に失敗しました")
            }
        }

        if (mTags.isNotEmpty()) {
            mSelectedTag = mSelectedTag ?: mTags[0]

            spinner.apply {
                adapter = ArrayAdapter(
                    context!!,
                    android.R.layout.simple_spinner_item,
                    mTags.map { "${it.text}  (${it.count})" }.toTypedArray()
                ).apply {
                    setDropDownViewResource(R.layout.spinner_drop_down_item)
                }

                if (mSelectedTag != null) {
                    val pos = mTags.indexOfFirst { it.text == mSelectedTag!!.text }
                    setSelection(pos, false)
                }

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val newSelectedTag = mTags[position]
                        if (newSelectedTag != mSelectedTag) {
                            mSelectedTag = newSelectedTag
                            refreshEntries()
                        }
                        else {
                            mSelectedTag = newSelectedTag
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
            }

            refreshEntries()
        }

        hideProgressBar()

        return@withContext
    }

    override fun refreshEntries() =
        super.refreshEntries { offset -> HatenaClient.searchMyEntriesAsync(mSelectedTag!!.text, SearchType.Tag, of = offset) }
}
