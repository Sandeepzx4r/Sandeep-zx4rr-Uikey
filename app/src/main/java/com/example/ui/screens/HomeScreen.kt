package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ConnectedGame
import com.example.data.GameSession
import com.example.data.SUPPORTED_GAMES
import com.example.data.SupportedGame
import com.example.data.UserProfile
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceVariant
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineViolet
import com.example.ui.theme.TextSecondary
import java.util.Locale

private fun findInstalledPackage(context: Context, packages: List<String>): String? {
    val pm = context.packageManager
    for (pkg in packages) {
        try {
            pm.getPackageInfo(pkg, 0)
            return pkg
        } catch (_: PackageManager.NameNotFoundException) {
            // Try next package
        }
    }
    return null
}

private fun launchGamePackage(context: Context, packageName: String): Boolean {
    val launchIntent: Intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(launchIntent)
    return true
}

fun formatDurationHms(seconds: Long): String {
    val clamped = seconds.coerceAtLeast(0L)
    val h = clamped / 3600L
    val m = (clamped % 3600L) / 60L
    val s = clamped % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

@Composable
fun HomeScreen(
    profile: UserProfile?,
    connectedGames: List<ConnectedGame>,
    sessions: List<GameSession>,
    nowEpochSeconds: Long,
    isBusy: Boolean,
    lastClaimedCoins: Long?,
    onConnectGame: (SupportedGame, String, String, () -> Unit) -> Unit,
    onStartSession: (SupportedGame) -> Unit,
    onClaimSession: (String) -> Unit,
    onCancelSession: (String) -> Unit,
    onDismissClaimCelebration: () -> Unit,
    onOpenSpinTab: () -> Unit
) {
    val context = LocalContext.current
    val connectedMap = remember(connectedGames) { connectedGames.associateBy { it.gameKey } }
    val activeSession = remember(sessions) { sessions.firstOrNull { it.status == "active" } }

    var gameToConnect by remember { mutableStateOf<SupportedGame?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile + Coin Balance Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OutlineViolet, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ID: ${profile?.playerCode ?: "---"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = CardSurfaceVariant,
                        modifier = Modifier
                            .border(1.5.dp, ChampionGold, RoundedCornerShape(24.dp))
                            .testTag("coin_balance_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = "Coins",
                                tint = ChampionGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%,d", profile?.coinBalance ?: 0L),
                                style = MaterialTheme.typography.titleLarge,
                                color = ChampionGold
                            )
                        }
                    }
                }
            }
        }

        // 1 Hour Gaming = 1000 Coins Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF3B1370), Color(0xFF6B21A8), Color(0xFF2E1065))
                        )
                    )
                    .border(1.5.dp, NeonPurple, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ChampionGold.copy(alpha = 0.2f))
                            .border(1.5.dp, ChampionGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "1 Hour Reward",
                            tint = ChampionGold,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "1 HOUR GAMING =",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "1,000 COINS (₹1,000)",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ChampionGold
                        )
                        Text(
                            text = "Real-time server-validated session timer. Connect a game below to start.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Active Game Session Tracker (when running)
        if (activeSession != null) {
            item {
                ActiveSessionCard(
                    session = activeSession,
                    nowEpochSeconds = nowEpochSeconds,
                    isBusy = isBusy,
                    onLaunchGame = {
                        val game = SUPPORTED_GAMES.firstOrNull { it.key == activeSession.gameKey }
                        val pkg = game?.let { findInstalledPackage(context, it.packageNames) }
                        if (pkg != null) {
                            launchGamePackage(context, pkg)
                        }
                    },
                    onClaim = { onClaimSession(activeSession.id) },
                    onCancel = { onCancelSession(activeSession.id) }
                )
            }
        }

        // Supported Games Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Supported Games",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onOpenSpinTab,
                    modifier = Modifier.testTag("home_spin_shortcut")
                ) {
                    Text(
                        text = "Bonus Spin →",
                        color = ChampionGold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Supported Game Cards: PUBG Mobile, BGMI, Free Fire
        items(SUPPORTED_GAMES, key = { it.key }) { game ->
            val connected = connectedMap[game.key]
            val installedPkg = remember(game.key) { findInstalledPackage(context, game.packageNames) }
            val isThisGameActive = activeSession?.gameKey == game.key

            SupportedGameCard(
                game = game,
                connectedGame = connected,
                installedPackage = installedPkg,
                hasAnyActiveSession = activeSession != null,
                isThisGameActive = isThisGameActive,
                isBusy = isBusy,
                onConnectClick = { gameToConnect = game },
                onStartSessionClick = {
                    onStartSession(game)
                    if (installedPkg != null) {
                        launchGamePackage(context, installedPkg)
                    }
                }
            )
        }

        // Recent Completed Sessions
        val completedSessions = sessions.filter { it.status == "claimed" }.take(5)
        if (completedSessions.isNotEmpty()) {
            item {
                Text(
                    text = "Verified Sessions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(completedSessions, key = { it.id }) { sess ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Claimed",
                                tint = EmeraldSuccess
                            )
                            Column {
                                Text(
                                    text = sess.gameTitle,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "1 Hour Verified",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "+${sess.rewardCoins} Coins",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampionGold
                        )
                    }
                }
            }
        }
    }

    // Connect Game Dialog
    gameToConnect?.let { game ->
        val existing = connectedMap[game.key]
        ConnectGameDialog(
            game = game,
            existingPlayerId = existing?.inGamePlayerId.orEmpty(),
            existingUsername = existing?.inGameUsername.orEmpty(),
            isBusy = isBusy,
            onDismiss = { gameToConnect = null },
            onConfirm = { playerId, username ->
                onConnectGame(game, playerId, username) {
                    gameToConnect = null
                }
            }
        )
    }

    // Reward Claimed Celebration Dialog
    if (lastClaimedCoins != null) {
        AlertDialog(
            onDismissRequest = onDismissClaimCelebration,
            containerColor = CardSurface,
            icon = {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Reward Claimed",
                    tint = ChampionGold,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Congratulations!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ChampionGold
                )
            },
            text = {
                Text(
                    text = "Your 1-hour gaming session was verified by the backend. +$lastClaimedCoins Coins have been credited to your wallet!",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismissClaimCelebration,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Awesome")
                }
            }
        )
    }
}

