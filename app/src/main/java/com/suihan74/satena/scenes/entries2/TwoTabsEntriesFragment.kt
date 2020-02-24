package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.suihan74.satena.R

class TwoTabsEntriesFragment : Fragment() {
    companion object {
        fun createInstance() = TwoTabsEntriesFragment()
    }

    /** EntriesActivityのViewModel */
    private lateinit var activityViewModel: EntriesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProviders.of(requireActivity())[EntriesViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_entries, container, false)

        return view
    }
}
