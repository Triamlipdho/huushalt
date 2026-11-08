package xyz.haselbach.huushalt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.data.ChecklistDraft
import xyz.haselbach.huushalt.data.ChoreRepository
import xyz.haselbach.huushalt.data.CompletionEntity
import xyz.haselbach.huushalt.data.CompletionWithName
import xyz.haselbach.huushalt.data.toRecord
import xyz.haselbach.huushalt.domain.Area
import xyz.haselbach.huushalt.domain.ChecklistItem
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.DueStatus
import xyz.haselbach.huushalt.domain.Holiday
import xyz.haselbach.huushalt.domain.Progress
import xyz.haselbach.huushalt.domain.Stats
import xyz.haselbach.huushalt.domain.Streaks
import xyz.haselbach.huushalt.reminders.OpenChores
import xyz.haselbach.huushalt.domain.Schedule

data class HomeState(
    val today: LocalDate,
    val hasChores: Boolean,
    val overdue: List<Chore>,
    val dueToday: List<Chore>,
    val upcoming: List<Chore>,
    /** Chores done today, for the progress "x von y erledigt" (F7.1). */
    val doneToday: Int,
    /** Holiday mode right now (F10.1). */
    val activeHoliday: Holiday? = null,
    /** Next planned holiday, if any. */
    val plannedHoliday: Holiday? = null,
) {
    val open: Int get() = overdue.size + dueToday.size
    val total: Int get() = doneToday + open
}

/** Streaks, statistics and the activity log (F6.3, F7.2 to F7.4). */
data class ProgressState(
    val streaks: Streaks,
    val stats: Stats,
    /** Most recent completions with chore names, newest first. */
    val recent: List<CompletionWithName>,
    /** Per chore: completions in a row done on time (F7.3). */
    val choreStreaks: Map<Long, Int>,
)

sealed interface UiEvent {
    /** [allDoneToday]: this was the last open chore of the day, time to celebrate (F7.5). */
    data class Completed(val choreName: String, val completionId: Long, val allDoneToday: Boolean) : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChoresViewModel(private val repository: ChoreRepository) : ViewModel() {

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** All chores; null while loading. */
    val chores: StateFlow<List<Chore>?> = repository.chores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val doneToday: Flow<Set<Long>> = _today.flatMapLatest { repository.choreIdsDoneOn(it) }

