package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.common.utils.StringUtils
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.InfoRemoteDataSource
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.NicknameUpdate
import com.hanialjti.allchat.data.remote.model.RemoteEntityInfo
import com.hanialjti.allchat.data.remote.xmpp.model.AvatarDataExtensionElement
import com.hanialjti.allchat.data.remote.xmpp.model.AvatarMetaDataExtensionElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.packet.StanzaBuilder
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.nick.packet.Nick
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import org.jxmpp.jid.impl.JidCreate
import timber.log.Timber
import java.util.*

class InfoXmppDataSource(
    private val connection: XMPPTCPConnection,
    private val mucManager: MucManager
) : InfoRemoteDataSource {

    private val vCardManager = VCardManager.getInstanceFor(connection)
    private val pepManager = PepManager.getInstanceFor(connection)
    private val multiUserChatManager = MultiUserChatManager.getInstanceFor(connection)

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

    override suspend fun fetchAvatarData(id: String, hash: String?, isGroupChat: Boolean): CallResult<Avatar?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (isGroupChat) {
                    val vCard = vCardManager.loadVCard(id.asJid().asEntityBareJidIfPossible())
                    CallResult.Success(Avatar.Raw(vCard.avatar))
                } else {
                    if (pepManager.isSupported) {
                        val avatarHash = hash ?: fetchAvatarHash(id)
                        val pubSubManager =
                            PubSubManager.getInstanceFor(connection, JidCreate.bareFrom(id))
                        val avatarNode =
                            pubSubManager.getLeafNode(AvatarDataExtensionElement.NAMESPACE)
                        val items =
                            avatarNode.getItems<PayloadItem<AvatarDataExtensionElement>>(
                                listOf(
                                    avatarHash
                                )
                            )
                        CallResult.Success(Avatar.Raw(decodeData(items.first().payload.data)))
                    } else {
                        val vCard = vCardManager.loadVCard()
                        CallResult.Success(Avatar.Raw(vCard.avatar))
                    }
                }
            } catch (e: Exception) {
                CallResult.Error("Error while retrieving avatar data")
            }
        }

    override suspend fun fetchNickname(id: String, isGroupChat: Boolean): CallResult<String?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (isGroupChat) {
                    CallResult.Success(null)
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
                    val muc = multiUserChatManager.getMultiUserChat(
                        id.asJid().asEntityBareJidIfPossible()
                    )
                    multiUserChatManager.getRoomInfo("".asJid().asEntityBareJidIfPossible()).pubSub
                    muc.changeSubject(nickname)
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
                    val vCard = VCard().apply {
                        setAvatar(data, "image/png")
                    }
                    // send this vCard to room
                    vCardManager.saveVCard(vCard, id, connection)
//                    val message = StanzaBuilder.buildMessage(UUID.randomUUID().toString())
//                        .apply {
//                            if (data == null) {
//                                val emptyMetadata = AvatarMetaDataExtensionElement()
//                                addExtension(emptyMetadata)
//                            } else {
//                                val avatarData =
//                                    AvatarDataExtensionElement(data = encodeData(data))
//                                val dataHash = StringUtils.sha1(data)
//                                    ?: return@withContext CallResult.Error("Enable to hash data")
//                                val metadata = AvatarMetaDataExtensionElement(
//                                    bytes = data.size,
//                                    id = dataHash,
//                                    height = 96,
//                                    width = 96,
//                                    type = "image/png"
//                                )
//                                addExtensions(listOf(avatarData, metadata))
//                            }
//                        }
//
//                    val muc = multiUserChatManager.getMultiUserChat(
//                        id.asJid().asEntityBareJidIfPossible()
//                    )
//                    muc.sendMessage(message)
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
        mucManager.subjectStream.map {
            NicknameUpdate(it.first, it.second)
        }
    )

    override suspend fun hashAvatarBytes(avatarBytes: ByteArray): String? =
        StringUtils.sha1(avatarBytes)
}