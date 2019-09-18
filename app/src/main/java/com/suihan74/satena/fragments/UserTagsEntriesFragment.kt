package com.suihan74.satena.fragments

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.HatenaLib.Tag
import com.suihan74.satena.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserTagsEntriesFragment : MultipurposeSingleTabEntriesFragment() {
    private lateinit var mUser : String
    private var mTags : List<Tag> = emptyList()
    private var mSelectedTag : Tag? = null

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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!

        val toolbar = root.findViewById<Toolbar>(R.id.toolbar)

        val spinnerContainer = inflater.inflate(R.layout.actionbar_spinner, toolbar, false)
        toolbar.addView(spinnerContainer)

        spinnerContainer.findViewById<TextView>(R.id.title).apply {
            text = String.format("%s/", mUser)
        }
        val spinner = spinnerContainer.findViewById<Spinner>(R.id.spinner)

        showProgressBar()

        launch(Dispatchers.Main) {
            if (mTags.isEmpty()) {
                mTags = HatenaClient.getUserTagsAsync(mUser).await()
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
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
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
        }
        return root
    }

    override fun refreshEntries() =
        super.refreshEntries { offset -> HatenaClient.searchMyEntriesAsync(mSelectedTag!!.text, SearchType.Tag, of = offset) }
}
