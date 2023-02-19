package io.github.quackerjack.app.android

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.quackerjack.app.android.DuckActions.*
import io.github.quackerjack.app.android.Moods.GET_WRECKED
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.random.Random

enum class Moods(val serverVal: String) {
    GET_WRECKED("mean"),
    GET_THERAPY("nice"),
    TESLA("tesla")
}

enum class DuckActions {
    IDLE,
    Triggered,
    LISTENING,
    SPEAKING
}

class Model(
    val moodState: MutableState<Moods> = mutableStateOf(GET_WRECKED),
    val duckActionState: MutableState<DuckActions> = mutableStateOf(IDLE),
    var userText: String = "",
    var duckText: String = "",
    var nameState: MutableState<String> = mutableStateOf("JOE")
): ViewModel() {
    fun triggerConvo() {
        duckActionState.value = Triggered
        viewModelScope.launch {
            delay(1000)
            duckActionState.value = LISTENING
        }
    }
    fun send(callback: Httpcall.HttpResponseCallback) {
        thread {
            val json = JSONObject()
            json.put(Httpcall.SNIPPET, userText)
            json.put(
                Httpcall.NAME,
                nameState.value.takeIf { it.isNotBlank() } ?: "Boss"
            )
            val mood = if (Random(42).nextFloat() < 0.1) Moods.TESLA else moodState.value
            json.put(Httpcall.MODE, mood.serverVal)
            Httpcall.main(json, callback)
        }
    }
}