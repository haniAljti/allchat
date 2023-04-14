package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.common.utils.StringUtils
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.InfoRemoteDataSource
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.ChatUpdate
import com.hanialjti.allchat.data.remote.model.RemoteEntityInfo
import com.hanialjti.allchat.data.remote.xmpp.model.AvatarDataExtensionElement
import com.hanialjti.allchat.data.remote.xmpp.model.AvatarMetaDataExtensionElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import org.apache.commons.codec.digest.DigestUtils
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.nick.packet.Nick
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.impl.JidCreate
import timber.log.Timber
import java.util.*

class InfoXmppDataSource(
    private val connection: XMPPTCPConnection,
    private val mucManager: MucManager,
    private val vCardManager: VCardManager = VCardManager.getInstanceFor(connection),
    private val pepManager: PepManager = PepManager.getInstanceFor(connection)
) : InfoRemoteDataSource {

    override suspend fun getUpdatedEntityInfo(
        id: String,
        isGroupChat: Boolean
    ): CallResult<RemoteEntityInfo> {
        val avatarResult = fetchAvatarData(id, null, isGroupChat)
        val nicknameResult = fetchNickname(id, isGroupChat)

        if (nicknameResult is CallResult.Error && avatarResult is CallResult.Error)
            return CallResult.Error("An error occurred while fetching info!")

        val avatar = (avatarResult as? CallResult.Success)?.data
        val nickname = (nicknameResult as? CallResult.Success)?.data

        return CallResult.Success(
            RemoteEntityInfo(
                id = id,
                name = nickname,
                avatar = avatar
            )
        )
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

    override suspend fun fetchAvatarData(
        id: String,
        hash: String?,
        isGroupChat: Boolean
    ): CallResult<Avatar?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (!isGroupChat && !pepManager.isSupported) {
                    val vCard = vCardManager.loadVCard()
                    CallResult.Success(Avatar.Raw(vCard.avatar))
                }

                if (isGroupChat) {
                    val roomInfoResult = mucManager.getRoomDescription(id)
                    if (roomInfoResult is CallResult.Success) {

                        return@withContext CallResult.Success(
                            roomInfoResult.data?.avatarUrl?.let { Avatar.Url(it) }
                        )
                    }
                    return@withContext CallResult.Error("An error occurred while fetching avatar")
                }

                val pubSubManager = PubSubManager.getInstanceFor(connection, JidCreate.bareFrom(id))


                val metaDataNode =
                    pubSubManager?.getLeafNode(AvatarMetaDataExtensionElement.NAMESPACE)

                val metaData = metaDataNode
                    ?.getItems<PayloadItem<AvatarMetaDataExtensionElement>>()
                    ?.first()
                    ?.payload

                if (metaData?.url != null) {
                    CallResult.Success(Avatar.Url(metaData.url))
                } else {

                    val avatarDataNode =
                        pubSubManager?.getLeafNode(AvatarDataExtensionElement.NAMESPACE)

                    val avatarHash = hash ?: fetchAvatarHash(id)
                    val avatarData = avatarDataNode
                        ?.getItems<PayloadItem<AvatarDataExtensionElement>>(listOf(avatarHash))
                        ?.first()
                        ?.payload

                    CallResult.Success(avatarData?.data?.let { Avatar.Raw(decodeData(it)) })

                }
                return@withContext CallResult.Success(null)
            } catch (e: Exception) {
                CallResult.Error("Error while retrieving avatar data")
            }
        }

    override suspend fun fetchNickname(id: String, isGroupChat: Boolean): CallResult<String?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (isGroupChat) {
                    CallResult.Success(mucManager.getRoomSubject(id))
                } else {
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
                }
            } catch (e: Exception) {
                CallResult.Error("Error while retrieving nickname")
            }
        }


    override suspend fun updateNickname(nickname: String, id: String?) =
        withContext(Dispatchers.IO) {
            val isMuc = id?.let { isMuc(id) } ?: false
            if (id != null && !isMuc)
                return@withContext CallResult.Error("You can only edit your nickname or a muc's (subject)")
            return@withContext try {
                if (id == null) {
                    if (pepManager.isSupported) {
                        pepManager.publish(
                            Nick.NAMESPACE,
                            PayloadItem(Nick(nickname))
                        )
                    } else {
                        val vCard = vCardManager.loadVCard()
                        vCardManager.saveVCard(
                            vCard.apply {
                                nickName = nickname
                            }
                        )
                    }
                } else {
                    mucManager.updateRoomSubject(id, nickname)
                }
                CallResult.Success(true)
            } catch (e: Exception) {
                Timber.e(e)
                CallResult.Error("Error")
            }
        }


    override suspend fun updateAvatar(data: ByteArray?, id: String?) =
        withContext(Dispatchers.IO) {
            if (id != null && !isMuc(id))
                return@withContext CallResult.Error("You can only edit your or a muc's avatar")

            return@withContext try {
                if (id == null) {
                    if (pepManager.isSupported) {
                        data?.let {

                            val dataHash = StringUtils.sha1(data)
                                ?: return@withContext CallResult.Error("Enable to hash data")

                            val encodedData = encodeData(data)

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
                                        height = 96,
                                        url = null,
                                        width = 96,
                                        type = "image/png"
                                    )
                                )
                            )

                        } ?: pepManager.publish(
                            AvatarMetaDataExtensionElement.NAMESPACE,
                            PayloadItem(AvatarMetaDataExtensionElement()) // disable avatar publishing
                        )
                    } else {
                        val vCard = vCardManager.loadVCard()
                        vCardManager.saveVCard(
                            vCard.apply {
                                setAvatar(data, "image/png")
                            }
                        )
                    }
                } else { // updating avatar of a muc

//                    if (data != null) {
//
//                        val avatarDataMessage = StanzaBuilder
//                            .buildMessage(UUID.randomUUID().toString())
//                            .apply {
//                                val avatarData =
//                                    AvatarDataExtensionElement(data = encodeData(data))
//
//                                addExtension(avatarData)
//                            }
//                            .build()
//
//                        mucManager.sendMessageToRoom(id, avatarDataMessage)
//
//                    }

//                    val metaDataMessage = StanzaBuilder.buildMessage(UUID.randomUUID().toString())
//                        .apply {
//                            if (data == null) {
//                                val emptyMetadata = AvatarMetaDataExtensionElement()
//                                addExtension(emptyMetadata)
//                            } else {
//
//                                val dataHash = StringUtils.sha1(data)
//                                    ?: return@withContext CallResult.Error("Enable to hash data")
//                                val metadata = AvatarMetaDataExtensionElement(
//                                    bytes = data.size,
//                                    id = dataHash,
//                                    url = null,
//                                    height = 96,
//                                    width = 96,
//                                    type = "image/png"
//                                )
//                                addExtension(metadata)
//                            }
//                        }
//                        .build()
//
//                    mucManager.sendMessageToRoom(id, metaDataMessage)
                }
                CallResult.Success(true)
            } catch (e: Exception) {
                Timber.e(e)
                CallResult.Error("Error")
            }
        }

    override suspend fun updateAvatar(url: String?, id: String?) =
        withContext(Dispatchers.IO) {
            if (id != null && !isMuc(id))
                return@withContext CallResult.Error("You can only edit your or a muc's avatar")

            return@withContext try {
                if (id == null) {
                    if (pepManager.isSupported) {
                        url?.let {

                            val dataHash = DigestUtils.sha1Hex(url)
                                ?: return@withContext CallResult.Error("Enable to hash data")

                            pepManager.publish(
                                AvatarMetaDataExtensionElement.NAMESPACE,
                                PayloadItem(
                                    dataHash,
                                    AvatarMetaDataExtensionElement(
                                        bytes = 0, //TODO
                                        id = dataHash,
                                        height = 96,
                                        url = null,
                                        width = 96,
                                        type = "image/png"
                                    )
                                )
                            )

                        } ?: pepManager.publish(
                            AvatarMetaDataExtensionElement.NAMESPACE,
                            PayloadItem(AvatarMetaDataExtensionElement()) // disable avatar publishing
                        )
                    } else {
                        val vCard = vCardManager.loadVCard()
                        vCardManager.saveVCard(
                            vCard.apply {
                                setAvatar(url, "image/png")
                            }
                        )
                    }
                } else { // updating avatar of a muc

//                    val roomInfoResult = mucManager.getRoomDescription(id)
//                    if (roomInfoResult is CallResult.Success) {
//
//                        val roomInfo = roomInfoResult.data?.copy(avatarUrl = url)
//                            ?: RoomInfo(
//                                description = null,
//                                createdAt = ZonedDateTime.now().toString(),
//                                createdBy = "",
//                                avatarUrl = url
//                            )

//                        mucManager
//                            .changeRoomDescription(
//                                id,
//                                Json.encodeToString(roomInfo)
//                            )
//                    }
//                    val message = StanzaBuilder.buildMessage(UUID.randomUUID().toString())
//                        .apply {
//                            if (url == null) {
//                                val emptyMetadata = AvatarMetaDataExtensionElement()
//                                addExtension(emptyMetadata)
//                            } else {
//                                val dataHash = DigestUtils.sha1Hex(url)
//                                    ?: return@withContext CallResult.Error("Enable to hash data")
//                                val metadata = AvatarMetaDataExtensionElement(
//                                    bytes = 0,
//                                    id = dataHash,
//                                    url = url,
//                                    height = 96,
//                                    width = 96,
//                                    type = "image/png"
//                                )
//                                addExtension(metadata)
//                            }
//                        }
//                        .build()
//
//                    mucManager.sendMessageToRoom(id, message)
                }
                CallResult.Success(true)
            } catch (e: Exception) {
                Timber.e(e)
                CallResult.Error("Error")
            }
        }


    private fun isMuc(id: String) = id.contains("@conference.")

    private fun encodeData(data: ByteArray): String =
        String(Base64.getEncoder().encode(data))

    private fun decodeData(data: String): ByteArray =
        Base64.getDecoder().decode(data)

    override suspend fun infoUpdateStream() = merge(
        emptyFlow<ChatUpdate>()
//        mucManager.subjectStream
    )

    override suspend fun hashAvatarBytes(avatarBytes: ByteArray): String? =
        StringUtils.sha1(avatarBytes)
}