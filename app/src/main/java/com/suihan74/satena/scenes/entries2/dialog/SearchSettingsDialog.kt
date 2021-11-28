package com.suihan74.satena.scenes.entries2.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.*
import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogEntrySearchSettingsBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.NumberPickerDialog
import com.suihan74.satena.models.EntrySearchDateMode
import com.suihan74.satena.models.EntrySearchSetting
import com.suihan74.satena.models.orDefault
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SearchSettingsDialog : BottomSheetDialogFragment() {
    companion object {
        fun createInstance(repository: EntriesRepository) = SearchSettingsDialog().also { f ->
            f.lifecycleScope.launchWhenCreated {
                f.viewModel.initialize(repository)
            }
        }
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        DialogViewModel()
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                dialog.alsoAs<BottomSheetDialog> { d ->
                    BottomSheetBehavior
                        .from(d.findViewById<FrameLayout>(R.id.design_bottom_sheet)!!)
                        .state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDialogEntrySearchSettingsBinding.inflate(
            layoutInflater,
            container,
            false
        ).also {
            it.fragment = this
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.okButton.setOnClickListener {
            lifecycleScope.launchWhenCreated {
                viewModel.save()
                dismiss()
            }
        }

        return binding.root
    }

    // ------ //

    class DialogViewModel : ViewModel() {
        private lateinit var repository: EntriesRepository

        val searchType = MutableLiveData<SearchType>()

        val users = MutableLiveData<Int>()

        /** 期間指定方法 */
        private val dateMode = MutableStateFlow(EntrySearchDateMode.RECENT)

        /** 期間指定: 開始日 */
        private val dateBeginFlow = MutableStateFlow<LocalDate?>(null)

        /** 期間指定: 終了日 */
        private val dateEndFlow = MutableStateFlow<LocalDate?>(null)

        /** 期間指定ボタンの表示文字列 */
        private val _dateStr = MutableLiveData<String>()
        val dateStr : LiveData<String> = _dateStr

        val safe = MutableLiveData<Boolean>()

        // ------ //

        init {
            dateBeginFlow
                .combine(dateEndFlow, ::Pair)
                .combine(dateMode) { (begin, end), mode ->
                    if (begin == null && end == null) "未設定"
                    else when (mode) {
                        EntrySearchDateMode.RECENT -> {
                            "直近${Duration.between(begin!!.atStartOfDay(), end!!.atStartOfDay()).toDays()}日間"
                        }

                        EntrySearchDateMode.CALENDAR -> buildString {
                            DateTimeFormatter.ofPattern("yy-MM-dd").let { formatter ->
                                append(begin?.format(formatter) ?: "未設定")
                                append(" ～ ")
                                append(end?.format(formatter) ?: "未設定")
                            }
                        }
                    }
                }
                .onEach { _dateStr.value = it }
                .launchIn(viewModelScope)
        }

        suspend fun initialize(repo: EntriesRepository) = withContext(Dispatchers.Main.immediate) {
            repository = repo.also { r ->
                r.searchSetting.value?.let {
                    searchType.value = it.searchType
                    users.value = it.users
                    dateMode.value = it.dateMode
                    dateBeginFlow.emit(it.dateBegin)
                    dateEndFlow.emit(it.dateEnd)
                    safe.value = it.safe
                }
            }
        }

        suspend fun save() = withContext(Dispatchers.Main.immediate) {
            repository.searchSetting.value = EntrySearchSetting(
                searchType.value ?: SearchType.Tag,
                users.value ?: 1,
                dateMode.value.orDefault,
                dateBeginFlow.value,
                dateEndFlow.value,
                safe.value ?: false
            )
        }

        // ------ //

        fun openSearchTypePicker(fragment: Fragment) {
            val items = SearchType.values()
            val labels = items.map { fragment.getString(it.textId) }
            val selected = items.indexOf(searchType.value ?: SearchType.Tag)

            AlertDialogFragment.Builder()
                .setTitle(R.string.desc_search_type)
                .setSingleChoiceItems(labels, selected) { dialog, which ->
                    searchType.value = items[which]
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_cancel)
                .dismissOnRestore()
                .create()
                .show(fragment.childFragmentManager, null)
        }

        fun openUsersPicker(fragment: Fragment) {
            NumberPickerDialog
                .createInstance(1, 500, users.value ?: 1, R.string.entry_search_settings_min_bookmarks_dialog_title)
                .setOnCompleteListener {
                    users.value = it
                }
                .show(fragment.childFragmentManager, null)
        }

        fun openDatePicker(fragment: Fragment) {
            val values = EntrySearchDateMode.values()
            val labels = values.map { it.textId }
            AlertDialogFragment.Builder()
                .setTitle(R.string.entry_search_settings_date_mode_title)
                .setItems(labels) { f, which ->
                    when (values[which]) {
                        EntrySearchDateMode.RECENT -> openDateRangePicker(f.parentFragmentManager)
                        EntrySearchDateMode.CALENDAR -> openCalendar(f.parentFragmentManager)
                    }
                }
                .setNegativeButton(R.string.dialog_cancel)
                .dismissOnRestore()
                .create()
                .show(fragment.childFragmentManager, null)
        }

        private fun openDateRangePicker(fragmentManager: FragmentManager) {
            val today = LocalDateTime.now()
            val oldest = LocalDateTime.of(2005, 2, 10, 0, 0, 0)
            NumberPickerDialog
                .createInstance(
                    0, Duration.between(oldest, today).toDays().toInt(), 0,
                    R.string.entry_search_date_picker_title,
                    R.string.entry_search_date_picker_desc,
                )
                .setOnCompleteListener {
                    viewModelScope.launch {
                        dateMode.emit(EntrySearchDateMode.RECENT)
                        dateBeginFlow.emit(today.minusDays(it.toLong()).toLocalDate())
                        dateEndFlow.emit(today.toLocalDate())
                    }
                }
                .show(fragmentManager, null)
        }

        private fun openCalendar(fragmentManager: FragmentManager) {
            MaterialDatePicker.Builder.dateRangePicker()
                .apply {
                    // はてブのサービス開始（2005-02-10）～今日の日付までに制限
                    val validators = listOf(
                        DateValidatorPointBackward.before(
                            LocalDate.now().atTime(23, 59).toEpochSecond(ZoneOffset.UTC) * 1000
                        ),
                        DateValidatorPointForward.from(
                            LocalDateTime.of(2005, 2, 10, 0, 0).toEpochSecond(ZoneOffset.UTC) * 1000
                        )
                    )
                    setCalendarConstraints(
                        CalendarConstraints.Builder()
                            .setValidator(CompositeDateValidator.allOf(validators))
                            .build()
                    )

                    // 初期値を設定
                    dateBeginFlow.value?.let { begin ->
                        val beginTime = begin.atTime(0, 0).toEpochSecond(ZoneOffset.UTC) * 1000
                        val endTime = (dateEndFlow.value ?: LocalDate.now()).atTime(0, 0).toEpochSecond(ZoneOffset.UTC) * 1000
                        setSelection(androidx.core.util.Pair(beginTime, endTime))
                    }
                }
                .build()
                .apply {
                    addOnPositiveButtonClickListener { result ->
                        viewModelScope.launch {
                            dateMode.emit(EntrySearchDateMode.CALENDAR)
                            dateBeginFlow.emit(
                                LocalDateTime.ofEpochSecond(result.first / 1000, 0, ZoneOffset.UTC)
                                    .toLocalDate()
                            )
                            dateEndFlow.emit(
                                LocalDateTime.ofEpochSecond(result.second / 1000, 0, ZoneOffset.UTC)
                                    .toLocalDate()
                            )
                        }
                    }
                }
                .show(fragmentManager, null)
        }

        fun clearDate() = viewModelScope.launch {
            dateBeginFlow.emit(null)
            dateEndFlow.emit(null)
        }
    }
}
