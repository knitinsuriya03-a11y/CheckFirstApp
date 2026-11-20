package com.example

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmsFailed
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.CheckFirstAiRepository
import com.example.data.FirestoreUserRepository
import com.example.data.PreferencesDataStore
import com.example.firebase.onGoogleSignInClicked
import com.example.firebase.signOutGoogle
import com.example.ui.components.PillShape
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmergencyScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.LiveVoiceScreen
import com.example.ui.screens.LoanCalculatorScreen
import com.example.ui.screens.LoanRiskScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.QrAnalyzerScreen
import com.example.ui.screens.ScamScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CheckFirstTheme
import com.example.ui.theme.LocalCheckFirstColors
import com.example.viewmodel.AiAssistantViewModel
import com.example.viewmodel.CheckFirstAppViewModel
import kotlinx.coroutines.launch

private data class NavDestination(
    val route: String,
    val icon: ImageVector,
    val groupSelector: ((AppStrings) -> String)? = null,
)

private val ALL_DESTINATIONS = listOf(
    NavDestination("dashboard", Icons.Filled.Dashboard),
    NavDestination("scanner", Icons.Filled.SmsFailed),
    NavDestination("qr-analyzer", Icons.Filled.QrCode2),
    NavDestination("guide", Icons.AutoMirrored.Filled.MenuBook),
    NavDestination("emergency", Icons.Filled.Support),
    NavDestination("loan-calculator", Icons.Filled.CurrencyRupee, groupSelector = { it.groupBorrowClarity }),
    NavDestination("loan-risk", Icons.Filled.GppBad),
    NavDestination("ai-chat", Icons.AutoMirrored.Filled.Chat, groupSelector = { it.groupAiCompanion }),
    NavDestination("live-voice", Icons.Filled.Mic),
    NavDestination("settings", Icons.Filled.Settings, groupSelector = { it.groupPreferences }),
)

private data class BottomDestination(
    val route: String,
    val icon: ImageVector,
    val labelSelector: (AppStrings) -> String,
)

