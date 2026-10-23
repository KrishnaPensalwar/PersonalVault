package com.example.sample.navigation

import kotlinx.serialization.Serializable

sealed interface Destinations {

    @Serializable
    data object Dashboard : Destinations

    @Serializable
    data object Storage : Destinations

    @Serializable
    data object Password : Destinations

    @Serializable
    data object Family : Destinations


}