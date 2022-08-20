package com.hanialjti.allchat

import androidx.lifecycle.ViewModel
import com.hanialjti.allchat.models.Media
import com.hanialjti.allchat.models.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(): ViewModel(){

    fun getDummyMessageList() = listOf(
        Message(
            id = UUID.randomUUID().toString(),
            body = "Hello",
            conversation = "",
            from = "2",
            type = "chat",
            timestamp = 1653755507000
        ),
        Message(
            id = UUID.randomUUID().toString(),
            conversation = "",
            from = "1",
            type = "chat",
            timestamp = 1653669107000,
            media = Media(
                type = Media.Type.Audio
            )
        ),
        Message(
            id = UUID.randomUUID().toString(),
            body = "Hello",
            conversation = "",
            from = "1",
            type = "chat",
            timestamp = 1653582707000,
            media = Media(
                type = Media.Type.Audio
            )
        ),
        Message(
            id = UUID.randomUUID().toString(),
            body = "Hello",
            conversation = "",
            from = "1",
            type = "chat",
            timestamp = 1653496307000,
            media = Media(
                type = Media.Type.Image
            )
        ),
        Message(
            id = UUID.randomUUID().toString(),
            body = "Hello",
            conversation = "",
            from = "1",
            type = "chat",
            timestamp = 1653496307000,
            media = Media(
                type = Media.Type.Image
            )
        ),
        Message(
            id = UUID.randomUUID().toString(),
            body = "Hello",
            conversation = "",
            from = "1",
            type = "chat",
            timestamp = 1653496307000,
            media = Media(
                type = Media.Type.Image
            )
        )
    )

}