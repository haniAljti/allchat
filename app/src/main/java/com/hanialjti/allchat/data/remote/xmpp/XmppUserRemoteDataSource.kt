package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.data.remote.xmpp.model.*
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.blocking.BlockingCommandManager
import org.jivesoftware.smackx.iqlast.LastActivityManager
import org.jivesoftware.smackx.nick.packet.Nick
import org.jivesoftware.smackx.pep.PepEventListener
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.AccessModel
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PubSubElementType
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jivesoftware.smackx.pubsub.packet.PubSub
import org.jivesoftware.smackx.pubsub.provider.PubSubProvider
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.impl.JidCreate
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class XmppUserRemoteDataSource(
    private val connection: XMPPTCPConnection,
    private val rosterManager: RosterManager
) : UserRemoteDataSource {

    private val vCardManager = VCardManager.getInstanceFor(connection)
    private val lastActivityManager = LastActivityManager.getInstanceFor(connection)
    private val pepManager = PepManager.getInstanceFor(connection)
    private val blockingCommandManager = BlockingCommandManager.getInstanceFor(connection)

    override suspend fun getUpdatedUserInfo(userId: String) = withContext(Dispatchers.IO) {
        return@withContext try {
            val avatarResult = fetchAvatar(userId)
            val nicknameResult = fetchNickname(userId)

            val avatar = if (avatarResult is CallResult.Success) avatarResult.data else null
            val nickname = if (nicknameResult is CallResult.Success) nicknameResult.data else null

            val presence = rosterManager.getUserPresence(userId)
                ?: return@withContext CallResult.Error("Failed to fetch user info")

            val lastOnline = if (presence.isAvailable) null else lastActivity(userId)

            val lastOnlineDateTime = lastOnline?.let {
                Instant.ofEpochMilli(it).atOffset(
                    ZoneOffset.UTC
                )
            }

            CallResult.Success(
                FullRemoteUserInfo(
                    id = userId,
                    name = nickname,
                    avatar = avatar,
                    isOnline = presence.isAvailable,
                    status = presence.status,
                    lastOnline = lastOnlineDateTime
                )
            )
        } catch (e: NoResponseException) {
            Timber.e(e)
            CallResult.Error("An error occurred while fetching user info", e)
        } catch (e: XMPPException) {
            Timber.e(e)
            CallResult.Error("An error occurred while fetching user info", e)
        } catch (e: NotConnectedException) {
            Timber.e(e)
            CallResult.Error("An error occurred while fetching user info", e)
        } catch (e: InterruptedException) {
            Timber.e(e)
            CallResult.Error("An error occurred while fetching user info", e)
        }
    }

    override suspend fun updateNickname(username: String): CallResult<Boolean> {
        return try {

            val pubSub = pepManager.pepPubSubManager
            val nicknameNode =
                pubSub.getOrCreateLeafNode(Nick.NAMESPACE)

            val nodeConfiguration = nicknameNode.nodeConfiguration
            val fillableForm = nodeConfiguration.fillableForm

            fillableForm.apply {
                setPersistentItems(true)
                accessModel = AccessModel.presence

                if (nodeConfiguration.hasField("pubsub#send_last_published_item")) {
                    setAnswer("pubsub#send_last_published_item", "never")
                }
            }

            nicknameNode.sendConfigurationForm(fillableForm)
            pepManager.publish(Nick.NAMESPACE, PayloadItem(Nick(username)))
            CallResult.Success(true)
        } catch (e: Exception) {
            Timber.e(e)
            CallResult.Error("Error")
        }
    }

    private suspend fun fetchNickname(id: String): CallResult<String?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (pepManager.isSupported) {
                    val pubSubManager =
                        PubSubManager.getInstanceFor(connection, JidCreate.bareFrom(id))
                    val nicknameNode = pubSubManager.getLeafNode(Nick.NAMESPACE)
                    val items = nicknameNode.getItems<PayloadItem<Nick>>()
                    CallResult.Success(items.first().payload.name)
                } else {
                    val vCard = vCardManager.loadVCard()
                    CallResult.Success(vCard.nickName)
                }
            } catch (e: Exception) {
                CallResult.Error("Error while retrieving nickname")
            }
        }


    override suspend fun updateAvatar(data: ByteArray?): CallResult<Boolean> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (pepManager.isSupported) {
                    data?.let {

                        val dataHash = sha1(data)
                            ?: return@withContext CallResult.Error("Enable to hash data")

                        val encodedData = String(Base64.getEncoder().encode(data))

                        pepManager.publish(
                            AvatarDataExtensionElement.NAMESPACE,
                            PayloadItem(
                                dataHash,
                                AvatarDataExtensionElement(data = encodedData)
                            )
                        )

                        pepManager.publish(
                            AvatarMetaDataExtensionElement.NAMESPACE,
                            PayloadItem(
                                dataHash,
                                AvatarMetaDataExtensionElement(
                                    bytes = data.size,
                                    id = dataHash,
                                    url = null,
                                    height = 96,
                                    width = 96,
                                    type = "image/png"
                                )
                            )
                        )

                    }
                } else {
                    val vCard = vCardManager.loadVCard()
                    vCardManager.saveVCard(
                        vCard.apply {
                            setAvatar(data, "image/png")
                        }
                    )
                }
                CallResult.Success(true)
            } catch (e: Exception) {
                Timber.e(e)
                CallResult.Error("Error")
            }
        }

    override suspend fun updateMyInfo(
        name: String,
        avatar: ContactImage?,
        status: String?
    ): CallResult<Boolean> {
        return try {

            if (!connection.isAuthenticated) return CallResult.Error("No user is logged in")

            updateNickname(name)

            when (avatar) {
                is ContactImage.DynamicRawImage -> {
                    updateAvatar(avatar.bytes)
                }
                is ContactImage.DefaultUserImage -> {
                    updateAvatar(null) // remove image and stop sending updates
                }
                else -> {}
            }

            connection.sendStanza(
                connection.stanzaFactory
                    .buildPresenceStanza()
                    .ofType(Presence.Type.available)
                    .setStatus(status)
                    .build()
            )
            CallResult.Success(true)
        } catch (e: Exception) {
            Logger.e(e)
            CallResult.Error("", e)
        }

    }

    private fun sha1(bytes: ByteArray): String? {
        return try {
            val crypt: MessageDigest = MessageDigest.getInstance("SHA-1")
            crypt.reset()
            crypt.update(bytes)
            byteToHex(crypt.digest())
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            null
        }
    }

    private fun byteToHex(hash: ByteArray): String {
        val formatter = Formatter()
        for (b in hash) {
            formatter.format("%02x", b)
        }
        val result: String = formatter.toString()
        formatter.close()
        return result
    }

    override fun usersUpdateStream(): Flow<UserUpdate> = merge(
        listenForNicknameUpdates(),
        listenForAvatarUpdates(),
        rosterManager.rosterUpdateStream
            .filter { it is PresenceUpdated || it is PresenceSubscriptionArrived || it is PresenceSubscriptionApproved }
            .onEach { Logger.d { "New Roster update" } }
            .transform {

                Logger.d { "New Presence from ${it.userId}" }

                suspend fun fetchAndEmitUserInfo() {

                    val avatarResult = fetchAvatar(it.userId)

                    if (avatarResult is CallResult.Success) {
                        emit(AvatarUpdated(it.userId, avatarResult.data))
                    }

                    val nicknameResult = fetchNickname(it.userId)

                    if (nicknameResult is CallResult.Success) {
                        nicknameResult.data?.let { nickname ->
                            emit(
                                NicknameUpdated(
                                    it.userId,
                                    nickname
                                )
                            )
                        }
                    }
                }

                when (it) {
                    is PresenceSubscriptionApproved -> {
                        fetchAndEmitUserInfo()
                    }
                    is PresenceSubscriptionArrived -> {
                        fetchAndEmitUserInfo()
                    }
                    is PresenceUpdated -> {
                        emit(it.toUserUpdate())
                    }
                    else -> null // will not happen since the stream is filtered
                }
            }
    )

    private fun listenForNicknameUpdates() = callbackFlow<UserUpdate> {
        val listener = PepEventListener<Nick> { from, nickname, _, _ ->
            launch { send(NicknameUpdated(from.asBareJid().toString(), nickname.name)) }
        }

        pepManager.addPepEventListener(Nick.NAMESPACE, Nick::class.java, listener)

        awaitClose { pepManager.removePepEventListener(listener) }
    }

    private fun listenForAvatarUpdates() = callbackFlow<UserUpdate> {
        val listener = PepEventListener<AvatarMetaDataExtensionElement> { from, metadata, _, _ ->
            metadata.url
            launch {
                send(
                    AvatarMetadataUpdated(
                        from.asBareJid().toString(),
                        metadata.bytes,
                        metadata.id,
                        metadata.height,
                        metadata.width,
                        metadata.url,
                        metadata.type
                    )
                )
            }
        }

        pepManager.addPepEventListener(
            AvatarMetaDataExtensionElement.NAMESPACE,
            AvatarMetaDataExtensionElement::class.java,
            listener
        )

        awaitClose { pepManager.removePepEventListener(listener) }
    }

    override suspend fun fetchAvatar(userId: String, hash: String?): CallResult<Avatar?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (!pepManager.isSupported) {
                    val vCard = vCardManager.loadVCard()
                    CallResult.Success(Avatar.Raw(vCard.avatar))
                }

                val pubSubManager = PubSubManager.getInstanceFor(connection, userId.asJid())


                val metaDataNode =
                    pubSubManager?.getLeafNode(AvatarMetaDataExtensionElement.NAMESPACE)

                val metaData = metaDataNode
                    ?.getItems<PayloadItem<AvatarMetaDataExtensionElement>>()
                    ?.first()
                    ?.payload

                return@withContext if (metaData?.url != null) {
                    CallResult.Success(Avatar.Url(metaData.url))
                } else {

                    val avatarDataNode =
                        pubSubManager?.getLeafNode(AvatarDataExtensionElement.NAMESPACE)

                    val avatarHash = hash ?: fetchAvatarHash(userId)
                    val avatarData = avatarDataNode
                        ?.getItems<PayloadItem<AvatarDataExtensionElement>>(listOf(avatarHash))
                        ?.first()
                        ?.payload

                    CallResult.Success(avatarData?.data?.let { Avatar.Raw(decodeData(it)) })

                }
            } catch (e: Exception) {
                CallResult.Error("Error while retrieving avatar data")
            }
        }

    private suspend fun fetchAvatarHash(id: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val pubSubManager =
                PubSubManager.getInstanceFor(connection, JidCreate.bareFrom(id))
            val avatarNode = pubSubManager.getLeafNode(AvatarMetaDataExtensionElement.NAMESPACE)
            val items =
                avatarNode.getItems<PayloadItem<AvatarMetaDataExtensionElement>>(1)
            items.last().payload.id
        } catch (e: Exception) {
            Logger.e { "Error while retrieving avatar data" }
            null
        }
    }

    private fun decodeData(data: String): ByteArray =
        Base64.getDecoder().decode(data)

    override suspend fun blockUser(userId: String): CallResult<Boolean> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                blockingCommandManager.blockContacts(listOf(JidCreate.bareFrom(userId)))
                CallResult.Success(true)
            } catch (e: Exception) {
                Logger.e(e)
                CallResult.Error("Failed to block user", e)
            }
        }

    override suspend fun unblockUser(userId: String): CallResult<Boolean> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                blockingCommandManager.unblockContacts(listOf(JidCreate.bareFrom(userId)))
                CallResult.Success(true)
            } catch (e: Exception) {
                Logger.e(e)
                CallResult.Error("Failed to block user", e)
            }
        }



    private suspend fun lastActivity(userId: String) = withContext(Dispatchers.IO) {
        return@withContext try {
            val lastActivity = lastActivityManager.getLastActivity(userId.asJid())
            lastActivity.lastActivity
        } catch (e: XMPPException) {
            Timber.e(e)
            null
        } catch (e: NoResponseException) {
            Timber.e(e)
            null
        } catch (e: NotConnectedException) {
            Timber.e(e)
            null
        } catch (e: InterruptedException) {
            Timber.e(e)
            null
        }
    }

}