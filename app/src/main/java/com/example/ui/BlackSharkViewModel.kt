package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BlackSharkRepository
import com.example.data.ConnectedGame
import com.example.data.GameSession
import com.example.data.RedeemMethod
import com.example.data.RedeemTier
import com.example.data.RedemptionRequest
import com.example.data.SPIN_WHEEL_SEGMENTS
import com.example.data.SpinLog
import com.example.data.SpinWheelSegment
import com.example.data.SupportedGame
import com.example.data.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ActionFeedbackState(
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val lastWonSpinSegment: SpinWheelSegment? = null,
    val lastClaimedRewardCoins: Long? = null,
    val lastSubmittedRedemption: RedemptionRequest? = null
)

class BlackSharkViewModel(
    private val repository: BlackSharkRepository,
    private val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private val _feedback = MutableStateFlow(ActionFeedbackState())
    val feedback: StateFlow<ActionFeedbackState> = _feedback.asStateFlow()

    private val _nowEpochSeconds = MutableStateFlow(System.currentTimeMillis() / 1000L)
    val nowEpochSeconds: StateFlow<Long> = _nowEpochSeconds.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toSafeUiState("Failed to load profile")

    val connectedGamesState: StateFlow<UiState<List<ConnectedGame>>> =
        repository.observeConnectedGames(currentUserId).toSafeUiState("Failed to load connected games")

    val gameSessionsState: StateFlow<UiState<List<GameSession>>> =
        repository.observeGameSessions(currentUserId)
            .map { list -> list.sortedByDescending { it.startedAt ?: Timestamp(0, 0) } }
            .toSafeUiState("Failed to load game sessions")

    val spinLogsState: StateFlow<UiState<List<SpinLog>>> =
        repository.observeSpinLogs(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toSafeUiState("Failed to load spin history")

    val redemptionsState: StateFlow<UiState<List<RedemptionRequest>>> =
        repository.observeRedemptions(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toSafeUiState("Failed to load redemption history")

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, initialDisplayName)
                .onFailure { err ->
                    _feedback.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to initialize profile") }
                }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _nowEpochSeconds.value = System.currentTimeMillis() / 1000L
            }
        }
    }

    private fun <T> Flow<T>.toSafeUiState(fallbackMsg: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMsg, rootError)
                emit(UiState.Error(rootError.message ?: fallbackMsg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    fun clearMessages() {
        _feedback.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun dismissClaimCelebration() {
        _feedback.update { it.copy(lastClaimedRewardCoins = null) }
    }

    fun dismissRedemptionConfirmation() {
        _feedback.update { it.copy(lastSubmittedRedemption = null) }
    }

    fun connectGame(game: SupportedGame, inGamePlayerId: String, inGameUsername: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _feedback.update { it.copy(isBusy = true, errorMessage = null) }
            repository.connectGame(currentUserId, game, inGamePlayerId, inGameUsername)
                .onSuccess {
                    _feedback.update { it.copy(isBusy = false, infoMessage = "${game.title} connected!") }
                    onSuccess()
                }
                .onFailure { e ->
                    _feedback.update { it.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Failed to connect ${game.title}") }
                }
        }
    }

    fun disconnectGame(gameKey: String) {
        viewModelScope.launch {
            _feedback.update { it.copy(isBusy = true, errorMessage = null) }
            repository.disconnectGame(currentUserId, gameKey)
                .onSuccess {
                    _feedback.update { it.copy(isBusy = false, infoMessage = "Game disconnected.") }
                }
                .onFailure { e ->
                    _feedback.update { it.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Failed to disconnect game") }
                }
        }
    }

    fun startGameSession(game: SupportedGame, onStarted: (String) -> Unit = {}) {
        viewModelScope.launch {
            _feedback.update { it.copy(isBusy = true, errorMessage = null) }
            repository.startGameSession(currentUserId, game)
                .onSuccess { sessionId ->
                    _feedback.update { it.copy(isBusy = false, infoMessage = "1-hour session started for ${game.title}!") }
                    onStarted(sessionId)
                }
                .onFailure { e ->
                    _feedback.update { it.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Unable to start session") }
                }
        }
    }

    fun claimGameSession(sessionId: String) {
        viewModelScope.launch {
            _feedback.update { it.copy(isBusy = true, errorMessage = null) }
            repository.claimGameSession(currentUserId, sessionId)
                .onSuccess { coins ->
                    _feedback.update {
                        it.copy(
                            isBusy = false,
                            lastClaimedRewardCoins = coins,
                            infoMessage = "+$coins coins credited!"
                        )
                    }
                }
                .onFailure { e ->
                    _feedback.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = "Server verification pending: complete the full 1-hour session before claiming."
                        )
                    }
                }
        }
    }

    fun cancelGameSession(sessionId: String) {
        viewModelScope.launch {
            _feedback.update { it.copy(isBusy = true, errorMessage = null) }
            repository.cancelGameSession(currentUserId, sessionId)
                .onSuccess {
                    _feedback.update { it.copy(isBusy = false, infoMessage = "Active session cancelled.") }
                }
                .onFailure { e ->
                    _feedback.update { it.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Unable to cancel session") }
                }
        }
    }

    fun spinWheel() {
        viewModelScope.launch {
            _feedback.update { it.copy(isBusy = true, errorMessage = null) }
            val chosen = SPIN_WHEEL_SEGMENTS[Random.nextInt(SPIN_WHEEL_SEGMENTS.size)]
            repository.recordSpinReward(currentUserId, chosen)
                .onSuccess { segment ->
                    _feedback.update {
                        it.copy(
                            isBusy = false,
                            lastWonSpinSegment = segment,
                            infoMessage = "Spin Wheel won +${segment.rewardCoins} coins!"
                        )
                    }
                }
                .onFailure { e ->
                    _feedback.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = "Cooldown active — please wait 60 seconds between spins."
                        )
                    }
                }
        }
    }

    fun submitRedemption(method: RedeemMethod, destination: String, tier: RedeemTier) {
        viewModelScope.launch {
            _feedback.update { it.copy(isBusy = true, errorMessage = null) }
            repository.submitRedemption(currentUserId, method, destination, tier)
                .onSuccess { request ->
                    _feedback.update {
                        it.copy(
                            isBusy = false,
                            lastSubmittedRedemption = request,
                            infoMessage = "Redemption request ${request.referenceCode} submitted!"
                        )
                    }
                }
                .onFailure { e ->
                    _feedback.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = e.localizedMessage ?: "Redemption failed. Check your coin balance."
                        )
                    }
                }
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            _feedback.update { it.copy(isBusy = true, errorMessage = null) }
            repository.updateDisplayName(currentUserId, newName)
                .onSuccess {
                    _feedback.update { it.copy(isBusy = false, infoMessage = "Display name updated.") }
                }
                .onFailure { e ->
                    _feedback.update { it.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Update failed.") }
                }
        }
    }

    private companion object {
        const val TAG = "BlackSharkVM"
    }
}
