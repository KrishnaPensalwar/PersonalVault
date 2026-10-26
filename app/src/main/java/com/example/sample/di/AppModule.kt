package com.example.sample.di

import com.example.sample.data.local.VaultDatabase
import com.example.sample.dashboard.DashboardViewModel
import com.example.sample.family.FamilyViewModel
import com.example.sample.password.PasswordDetailViewModel
import com.example.sample.password.PasswordFormViewModel
import com.example.sample.password.PasswordViewModel
import com.example.sample.password.data.PasswordDao
import com.example.sample.password.data.PasswordRepository
import com.example.sample.password.data.PasswordRepositoryImpl
import com.example.sample.storage.DocumentDetailViewModel
import com.example.sample.storage.StorageViewModel
import com.example.sample.storage.data.VaultDocumentDao
import com.example.sample.storage.data.VaultDocumentImpl
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Database ──────────────────────────────────────────────────────────────

    single<VaultDatabase> {
        VaultDatabase.getDatabase(androidContext())
    }

    // ── Storage DAOs & Repositories ───────────────────────────────────────────

    single<VaultDocumentDao> {
        get<VaultDatabase>().vaultDocumentDao()
    }

    single {
        VaultDocumentImpl(get<VaultDocumentDao>())
    }

    // ── Password DAOs & Repositories ──────────────────────────────────────────

    single<PasswordDao> {
        get<VaultDatabase>().passwordDao()
    }

    single<PasswordRepository> {
        PasswordRepositoryImpl(get<PasswordDao>())
    }

    // ── ViewModels ────────────────────────────────────────────────────────────

    viewModel { StorageViewModel(get()) }
    viewModel { DocumentDetailViewModel(get()) }
    viewModel { DashboardViewModel() }
    viewModel { PasswordViewModel(get<PasswordRepository>()) }
    viewModel { PasswordDetailViewModel(get<PasswordRepository>()) }
    viewModel { PasswordFormViewModel(get<PasswordRepository>()) }
    viewModel { FamilyViewModel() }
}
