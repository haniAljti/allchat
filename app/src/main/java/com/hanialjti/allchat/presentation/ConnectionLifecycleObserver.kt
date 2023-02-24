package com.hanialjti.allchat.presentation

import android.app.Activity
import androidx.lifecycle.*
import com.hanialjti.allchat.common.utils.ApplicationUtils
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.IMessageRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.data.tasks.ChatForegroundService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class ConnectionLifeCycleObserver(
    private val connectionManager: ConnectionManager,
    private val chatRepository: IMessageRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) : DefaultLifecycleObserver {
//    private var isInBackground = true

    override fun onCreate(owner: LifecycleOwner) {
//        owner.lifecycleScope.launch {
//            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                Timber.d("listening for messages...")
//                chatRepository.listenForMessageUpdates()
//            }
//        }
        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Timber.d("listening for chat updates...")
                conversationRepository.listenForConversationUpdates().collect()
            }
        }
        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userRepository.listenForUserUpdates()
            }
        }
//        owner.lifecycleScope.launch {
//            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                userRepository.listenForUserUpdates()
//            }
//        }
    }

    override fun onResume(owner: LifecycleOwner) {
        if (owner is Activity && !owner.isChangingConfigurations && ApplicationUtils.isInBackground) {
            ApplicationUtils.isInBackground = false

//            owner.lifecycleScope.launch { conversationRepository.startListeners() }
            owner.lifecycleScope.launch {
                connectionManager.onResume()
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        if (owner is Activity && !owner.isChangingConfigurations && !ApplicationUtils.isInBackground) {
//            is Activity -> {
//                if (!owner.isChangingConfigurations) {
            ApplicationUtils.isInBackground = true
//                    owner.lifecycleScope.launch { conversationRepository.stopListeners() }
            owner.lifecycleScope.launch {
                connectionManager.onPause()
            }
//                }
        }

    }

}