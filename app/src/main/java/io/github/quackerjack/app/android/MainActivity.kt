package io.github.quackerjack.app.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.quackerjack.app.android.ui.theme.QuackerJackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    companion object {
        private const val ACTIVATION_KEYWORD = "HELLO WORLD"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[Model::class.java]

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val RECORD_AUDIO_REQUEST_CODE = 1
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        }

        val stt: SpeechToText = BasicSpeechToText(applicationContext)
        if (stt.isAvailable()) {
            Toast.makeText(applicationContext, "TTS Available", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(applicationContext, "TTS Unavailable", Toast.LENGTH_SHORT).show()
        }
        var ttsInitialized = false
        val tts = TextToSpeech(applicationContext) { status ->
            ttsInitialized = status == TextToSpeech.SUCCESS
        }
        val convoLoop = object : ConvoLoop {
            override fun speak() {
                val text = model.duckText
                if (ttsInitialized) {
                    model.duckActionState.value = DuckActions.SPEAKING
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "0")
                    tts.setOnUtteranceProgressListener(object : SimpleUtteranceDoneListener() {
                        override fun onDone(p0: String?) {
                            model.viewModelScope.launch {
                                withContext(Dispatchers.Main) {
                                    listen()
                                }
                            }
                        }
                    })
                }
            }

            override fun listen() {
                model.duckActionState.value = DuckActions.LISTENING
                stt.listen {
                    it?.let {
                        model.userText = it
                        model.duckText = it
                        speak()
                    } ?: exit()
                }
            }

            override fun exit() {
                model.duckActionState.value = DuckActions.IDLE
                stt.keepListeningForKeyword(
                    keyword = ACTIVATION_KEYWORD,
                    onKeywordHeard = {
                        model.triggerConvo()
                        listen()
                    }
                )
            }
        }
        convoLoop.exit()

        setContent {
            QuackerJackTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Screen()
                }
            }
        }
    }

    @Composable
    @Preview
    fun Screen() {
        val model: Model = viewModel()
        var mood: Moods by remember {
            model.moodState
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.duck),
                contentDescription = "This is Quacker Jack",
                modifier = Modifier.border(
                    3.dp,
                    when(model.duckActionState.value) {
                        DuckActions.SPEAKING -> Color.Green
                        else -> Color.LightGray
                    },
                    CircleShape
                )
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Moods.values().forEach {
                    val onClick = { mood = it }
                    if (it == mood) {
                        OutlinedButton(onClick = {}) {
                            Text(text = it.name)
                        }
                    } else {
                        Button(onClick = onClick) {
                            Text(text = it.name)
                        }
                    }
                }
            }
            Image(
                painter = painterResource(id = R.drawable.ic_mic),
                contentDescription = "This is a mic",
                modifier = Modifier.border(
                    3.dp,
                    when(model.duckActionState.value) {
                        DuckActions.Triggered -> Color.Magenta
                        DuckActions.LISTENING -> Color.Green
                        else -> Color.LightGray
                    },
                    CircleShape
                )
            )

        }


    }
}
