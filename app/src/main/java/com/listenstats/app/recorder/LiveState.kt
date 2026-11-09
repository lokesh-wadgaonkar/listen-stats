package com.listenstats.app.recorder

import com.listenstats.app.data.LiveSession
import kotlinx.coroutines.flow.MutableStateFlow

/** In-process state published by the service so the UI can show what is being tracked right now. */
object LiveState {
    val connected = MutableStateFlow(false)
    val sessions = MutableStateFlow<List<LiveSession>>(emptyList())
}
