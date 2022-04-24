package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.databinding.FragmentListPreferencesBinding
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.letAs
import com.suihan74.utilities.extensions.simulateRippleEffect

/**
 * 設定リスト画面共通フラグメント
 */
abstract class ListPreferencesFragment : Fragment() {
    val preferencesActivity
        get() = requireActivity() as PreferencesActivity

    // ------ //

    abstract val viewModel : ListPreferencesViewModel

    private var binding : FragmentListPreferencesBinding? = null

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentListPreferencesBinding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
            initializeRecyclerView(it.recyclerView)
        }
        this.binding = binding
        viewModel.onCreateView(this)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    // ------ //

    private fun initializeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = PreferencesAdapter(viewLifecycleOwner)
    }

    // ------ //

    fun scrollTo(item: PreferencesAdapter.Item) {
        binding?.recyclerView?.let { recyclerView ->
            val index = recyclerView.adapter.letAs<PreferencesAdapter, Int> { adapter ->
                adapter.currentList.indexOfFirst {
                    it.areItemsTheSame(it, item)
                }
            } ?: return@let

            runCatching {
                highlightItem(recyclerView, index)
            }.onFailure {
                Log.e("ListPreferences", it.stackTraceToString())
            }
        }
    }

    private fun highlightItem(view: RecyclerView, index: Int) {
        view.smoothScrollToPosition(index)
        view.findViewHolderForAdapterPosition(index).alsoAs<PreferencesAdapter.ViewHolder> {
            // スクロール不要の場合
            it.binding.root.simulateRippleEffect(250L, 2)
        } ?: run {
            // 画面上にないViewHolderまでスクロールしてからアニメーション
            view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                    view.findViewHolderForAdapterPosition(index).alsoAs<PreferencesAdapter.ViewHolder> {
                        it.binding.root.simulateRippleEffect(
                            duration = 250L,
                            times = 2,
                            delay = 100L
                        )
                    }
                    view.removeOnScrollListener(this)
                }
            })
        }
    }
}
