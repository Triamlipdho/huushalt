package xyz.haselbach.huushalt.ui

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.ui.areas.AreasScreen
import xyz.haselbach.huushalt.ui.catalog.CatalogScreen
import xyz.haselbach.huushalt.ui.settings.DataViewModel
import xyz.haselbach.huushalt.ui.chores.AllChoresScreen
import xyz.haselbach.huushalt.ui.detail.ChoreDetailScreen
import xyz.haselbach.huushalt.ui.edit.ChoreEditScreen
import xyz.haselbach.huushalt.ui.components.PermissionCard
import xyz.haselbach.huushalt.ui.components.everyLabel
import xyz.haselbach.huushalt.ui.holiday.HolidayScreen
import xyz.haselbach.huushalt.ui.home.HomeScreen
import xyz.haselbach.huushalt.ui.progress.AnnounceNewBadges
import xyz.haselbach.huushalt.ui.progress.ProgressScreen
import xyz.haselbach.huushalt.ui.settings.SettingsScreen
import xyz.haselbach.huushalt.ui.settings.SettingsViewModel
import xyz.haselbach.huushalt.ui.settings.rememberNotificationAccess

/** Screens, encoded as strings so the back stack survives configuration changes and process death. */
private object Route {
    const val TODAY = "today"
    const val ALL = "all"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val HOLIDAY = "holiday"
    const val AREAS = "areas"
    const val CATALOG = "catalog"
    val tabs = setOf(TODAY, ALL, PROGRESS)
    fun detail(id: Long) = "detail:$id"
    fun edit(id: Long?) = "edit:${id ?: "new"}"
    fun copy(id: Long) = "copy:$id"
}

private val stackSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

/** A request from outside the app (notification, app shortcut) to show a screen; [seq] makes repeats distinct. */
data class NavRequest(val target: NavTarget, val seq: Int)

enum class NavTarget { TODAY, NEW_CHORE, CATALOG }

