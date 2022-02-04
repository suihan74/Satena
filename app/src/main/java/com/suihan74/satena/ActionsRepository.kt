package com.suihan74.satena

import com.suihan74.hatenaLib.Entry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull

interface Action

data class UpdateEntryAction(
    val entry : Entry
) : Action

class ActionsRepository {
    private val actionsFlow = MutableSharedFlow<Action>()
    val updateEntryActionFlow = actionsFlow.mapNotNull { it as? UpdateEntryAction }

    suspend fun emitUpdatingEntry(entry: Entry) {
        actionsFlow.emit(UpdateEntryAction(entry))
    }
}
