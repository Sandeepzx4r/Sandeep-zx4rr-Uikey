package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.REDEEM_TIERS
import com.example.data.RedeemMethod
import com.example.data.RedeemTier
import com.example.data.RedemptionRequest
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
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RedeemScreen(
    profile: UserProfile?,
    redemptions: List<RedemptionRequest>,
    lastSubmittedRedemption: RedemptionRequest?,
    isBusy: Boolean,
    onSubmitRedemption: (RedeemMethod, String, RedeemTier) -> Unit,
    onDismissConfirmation: () -> Unit
) {
    val balance = profile?.coinBalance ?: 0L
    var selectedMethod by remember { mutableStateOf(RedeemMethod.UPI) }
    var destination by remember { mutableStateOf("") }
    var destinationError by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Wallet Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF3E1075), Color(0xFF210B45))
                        )
                    )
                    .border(1.5.dp, ChampionGold, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "YOUR COINS",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = "Coins",
                                tint = ChampionGold,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%,d", balance),
                                style = MaterialTheme.typography.displaySmall,
                                color = ChampionGold
                            )
                        }
                        Text(
                            text = "1,000 Coins = ₹1,000 (UPI / Google Play Card)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        // Withdrawal Method Selector: UPI vs Google Play Card
        item {
            Text(
                text = "Select Withdrawal Method",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MethodCard(
                    title = "UPI Transfer",
                    subtitle = "Instant UPI Request",
                    selected = selectedMethod == RedeemMethod.UPI,
                    icon = Icons.Filled.AccountBalanceWallet,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("method_upi_card"),
                    onClick = {
                        selectedMethod = RedeemMethod.UPI
                        destinationError = null
                    }
                )
                MethodCard(
                    title = "Google Play",
                    subtitle = "Redeem Card Code",
                    selected = selectedMethod == RedeemMethod.GOOGLE_PLAY,
                    icon = Icons.Filled.CardGiftcard,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("method_google_play_card"),
                    onClick = {
                        selectedMethod = RedeemMethod.GOOGLE_PLAY
                        destinationError = null
                    }
                )
            }
        }

        // UPI ID / Email Input
        item {
            OutlinedTextField(
                value = destination,
                onValueChange = {
                    destination = it.take(100)
                    destinationError = null
                },
                label = {
                    Text(
                        if (selectedMethod == RedeemMethod.UPI) "Enter UPI ID (e.g. player@upi)"
                        else "Enter Email for Google Play Redeem Code"
                    )
                },
                singleLine = true,
                isError = destinationError != null,
                supportingText = {
                    if (destinationError != null) {
                        Text(destinationError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Fast 2-second backend submission & balance lock.", color = TextSecondary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("redeem_destination_input")
            )
        }

        // Redeem Tiers
        item {
            Text(
                text = "Redeem Options",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(REDEEM_TIERS, key = { it.coinsCost }) { tier ->
            val hasEnoughCoins = balance >= tier.coinsCost
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (tier.coinsCost == 1000L) 1.5.dp else 1.dp,
                        color = if (tier.coinsCost == 1000L) ChampionGold else OutlineViolet,
                        shape = RoundedCornerShape(18.dp)
                    )
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
                            text = "${tier.label} ${if (selectedMethod == RedeemMethod.UPI) "UPI" else "Play Card"}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format(Locale.US, "%,d", tier.coinsCost)} Coins Required",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasEnoughCoins) EmeraldSuccess else TextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            val clean = destination.trim()
                            val valid = if (selectedMethod == RedeemMethod.UPI) {
                                clean.contains("@") && clean.length >= 5
                            } else {
                                clean.contains("@") && clean.contains(".") && clean.length >= 6
                            }
                            if (!valid) {
                                destinationError = if (selectedMethod == RedeemMethod.UPI) {
                                    "Please enter a valid UPI ID (e.g. name@okaxis)."
                                } else {
                                    "Please enter a valid email address."
                                }
                            } else {
                                onSubmitRedemption(selectedMethod, clean, tier)
                            }
                        },
                        enabled = hasEnoughCoins && !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChampionGold,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("redeem_tier_${tier.coinsCost}_button")
                    ) {
                        Text(
                            text = if (hasEnoughCoins) "Redeem" else "Need More",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Redeem History
        item {
            Text(
                text = "Redeem History (${redemptions.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (redemptions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No redemptions yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Earn coins via 1-hour game sessions or Spin Wheel, then redeem for UPI or Google Play Cards.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(redemptions, key = { it.id }) { req ->
                val method = RedeemMethod.fromWire(req.method)
                val dateLabel = req.createdAt?.toDate()?.let {
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(it)
                } ?: "Just now"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OutlineViolet, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "₹${req.amountInr} • ${method.displayTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ChampionGold
                            )
                            Text(
                                text = req.destination,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ref: ${req.referenceCode} • $dateLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(EmeraldSuccess.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = req.status.uppercase(Locale.US),
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }
    }

    // Redemption Submitted Confirmation Dialog
    if (lastSubmittedRedemption != null) {
        val method = RedeemMethod.fromWire(lastSubmittedRedemption.method)
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            containerColor = CardSurface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Submitted",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(38.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Redemption Submitted!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = EmeraldSuccess,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "₹${lastSubmittedRedemption.amountInr}",
                        style = MaterialTheme.typography.displaySmall,
                        color = ChampionGold
                    )
                    Text(
                        text = "${method.displayTitle} request for ${lastSubmittedRedemption.destination} has been recorded and coins deducted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Reference ID", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                text = lastSubmittedRedemption.referenceCode,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(lastSubmittedRedemption.referenceCode))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Reference ID",
                                tint = ChampionGold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissConfirmation,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun MethodCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) CardSurfaceVariant else CardSurface
        ),
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) ChampionGold else OutlineViolet,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) ChampionGold else TextSecondary,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}