    /** Home screen state; null while loading. */
    val home: StateFlow<HomeState?> = combine(repository.chores, doneToday, _today, repository.holidays) { chores, done, today, holidays ->
        val order = compareBy<Chore> { it.dueDate }.thenBy { it.name.lowercase() }
        val statuses = chores.associateWith { it.status(today) }
        fun matching(predicate: (DueStatus) -> Boolean) =
            chores.filter { predicate(statuses.getValue(it)) }.sortedWith(order)
        HomeState(
            today = today,
            hasChores = chores.isNotEmpty(),
            overdue = matching { it is DueStatus.Overdue },
            dueToday = matching { it is DueStatus.Today },
            upcoming = matching { it is DueStatus.Upcoming },
            doneToday = done.size,
            activeHoliday = holidays.firstOrNull { it.isActive(today) },
            plannedHoliday = holidays.firstOrNull { it.isPlanned(today) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Null while loading. */
    val progress: StateFlow<ProgressState?> =
        combine(
            repository.chores,
            repository.allCompletions,
            _today,
            repository.holidays,
            repository.missedPeriods,
        ) { chores, completions, today, holidays, missed ->
            val records = completions.map { it.completion.toRecord() }
            val start = chores.minOfOrNull { it.createdDate }
            val streaks = if (start == null) {
                Streaks(0, 0)
            } else {
                Progress.streaks(
                    overdueDays = Progress.overdueDays(
                        records,
                        chores.filterNot { it.paused }.mapNotNull { it.dueDate },
                        start,
                        today,
                        missed,
                    ),
                    start = start,
                    today = today,
                    todayClean = OpenChores.of(chores, today).isEmpty,
                    holidayDays = holidays.flatMap { it.daysUntil(today).toList() }.toSet(),
                )
            }
            val byChore = records.groupBy { it.choreId }
            ProgressState(
                streaks = streaks,
                stats = Progress.stats(records, today),
                recent = completions.take(RECENT_COUNT),
                choreStreaks = chores.associate { chore ->
                    chore.id to Progress.choreStreak(byChore[chore.id].orEmpty(), chore.status(today) is DueStatus.Overdue)
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val events = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<UiEvent> = events.receiveAsFlow()

    /** Called when the app comes to the foreground, so "today" is right after midnight. */
    fun refreshToday() {
        _today.value = LocalDate.now()
        viewModelScope.launch { repository.applyFinishedHolidays(LocalDate.now()) }
    }

    /** "Neu planen ab heute" (F10.6). */
    fun rescheduleOverdue() {
        viewModelScope.launch { repository.rescheduleOverdue() }
    }

    fun saveHoliday(id: Long?, start: LocalDate, end: LocalDate) {
        viewModelScope.launch { repository.saveHoliday(id, start, end) }
    }

    fun endHoliday(id: Long) {
        viewModelScope.launch { repository.endHoliday(id) }
    }

    fun deleteHoliday(id: Long) {
        viewModelScope.launch { repository.deleteHoliday(id) }
    }

    fun completions(choreId: Long): Flow<List<CompletionEntity>> = repository.completions(choreId)

    fun checklist(choreId: Long): Flow<List<ChecklistItem>> = repository.checklist(choreId)

    val areas: StateFlow<List<Area>?> = repository.areas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setChecked(itemId: Long, checked: Boolean) {
        viewModelScope.launch { repository.setChecked(itemId, checked) }
    }

    fun setPaused(choreId: Long, paused: Boolean) {
        viewModelScope.launch { repository.setPaused(choreId, paused) }
    }

    fun addArea(name: String) {
        viewModelScope.launch { repository.addArea(name) }
    }

    fun renameArea(id: Long, name: String) {
        viewModelScope.launch { repository.renameArea(id, name) }
    }

    fun deleteArea(id: Long) {
        viewModelScope.launch { repository.deleteArea(id) }
    }

    fun complete(chore: Chore) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val wasOpen = chore.status(today).let { it is DueStatus.Overdue || it == DueStatus.Today }
            val completionId = repository.complete(chore.id, today) ?: return@launch
            val allDone = wasOpen && OpenChores.of(repository.chores.first(), today).isEmpty
            events.send(UiEvent.Completed(chore.name, completionId, allDone))
        }
    }

    fun changeCompletionDate(completionId: Long, date: LocalDate) {
        viewModelScope.launch { repository.changeCompletionDate(completionId, date) }
    }

    fun deleteCompletion(completionId: Long) {
        viewModelScope.launch { repository.deleteCompletion(completionId) }
    }

    fun undoCompletion(completionId: Long) {
        viewModelScope.launch { repository.undoCompletion(completionId) }
    }

    fun postpone(choreId: Long, date: LocalDate) {
        viewModelScope.launch { repository.postpone(choreId, date) }
    }

    fun save(
        id: Long?,
        name: String,
        notes: String,
        schedule: Schedule,
        nextDue: LocalDate,
        areaId: Long?,
        checklist: List<ChecklistDraft>,
        newAreaName: String? = null,
    ) {
        viewModelScope.launch {
            val area = if (!newAreaName.isNullOrBlank()) repository.addArea(newAreaName) else areaId
            repository.save(id, name, notes, schedule, nextDue, area, checklist)
        }
    }

    fun delete(choreId: Long) {
        viewModelScope.launch { repository.delete(choreId) }
    }

    companion object {
        private const val RECENT_COUNT = 100

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HuushaltApplication
                ChoresViewModel(app.repository)
            }
        }
    }
}
