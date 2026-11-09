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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.data.ChecklistDraft
import xyz.haselbach.huushalt.data.ChoreRepository
import xyz.haselbach.huushalt.data.ChoreSnapshot
import xyz.haselbach.huushalt.data.CompletionEntity
import xyz.haselbach.huushalt.data.CompletionWithName
import xyz.haselbach.huushalt.data.toRecord
import xyz.haselbach.huushalt.domain.Area
import xyz.haselbach.huushalt.domain.ChecklistItem
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.ChoreColor
import xyz.haselbach.huushalt.domain.ChoreSymbol
import xyz.haselbach.huushalt.domain.ChoreTemplate
import xyz.haselbach.huushalt.domain.DueStatus
import xyz.haselbach.huushalt.domain.Holiday
import xyz.haselbach.huushalt.domain.Progress
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Stats
import xyz.haselbach.huushalt.domain.Streaks
import xyz.haselbach.huushalt.reminders.OpenChores
import xyz.haselbach.huushalt.settings.AppSettings
import xyz.haselbach.huushalt.domain.Schedule

data class HomeState(
    val today: LocalDate,
    val hasChores: Boolean,
    val overdue: List<Chore>,
    val dueToday: List<Chore>,
    val upcoming: List<Chore>,
    /** Chores done today, for the progress "x von y erledigt" (F7.1). */
    val doneToday: Int,
    /** What was done today, newest last, so it can be seen and undone on the home screen. */
    val doneTodayList: List<DoneToday> = emptyList(),
    /** Holiday mode right now (F10.1). */
    val activeHoliday: Holiday? = null,
    /** Next planned holiday, if any. */
    val plannedHoliday: Holiday? = null,
    /** Chores with a date list that has at most one date left (F2.11). */
    val datesRunningOut: List<Chore> = emptyList(),
) {
    val open: Int get() = overdue.size + dueToday.size
    val total: Int get() = doneToday + open
}

/** A chore done today, with the completion to undo. */
data class DoneToday(val chore: Chore, val completionId: Long)

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

    /** A change that can be undone by restoring [snapshot]; shown as a snackbar with "Rückgängig". */
    data class Changed(val kind: ChangeKind, val snapshot: ChoreSnapshot, val date: LocalDate? = null) : UiEvent {
        val choreName: String get() = snapshot.chores.firstOrNull()?.name.orEmpty()
        val count: Int get() = snapshot.chores.size
    }
}

enum class ChangeKind { SKIPPED, POSTPONED, PAUSED, RESUMED, DELETED, COMPLETION_DELETED, COMPLETION_MOVED, RESCHEDULED, REOPENED }

