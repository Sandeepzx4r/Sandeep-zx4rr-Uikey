package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ConnectedGame
import com.example.data.UserProfile
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineViolet
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ProfileScreen(
    profile: UserProfile?,
    userEmail: String?,
    connectedGames: List<ConnectedGame>,
    isBusy: Boolean,
    onUpdateDisplayName: (String) -> Unit,
    onDisconnectGame: (String) -> Unit,
    onSignOutClick: () -> Unit
) {
    var showEditNameDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Identity Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, OutlineViolet, RoundedCornerShape(22.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.25f))
                            .border(2.dp, ChampionGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile Avatar",
                            tint = ChampionGold,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName ?: "BlackShark Gamer",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "Player ID: ${profile?.playerCode ?: "---"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampionGold
                        )
                    }

                    IconButton(
                        onClick = { showEditNameDialog = true },
                        modifier = Modifier.testTag("edit_profile_name_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Display Name",
                            tint = ElectricCyan
                        )
                    }
                }
            }
        }

        // Lifetime Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Total Earned",
                    value = String.format(Locale.US, "%,d", profile?.totalEarnedCoins ?: 0L),
                    accent = ChampionGold,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total Redeemed",
                    value = String.format(Locale.US, "%,d", profile?.totalRedeemedCoins ?: 0L),
                    accent = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "1h Sessions",
                    value = (profile?.totalSessionsCompleted ?: 0L).toString(),
                    accent = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total Spins",
                    value = (profile?.totalSpins ?: 0L).toString(),
                    accent = ChampionGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Connected Games List
        item {
            Text(
                text = "Connected Games (${connectedGames.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (connectedGames.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No games connected yet. Connect PUBG Mobile, BGMI, or Free Fire from the Home tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(connectedGames, key = { it.gameKey }) { cg ->
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SportsEsports,
                                contentDescription = null,
                                tint = ElectricCyan
                            )
                            Column {
                                Text(
                                    text = cg.gameTitle,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${cg.inGameUsername} • UID: ${cg.inGamePlayerId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDisconnectGame(cg.gameKey) },
                            enabled = !isBusy,
                            modifier = Modifier.testTag("disconnect_${cg.gameKey}_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Disconnect ${cg.gameTitle}",
                                tint = CrimsonError
                            )
                        }
                    }
                }
            }
        }

        // Sign Out Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSignOutClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showEditNameDialog) {
        var newName by remember { mutableStateOf(profile?.displayName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = CardSurface,
            title = { Text("Edit Display Name", color = ChampionGold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(80) },
                    label = { Text("Gamer Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.trim().isNotEmpty()) {
                            onUpdateDisplayName(newName.trim())
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = ObsidianBg)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        modifier = modifier.border(1.dp, OutlineViolet, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent
            )
        }
    }
}
