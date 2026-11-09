package xyz.haselbach.huushalt.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import xyz.haselbach.huushalt.ui.holiday.HolidayScreen
import xyz.haselbach.huushalt.ui.home.HomeScreen
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

    fun open(route: String) = stack.add(route)
    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
    fun backToTab() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
    BackHandler(enabled = stack.size > 1) { back() }

    // A tapped reminder shows the daily overview (F5.3); app shortcuts open "Neue Aufgabe" or the
    // suggestions (F9.7).
    LaunchedEffect(navRequest) {
        val target = navRequest?.target ?: return@LaunchedEffect
        stack.clear()
        stack.add(Route.TODAY)
        when (target) {
            NavTarget.TODAY -> Unit
            NavTarget.NEW_CHORE -> stack.add(Route.edit(null))
            NavTarget.CATALOG -> stack.add(Route.CATALOG)
        }
    }

    // Snackbar "«Staubsaugen» erledigt – Rückgängig" after marking a chore done (F4.2).
    val completedText = stringResource(R.string.snackbar_completed)
    val allDoneText = stringResource(R.string.celebration_all_done)
    val motivationEnabled = settings?.motivationEnabled ?: true
    val undoText = stringResource(R.string.action_undo)
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
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoCompletion(event.completionId)
                    }
                }
            }
        }
    }

    val allChores = chores ?: return
    val homeState = home ?: return
    val appSettings = settings ?: return
    val progressState = progress ?: return
    val allAreas = areas ?: return

    fun complete(chore: Chore) {
        if (chore.checklistUnfinished) confirmCompleteId = chore.id else viewModel.complete(chore)
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
    val context = LocalContext.current
    LaunchedEffect(homeState.open) {
        if (homeState.open == 0) (context.applicationContext as HuushaltApplication).notifier.cancel()
    }
    val route = stack.last()

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
                    today = today,
                    contentPadding = padding,
                    onOpen = { open(Route.detail(it.id)) },
                    onComplete = ::complete,
                )
            } else {
                ProgressScreen(
                    state = progressState,
                    motivationEnabled = appSettings.motivationEnabled,
                    contentPadding = padding,
                    onOpenChore = { open(Route.detail(it)) },
                )
            }
        }

        route.startsWith("detail:") -> {
            val id = route.removePrefix("detail:").toLong()
            val chore = allChores.find { it.id == id }
            if (chore == null) {
                // Deleted in the meantime.
                LaunchedEffect(route) { back() }
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
                onChangeCompletionDate = viewModel::changeCompletionDate,
                onDeleteCompletion = viewModel::deleteCompletion,
                streak = progressState.choreStreaks[id].takeIf { appSettings.motivationEnabled },
                checklist = checklist,
                onToggleItem = { viewModel.setChecked(it.id, !it.checked) },
                onPausedChange = { viewModel.setPaused(id, it) },
                onCompleteOn = { date -> viewModel.complete(chore, date) },
                onSkip = { viewModel.skip(id) },
                onDuplicate = { open(Route.copy(id)) },
            )
        }

        route.startsWith("edit:") || route.startsWith("copy:") -> {
            val isCopy = route.startsWith("copy:")
            val sourceId = route.substringAfter(':').toLongOrNull()
            val source = sourceId?.let { choreId -> allChores.find { it.id == choreId } }
            if (sourceId != null && source == null) {
                LaunchedEffect(route) { back() }
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
                DataViewModel.Message.DELETED to stringResource(R.string.data_deleted),
            )
            LaunchedEffect(dataViewModel) {
                dataViewModel.events.collect { snackbarHostState.showSnackbar(dataMessages.getValue(it)) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    tab: String,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (String) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (tab) {
                        Route.TODAY -> R.string.home_title
                        Route.ALL -> R.string.nav_all
                        else -> R.string.nav_progress
                    }
                    Text(stringResource(title))
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
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
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
                    icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_progress)) },
                )
            }
        },
        floatingActionButton = {
            if (tab != Route.PROGRESS) {
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content,
    )
}
