package com.hanialjti.allchat.di

import androidx.room.Room
import androidx.work.WorkManager
import com.hanialjti.allchat.data.local.datastore.CryptoManager
import com.hanialjti.allchat.data.local.datastore.PreferencesLocalDataStore
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.remote.AllChatSynchronizer
import com.hanialjti.allchat.data.remote.DefaultFileDownloader
import com.hanialjti.allchat.data.remote.FileDownloader
import com.hanialjti.allchat.data.repository.*
import com.hanialjti.allchat.data.tasks.ConversationTasksDataStore
import com.hanialjti.allchat.data.tasks.MessageTasksDataSource
import com.hanialjti.allchat.data.tasks.MessageTasksDataSourceImpl
import com.hanialjti.allchat.data.tasks.CreateChatRoomWorker
import com.hanialjti.allchat.data.tasks.SendMessageWorker
import com.hanialjti.allchat.presentation.ConnectionLifeCycleObserver
import com.hanialjti.allchat.presentation.MainViewModel
import com.hanialjti.allchat.presentation.chat.ChatViewModel
import com.hanialjti.allchat.presentation.conversation.ConversationsViewModel
import com.hanialjti.allchat.presentation.create_chat_room.CreateChatRoomViewModel
import com.hanialjti.allchat.presentation.invite_users.InviteUsersViewModel
import com.hanialjti.allchat.presentation.preview_attachment.PreviewAttachmentViewModel
import com.hanialjti.allchat.presentation.viewmodels.AddContactViewModel
import com.hanialjti.allchat.presentation.authentication.AuthenticationViewModel
import com.hanialjti.allchat.presentation.chat_entity_details.chat_details.ChatDetailsViewModel
import com.hanialjti.allchat.presentation.chat_entity_details.chat_details.UpdateChatDetailsViewModel
import com.hanialjti.allchat.presentation.chat_entity_details.user_details.UserDetailsViewModel
import com.hanialjti.allchat.presentation.chat_entity_details.user_details.UpdateMyProfileInfoViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.qualifier.named
import org.koin.dsl.module


val dispatcherModule = module {
    single(qualifier = named(DispatcherQualifiers.Main)) {
        Dispatchers.Main
    }
    single(qualifier = named(DispatcherQualifiers.Default)) {
        Dispatchers.Default
    }
    single(qualifier = named(DispatcherQualifiers.Io)) {
        Dispatchers.IO
    }
}

val scopeModule = module {
    single(qualifier = named(ScopeQualifiers.Application)) {
        CoroutineScope(
            SupervisorJob() + get<CoroutineDispatcher>(
                qualifier = named(
                    DispatcherQualifiers.Default
                )
            )
        )
    }
}

val preferencesModule = module {
    single {
        CryptoManager()
    }
    single {
        PreferencesLocalDataStore(androidContext())
    }
    single {
        PreferencesRepository(get())
    }
}

val authenticationModule = module {
    single<AuthRepository> {
        AuthenticationRepositoryImpl(
            get(),
            get(),
            get(named(DispatcherQualifiers.Io)),
            get(named(ScopeQualifiers.Application)),
        )
    }
    single {
        AuthenticationViewModel(get())
    }
}

val filesModule = module {
    single<FileDownloader> {
        DefaultFileDownloader(externalScope = get(named(ScopeQualifiers.Application)))
    }
    single<FileRepository> {
        FileRepositoryImpl(androidContext(), get(named(DispatcherQualifiers.Io)), get(), get())
    }
}

val infoModule = module {
    single{
        InfoRepository(
            get(),
            get(),
            get(),
            get(),
            get(named(DispatcherQualifiers.Io)),
            get(named(ScopeQualifiers.Application)),
        )
    }
    factory { params ->
        ChatDetailsViewModel(get(), params.get())
    }
    factory { params ->
        UserDetailsViewModel(get(), params.get())
    }
    factory { params ->
        UpdateChatDetailsViewModel(get(), params.get())
    }
}

val appModule = module {
    single {
        AllChatSynchronizer(
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        ConversationRepository(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(named(DispatcherQualifiers.Io)),
            get(named(ScopeQualifiers.Application)),
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
    single<UserRepository> {
        UserRepositoryImpl(get(), get(), get(), get(), get(), get())
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
            params.get()
        )
    }
    factory { params ->
        PreviewAttachmentViewModel(params.get(), get())
    }
    factory {
        CreateChatRoomViewModel(get(), get())
    }
    single {
        UpdateMyProfileInfoViewModel(get())
    }
    factory {
        ConversationsViewModel(get(), get(), get(), get())
    }
    factory { params ->
        InviteUsersViewModel(params.get(), get(), get())
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
        ConnectionLifeCycleObserver(get(), get(), get())
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
        get<AllChatLocalRoomDatabase>().blockedUserDao()
    }
    single {
        get<AllChatLocalRoomDatabase>().infoDao()
    }
    single {
        get<AllChatLocalRoomDatabase>().messageDao()
    }
    single {
        get<AllChatLocalRoomDatabase>().markerDao()
    }
    single {
        get<AllChatLocalRoomDatabase>().conversationDao()
    }
    single {
        get<AllChatLocalRoomDatabase>().participantDao()
    }
}

val workerFactoryModule = module {
    factory { LocalContextWorkerFactory() }
}