@Composable
private fun ActiveSessionCard(
    session: GameSession,
    nowEpochSeconds: Long,
    isBusy: Boolean,
    onLaunchGame: () -> Unit,
    onClaim: () -> Unit,
    onCancel: () -> Unit
) {
    val startedSec = session.startedAt?.seconds ?: nowEpochSeconds
    val elapsedSec = (nowEpochSeconds - startedSec).coerceAtLeast(0L)
    val remainingSec = (session.targetDurationSeconds - elapsedSec).coerceAtLeast(0L)
    val progress = (elapsedSec.toFloat() / session.targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
    val isReadyToClaim = remainingSec == 0L

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, if (isReadyToClaim) EmeraldSuccess else ElectricCyan, RoundedCornerShape(22.dp))
            .testTag("active_session_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SportsEsports,
                    contentDescription = null,
                    tint = ElectricCyan
                )
                Text(
                    text = "Playing ${session.gameTitle}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (isReadyToClaim) EmeraldSuccess else NeonPurple,
                    trackColor = ObsidianBg,
                    strokeWidth = 10.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDurationHms(remainingSec),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isReadyToClaim) EmeraldSuccess else ChampionGold
                    )
                    Text(
                        text = if (isReadyToClaim) "Ready to Claim!" else "Time Left",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onLaunchGame,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("launch_game_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Game")
                }

                Button(
                    onClick = onClaim,
                    enabled = isReadyToClaim && !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("claim_session_button")
                ) {
                    Text(
                        text = "Claim +1,000",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = onCancel,
                enabled = !isBusy,
                modifier = Modifier.testTag("cancel_session_button")
            ) {
                Text(
                    text = "Cancel Session",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun SupportedGameCard(
    game: SupportedGame,
    connectedGame: ConnectedGame?,
    installedPackage: String?,
    hasAnyActiveSession: Boolean,
    isThisGameActive: Boolean,
    isBusy: Boolean,
    onConnectClick: () -> Unit,
    onStartSessionClick: () -> Unit
) {
    val accent = Color(game.accentHex)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (connectedGame != null) accent.copy(alpha = 0.6f) else OutlineViolet, RoundedCornerShape(18.dp))
            .testTag("game_card_${game.key}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.18f))
                        .border(1.5.dp, accent, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = game.badgeText,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (connectedGame != null) {
                            "Linked: ${connectedGame.inGameUsername} (${connectedGame.inGamePlayerId})"
                        } else {
                            game.subtitle
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = if (installedPackage != null) "Installed on device" else "Game not detected on device",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (installedPackage != null) EmeraldSuccess else TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onConnectClick,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("connect_${game.key}_button")
                ) {
                    Icon(
                        imageVector = if (connectedGame != null) Icons.Filled.VerifiedUser else Icons.Filled.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (connectedGame != null) "Edit Link" else "Connect")
                }

                Button(
                    onClick = onStartSessionClick,
                    enabled = connectedGame != null && !hasAnyActiveSession && !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("play_${game.key}_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isThisGameActive) "Running..." else "Play 1 Hour")
                }
            }
        }
    }
}

@Composable
private fun ConnectGameDialog(
    game: SupportedGame,
    existingPlayerId: String,
    existingUsername: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var playerId by remember(game.key) { mutableStateOf(existingPlayerId) }
    var username by remember(game.key) { mutableStateOf(existingUsername) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Text(
                text = "Connect ${game.title}",
                style = MaterialTheme.typography.titleLarge,
                color = ChampionGold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Link your ${game.title} Player ID to enable real-time 1-hour session tracking (+1,000 Coins per hour).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = playerId,
                    onValueChange = {
                        playerId = it.take(40)
                        validationError = null
                    },
                    label = { Text("In-Game Player ID / UID") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("connect_player_id_input")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.take(40)
                        validationError = null
                    },
                    label = { Text("In-Game Character Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("connect_username_input")
                )
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pid = playerId.trim()
                    val uname = username.trim()
                    when {
                        pid.length < 3 -> validationError = "Player ID must be at least 3 characters."
                        uname.length < 2 -> validationError = "Character name must be at least 2 characters."
                        else -> onConfirm(pid, uname)
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = ObsidianBg),
                modifier = Modifier.testTag("confirm_connect_game_button")
            ) {
                Text("Connect Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
