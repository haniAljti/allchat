package com.hanialjti.allchat.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

class LocalContextWorkerFactory: WorkerFactory(), CustomKoinComponent {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return getKoin().getOrNull(qualifier = named(workerClassName)) { parametersOf(workerParameters) }
    }
}