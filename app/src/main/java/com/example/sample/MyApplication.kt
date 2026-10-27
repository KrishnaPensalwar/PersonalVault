package com.example.sample

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.sample.di.appModule
import com.example.sample.security.AppBackgroundTracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MyApplication)
            modules(appModule)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppBackgroundTracker)
    }
}
