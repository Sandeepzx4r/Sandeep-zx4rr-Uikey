package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.DeepVioletSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineViolet
import com.example.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Ready for interactive Google Sign-In */ },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ColorBgTop, DeepVioletSurface, ObsidianBg)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Brand emblem
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonPurple.copy(alpha = 0.45f), CardSurface)
                        )
                    )
                    .border(2.dp, ChampionGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SportsEsports,
                    contentDescription = "Black Shark Gaming Emblem",
                    tint = ChampionGold,
                    modifier = Modifier.size(50.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BLACK SHARK",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "GAMING",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonPurple,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Play  •  Spin  •  Earn  •  Redeem",
                    style = MaterialTheme.typography.labelLarge,
                    color = ChampionGold,
                    textAlign = TextAlign.Center
                )
            }

            // Core highlights
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OutlineViolet, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Filled.SportsEsports,
                        tint = ElectricCyan,
                        title = "1 Hour Gaming = 1,000 Coins",
                        subtitle = "Connect PUBG, BGMI, or Free Fire with real-time server session verification."
                    )
                    FeatureRow(
                        icon = Icons.Filled.Casino,
                        tint = NeonPurple,
                        title = "Spin Wheel Rewards",
                        subtitle = "Spin to earn bonus coins directly into your cloud wallet."
                    )
                    FeatureRow(
                        icon = Icons.Filled.EmojiEvents,
                        tint = ChampionGold,
                        title = "Fast UPI & Google Play Redeem",
                        subtitle = "Convert 1,000 Coins to ₹1,000 UPI or Google Play Redeem Card requests."
                    )
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    errorText = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorText = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChampionGold,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = ObsidianBg,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Your wallet, sessions, spins, and redemptions sync securely with Firebase.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val ColorBgTop = androidx.compose.ui.graphics.Color(0xFF230D45)

@Composable
private fun FeatureRow(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
