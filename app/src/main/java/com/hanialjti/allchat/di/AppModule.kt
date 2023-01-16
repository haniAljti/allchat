package com.hanialjti.allchat.di

import androidx.room.Room
import androidx.work.WorkManager
import com.hanialjti.allchat.data.local.FileRepository
import com.hanialjti.allchat.data.local.datastore.CryptoManager
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.data.repository.ChatRepository
import com.hanialjti.allchat.data.repository.IChatRepository
import com.hanialjti.allchat.data.tasks.ConversationTasksDataStore
import com.hanialjti.allchat.data.tasks.MessageTasksDataSource
import com.hanialjti.allchat.domain.MessageTasksDataSourceImpl
import com.hanialjti.allchat.domain.usecase.*
import com.hanialjti.allchat.domain.worker.CreateChatRoomWorker
import com.hanialjti.allchat.domain.worker.SendMessageWorker
import com.hanialjti.allchat.presentation.ConnectionLifeCycleObserver
import com.hanialjti.allchat.presentation.MainViewModel
import com.hanialjti.allchat.presentation.chat.ChatViewModel
import com.hanialjti.allchat.presentation.conversation.ConversationsViewModel
import com.hanialjti.allchat.presentation.create_chat_room.CreateChatRoomViewModel
import com.hanialjti.allchat.presentation.invite_users.InviteUsersViewModel
import com.hanialjti.allchat.presentation.viewmodels.AddContactViewModel
import com.hanialjti.allchat.presentation.viewmodels.AuthenticationViewModel
import com.hanialjti.allchat.presentation.viewmodels.EditUserInfoViewModel
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
    single(qualifier = named(DispatcherQualifiers.Io)) {
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
        CryptoManager()
    }
    single {
        UserPreferencesManager(androidContext())
    }
    single {
        FileRepository(androidContext(), get(named(DispatcherQualifiers.Io)))
    }
    single {
        ConversationRepository(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(named(ScopeQualifiers.Application)),
            get(named(DispatcherQualifiers.Io))
        )
    }
    single<IChatRepository> {
        ChatRepository(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(qualifier = named(ScopeQualifiers.Application)),
            get(qualifier = named(DispatcherQualifiers.Io))
        )
    }
    single(createdAtStart = true) {
        UserRepository(get(), get(), get(), get())
    }
    single {
        AddContactViewModel(get(), get())
    }
    factory { params ->
        ChatViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            params.get(),
            params.get()
        )
    }
    factory {
        CreateChatRoomViewModel(get())
    }
    single {
        EditUserInfoViewModel(get())
    }
    factory {
        ConversationsViewModel(get(), get(), get(), get(), get(), get())
    }
    factory { params ->
        InviteUsersViewModel(params.get(), get(), get())
    }
    single {
        AuthenticationViewModel(get())
    }
    single {
        MainViewModel(get(), get(), get())
    }
    single {
        WorkManager.getInstance(androidContext())
    }
    single {
        ConversationTasksDataStore(get())
    }
    single {
        ConnectionLifeCycleObserver(get(), get(), get())
    }
    single<MessageTasksDataSource> {
        MessageTasksDataSourceImpl(get())
    }
    worker { SendMessageWorker(androidContext(), get(), get(), get()) }
    worker { CreateChatRoomWorker(androidContext(), get(), get(), get()) }
}

val roomModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AllChatLocalRoomDatabase::class.java,
            "all-chat-database"
        ).build()
    }
    single {
        get<AllChatLocalRoomDatabase>().userDao()
    }
}

val useCaseModule = module {
    single {
        GetContactsUseCase(get(), get())
    }
    single {
        ResetUnreadCounterUseCase(get())
    }
    single {
        GetMessagesUseCase(get())
    }
    single {
        GetContactInfoUseCase(get(), get())
    }
    single {
        CreateChatRoomUseCase(get(), get())
    }
    single {
        AddUserToContactsUseCase(get())
    }
    single {
        SendReadMarkerForMessageUseCase(get())
    }
    single {
        SyncChatsUseCase(get())
    }
    single {
        SyncMessagesUseCase(get(), get())
    }
    single {
        GetUsersUseCase(get())
    }
    single {
        InviteUsersToChatRoomUseCase(get(), get())
    }
    single {
        SendMessageUseCase(get(), get())
    }
    single {
        GetConnectedUserUseCase(get())
    }
    single {
        GetAttachmentUseCase(get())
    }
    single {
        LoggedInUserUseCase(get())
    }
    single {
        SignOut(get())
    }
    single {
        SignIn(get())
    }
    single {
        AuthenticationUseCases(get(), get(), get())
    }
}

val workerFactoryModule = module {
    factory { LocalContextWorkerFactory() }
}