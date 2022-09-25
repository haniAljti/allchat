package com.hanialjti.allchat.di

import androidx.room.Room
import androidx.work.WorkManager
import com.hanialjti.allchat.domain.AllChatWorkManager
import com.hanialjti.allchat.data.local.datastore.CryptoManager
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.repository.XmppChatRepository
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.presentation.viewmodels.*
import com.hanialjti.allchat.domain.worker.SendMessageWorker
import com.hanialjti.allchat.presentation.ConnectionLifeCycleObserver
import com.hanialjti.allchat.presentation.MainViewModel
import com.hanialjti.allchat.presentation.chat.ChatViewModel
import com.hanialjti.allchat.presentation.conversation.ConversationsViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single(qualifier = named(DispatcherQualifiers.Main)) {
        Dispatchers.Main
    }
    single(qualifier = named(DispatcherQualifiers.Default)) {
        Dispatchers.Default
    }
    factory(qualifier = named(DispatcherQualifiers.Io)) {
        Dispatchers.IO
    }
    single(qualifier = named(ScopeQualifiers.Application)) {
        CoroutineScope(
            SupervisorJob() + get<CoroutineDispatcher>(
                qualifier = named(
                    DispatcherQualifiers.Default
                )
            )
        )
    }
    single {
        Room.databaseBuilder(
            androidContext(),
            AllChatLocalRoomDatabase::class.java,
            "all-chat-database"
        ).build()
    }
    single {
        CryptoManager()
    }
    single {
        UserPreferencesManager(androidContext(), get(qualifier = named(ScopeQualifiers.Application)))
    }
    single {
        ConversationRepository(get(), get())
    }
    single {
        XmppChatRepository(get(), get(), get())
    }
    single {
        UserRepository(get())
    }
    single {
        AddContactViewModel(get(), get())
    }
    factory { params ->
        ChatViewModel(get(), get(), get(), get(), params.get(), params.get())
    }
    single {
        EditUserInfoViewModel()
    }
    factory {
        ConversationsViewModel(get(), get(), get(), get(), get())
    }
    single {
        AuthenticationViewModel(get(), get())
    }
    single {
        MainViewModel(get(), get(), get(), get())
    }
    single {
        WorkManager.getInstance(androidContext())
    }
    single {
        ConnectionLifeCycleObserver(get(), get(), get(named(ScopeQualifiers.Application)))
    }
    single {
        AllChatWorkManager(get())
    }
    worker { SendMessageWorker(androidContext(), get(), get(), get()) }
}

val workerFactoryModule = module {
    factory { LocalContextWorkerFactory() }
}