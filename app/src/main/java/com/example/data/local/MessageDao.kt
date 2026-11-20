package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun observeAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationPeerAddress = :peerAddress ORDER BY timestamp ASC")
    fun observeMessagesForPeer(peerAddress: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageType = 'SOS' AND isOutgoing = 0 AND isAcknowledged = 0 ORDER BY timestamp DESC")
    fun observeActiveIncomingSos(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageType = 'SOS' ORDER BY timestamp DESC")
    fun observeAllSosMessages(): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages")
    fun observeMessageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isAcknowledged = 1 WHERE id = :messageId")
    suspend fun markMessageAcknowledged(messageId: String)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE id = :messageId")
    suspend fun updateDeliveryStatus(messageId: String, status: String)

    @Query("DELETE FROM messages WHERE conversationPeerAddress = :peerAddress")
    suspend fun deleteConversation(peerAddress: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    // Seen packets (Deduplication)
    @Query("SELECT EXISTS(SELECT 1 FROM seen_packets WHERE packetId = :packetId)")
    suspend fun isPacketSeen(packetId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markPacketSeen(packet: SeenPacketEntity)

    @Query("SELECT COUNT(*) FROM seen_packets")
    fun observeSeenPacketCount(): Flow<Int>

    @Query("DELETE FROM seen_packets WHERE seenAt < :cutoffTimestamp")
    suspend fun pruneOldSeenPackets(cutoffTimestamp: Long)

    @Query("DELETE FROM seen_packets")
    suspend fun clearSeenPackets()

    // Pending relays (Store-and-Forward)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingRelay(relay: PendingRelayEntity)

    @Query("SELECT * FROM pending_relays WHERE expiresAt > :now ORDER BY createdAt ASC")
    fun observePendingRelays(now: Long): Flow<List<PendingRelayEntity>>

    @Query("SELECT * FROM pending_relays WHERE expiresAt > :now ORDER BY createdAt ASC")
    suspend fun getActivePendingRelays(now: Long): List<PendingRelayEntity>

    @Query("DELETE FROM pending_relays WHERE expiresAt <= :now")
    suspend fun deleteExpiredRelays(now: Long)

    @Query("DELETE FROM pending_relays")
    suspend fun clearPendingRelays()

    // Relay logs
    @Insert
    suspend fun insertRelayLog(log: RelayLogEntity)

    @Query("SELECT * FROM relay_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeRelayLogs(): Flow<List<RelayLogEntity>>

    @Query("DELETE FROM relay_logs")
    suspend fun clearRelayLogs()
}
