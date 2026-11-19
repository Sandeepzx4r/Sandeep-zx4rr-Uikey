package com.example.data

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class SupportedGame(
    val key: String,
    val title: String,
    val subtitle: String,
    val packageNames: List<String>,
    val accentHex: Long,
    val badgeText: String
)

val SUPPORTED_GAMES = listOf(
    SupportedGame(
        key = "pubg",
        title = "PUBG Mobile",
        subtitle = "Battlegrounds Global",
        packageNames = listOf("com.tencent.ig"),
        accentHex = 0xFFFFB300,
        badgeText = "PUBG"
    ),
    SupportedGame(
        key = "bgmi",
        title = "BGMI",
        subtitle = "Battlegrounds Mobile India",
        packageNames = listOf("com.pubg.imobile"),
        accentHex = 0xFF00E5FF,
        badgeText = "BGMI"
    ),
    SupportedGame(
        key = "free_fire",
        title = "Free Fire",
        subtitle = "Garena Free Fire / MAX",
        packageNames = listOf("com.dts.freefiremax", "com.dts.freefireth"),
        accentHex = 0xFFFF5722,
        badgeText = "FF"
    )
)

data class SpinWheelSegment(
    val index: Int,
    val rewardCoins: Long,
    val label: String,
    val color: Color
)

val SPIN_WHEEL_SEGMENTS = listOf(
    SpinWheelSegment(0, 10L, "10 Coins", Color(0xFF4C1D95)),
    SpinWheelSegment(1, 50L, "50 Coins", Color(0xFF7C3AED)),
    SpinWheelSegment(2, 25L, "25 Coins", Color(0xFF3B0764)),
    SpinWheelSegment(3, 100L, "100 Coins", Color(0xFFD97706)),
    SpinWheelSegment(4, 150L, "150 Coins", Color(0xFF6D28D9)),
    SpinWheelSegment(5, 200L, "200 Coins", Color(0xFF0E7490)),
    SpinWheelSegment(6, 250L, "250 Coins", Color(0xFF9333EA)),
    SpinWheelSegment(7, 500L, "500 Coins", Color(0xFFF59E0B))
)

data class RedeemTier(
    val coinsCost: Long,
    val amountInr: Long,
    val label: String
)

val REDEEM_TIERS = listOf(
    RedeemTier(500L, 500L, "₹500"),
    RedeemTier(1000L, 1000L, "₹1,000"),
    RedeemTier(2000L, 2000L, "₹2,000")
)

enum class RedeemMethod(val wireValue: String, val displayTitle: String) {
    UPI("upi", "UPI Transfer"),
    GOOGLE_PLAY("google_play", "Google Play Card");

    companion object {
        fun fromWire(raw: String): RedeemMethod =
            entries.firstOrNull { it.wireValue == raw } ?: UPI
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val playerCode: String = "",
    val coinBalance: Long = 0L,
    val totalEarnedCoins: Long = 0L,
    val totalRedeemedCoins: Long = 0L,
    val totalSessionsCompleted: Long = 0L,
    val totalSpins: Long = 0L,
    @ServerTimestamp val lastSpinAt: Timestamp? = null,
    val lastActionRefId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConnectedGame(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameKey: String = "",
    val gameTitle: String = "",
    val inGamePlayerId: String = "",
    val inGameUsername: String = "",
    val connected: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameKey: String = "",
    val gameTitle: String = "",
    val status: String = "active",
    val targetDurationSeconds: Long = 3600L,
    val rewardCoins: Long = 1000L,
    @ServerTimestamp val startedAt: Timestamp? = null,
    @ServerTimestamp val completedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SpinLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val rewardCoins: Long = 0L,
    val segmentLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RedemptionRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val method: String = "upi",
    val destination: String = "",
    val coinsCost: Long = 0L,
    val amountInr: Long = 0L,
    val status: String = "submitted",
    val referenceCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
