package io.github.quackerjack.app.android

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.quackerjack.app.android.Moods.*
import io.github.quackerjack.app.android.DuckActions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Moods {
    MEAN,
    HELPFUL,
    TALKATIVE
}

enum class DuckActions {
    IDLE,
    Triggered,
    LISTENING,
    SPEAKING
}

class Model(
    val moodState: MutableState<Moods> = mutableStateOf(MEAN),
    val duckActionState: MutableState<DuckActions> = mutableStateOf(IDLE),
    var userText: String = "",
    var duckText: String = "",
): ViewModel() {
    fun triggerConvo() {
        duckActionState.value = Triggered
        viewModelScope.launch {
            delay(1000)
            duckActionState.value = LISTENING
        }

    }
}