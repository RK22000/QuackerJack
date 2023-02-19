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

enum class Moods(val serverVal: String) {
    GET_REASSURED("nice"),
    GET_WRECKED("mean"),
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
            json.put(Httpcall.MODE, moodState.value.serverVal)
            Httpcall.main(json, callback)
        }
//        viewModelScope.launch {
//            with(Dispatchers.IO) {
//                val json = JSONObject()
//                json.put("snippet", userText)
//                json.put("mode", moodState.value.serverVal)
//                val url = "https://duck123.uw.r.appspot.com/chatbot"
//
//                val response = post(url, json, object : Callback {
//                    override fun onFailure(call: Call, e: IOException) {
//                        Log.v("Model", "Server response failed \n{${e.message}")
//
//                    }
//
//                    override fun onResponse(call: Call, response: Response) {
//                        Log.v("Model", "Server response: $response")
//                    }
//
//                })
//            }
//        }
    }
}