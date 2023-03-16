package com.hanialjti.allchat.presentation

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.*
import com.hanialjti.allchat.common.utils.ApplicationUtils
import com.hanialjti.allchat.data.repository.AuthenticationRepository
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.InfoRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.data.tasks.ChatForegroundService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class ConnectionLifeCycleObserver(
    private val authenticationRepository: AuthenticationRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val infoRepository: InfoRepository
) : DefaultLifecycleObserver {

    private var isBackgroundServiceRunning = false
    override fun onCreate(owner: LifecycleOwner) {
        if (owner is Activity && !owner.isChangingConfigurations && !isBackgroundServiceRunning) {
            isBackgroundServiceRunning = true
            val chatForegroundService = Intent(owner, ChatForegroundService::class.java)
            owner.startForegroundService(chatForegroundService)
        }

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
        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Timber.d("listening for info updates...")
                infoRepository.nicknameStream().collect()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (owner is Activity && !owner.isChangingConfigurations && ApplicationUtils.isInBackground) {
            ApplicationUtils.isInBackground = false

            owner.lifecycleScope.launch {
                authenticationRepository.onResume()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (owner is Activity && !owner.isChangingConfigurations && !ApplicationUtils.isInBackground) {
            ApplicationUtils.isInBackground = true

            owner.lifecycleScope.launch {
                authenticationRepository.onPause()
            }
        }
    }

}