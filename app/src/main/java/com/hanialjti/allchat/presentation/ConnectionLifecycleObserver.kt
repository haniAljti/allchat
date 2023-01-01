package com.hanialjti.allchat.presentation

import android.app.Activity
import androidx.lifecycle.*
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.model.Presence
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.IChatRepository
import kotlinx.coroutines.launch
import timber.log.Timber

class ConnectionLifeCycleObserver(
    private val connectionManager: ConnectionManager,
    private val chatRepository: IChatRepository,
    private val conversationRepository: ConversationRepository
) : DefaultLifecycleObserver {
    private var isInBackground = true

    override fun onCreate(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Timber.d("listening for messages...")
                chatRepository.listenForMessageUpdates()
            }
        }
        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Timber.d("listening for chat updates...")
                conversationRepository.listenForConversationUpdates()
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        if (owner is Activity && !owner.isChangingConfigurations && isInBackground) {
            isInBackground = false

            owner.lifecycleScope.launch {
                connectionManager.onResume()
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        when (owner) {
            is Activity -> {
                if (!owner.isChangingConfigurations) {
                    isInBackground = true
                    owner.lifecycleScope.launch {
                        connectionManager.onPause()
                    }
                }
            }
        }
    }

}