@OptIn(ExperimentalCoroutinesApi::class)
class ChoresViewModel(
    private val repository: ChoreRepository,
    settings: Flow<AppSettings> = flowOf(AppSettings()),
) : ViewModel() {

    private val weekStart = settings.map { it.firstDayOfWeek }.distinctUntilChanged()

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** All chores; null while loading. */
    val chores: StateFlow<List<Chore>?> = repository.chores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val doneToday: Flow<List<CompletionEntity>> = _today.flatMapLatest { repository.completionsOn(it) }

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
            dueToday = matching { it == DueStatus.Today || it is DueStatus.InWindow },
            upcoming = matching { it is DueStatus.Upcoming },
            doneToday = done.map { it.choreId }.toSet().size,
            doneTodayList = done.groupBy { it.choreId }.mapNotNull { (choreId, list) ->
                chores.find { it.id == choreId }?.let { DoneToday(it, list.last().id) }
            },
            activeHoliday = holidays.firstOrNull { it.isActive(today) },
            plannedHoliday = holidays.firstOrNull { it.isPlanned(today) },
            datesRunningOut = chores.filter { chore ->
                chore.schedule.unit == RepeatUnit.DATES && !chore.paused && chore.schedule.dates.count { it >= today } <= 1
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Null while loading. */
    val progress: StateFlow<ProgressState?> =
        combine(
            repository.chores,
            repository.allCompletions,
            combine(_today, weekStart, ::Pair),
            repository.holidays,
            repository.missedPeriods,
        ) { chores, completions, (today, firstDay), holidays, missed ->
            val tolerance = chores.associate { it.id to it.toleranceDays }
            val records = completions.map { it.completion.toRecord(tolerance[it.completion.choreId] ?: 0) }
            val start = chores.minOfOrNull { it.createdDate }
            val streaks = if (start == null) {
                Streaks(0, 0)
            } else {
                Progress.streaks(
                    overdueDays = Progress.overdueDays(
                        records,
                        chores.filterNot { it.paused }.mapNotNull { chore -> chore.dueDate?.plusDays(chore.toleranceDays.toLong()) },
                        start,
                        today,
                        missed,
                    ),
                    start = start,
                    today = today,
                    todayClean = OpenChores.of(chores, today).isEmpty,
                    holidayDays = holidays.flatMap { it.daysUntil(today).toList() }.toSet(),
                    weekStart = firstDay,
                )
            }
            val byChore = records.groupBy { it.choreId }
            ProgressState(
                streaks = streaks,
                stats = Progress.stats(records, today, firstDay),
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
        viewModelScope.launch {
            val snapshot = repository.snapshotOverdue()
            repository.rescheduleOverdue()
            if (snapshot.chores.isNotEmpty()) events.send(UiEvent.Changed(ChangeKind.RESCHEDULED, snapshot))
        }
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

    fun setPaused(choreId: Long, paused: Boolean) =
        undoable(if (paused) ChangeKind.PAUSED else ChangeKind.RESUMED, choreId) { repository.setPaused(choreId, paused) }

    fun addArea(name: String) {
        viewModelScope.launch { repository.addArea(name) }
    }

    fun renameArea(id: Long, name: String) {
        viewModelScope.launch { repository.renameArea(id, name) }
    }

    fun deleteArea(id: Long) {
        viewModelScope.launch { repository.deleteArea(id) }
    }

    /** Marks a chore as done today, or on an earlier [date] ("Früher erledigt", F4.3). */
    fun complete(chore: Chore, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val wasOpen = chore.status(today) is DueStatus.Overdue || chore.isDueNow(today)
            val completionId = repository.complete(chore.id, date) ?: return@launch
            val allDone = wasOpen && OpenChores.of(repository.chores.first(), today).isEmpty
            events.send(UiEvent.Completed(chore.name, completionId, allDone))
        }
    }

    fun addFromTemplates(templates: List<ChoreTemplate>) {
        viewModelScope.launch { repository.addFromTemplates(templates) }
    }

    /** Skips the current occurrence (F4.5). */
    fun skip(choreId: Long) = undoable(ChangeKind.SKIPPED, choreId) { repository.skip(choreId) }

    fun changeCompletionDate(choreId: Long, completionId: Long, date: LocalDate) =
        undoable(ChangeKind.COMPLETION_MOVED, choreId, date) { repository.changeCompletionDate(completionId, date) }

    fun deleteCompletion(choreId: Long, completionId: Long) =
        undoable(ChangeKind.COMPLETION_DELETED, choreId) { repository.deleteCompletion(completionId) }

    fun undoCompletion(completionId: Long) {
        viewModelScope.launch { repository.undoCompletion(completionId) }
    }

    fun postpone(choreId: Long, date: LocalDate) = undoable(ChangeKind.POSTPONED, choreId, date) { repository.postpone(choreId, date) }

    /** Takes back today's completion from the home screen ("Heute erledigt"). */
    fun reopen(choreId: Long, completionId: Long) =
        undoable(ChangeKind.REOPENED, choreId) { repository.deleteCompletion(completionId) }

    /** Undoes a [UiEvent.Changed]. */
    fun undo(snapshot: ChoreSnapshot) {
        viewModelScope.launch { repository.restore(snapshot) }
    }

    /** Runs [change] and offers to undo it, by restoring the chore as it was before. */
    private fun undoable(kind: ChangeKind, choreId: Long, date: LocalDate? = null, change: suspend () -> Unit) {
        viewModelScope.launch {
            val snapshot = repository.snapshot(listOf(choreId))
            change()
            events.send(UiEvent.Changed(kind, snapshot, date))
        }
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
        toleranceDays: Int = 0,
        symbol: ChoreSymbol? = null,
        color: ChoreColor? = null,
    ) {
        viewModelScope.launch {
            val area = if (!newAreaName.isNullOrBlank()) repository.addArea(newAreaName) else areaId
            repository.save(id, name, notes, schedule, nextDue, area, checklist, toleranceDays, symbol, color)
        }
    }

    fun delete(choreId: Long) = undoable(ChangeKind.DELETED, choreId) { repository.delete(choreId) }

    companion object {
        private const val RECENT_COUNT = 100

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HuushaltApplication
                ChoresViewModel(app.repository, app.settings.settings)
            }
        }
    }
}
