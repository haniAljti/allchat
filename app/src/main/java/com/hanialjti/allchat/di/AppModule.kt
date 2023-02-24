package com.hanialjti.allchat.di

import androidx.room.Room
import androidx.work.WorkManager
import com.hanialjti.allchat.data.local.datastore.CryptoManager
import com.hanialjti.allchat.data.local.datastore.PreferencesLocalDataStore
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.remote.DefaultFileDownloader
import com.hanialjti.allchat.data.repository.*
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
import com.hanialjti.allchat.presentation.preview_attachment.PreviewAttachmentViewModel
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
        PreferencesLocalDataStore(androidContext())
    }
    single {
        DefaultFileDownloader(externalScope = get(named(ScopeQualifiers.Application)))
    }
    single {
        FileRepository(androidContext(), get(named(DispatcherQualifiers.Io)), get(), get())
    }
    single {
        PreferencesRepository(get())
    }
    single {
        InfoRepository(
            get(),
            get(),
            get()
        )
    }
    single {
        ConversationRepository(
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    single<IMessageRepository> {
        MessageRepository(
            get(),
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
            get(),
            params.get(),
            params.get()
        )
    }
    factory { params ->
        PreviewAttachmentViewModel(params.get(), get())
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
        MainViewModel(get(), get())
    }
    single {
        WorkManager.getInstance(androidContext())
    }
    single {
        ConversationTasksDataStore(get())
    }
    single {
        ConnectionLifeCycleObserver(get(), get(), get(), get())
    }
    single<MessageTasksDataSource> {
        MessageTasksDataSourceImpl(get())
    }
    worker { SendMessageWorker(androidContext(), get(), get(), get(), get()) }
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
    single {
        get<AllChatLocalRoomDatabase>().infoDao()
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
        GetConnectedUserUseCase(get())
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