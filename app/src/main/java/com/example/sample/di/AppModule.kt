package com.example.sample.di

import com.example.sample.storage.StorageViewModel
import com.example.sample.dashboard.DashboardViewModel
import com.example.sample.password.PasswordViewModel
import com.example.sample.family.FamilyViewModel
import com.example.sample.data.local.VaultDatabase
import com.example.sample.storage.data.VaultDocumentImpl
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { VaultDatabase.getDatabase(androidContext()) }
    single { get<VaultDatabase>().vaultDocumentDao() }
    single { VaultDocumentImpl(get()) }
    
    viewModel { StorageViewModel(get()) }
    viewModel { DashboardViewModel() }
    viewModel { PasswordViewModel() }
    viewModel { FamilyViewModel() }
}
