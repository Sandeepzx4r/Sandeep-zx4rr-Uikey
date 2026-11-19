package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.SPIN_WHEEL_SEGMENTS
import com.example.data.SpinLog
import com.example.data.SpinWheelSegment
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
fun SpinWheelScreen(
    profile: UserProfile?,
    spinLogs: List<SpinLog>,
    lastWonSegment: SpinWheelSegment?,
    nowEpochSeconds: Long,
    isBusy: Boolean,
    onSpinClick: () -> Unit
) {
    val lastSpinSec = profile?.lastSpinAt?.seconds
    val cooldownRemaining = if (lastSpinSec != null) {
        (60L - (nowEpochSeconds - lastSpinSec)).coerceAtLeast(0L)
    } else {
        0L
    }
    val canSpin = cooldownRemaining == 0L && !isBusy

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SPIN WHEEL",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ChampionGold
                )
                Text(
                    text = "Win up to 500 Bonus Coins every spin (server-verified)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Static / clean-architecture Spin Wheel Canvas (ready for custom animations later)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, OutlineViolet, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SpinWheelCanvas(
                            highlightedIndex = lastWonSegment?.index,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(ObsidianBg, CircleShape)
                                .border(2.dp, ChampionGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Casino,
                                contentDescription = "Spin Center",
                                tint = ChampionGold,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Legend of wheel prize tiers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SPIN_WHEEL_SEGMENTS.take(4).forEach { seg ->
                            PrizeChip(seg, isHighlighted = lastWonSegment?.index == seg.index)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SPIN_WHEEL_SEGMENTS.drop(4).forEach { seg ->
                            PrizeChip(seg, isHighlighted = lastWonSegment?.index == seg.index)
                        }
                    }

                    if (lastWonSegment != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSurfaceVariant),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EmeraldSuccess, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stars,
                                    contentDescription = null,
                                    tint = ChampionGold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Last Spin Result: +${lastWonSegment.rewardCoins} Coins Credited!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onSpinClick,
                        enabled = canSpin,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChampionGold,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("spin_wheel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Casino,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (cooldownRemaining > 0L) {
                                "Next Spin in ${cooldownRemaining}s"
                            } else if (isBusy) {
                                "Verifying Spin..."
                            } else {
                                "SPIN NOW"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Real Spin History from Firestore
        item {
            Text(
                text = "Spin History (${spinLogs.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (spinLogs.isEmpty()) {
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
                            imageVector = Icons.Filled.Casino,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No spins yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap SPIN NOW above to claim your first verified Spin Wheel reward.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(spinLogs.take(15), key = { it.id }) { log ->
                val formattedDate = log.createdAt?.toDate()?.let {
                    SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(it)
                } ?: "Just now"

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
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = null,
                                tint = ChampionGold
                            )
                            Column {
                                Text(
                                    text = log.segmentLabel,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "+${log.rewardCoins} Coins",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpinWheelCanvas(
    highlightedIndex: Int?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val diameter = size.minDimension - 16.dp.toPx()
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val wheelSize = Size(diameter, diameter)
        val sweep = 360f / SPIN_WHEEL_SEGMENTS.size

        SPIN_WHEEL_SEGMENTS.forEachIndexed { idx, segment ->
            val startAngle = -90f + idx * sweep
            drawArc(
                color = if (highlightedIndex == idx) ChampionGold else segment.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = wheelSize
            )
            drawArc(
                color = Color(0xFF0A0614),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = wheelSize,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Outer gold rim
        drawCircle(
            color = ChampionGold,
            radius = diameter / 2f,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        // Top pointer triangle
        val pointerPath = Path().apply {
            val cx = size.width / 2f
            val topY = 0f
            moveTo(cx, topY + 22.dp.toPx())
            lineTo(cx - 11.dp.toPx(), topY)
            lineTo(cx + 11.dp.toPx(), topY)
            close()
        }
        drawPath(path = pointerPath, color = ChampionGold)
    }
}

@Composable
private fun PrizeChip(segment: SpinWheelSegment, isHighlighted: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (isHighlighted) ChampionGold else segment.color.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (isHighlighted) ChampionGold else OutlineViolet,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "+${segment.rewardCoins}",
            style = MaterialTheme.typography.labelMedium,
            color = if (isHighlighted) ObsidianBg else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
