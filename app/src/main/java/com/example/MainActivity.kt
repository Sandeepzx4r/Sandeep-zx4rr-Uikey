package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutGoogle
import com.example.data.BlackSharkRepository
import com.example.ui.BlackSharkViewModel
import com.example.ui.UiState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RedeemScreen
import com.example.ui.screens.SpinWheelScreen
import com.example.ui.theme.BlackSharkTheme
import com.example.ui.theme.CardSurfaceVariant
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.DeepVioletSurface
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            BlackSharkTheme {
                AppNavigation()
            }
        }
    }
}

private enum class BottomTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    SPIN("Spin", Icons.Filled.Casino, Icons.Outlined.Casino, "nav_spin"),
    REDEEM("Redeem", Icons.Filled.CardGiftcard, Icons.Outlined.CardGiftcard, "nav_redeem"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ })
    } else {
        key(user.uid) {
            BlackSharkMainShell(
                currentUserId = user.uid,
                userEmail = user.email,
                initialDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "BlackShark Gamer"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlackSharkMainShell(
    currentUserId: String,
    userEmail: String?,
    initialDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: BlackSharkViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BlackSharkViewModel(
                    repository = BlackSharkRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName
                )
            }
        }
    )

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val connectedGamesState by viewModel.connectedGamesState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.gameSessionsState.collectAsStateWithLifecycle()
    val spinLogsState by viewModel.spinLogsState.collectAsStateWithLifecycle()
    val redemptionsState by viewModel.redemptionsState.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val nowEpochSeconds by viewModel.nowEpochSeconds.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }

    // Return to Home tab when pressing Back on secondary tabs
    BackHandler(enabled = selectedTab != BottomTab.HOME) {
        selectedTab = BottomTab.HOME
    }

    LaunchedEffect(feedback.errorMessage, feedback.infoMessage) {
        val msg = feedback.errorMessage ?: feedback.infoMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    val profile = (profileState as? UiState.Success)?.data
    val connectedGames = (connectedGamesState as? UiState.Success)?.data.orEmpty()
    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val spinLogs = (spinLogsState as? UiState.Success)?.data.orEmpty()
    val redemptions = (redemptionsState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SportsEsports,
                            contentDescription = null,
                            tint = ChampionGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BLACK SHARK",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepVioletSurface,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepVioletSurface,
                tonalElevation = 0.dp
            ) {
                BottomTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChampionGold,
                            selectedTextColor = ChampionGold,
                            indicatorColor = CardSurfaceVariant,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (profileState is UiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonPurple)
                }
            } else {
                when (selectedTab) {
                    BottomTab.HOME -> HomeScreen(
                        profile = profile,
                        connectedGames = connectedGames,
                        sessions = sessions,
                        nowEpochSeconds = nowEpochSeconds,
                        isBusy = feedback.isBusy,
                        lastClaimedCoins = feedback.lastClaimedRewardCoins,
                        onConnectGame = { game, pid, uname, onDone ->
                            viewModel.connectGame(game, pid, uname, onDone)
                        },
                        onStartSession = { game -> viewModel.startGameSession(game) },
                        onClaimSession = { sessionId -> viewModel.claimGameSession(sessionId) },
                        onCancelSession = { sessionId -> viewModel.cancelGameSession(sessionId) },
                        onDismissClaimCelebration = { viewModel.dismissClaimCelebration() },
                        onOpenSpinTab = { selectedTab = BottomTab.SPIN }
                    )

                    BottomTab.SPIN -> SpinWheelScreen(
                        profile = profile,
                        spinLogs = spinLogs,
                        lastWonSegment = feedback.lastWonSpinSegment,
                        nowEpochSeconds = nowEpochSeconds,
                        isBusy = feedback.isBusy,
                        onSpinClick = { viewModel.spinWheel() }
                    )

                    BottomTab.REDEEM -> RedeemScreen(
                        profile = profile,
                        redemptions = redemptions,
                        lastSubmittedRedemption = feedback.lastSubmittedRedemption,
                        isBusy = feedback.isBusy,
                        onSubmitRedemption = { method, dest, tier ->
                            viewModel.submitRedemption(method, dest, tier)
                        },
                        onDismissConfirmation = { viewModel.dismissRedemptionConfirmation() }
                    )

                    BottomTab.PROFILE -> ProfileScreen(
                        profile = profile,
                        userEmail = userEmail,
                        connectedGames = connectedGames,
                        isBusy = feedback.isBusy,
                        onUpdateDisplayName = { newName -> viewModel.updateDisplayName(newName) },
                        onDisconnectGame = { gameKey -> viewModel.disconnectGame(gameKey) },
                        onSignOutClick = {
                            signOutGoogle(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }
}
