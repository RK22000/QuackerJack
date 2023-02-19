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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
        private const val ACTIVATION_KEYWORD = "Damn It"
        private const val ACTIVATION_RESPONSE = "What's up BOSS!"
        private const val CONVERSATION_STOPPER = "Goodbye"
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
//                        model.send(it)
//                        speak()
                        if (it.contains(CONVERSATION_STOPPER, ignoreCase = true))
                            exit()
                        else
                            sendForServerResponse()
                    } ?: exit()
                }
            }

            override fun sendForServerResponse() {
                model.send{
                    model.duckText = it
                    speak()
                }
            }

            override fun exit() {
                model.duckActionState.value = DuckActions.IDLE
                stt.keepListeningForKeyword(
                    keyword = ACTIVATION_KEYWORD,
                    onKeywordHeard = {
//                        model.triggerConvo()
                        model.duckText = ACTIVATION_RESPONSE
                        speak()
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
                painter = painterResource(id = R.drawable.quackerjack),
                contentDescription = "This is Quacker Jack",
                modifier = Modifier
                    .scale(1.5f)
                    .border(
                        3.dp,
                        when(model.duckActionState.value) {
                            DuckActions.SPEAKING -> Color.Green
                            else -> Color.LightGray
                        },
                        CircleShape
                    )
            )
            Column(
                modifier = Modifier.fillMaxWidth(0.7f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Moods.values().filter {
                    it != Moods.TESLA
                }.forEach {
                    val onClick = { mood = it }
                    if (it == mood) {
                        Button(onClick = {}, Modifier.fillMaxWidth().padding(5.dp)) {
                            Text(text = it.name, fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        OutlinedButton(onClick = onClick, Modifier.fillMaxWidth().padding(5.dp)) {
                            Text(text = it.name, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            Image(
                painter = painterResource(id = R.drawable.ic_mic),
                contentDescription = "This is a mic",
                modifier = Modifier
                    .scale(3f)
                    .border(
                        2.dp,
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
