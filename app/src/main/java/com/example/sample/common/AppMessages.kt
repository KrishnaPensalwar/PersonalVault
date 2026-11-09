package com.example.sample.common

import kotlinx.coroutines.flow.MutableSharedFlow

object AppMessages {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun show(text: String) {
        messages.tryEmit(text)
    }
}
