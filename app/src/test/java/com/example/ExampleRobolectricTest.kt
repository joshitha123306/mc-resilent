package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MessageEntity
import com.example.data.local.ResilientDatabase
import com.example.data.local.SeenPacketEntity
import com.example.model.PacketEnvelope
import com.example.model.PacketType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: ResilientDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ResilientDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Resilient Connect", appName)
    }

    @Test
    fun `packet envelope round trip serialization preserves fields`() {
        val original = PacketEnvelope(
            messageId = "pkt-123",
            type = PacketType.SOS,
            originNodeId = "NODE-A1",
            senderName = "Responder-A",
            targetAddress = PacketEnvelope.BROADCAST_ADDRESS,
            content = "SOS ALERT: Medical assistance needed",
            timestamp = 1700000000000L,
            hopCount = 1,
            maxHops = 3,
            routeTrace = "Responder-A -> Relay-B"
        )
        val jsonLine = original.toJsonLine()
        val parsed = PacketEnvelope.fromJsonLine(jsonLine)
        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test
    fun `room dao stores messages and deduplicates seen packets`() = runTest {
        val dao = db.messageDao()
        dao.markPacketSeen(SeenPacketEntity("pkt-999", 1000L))
        assertTrue(dao.isPacketSeen("pkt-999"))

        val msg = MessageEntity(
            id = "pkt-999",
            conversationPeerAddress = "AA:BB:CC:DD:EE:FF",
            conversationPeerName = "Phone B",
            senderName = "Phone B",
            originNodeId = "NODE-B",
            content = "Offline check-in",
            timestamp = 2000L,
            isOutgoing = false,
            messageType = PacketType.CHAT.name,
            hopCount = 0,
            maxHops = 3,
            routeTrace = "Phone B",
            deliveryStatus = "DELIVERED"
        )
        dao.insertMessage(msg)

        val saved = dao.observeMessagesForPeer("AA:BB:CC:DD:EE:FF").first()
        assertEquals(1, saved.size)
        assertEquals("Offline check-in", saved.first().content)
    }

    @Test
    fun `main activity launches and renders home screen without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
        val activity = controller.get()
        assertNotNull(activity)
        assertEquals("Resilient Connect", activity.getString(R.string.app_name))
    }
}
