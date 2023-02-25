package io.github.quackerjack.app.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.quackerjack.app.android.ui.theme.EarthYellow
import io.github.quackerjack.app.android.ui.theme.QuackerJackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    companion object {
        private const val ACTIVATION_KEYWORD = "Damn It"
        private val ACTIVATION_KEYWORDS = listOf("Damn It", "Hey Jack", "*")
        private const val ACTIVATION_RESPONSE = "What's up BOSS!"
        private const val CONVERSATION_STOPPER = "Goodbye"
        private val CONVERSATION_STOPPERS = listOf("Goodbye", "Stop talking Jack")
    }
    lateinit var stt: SpeechToText
    lateinit var convoLoop: ConvoLoop
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[Model::class.java]

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val RECORD_AUDIO_REQUEST_CODE = 1
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        }

        var ttsInitialized = false
        val tts = TextToSpeech(applicationContext) { status ->
            ttsInitialized = status == TextToSpeech.SUCCESS
        }
        convoLoop = object : ConvoLoop {
            override fun speak() {
                val text = model.duckText
                if (ttsInitialized) {
                    tts.language = Locale.UK
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
                    it?.let {usrTxt ->
                        model.userText = usrTxt
                        model.duckText = usrTxt
//                        model.send(it)
//                        speak()
//                        if (it.contains(CONVERSATION_STOPPER, ignoreCase = true))
                        if (
                            CONVERSATION_STOPPERS.any { usrTxt.contains(it, true) }
                        )
                            exit()
                        else
                            sendForServerResponse()
                    } ?: exit()
                }
            }

            override fun sendForServerResponse() {
                model.duckActionState.value = DuckActions.Triggered
                model.send{
                    model.duckText = it
                    speak()
                }
            }

            override fun exit() {
                model.duckActionState.value = DuckActions.IDLE
                stt.keepListeningForKeywords(
                    keywords = ACTIVATION_KEYWORDS,
                    onKeywordHeard = {
//                        model.triggerConvo()
                        model.duckText = ACTIVATION_RESPONSE
                        speak()
                    }
                )
            }
        }

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        sharedPref.apply {
            getString(Preferences.SECRET_KEY.key, null)?.let {
                model.secretKey = it
            }
            getString(Preferences.USER_NAME.key, null)?.let {
                model.nameState.value = it
            }
        }
        model.saveName = {
            with(sharedPref.edit()) {
                putString(Preferences.USER_NAME.key, it)
                apply()
            }
        }
        setContent {
            QuackerJackTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Scaffold { it
                        Screen()
                        SecretKeyDialog (
                            onKeyEntered = {
                                with(sharedPref.edit()) {
                                    putString(Preferences.SECRET_KEY.key, it)
                                    apply()
                                }
                                model.secretKey = it

                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::stt.isInitialized)
            stt.destroy()
        stt = BasicSpeechToText(applicationContext)
        if (stt.isAvailable()) {
            Toast.makeText(applicationContext, "TTS Available", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(applicationContext, "TTS Unavailable", Toast.LENGTH_SHORT).show()
        }
        convoLoop.exit()
    }

    override fun onPause() {
        super.onPause()
        stt.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        stt.stopListening()
        stt.destroy()
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
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box{}
            Image(
                painter = painterResource(id = R.drawable.quackerjack),
                contentDescription = "This is Quacker Jack",
                modifier = Modifier
                    .scale(1.5f)
                    .border(
                        3.dp,
                        when (model.duckActionState.value) {
                            DuckActions.SPEAKING -> Color.Green
                            DuckActions.Triggered -> EarthYellow
                            else -> Color.LightGray
                        },
                        CircleShape
                    )
                    .clickable {
                        model.dialogOpenState.value = true
                    }
            )
            val focusManager = LocalFocusManager.current
            TextField(
                value = model.nameState.value,
                onValueChange = {
                    model.nameState.value = it
                },
                singleLine = true,
                label = { Text(text = "User Name") },
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = "User ${model.nameState.value}",
                        modifier = Modifier.scale(1.7f)
                    )
                },
                keyboardActions = KeyboardActions {
                    model.saveName(model.nameState.value)
                    focusManager.clearFocus()
                }

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
                        Button(onClick = {},
                            Modifier
                                .fillMaxWidth()
                                .padding(5.dp)) {
                            Text(text = it.name, fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        OutlinedButton(onClick = onClick,
                            Modifier
                                .fillMaxWidth()
                                .padding(5.dp)) {
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
                        when (model.duckActionState.value) {
//                            DuckActions.Triggered -> Color.Magenta
                            DuckActions.LISTENING -> Color.Green
                            else -> Color.LightGray
                        },
                        CircleShape
                    )
            )

            Button(
                onClick = {
                          thread {
                              ClearHistroyHttpCall.main()
                          }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.error,
                    contentColor = MaterialTheme.colors.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Clear Chat History")
            }

        }


    }
}
