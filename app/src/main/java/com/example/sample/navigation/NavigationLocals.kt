package com.example.sample.navigation


import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

val LocalBackStack = staticCompositionLocalOf<NavBackStack<NavKey>> {
    error("BackStack not provided")
}

val LocalTopBarState = staticCompositionLocalOf<TopBarState> {
    error("TopBarState not provided")
}

val LocalCurrentDestination = staticCompositionLocalOf<Destinations?> {
    null
}