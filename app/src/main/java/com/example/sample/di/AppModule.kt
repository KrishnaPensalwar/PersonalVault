package com.example.sample.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.sample.data.local.VaultDatabase
import com.example.sample.dashboard.DashboardViewModel
import com.example.sample.family.memberProfile.FamilyMemberProfileViewModel
import com.example.sample.family.membersListingScreen.FamilyViewModel
import com.example.sample.family.data.FamilyMemberDao
import com.example.sample.family.data.FamilyMemberRepository
import com.example.sample.family.data.FamilyMemberRepositoryImpl
import com.example.sample.password.passwordDetail.PasswordDetailViewModel
import com.example.sample.password.passwordInputEntry.PasswordFormViewModel
import com.example.sample.password.passwordListingScreen.PasswordViewModel
import com.example.sample.password.data.PasswordDao
import com.example.sample.password.data.PasswordRepository
import com.example.sample.password.data.PasswordRepositoryImpl
import com.example.sample.storage.documentDetailScreen.DocumentDetailViewModel
import com.example.sample.storage.documentsListingScreen.StorageViewModel
import com.example.sample.storage.data.VaultDocumentDao
import com.example.sample.storage.data.VaultDocumentImpl
import com.example.sample.security.AppLockViewModel
import com.example.sample.security.BiometricAvailabilityChecker
import com.example.sample.security.BiometricAvailabilityCheckerImpl
import com.example.sample.security.data.AppLockRepository
import com.example.sample.security.data.AppLockRepositoryImpl
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

    // ── Family DAOs & Repositories ────────────────────────────────────────────

    single<FamilyMemberDao> {
        get<VaultDatabase>().familyMemberDao()
    }

    single<FamilyMemberRepository> {
        FamilyMemberRepositoryImpl(get<FamilyMemberDao>())
    }

    // ── App Lock ──────────────────────────────────────────────────────────────

    single(qualifier = org.koin.core.qualifier.named("appLockPrefs")) {
        val context = androidContext()
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "app_lock_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    single<AppLockRepository> {
        AppLockRepositoryImpl(get(qualifier = org.koin.core.qualifier.named("appLockPrefs")))
    }

    single<BiometricAvailabilityChecker> {
        BiometricAvailabilityCheckerImpl(androidContext())
    }

    // ── ViewModels ────────────────────────────────────────────────────────────

    viewModel { StorageViewModel(get()) }
    viewModel { DocumentDetailViewModel(get()) }
    viewModel { DashboardViewModel() }
    viewModel { PasswordViewModel(get<PasswordRepository>()) }
    viewModel { PasswordDetailViewModel(get<PasswordRepository>()) }
    viewModel { PasswordFormViewModel(get<PasswordRepository>()) }
    viewModel { FamilyViewModel(get<FamilyMemberRepository>(), get<VaultDocumentImpl>()) }
    viewModel { FamilyMemberProfileViewModel(get<FamilyMemberRepository>(), get<VaultDocumentImpl>()) }
    viewModel { AppLockViewModel(get<AppLockRepository>(), get<BiometricAvailabilityChecker>()) }
}