private val BOTTOM_DESTINATIONS = listOf(
    BottomDestination("dashboard", Icons.Filled.Dashboard) { it.bottomHome },
    BottomDestination("scanner", Icons.Filled.SmsFailed) { it.bottomScanner },
    BottomDestination("qr-analyzer", Icons.Filled.QrCode2) { it.bottomQrUpi },
    BottomDestination("ai-chat", Icons.AutoMirrored.Filled.Chat) { it.bottomAiChat },
    BottomDestination("live-voice", Icons.Filled.Mic) { it.bottomVoice },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckFirstApp(
    aiRepository: CheckFirstAiRepository = remember { CheckFirstAiRepository() },
    bypassLoginForTestUid: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val appViewModel: CheckFirstAppViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                CheckFirstAppViewModel(
                    localStore = PreferencesDataStore(app.applicationContext),
                    firestoreRepo = FirestoreUserRepository(app.applicationContext),
                )
            }
        },
    )

    val state by appViewModel.uiState.collectAsStateWithLifecycle()

    // Mandatory startup login gate: the app ALWAYS starts on LoginScreen until the user explicitly signs in/continues.
    var hasPassedLoginGate by rememberSaveable { mutableStateOf(bypassLoginForTestUid != null) }

    val effectiveUid = state.currentUser?.uid ?: bypassLoginForTestUid

    // If user signs out anywhere in the app, immediately re-lock behind the login screen.
    LaunchedEffect(effectiveUid) {
        if (effectiveUid == null) {
            hasPassedLoginGate = false
        }
    }

    CheckFirstTheme(largeText = state.prefs.largeText, tamil = state.prefs.tamil) {
        val c = LocalCheckFirstColors.current
        val s = LocalStrings.current

        if (!hasPassedLoginGate || effectiveUid == null) {
            LoginScreen(
                currentUser = state.currentUser,
                tamil = state.prefs.tamil,
                onToggleLanguage = {
                    appViewModel.updatePrefs { it.copy(tamil = !it.tamil) }
                },
                onSignInSuccess = {
                    hasPassedLoginGate = true
                    appViewModel.showNotice(s.noticeSignedIn)
                },
                onContinueSignedInUser = {
                    if (state.currentUser != null) {
                        hasPassedLoginGate = true
                    }
                },
                onSwitchAccount = {
                    signOutGoogle(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeSignOut = {
                            appViewModel.stopCloudListeners()
                            hasPassedLoginGate = false
                        },
                        onSignOutComplete = {
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    hasPassedLoginGate = true
                                    appViewModel.showNotice(s.noticeSignedIn)
                                },
                                onAuthError = { err -> appViewModel.showNotice(err) },
                                scope = scope,
                            )
                        },
                    )
                },
            )
            return@CheckFirstTheme
        }

        // Scope authenticated workspace & AI assistant strictly to the signed-in UID
        key(effectiveUid) {
            val aiViewModel: AiAssistantViewModel = viewModel(
                key = "ai_$effectiveUid",
                factory = viewModelFactory {
                    initializer { AiAssistantViewModel(aiRepository) }
                },
            )

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route ?: "dashboard"

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val snackbarHostState = remember { SnackbarHostState() }
            var profileMenuOpen by remember { mutableStateOf(false) }

            LaunchedEffect(state.notice) {
                val msg = state.notice ?: return@LaunchedEffect
                snackbarHostState.showSnackbar(msg)
                appViewModel.dismissNotice()
            }

            fun navigateTo(route: String) {
                if (currentRoute != route) {
                    navController.navigate(route) {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                scope.launch { drawerState.close() }
            }

            fun performSignOut() {
                profileMenuOpen = false
                signOutGoogle(
                    credentialManager = credentialManager,
                    scope = scope,
                    onBeforeSignOut = {
                        appViewModel.stopCloudListeners()
                        aiViewModel.resetChat()
                        aiViewModel.stopLiveVoiceSession()
                        hasPassedLoginGate = false
                    },
                    onSignOutComplete = {
                        appViewModel.showNotice(s.noticeSignedOut)
                    },
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(c.canvas)) {
                val isExpandedScreen = maxWidth >= 720.dp

                val appScaffold: @Composable () -> Unit = {
                    Scaffold(
                        containerColor = c.canvas,
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    containerColor = c.surfaceRaised,
                                    contentColor = c.navy,
                                    actionColor = c.accentLight,
                                    shape = RoundedCornerShape(14.dp),
                                )
                            }
                        },
                        topBar = {
                            Column(modifier = Modifier.fillMaxWidth().background(c.barSurface)) {
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = c.barSurface,
                                        titleContentColor = c.navy,
                                    ),
                                    navigationIcon = {
                                        if (!isExpandedScreen) {
                                            IconButton(
                                                onClick = { scope.launch { drawerState.open() } },
                                                modifier = Modifier.testTag("topbar_menu_button"),
                                            ) {
                                                Icon(Icons.Filled.Menu, contentDescription = s.openNavMenuDesc, tint = c.navy)
                                            }
                                        }
                                    },
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            if (!isExpandedScreen) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(c.accentTint),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        Icons.Filled.VerifiedUser,
                                                        contentDescription = null,
                                                        tint = c.accentGlow,
                                                        modifier = Modifier.size(17.dp),
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = "CheckFirst",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = c.accentLight,
                                                    maxLines = 1,
                                                )
                                                Text(
                                                    text = s.navLabelForRoute(currentRoute),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = c.navy,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        Surface(
                                            modifier = Modifier
                                                .padding(end = 6.dp)
                                                .clip(PillShape)
                                                .clickable {
                                                    val nextTamil = !state.prefs.tamil
                                                    appViewModel.updatePrefs { it.copy(tamil = nextTamil) }
                                                    appViewModel.showNotice(
                                                        if (nextTamil) s.noticeSwitchedToTamil else s.noticeSwitchedToEnglish,
                                                    )
                                                }
                                                .testTag("topbar_language_toggle"),
                                            color = if (state.prefs.tamil) c.accentTint else c.surfaceRaised,
                                            shape = PillShape,
                                            border = BorderStroke(1.dp, if (state.prefs.tamil) c.accentBorder else c.line),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    Icons.Filled.Translate,
                                                    contentDescription = s.toggleLangDesc,
                                                    tint = if (state.prefs.tamil) c.accentLight else c.muted,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                                Text(
                                                    text = if (state.prefs.tamil) "தமிழ்" else "EN",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (state.prefs.tamil) c.accentLight else c.navy,
                                                )
                                            }
                                        }
                                        Box {
                                            IconButton(
                                                onClick = { profileMenuOpen = true },
                                                modifier = Modifier.testTag("topbar_profile_button"),
                                            ) {
                                                val initial = state.currentUser?.displayName?.firstOrNull()?.uppercaseChar()?.toString()
                                                    ?: state.currentUser?.email?.firstOrNull()?.uppercaseChar()?.toString()
                                                if (initial != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(c.accentTint),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(initial, style = MaterialTheme.typography.labelLarge, color = c.accentLight)
                                                    }
                                                } else {
                                                    Icon(Icons.Filled.AccountCircle, contentDescription = s.accountMenuDesc, tint = c.muted)
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = profileMenuOpen,
                                                onDismissRequest = { profileMenuOpen = false },
                                                containerColor = c.surfaceRaised,
                                                border = BorderStroke(1.dp, c.line),
                                            ) {
                                                val user = state.currentUser
                                                if (user != null) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text(user.displayName ?: s.menuSignedIn, style = MaterialTheme.typography.titleSmall, color = c.navy)
                                                                Text(user.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = c.muted)
                                                            }
                                                        },
                                                        onClick = { profileMenuOpen = false },
                                                    )
                                                    HorizontalDivider(color = c.line)
                                                    DropdownMenuItem(
                                                        text = { Text(s.menuPreferences, color = c.navy) },
                                                        onClick = {
                                                            profileMenuOpen = false
                                                            navigateTo("settings")
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(s.menuSignOut, color = c.danger) },
                                                        onClick = { performSignOut() },
                                                    )
                                                } else {
                                                    DropdownMenuItem(
                                                        text = { Text(s.menuPreferencesPrivacy, color = c.navy) },
                                                        onClick = {
                                                            profileMenuOpen = false
                                                            navigateTo("settings")
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(s.menuSignOut, color = c.danger) },
                                                        onClick = { performSignOut() },
                                                    )
                                                }
                                            }
                                        }
                                    },
                                )
                                HorizontalDivider(color = c.line)
                            }
                        },
                        bottomBar = {
                            if (!isExpandedScreen) {
                                Column(modifier = Modifier.fillMaxWidth().background(c.barSurface)) {
                                    HorizontalDivider(color = c.line)
                                    NavigationBar(
                                        containerColor = c.barSurface,
                                        tonalElevation = 0.dp,
                                        windowInsets = WindowInsets.navigationBars,
                                    ) {
                                        BOTTOM_DESTINATIONS.forEach { dest ->
                                            val selected = currentRoute == dest.route
                                            val label = dest.labelSelector(s)
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateTo(dest.route) },
                                                icon = { Icon(dest.icon, contentDescription = label) },
                                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = c.accentLight,
                                                    selectedTextColor = c.navy,
                                                    indicatorColor = c.accentTint,
                                                    unselectedIconColor = c.muted,
                                                    unselectedTextColor = c.muted,
                                                ),
                                                modifier = Modifier.testTag("bottom_nav_${dest.route}"),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = "dashboard",
                                enterTransition = { fadeIn(animationSpec = tween(180)) },
                                exitTransition = { fadeOut(animationSpec = tween(140)) },
                                modifier = Modifier.fillMaxSize().widthIn(max = 780.dp),
                            ) {
                                composable("dashboard") {
                                    DashboardScreen(
                                        prefs = state.prefs,
                                        history = state.history,
                                        isSignedIn = state.currentUser != null,
                                        onNavigate = { navigateTo(it) },
                                        onDisableAndClearHistory = { appViewModel.clearHistory(disableToggle = true) },
                                    )
                                }
                                composable("scanner") {
                                    ScamScannerScreen(
                                        aiRepository = aiRepository,
                                        onRecordActivity = { k, l, r -> appViewModel.recordCheckActivity(k, l, r) },
                                        onShowNotice = { appViewModel.showNotice(it) },
                                    )
                                }
                                composable("qr-analyzer") {
                                    QrAnalyzerScreen(
                                        tamil = state.prefs.tamil,
                                        aiRepository = aiRepository,
                                        onRecordActivity = { k, l, r -> appViewModel.recordCheckActivity(k, l, r) },
                                    )
                                }
                                composable("guide") {
                                    GuideScreen(onOpenEmergency = { navigateTo("emergency") })
                                }
                                composable("emergency") {
                                    EmergencyScreen(onShowNotice = { appViewModel.showNotice(it) })
                                }
                                composable("loan-calculator") {
                                    LoanCalculatorScreen(
                                        onRecordActivity = { k, l, r -> appViewModel.recordCheckActivity(k, l, r) },
                                    )
                                }
                                composable("loan-risk") {
                                    LoanRiskScreen(
                                        onRecordActivity = { k, l, r -> appViewModel.recordCheckActivity(k, l, r) },
                                    )
                                }
                                composable("ai-chat") {
                                    AiChatScreen(
                                        tamil = state.prefs.tamil,
                                        viewModel = aiViewModel,
                                    )
                                }
                                composable("live-voice") {
                                    LiveVoiceScreen(
                                        tamil = state.prefs.tamil,
                                        viewModel = aiViewModel,
                                    )
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        prefs = state.prefs,
                                        currentUser = state.currentUser,
                                        syncError = state.syncError,
                                        onUpdatePrefs = { appViewModel.updatePrefs(it) },
                                        onClearHistory = { appViewModel.clearHistory(disableToggle = false) },
                                        onSignInSuccess = { appViewModel.showNotice(s.noticeSignedIn) },
                                        onSignInError = { err -> appViewModel.showNotice(err) },
                                        onSignOut = { performSignOut() },
                                    )
                                }
                            }
                        }
                    }
                }

                if (isExpandedScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        SidebarNavigationContent(
                            currentRoute = currentRoute,
                            simpleMode = state.prefs.simpleMode,
                            onNavigate = { navigateTo(it) },
                            modifier = Modifier.width(248.dp).fillMaxHeight().background(c.barSurface),
                        )
                        VerticalDivider(color = c.line)
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            appScaffold()
                        }
                    }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = c.barSurface,
                                drawerContentColor = c.navy,
                                modifier = Modifier.widthIn(max = 300.dp),
                            ) {
                                SidebarNavigationContent(
                                    currentRoute = currentRoute,
                                    simpleMode = state.prefs.simpleMode,
                                    onNavigate = { navigateTo(it) },
                                    modifier = Modifier.fillMaxHeight(),
                                )
                            }
                        },
                        content = appScaffold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarNavigationContent(
    currentRoute: String,
    simpleMode: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    Column(
        modifier = modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        // Brand Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.accentTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = c.accentGlow, modifier = Modifier.size(20.dp))
            }
            Column {
                Text("CheckFirst", style = MaterialTheme.typography.titleLarge, color = c.navy)
                Text(s.brandTagline, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }

        Spacer(Modifier.height(20.dp))

        ALL_DESTINATIONS.forEach { dest ->
            val groupLabel = dest.groupSelector?.invoke(s)
            if (groupLabel != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = groupLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.subtle,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            val active = currentRoute == dest.route
            val displayLabel = s.navLabelForRoute(dest.route)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(PillShape)
                    .clickable { onNavigate(dest.route) }
                    .testTag("drawer_nav_${dest.route}"),
                color = if (active) c.surfaceRaised else Color.Transparent,
                shape = PillShape,
                border = if (active) BorderStroke(1.dp, c.accentBorder) else null,
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = if (simpleMode) 14.dp else 11.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = null,
                        tint = if (active) c.accentLight else c.muted,
                        modifier = Modifier.size(19.dp),
                    )
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (active) c.navy else c.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Urgent 1930 Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onNavigate("emergency") }
                .testTag("drawer_urgent_card"),
            color = c.emergencyBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, c.emergencyBorder),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.drawerUrgentTitle, style = MaterialTheme.typography.titleSmall, color = c.navy)
                Text(s.drawerUrgentBody, style = MaterialTheme.typography.bodySmall, color = c.ink)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.drawerUrgentCta, style = MaterialTheme.typography.labelLarge, color = c.emergencySymbolTint)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c.emergencySymbolTint, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = c.subtle, modifier = Modifier.size(14.dp))
            Text(s.brandFooter, style = MaterialTheme.typography.labelSmall, color = c.subtle)
        }
    }
}
