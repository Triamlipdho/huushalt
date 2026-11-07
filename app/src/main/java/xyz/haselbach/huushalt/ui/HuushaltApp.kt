package xyz.haselbach.huushalt.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.ui.chores.AllChoresScreen
import xyz.haselbach.huushalt.ui.detail.ChoreDetailScreen
import xyz.haselbach.huushalt.ui.edit.ChoreEditScreen
import xyz.haselbach.huushalt.ui.components.PermissionCard
import xyz.haselbach.huushalt.ui.home.HomeScreen
import xyz.haselbach.huushalt.ui.settings.SettingsScreen
import xyz.haselbach.huushalt.ui.settings.SettingsViewModel
import xyz.haselbach.huushalt.ui.settings.rememberNotificationAccess

/** Screens, encoded as strings so the back stack survives configuration changes and process death. */
private object Route {
    const val TODAY = "today"
    const val ALL = "all"
    const val SETTINGS = "settings"
    fun detail(id: Long) = "detail:$id"
    fun edit(id: Long?) = "edit:${id ?: "new"}"
}

private val stackSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

@Composable
fun HuushaltApp(
    openTodayRequests: Int = 0,
    viewModel: ChoresViewModel = viewModel(factory = ChoresViewModel.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshToday() }

    val chores by viewModel.chores.collectAsStateWithLifecycle()
    val home by viewModel.home.collectAsStateWithLifecycle()
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

    // A tapped reminder notification shows the daily overview (F5.3).
    LaunchedEffect(openTodayRequests) {
        if (openTodayRequests > 0) {
            stack.clear()
            stack.add(Route.TODAY)
        }
    }

    // Snackbar "«Staubsaugen» erledigt – Rückgängig" after marking a chore done (F4.2).
    val completedText = stringResource(R.string.snackbar_completed)
    val undoText = stringResource(R.string.action_undo)
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.Completed -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        val result = snackbarHostState.showSnackbar(
                            message = completedText.format(event.choreName),
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

    // Once everything is done, a reminder still showing is out of date.
    val context = LocalContext.current
    LaunchedEffect(homeState.open) {
        if (homeState.open == 0) (context.applicationContext as HuushaltApplication).notifier.cancel()
    }
    val route = stack.last()

    when {
        route == Route.TODAY || route == Route.ALL -> MainScaffold(
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
                    onComplete = viewModel::complete,
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
            } else {
                AllChoresScreen(
                    chores = allChores,
                    today = today,
                    contentPadding = padding,
                    onOpen = { open(Route.detail(it.id)) },
                    onComplete = viewModel::complete,
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
            ChoreDetailScreen(
                chore = chore,
                completions = completions,
                today = today,
                snackbarHostState = snackbarHostState,
                onBack = ::back,
                onEdit = { open(Route.edit(id)) },
                onComplete = { viewModel.complete(chore) },
                onPostpone = { viewModel.postpone(id, it) },
            )
        }

        route.startsWith("edit:") -> {
            val id = route.removePrefix("edit:").toLongOrNull()
            val chore = id?.let { choreId -> allChores.find { it.id == choreId } }
            if (id != null && chore == null) {
                LaunchedEffect(route) { back() }
                return
            }
            ChoreEditScreen(
                chore = chore,
                today = today,
                onClose = ::back,
                onSave = { name, notes, schedule, nextDue ->
                    viewModel.save(id, name, notes, schedule, nextDue)
                    back()
                },
                onDelete = {
                    if (id != null) viewModel.delete(id)
                    backToTab()
                },
            )
        }

        route == Route.SETTINGS -> SettingsScreen(
            settings = appSettings,
            access = notificationAccess,
            onBack = ::back,
            onChange = { change -> settingsViewModel.update { it.copy(reminders = change(it.reminders)) } },
        )

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
                title = { Text(stringResource(if (tab == Route.TODAY) R.string.home_title else R.string.nav_all)) },
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
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content,
    )
}
