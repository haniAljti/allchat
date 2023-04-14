package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.remote.model.CallResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.pep.PepEventListener
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.AccessModel
import org.jivesoftware.smackx.pubsub.PayloadItem
import timber.log.Timber

class PepPublisher(
    connection: XMPPTCPConnection,
    private val pepManager: PepManager = PepManager.getInstanceFor(connection)
) {

    suspend fun <T : ExtensionElement> publishPrivateItem(
        itemId: String,
        maxItemCount: Int,
        item: T
    ): CallResult<Boolean> {
        return try {

            if (pepManager.isSupported) {
                val pubSub = pepManager.pepPubSubManager
                val bookmarksNode =
                    pubSub.getOrCreateLeafNode(item.namespace)

                val nodeConfiguration = bookmarksNode.nodeConfiguration
                val fillableForm = nodeConfiguration.fillableForm

                fillableForm.apply {
                    setPersistentItems(true)
                    accessModel = AccessModel.whitelist
                    if (nodeConfiguration.hasField("pubsub#send_last_published_item")) {
                        setAnswer("pubsub#send_last_published_item", "never")
                    }
                    maxItems = maxItemCount
                }

                bookmarksNode.sendConfigurationForm(fillableForm)
                pepManager.publish(
                    item.namespace,
                    PayloadItem(itemId, item)
                )
            }
            CallResult.Success(true)
        } catch (e: Exception) {
            Logger.e(e)
            CallResult.Error("Could not publish item", e)
        }
    }

    suspend fun <T : ExtensionElement> publishPublicItem(
        itemId: String,
        maxItemCount: Int,
        item: T
    ): CallResult<Boolean> {
        return try {

            val pubSub = pepManager.pepPubSubManager
            val nicknameNode =
                pubSub.getOrCreateLeafNode(item.namespace)

            val nodeConfiguration = nicknameNode.nodeConfiguration
            val fillableForm = nodeConfiguration.fillableForm

            fillableForm.apply {
                setPersistentItems(true)

                if (nodeConfiguration.hasField("pubsub#send_last_published_item")) {
                    setAnswer("pubsub#send_last_published_item", "never")
                }
                maxItems = maxItemCount
            }

            nicknameNode.sendConfigurationForm(fillableForm)
            pepManager.publish(
                item.namespace,
                PayloadItem(itemId, item)
            )
            CallResult.Success(true)
        } catch (e: Exception) {
            Timber.e(e)
            CallResult.Error("Error")
        }
    }

    suspend fun <T : ExtensionElement> getItems(namespace: String): CallResult<List<T>> {
        return try {
            val pubSub = pepManager.pepPubSubManager

            val node = pubSub.getLeafNode(namespace)

            val item = node.getItems<PayloadItem<T>>()
                .map { it.payload }

            CallResult.Success(item)
        } catch (e: Exception) {
            Logger.e(e)
            CallResult.Error("Could not fetch items", e)
        }
    }

    suspend fun <T : ExtensionElement> getFirstItem(namespace: String): CallResult<T> =
        when (val items = getItems<T>(namespace)) {
            is CallResult.Success -> CallResult.Success(items.data?.first())
            is CallResult.Error -> CallResult.Error("Could not fetch item", items.cause)
        }


    suspend fun <T : ExtensionElement> itemUpdateStream(
        namespace: String,
        clazz: Class<T>
    ): Flow<PepItemInfo<T>> = callbackFlow {

        val listener = PepEventListener<T> { from, event, id, _ ->
            launch { send(PepItemInfo(event, id, from.asEntityBareJidString())) }
        }

        pepManager.addPepEventListener(
            namespace,
            clazz,
            listener
        )

        awaitClose {
            pepManager.removePepEventListener(listener)
        }
    }
}

data class PepItemInfo<T>(
    val item: T,
    val id: String,
    val from: String
)