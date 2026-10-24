package com.example.sample.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destinations : NavKey{

    @Serializable
    data object Dashboard : Destinations

    @Serializable
    data object Storage : Destinations

    @Serializable
    data object Password : Destinations

    @Serializable
    data object Family : Destinations


}