package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class BlackSharkRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    suspend fun ensureUserProfile(userId: String, rawDisplayName: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val ref = userDoc(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val safeName = rawDisplayName.trim().ifBlank { "BlackShark Gamer" }.take(80)
                val playerCode = "BS" + uid.filter { it.isLetterOrDigit() }.take(6).uppercase(Locale.US).padEnd(6, '0')
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "playerCode" to playerCode,
                    "coinBalance" to 0L,
                    "totalEarnedCoins" to 0L,
                    "totalRedeemedCoins" to 0L,
                    "totalSessionsCompleted" to 0L,
                    "totalSpins" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            userDoc(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeConnectedGames(userId: String): Flow<List<ConnectedGame>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/connected_games"
        emitAll(
            userDoc(uid).collection("connected_games")
                .snapshots()
                .map { snap -> snap.toObjects(ConnectedGame::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeGameSessions(userId: String): Flow<List<GameSession>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/game_sessions"
        emitAll(
            userDoc(uid).collection("game_sessions")
                .snapshots()
                .map { snap -> snap.toObjects(GameSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSpinLogs(userId: String): Flow<List<SpinLog>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/spin_logs"
        emitAll(
            userDoc(uid).collection("spin_logs")
                .snapshots()
                .map { snap -> snap.toObjects(SpinLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRedemptions(userId: String): Flow<List<RedemptionRequest>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/redemptions"
        emitAll(
            userDoc(uid).collection("redemptions")
                .snapshots()
                .map { snap -> snap.toObjects(RedemptionRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun connectGame(
        userId: String,
        game: SupportedGame,
        inGamePlayerId: String,
        inGameUsername: String
    ): Result<Unit> {
        val docPath = "users/$userId/connected_games/${game.key}"
        return try {
            val uid = requireUserId(userId)
            val cleanPlayerId = inGamePlayerId.trim()
            val cleanUsername = inGameUsername.trim()
            require(cleanPlayerId.length in 3..40) { "In-game Player ID must be 3 to 40 characters." }
            require(cleanUsername.length in 2..40) { "In-game Username must be 2 to 40 characters." }

            val ref = userDoc(uid).collection("connected_games").document(game.key)
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "inGamePlayerId" to cleanPlayerId,
                        "inGameUsername" to cleanUsername,
                        "connected" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "gameKey" to game.key,
                        "gameTitle" to game.title,
                        "inGamePlayerId" to cleanPlayerId,
                        "inGameUsername" to cleanUsername,
                        "connected" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun disconnectGame(userId: String, gameKey: String): Result<Unit> {
        val docPath = "users/$userId/connected_games/$gameKey"
        return try {
            val uid = requireUserId(userId)
            userDoc(uid).collection("connected_games").document(gameKey).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun startGameSession(userId: String, game: SupportedGame): Result<String> {
        var docPath = "users/$userId/game_sessions"
        return try {
            val uid = requireUserId(userId)
            val ref = userDoc(uid).collection("game_sessions").document()
            docPath = ref.path
            val payload = mapOf(
                "userId" to uid,
                "gameKey" to game.key,
                "gameTitle" to game.title,
                "status" to "active",
                "targetDurationSeconds" to 3600L,
                "rewardCoins" to 1000L,
                "startedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun claimGameSession(userId: String, sessionId: String): Result<Long> {
        val docPath = "users/$userId/game_sessions/$sessionId"
        return try {
            val uid = requireUserId(userId)
            require(sessionId.isNotBlank()) { "Session ID is required." }
            val sessionRef = userDoc(uid).collection("game_sessions").document(sessionId)
            val profileRef = userDoc(uid)

            val batch = db.batch()
            batch.update(
                sessionRef,
                mapOf(
                    "status" to "claimed",
                    "completedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                profileRef,
                mapOf(
                    "coinBalance" to FieldValue.increment(1000L),
                    "totalEarnedCoins" to FieldValue.increment(1000L),
                    "totalSessionsCompleted" to FieldValue.increment(1L),
                    "lastActionRefId" to sessionId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(1000L)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun cancelGameSession(userId: String, sessionId: String): Result<Unit> {
        val docPath = "users/$userId/game_sessions/$sessionId"
        return try {
            val uid = requireUserId(userId)
            require(sessionId.isNotBlank()) { "Session ID is required." }
            userDoc(uid).collection("game_sessions").document(sessionId).update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordSpinReward(userId: String, segment: SpinWheelSegment): Result<SpinWheelSegment> {
        var docPath = "users/$userId/spin_logs"
        return try {
            val uid = requireUserId(userId)
            val spinRef = userDoc(uid).collection("spin_logs").document()
            docPath = spinRef.path
            val profileRef = userDoc(uid)

            val batch = db.batch()
            batch.set(
                spinRef,
                mapOf(
                    "userId" to uid,
                    "rewardCoins" to segment.rewardCoins,
                    "segmentLabel" to segment.label,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                profileRef,
                mapOf(
                    "coinBalance" to FieldValue.increment(segment.rewardCoins),
                    "totalEarnedCoins" to FieldValue.increment(segment.rewardCoins),
                    "totalSpins" to FieldValue.increment(1L),
                    "lastSpinAt" to FieldValue.serverTimestamp(),
                    "lastActionRefId" to spinRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(segment)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitRedemption(
        userId: String,
        method: RedeemMethod,
        destination: String,
        tier: RedeemTier
    ): Result<RedemptionRequest> {
        var docPath = "users/$userId/redemptions"
        return try {
            val uid = requireUserId(userId)
            val cleanDest = destination.trim()
            require(cleanDest.length in 4..100) {
                if (method == RedeemMethod.UPI) "Enter a valid UPI ID (e.g. name@upi)."
                else "Enter a valid email address for Google Play code delivery."
            }

            val redRef = userDoc(uid).collection("redemptions").document()
            docPath = redRef.path
            val profileRef = userDoc(uid)
            val referenceCode = "BSG-" + redRef.id.take(8).uppercase(Locale.US)

            val batch = db.batch()
            batch.set(
                redRef,
                mapOf(
                    "userId" to uid,
                    "method" to method.wireValue,
                    "destination" to cleanDest,
                    "coinsCost" to tier.coinsCost,
                    "amountInr" to tier.amountInr,
                    "status" to "submitted",
                    "referenceCode" to referenceCode,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                profileRef,
                mapOf(
                    "coinBalance" to FieldValue.increment(-tier.coinsCost),
                    "totalRedeemedCoins" to FieldValue.increment(tier.coinsCost),
                    "lastActionRefId" to redRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success(
                RedemptionRequest(
                    id = redRef.id,
                    userId = uid,
                    method = method.wireValue,
                    destination = cleanDest,
                    coinsCost = tier.coinsCost,
                    amountInr = tier.amountInr,
                    status = "submitted",
                    referenceCode = referenceCode
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(userId: String, newName: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val clean = newName.trim()
            require(clean.length in 1..80) { "Display name must be 1 to 80 characters." }
            userDoc(uid).update(
                mapOf(
                    "displayName" to clean,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