@Composable
fun HuushaltApp(
    navRequest: NavRequest? = null,
    viewModel: ChoresViewModel = viewModel(factory = ChoresViewModel.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    dataViewModel: DataViewModel = viewModel(factory = DataViewModel.Factory),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshToday() }

    val chores by viewModel.chores.collectAsStateWithLifecycle()
    val home by viewModel.home.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val areas by viewModel.areas.collectAsStateWithLifecycle()
    val intervalSuggestions by viewModel.intervalSuggestions.collectAsStateWithLifecycle()
    // A chore with an unfinished checklist waiting for "Trotzdem erledigt?" (F1.8).
    var confirmCompleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val today by viewModel.today.collectAsStateWithLifecycle()
    val stack = rememberSaveable(saver = stackSaver) { mutableStateListOf(Route.TODAY) }
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val notificationAccess = rememberNotificationAccess(
        permissionRequested = settings?.permissionRequested ?: false,
        onRequested = { settingsViewModel.update { it.copy(permissionRequested = true) } },
    )

    val screenStates = rememberSaveableStateHolder()
    // Each opened screen gets its own entry ("detail:5@12"), so opening the same screen again
    // never reuses an old one, e.g. one still animating out.
    var openCount by rememberSaveable { mutableIntStateOf(0) }
    fun entry(route: String) = if (route in Route.tabs) route else "$route@${++openCount}"
    fun open(route: String) = stack.add(entry(route))
    fun back() {
        if (stack.size > 1) screenStates.removeState(stack.removeAt(stack.lastIndex))
    }
    fun backToTab() {
        while (stack.size > 1) screenStates.removeState(stack.removeAt(stack.lastIndex))
    }
    // In the editor, back is passed on, so it can ask before unsaved changes are lost.
    var editorBackRequests by remember { mutableIntStateOf(0) }
    BackHandler(enabled = stack.size > 1) {
        if (stack.last().let { it.startsWith("edit:") || it.startsWith("copy:") }) editorBackRequests++ else back()
    }

    // A tapped reminder shows the daily overview (F5.3); app shortcuts open "Neue Aufgabe" or the
    // suggestions (F9.7).
    LaunchedEffect(navRequest) {
        val target = navRequest?.target ?: return@LaunchedEffect
        stack.filter { it !in Route.tabs }.forEach(screenStates::removeState)
        stack.clear()
        stack.add(Route.TODAY)
        when (target) {
            NavTarget.TODAY -> Unit
            NavTarget.NEW_CHORE -> open(Route.edit(null))
            NavTarget.CATALOG -> open(Route.CATALOG)
        }
    }

    // Snackbar "«Staubsaugen» erledigt – Rückgängig" after marking a chore done (F4.2), and the
    // same for every other change (skip, postpone, pause, delete, ...).
    val context = LocalContext.current
    val completedText = stringResource(R.string.snackbar_completed)
    val allDoneText = stringResource(R.string.celebration_all_done)
    val motivationEnabled = settings?.motivationEnabled ?: true
    val undoText = stringResource(R.string.action_undo)
    val undoNotPossibleText = stringResource(R.string.undo_not_possible)
    LaunchedEffect(viewModel, motivationEnabled) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.Completed -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        // Celebrate the last chore of the day (F7.5).
                        val message = if (event.allDoneToday && motivationEnabled) allDoneText else completedText.format(event.choreName)
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = undoText,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoCompletion(event.completionId, event.recheckOnUndo)
                    }
                }
                is UiEvent.Changed -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        val result = snackbarHostState.showSnackbar(
                            message = changeMessage(context.resources, event),
                            actionLabel = undoText,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
                    }
                }
                UiEvent.UndoNotPossible -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch { snackbarHostState.showSnackbar(undoNotPossibleText) }
                }
            }
        }
    }

    val allChores = chores ?: return
    val homeState = home ?: return
    val appSettings = settings ?: return
    val progressState = progress ?: return
    val allAreas = areas ?: return
    val allHolidays by viewModel.holidays.collectAsStateWithLifecycle()

    // "Neues Abzeichen" once per badge (F7.7).
    AnnounceNewBadges(
        progressState.badges,
        appSettings.announcedBadges,
        appSettings.motivationEnabled,
        snackbarHostState,
        onForget = { ids -> settingsViewModel.update { it.copy(announcedBadges = it.announcedBadges.orEmpty() - ids) } },
    ) { ids ->
        settingsViewModel.update { it.copy(announcedBadges = it.announcedBadges.orEmpty() + ids) }
    }

    fun complete(chore: Chore) {
        if (chore.checklistUnfinished) confirmCompleteId = chore.id else viewModel.complete(chore)
    }

    /** Swipe left: due chores move to tomorrow, upcoming ones one day after their due date. */
    fun postponeOneDay(chore: Chore) {
        val from = chore.dueDate?.takeIf { it > today } ?: today
        viewModel.postpone(chore.id, from.plusDays(1))
    }

    allChores.find { it.id == confirmCompleteId }?.let { chore ->
        AlertDialog(
            onDismissRequest = { confirmCompleteId = null },
            title = { Text(stringResource(R.string.checklist_unfinished_title)) },
            text = { Text(stringResource(R.string.checklist_unfinished_body, chore.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCompleteId = null
                    viewModel.complete(chore)
                }) { Text(stringResource(R.string.checklist_done_anyway)) }
            },
            dismissButton = { TextButton(onClick = { confirmCompleteId = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    // Once everything is done, a reminder still showing is out of date.
    LaunchedEffect(homeState.open) {
        if (homeState.open == 0) (context.applicationContext as HuushaltApplication).notifier.cancel()
    }
    /** One screen of the back stack. */
    @Composable
    fun Screen(entry: String) {
        val route = entry.substringBefore('@')
        when {
            route in Route.tabs -> MainScaffold(
                tab = route,
                snackbarHostState = snackbarHostState,
                onSelectTab = { tab ->
                    stack.clear()
                    stack.add(tab)
                },
                onAdd = { open(Route.edit(null)) },
                onSettings = { open(Route.SETTINGS) },
                subtitle = if (route == Route.TODAY) today.formatLong() else null,
            ) { padding ->
                if (route == Route.TODAY) {
                    val showPermissionCard = appSettings.reminders.enabled &&
                        !notificationAccess.canNotify &&
                        !appSettings.permissionCardDismissed &&
                        homeState.hasChores
                    HomeScreen(
                        state = homeState,
                        contentPadding = padding,
                        onOpen = { open(Route.detail(it.id)) },
                        onComplete = ::complete,
                        streak = progressState.streaks.current.takeIf { appSettings.motivationEnabled },
                        onReschedule = viewModel::rescheduleOverdue,
                        onEndHoliday = { viewModel.endHoliday(it.id) },
                        onChangeHoliday = { open(Route.HOLIDAY) },
                        areas = allAreas,
                        onEditChore = { open(Route.edit(it.id)) },
                        onOpenCatalog = { open(Route.CATALOG) },
                        onReopen = { viewModel.reopen(it.chore.id, it.completionId) },
                        onPostpone = ::postponeOneDay,
                        intervalSuggestions = intervalSuggestions.values.mapNotNull { suggestion ->
                            allChores.find { it.id == suggestion.choreId }?.let { it to suggestion }
                        },
                        header = {
                            if (showPermissionCard) {
                                PermissionCard(
                                    opensSettings = notificationAccess.opensSettings,
                                    onAllow = notificationAccess.request,
                                    onDismiss = { settingsViewModel.update { it.copy(permissionCardDismissed = true) } },
                                )
                            }
                        },
                    )
                } else if (route == Route.ALL) {
                    AllChoresScreen(
                        chores = allChores,
                        showAreaSort = allAreas.isNotEmpty(),
                        firstDayOfWeek = appSettings.firstDayOfWeek,
                        holidays = allHolidays,
                        today = today,
                        contentPadding = padding,
                        onOpen = { open(Route.detail(it.id)) },
                        onComplete = ::complete,
                        onPostpone = ::postponeOneDay,
                    )
                } else {
                    ProgressScreen(
                        state = progressState,
                        motivationEnabled = appSettings.motivationEnabled,
                        contentPadding = padding,
                        onOpenChore = { open(Route.detail(it)) },
                        areas = allAreas,
                    )
                }
            }

            route.startsWith("detail:") -> {
                val id = route.removePrefix("detail:").toLong()
                val chore = allChores.find { it.id == id }
                if (chore == null) {
                    // Deleted in the meantime.
                    // Only the screen on top reacts; one still animating out must not.
                    if (entry == stack.last()) LaunchedEffect(entry) { back() }
                    return
                }
                val completions by remember(id) { viewModel.completions(id) }.collectAsStateWithLifecycle(emptyList())
                val checklist by remember(id) { viewModel.checklist(id) }.collectAsStateWithLifecycle(emptyList())
                ChoreDetailScreen(
                    chore = chore,
                    completions = completions,
                    today = today,
                    snackbarHostState = snackbarHostState,
                    onBack = ::back,
                    onEdit = { open(Route.edit(id)) },
                    onComplete = { complete(chore) },
                    onPostpone = { viewModel.postpone(id, it) },
                    onChangeCompletionDate = { completionId, date -> viewModel.changeCompletionDate(id, completionId, date) },
                    onDeleteCompletion = { completionId -> viewModel.deleteCompletion(id, completionId) },
                    streak = progressState.choreStreaks[id].takeIf { appSettings.motivationEnabled },
                    checklist = checklist,
                    onToggleItem = { viewModel.toggleChecklistItem(chore, it, checklist) },
                    onPausedChange = { viewModel.setPaused(id, it) },
                    onCompleteOn = { date -> viewModel.complete(chore, date) },
                    onSkip = { viewModel.skip(id) },
                    onDuplicate = { open(Route.copy(id)) },
                    onDelete = {
                        viewModel.delete(id)
                        backToTab()
                    },
                    intervalSuggestion = intervalSuggestions[id],
                    onAcceptSuggestion = { intervalSuggestions[id]?.let(viewModel::acceptIntervalSuggestion) },
                    onDismissSuggestion = {
                        intervalSuggestions[id]?.let { suggestion ->
                            settingsViewModel.update { it.copy(dismissedIntervalSuggestions = it.dismissedIntervalSuggestions + suggestion.key) }
                        }
                    },
                )
            }

            route.startsWith("edit:") || route.startsWith("copy:") -> {
                val isCopy = route.startsWith("copy:")
                val sourceId = route.substringAfter(':').toLongOrNull()
                val source = sourceId?.let { choreId -> allChores.find { it.id == choreId } }
                if (sourceId != null && source == null) {
                    // Only the screen on top reacts; one still animating out must not.
                    if (entry == stack.last()) LaunchedEffect(entry) { back() }
                    return
                }
                // Editing keeps the id; a copy (F1.10) is saved as a new chore.
                val id = if (isCopy) null else sourceId
                val chore = if (isCopy) null else source
                // The checklist must be loaded before the editor starts, it initialises its fields once.
                val checklist by remember(sourceId) { sourceId?.let { viewModel.checklist(it) } ?: flowOf(emptyList()) }
                    .collectAsStateWithLifecycle(null)
                val initialChecklist = checklist ?: return
                ChoreEditScreen(
                    chore = chore,
                    today = today,
                    areas = allAreas,
                    initialChecklist = initialChecklist,
                    firstDayOfWeek = appSettings.firstDayOfWeek,
                    copyOf = if (isCopy) source else null,
                    existingNames = allChores.map { it.name }.toSet(),
                    backRequests = editorBackRequests,
                    onClose = ::back,
                    onSave = { draft ->
                        viewModel.save(
                            id,
                            draft.name,
                            draft.notes,
                            draft.schedule,
                            draft.nextDue,
                            draft.areaId,
                            draft.checklist,
                            draft.newAreaName,
                            draft.toleranceDays,
                            draft.symbol,
                            draft.color,
                        )
                        // A saved copy shows up in the list, not on the original's detail screen.
                        if (isCopy) backToTab() else back()
                    },
                    onDelete = {
                        if (id != null) viewModel.delete(id)
                        backToTab()
                    },
                )
            }

            route == Route.SETTINGS -> {
                // Export / import via the system file picker (F9.3).
                val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                    uri?.let(dataViewModel::exportTo)
                }
                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    uri?.let(dataViewModel::readImport)
                }
                val pendingImport by dataViewModel.pendingImport.collectAsStateWithLifecycle()
                val dataMessages = mapOf(
                    DataViewModel.Message.EXPORTED to stringResource(R.string.data_exported),
                    DataViewModel.Message.EXPORT_FAILED to stringResource(R.string.data_export_failed),
                    DataViewModel.Message.IMPORTED to stringResource(R.string.data_imported),
                    DataViewModel.Message.IMPORT_FAILED to stringResource(R.string.data_import_failed),
                    DataViewModel.Message.IMPORT_INVALID to stringResource(R.string.data_import_invalid),
                    DataViewModel.Message.IMPORT_TOO_LARGE to stringResource(R.string.data_import_too_large),
                    DataViewModel.Message.DELETED to stringResource(R.string.data_deleted),
                )
                LaunchedEffect(dataViewModel) {
                    dataViewModel.events.collect {
                    // All data was replaced: an undo offered before must not touch it any more.
                    if (it == DataViewModel.Message.IMPORTED || it == DataViewModel.Message.DELETED) {
                        viewModel.forgetUndo()
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                    snackbarHostState.showSnackbar(dataMessages.getValue(it))
                }
                }
                SettingsScreen(
                    settings = appSettings,
                    access = notificationAccess,
                    onBack = ::back,
                    onChange = { change -> settingsViewModel.update { it.copy(reminders = change(it.reminders)) } },
                    onMotivationChange = { on -> settingsViewModel.update { it.copy(motivationEnabled = on) } },
                    holiday = homeState.activeHoliday ?: homeState.plannedHoliday,
                    onOpenHoliday = { open(Route.HOLIDAY) },
                    onOpenAreas = { open(Route.AREAS) },
                    onThemeChange = { theme -> settingsViewModel.update { it.copy(theme = theme) } },
                    onDynamicColorChange = { on -> settingsViewModel.update { it.copy(dynamicColor = on) } },
                    onFirstDayChange = { day -> settingsViewModel.update { it.copy(firstDayOfWeek = day) } },
                    onOpenCatalog = { open(Route.CATALOG) },
                    onExport = { exportLauncher.launch("huushalt-${today}.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    onDeleteAll = dataViewModel::deleteAll,
                    snackbarHostState = snackbarHostState,
                )
                pendingImport?.let { backup ->
                    AlertDialog(
                        onDismissRequest = dataViewModel::cancelImport,
                        title = { Text(stringResource(R.string.data_import_title)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.data_import_body,
                                    pluralStringResource(R.plurals.count_chores, backup.chores.size, backup.chores.size),
                                    pluralStringResource(R.plurals.count_completions, backup.completions.size, backup.completions.size),
                                ),
                            )
                        },
                        confirmButton = { TextButton(onClick = dataViewModel::confirmImport) { Text(stringResource(R.string.data_import_confirm)) } },
                        dismissButton = { TextButton(onClick = dataViewModel::cancelImport) { Text(stringResource(R.string.action_cancel)) } },
                    )
                }
            }

            route == Route.CATALOG -> CatalogScreen(
                existingNames = allChores.map { it.name }.toSet(),
                onClose = ::back,
                onAdd = { templates ->
                    viewModel.addFromTemplates(templates)
                    backToTab()
                },
            )

            route == Route.AREAS -> AreasScreen(
                areas = allAreas,
                onBack = ::back,
                onAdd = viewModel::addArea,
                onRename = viewModel::renameArea,
                onDelete = viewModel::deleteArea,
            )

            route == Route.HOLIDAY -> {
                val holiday = homeState.activeHoliday ?: homeState.plannedHoliday
                HolidayScreen(
                    holiday = holiday,
                    today = today,
                    onClose = ::back,
                    onSave = { start, end ->
                        viewModel.saveHoliday(holiday?.id, start, end)
                        back()
                    },
                    onEnd = {
                        holiday?.let { viewModel.endHoliday(it.id) }
                        back()
                    },
                    onDelete = {
                        holiday?.let { viewModel.deleteHoliday(it.id) }
                        back()
                    },
                )
            }

            else -> LaunchedEffect(route) { backToTab() }
        }
    }

    // Opening a screen slides it in; going back slides it out. Switching tabs is instant.
    // Every screen keeps its state (e.g. scroll position) while it is on the back stack.
    AnimatedContent(
        targetState = stack.last(),
        contentKey = { if (it in Route.tabs) "tabs" else it },
        transitionSpec = {
            val opened = initialState in stack
            if (opened) {
                (slideInHorizontally { it / 3 } + fadeIn(tween(220))) togetherWith fadeOut(tween(120))
            } else {
                fadeIn(tween(120)) togetherWith (slideOutHorizontally { it / 3 } + fadeOut(tween(220)))
            }.apply { targetContentZIndex = if (opened) 1f else -1f }
        },
        label = "screen",
    ) { entry ->
        screenStates.SaveableStateProvider(entry) { Screen(entry) }
    }
}

/** Snackbar text for a change that can be undone. */
private fun changeMessage(res: Resources, event: UiEvent.Changed): String = when (event.kind) {
    ChangeKind.SKIPPED -> res.getString(R.string.undo_skipped, event.choreName)
    ChangeKind.POSTPONED -> res.getString(R.string.undo_postponed, event.choreName, event.date?.formatShort().orEmpty())
    ChangeKind.PAUSED -> res.getString(R.string.undo_paused, event.choreName)
    ChangeKind.RESUMED -> res.getString(R.string.undo_resumed, event.choreName)
    ChangeKind.DELETED -> res.getString(R.string.undo_deleted, event.choreName)
    ChangeKind.COMPLETION_DELETED -> res.getString(R.string.undo_completion_deleted)
    ChangeKind.COMPLETION_MOVED -> res.getString(R.string.undo_completion_moved, event.date?.formatShort().orEmpty())
    ChangeKind.REOPENED -> res.getString(R.string.undo_reopened, event.choreName)
    ChangeKind.RESCHEDULED -> res.getQuantityString(R.plurals.undo_rescheduled, event.count, event.count)
    ChangeKind.INTERVAL_CHANGED -> res.getString(
        R.string.undo_interval_changed,
        event.choreName,
        event.token.after.chores.firstOrNull()?.let { res.everyLabel(it.interval, it.unit) }.orEmpty(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    tab: String,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (String) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    /** Second line of the title, e.g. today's date. */
    subtitle: String? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    // The "+" button moves out of the way while scrolling down and comes back when scrolling up.
    var fabVisible by rememberSaveable { mutableStateOf(true) }
    val scrollWatcher = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) fabVisible = false
                if (available.y > 1f) fabVisible = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(tab) { fabVisible = true }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollWatcher),
        topBar = {
            TopAppBar(
                title = {
                    val title = when (tab) {
                        Route.TODAY -> R.string.home_title
                        Route.ALL -> R.string.nav_all
                        else -> R.string.nav_progress
                    }
                    Column {
                        Text(stringResource(title))
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Route.TODAY,
                    onClick = { onSelectTab(Route.TODAY) },
                    icon = { Icon(Icons.Filled.Today, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_today)) },
                )
                NavigationBarItem(
                    selected = tab == Route.ALL,
                    onClick = { onSelectTab(Route.ALL) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_all)) },
                )
                NavigationBarItem(
                    selected = tab == Route.PROGRESS,
                    onClick = { onSelectTab(Route.PROGRESS) },
                    icon = { Icon(Icons.Filled.InsertChart, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_progress)) },
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = tab != Route.PROGRESS && fabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content,
    )
}
