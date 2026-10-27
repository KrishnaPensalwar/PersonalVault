package com.example.sample.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.security.ui.LockScreen
import org.koin.androidx.compose.koinViewModel

// NOTE: this file is in package com.example.sample.security, so LockScreenState
// below refers to the sealed interface defined in that same package (Task 7) —
// no import needed.

/**
 * Wraps [content] and shows [LockScreen] until the user authenticates, on
 * cold start and whenever the app returns to the foreground after exceeding
 * [AppLockConfig.backgroundLockTimeout].
 */
@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    val viewModel: AppLockViewModel = koinViewModel()
    val state by viewModel.lockState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state is LockScreenState.Unlocked) {
        content()
    } else {
        LockScreen(viewModel)
    }
}
