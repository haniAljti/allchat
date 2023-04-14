package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.local.datastore.xmpp.asRosterItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jivesoftware.smack.roster.packet.RosterPacket
import org.jivesoftware.smack.roster.rosterstore.RosterStore
import org.jxmpp.jid.Jid

class RosterDataStore(
    private val clientDataSource: XmppClientDataSource
) : RosterStore {

    override fun getEntries(): MutableList<RosterPacket.Item>? = runBlocking {
        try {
            clientDataSource.rosterItems.first()
                .map { it.asSmackRosterItem() }
                .toMutableList()
        } catch (e: Exception) {
            Logger.e(e)
            null
        }
    }

    override fun getEntry(bareJid: Jid?): RosterPacket.Item = runBlocking {
        clientDataSource.rosterItems
            .first()
            .first { it.jid == bareJid?.toString() }
            .asSmackRosterItem()
    }

    override fun getRosterVersion(): String = runBlocking {
        clientDataSource.rosterVersion.first()
    }

    override fun addEntry(item: RosterPacket.Item?, version: String?): Boolean = runBlocking {
        return@runBlocking try {
            item?.asRosterItem()?.let { clientDataSource.addRosterItem(it) }
            version?.let { clientDataSource.updateRosterVersion(version) }
            true
        } catch (e: Exception) {
            Logger.e(e)
            false
        }
    }

    override fun resetEntries(
        items: MutableCollection<RosterPacket.Item>?,
        version: String?
    ): Boolean = runBlocking {
        return@runBlocking try {
            clientDataSource.resetRosterItems(items?.map { it.asRosterItem() } ?: listOf())
            version?.let { clientDataSource.updateRosterVersion(version) }
            true
        } catch (e: Exception) {
            Logger.e(e)
            false
        }
    }

    override fun removeEntry(bareJid: Jid, version: String): Boolean = runBlocking {
        return@runBlocking try {
            clientDataSource.removeRosterItem(bareJid.toString())
            clientDataSource.updateRosterVersion(version)
            true
        } catch (e: Exception) {
            Logger.e(e)
            false
        }
    }

    override fun resetStore() = runBlocking {
        try {
            clientDataSource.resetRosterItems(listOf())
            clientDataSource.updateRosterVersion("")
        } catch (e: Exception) {
            Logger.e(e)
        }
    }
}