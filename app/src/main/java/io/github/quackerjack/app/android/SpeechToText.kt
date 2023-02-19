package io.github.quackerjack.app.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import android.util.Log

interface SpeechToText {
    fun listenForKeyword(keyword: String, onKeywordHeard: ()->Unit)
    fun listen(doAfterListening: (String?)->Unit)
    fun isAvailable(): Boolean
    fun keepListeningForKeyword(keyword: String, onKeywordHeard: () -> Unit)
    fun stopListening()
    fun destroy()
}

class BasicSpeechToText(val appContext: Context): SpeechToText {
    private abstract class SimpleListener: RecognitionListener {
        val TAG = "SimpleListener"
    override fun onReadyForSpeech(p0: Bundle?) {
        //Do Nothing
    }

    override fun onBeginningOfSpeech() {
        //Do Nothing
        Log.v(TAG, "Started hearing speech")
    }

    override fun onRmsChanged(p0: Float) {
        //Do Nothing
    }

    override fun onBufferReceived(p0: ByteArray?) {
        //Do Nothing
    }

    override fun onEndOfSpeech() {
        //Do Nothing
        Log.v(TAG, "Stopped hearing speech")
    }

    override fun onError(p0: Int) {
        //Do Nothing
        Log.v(TAG, "Error($p0) in hearing speech")
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        //Do Nothing
    }
}
    private val speechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(appContext)
    }
    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(
            RecognizerIntent.EXTRA_CALLING_PACKAGE,
            appContext.packageName
        )
        putExtra(
            RecognizerIntent.EXTRA_MAX_RESULTS,
            1
        )
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        // --- You could prefer offline here
    }

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)
    override fun keepListeningForKeyword(keyword: String, onKeywordHeard: () -> Unit) {
        var keywordHeard = false
        speechRecognizer.setRecognitionListener(object : SimpleListener() {
            override fun onResults(p0: Bundle?) {
                p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let {
                    if (keywordHeard || it.contains(keyword, true)) {
                        onKeywordHeard()
                    } else {
                        tryAgain()
                    }
                }
            }
            override fun onPartialResults(p0: Bundle?) {
                Log.v("SpeechToTextListen", "Called On Partial Result")
                p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let {
                    Log.v("SpeechToTextListen", it.toString())
                    if (it.get(0).lowercase().contains(keyword.lowercase())) {
                        Log.v("SpeechToTextListen", "Recognized keyword")
                        speechRecognizer.stopListening()
                        keywordHeard = true
                    }
                }
            }
            override fun onError(p0: Int) {
                super.onError(p0)
                if (keywordHeard)
                    onKeywordHeard()
                else
                    tryAgain()
            }
        })
        speechRecognizer.startListening(recognizerIntent)
    }

    private fun tryAgain() {
        Log.v("SpeechToTextListen", "Awe shit, here we go again")
//        speechRecognizer.stopListening()
        speechRecognizer.startListening(recognizerIntent)
    }

    override fun listenForKeyword(keyword: String, onKeywordHeard: () -> Unit) {

        speechRecognizer.setRecognitionListener(object : SimpleListener() {
            override fun onResults(p0: Bundle?) {
                // Do Nothing
            }

            override fun onPartialResults(p0: Bundle?) {
                Log.v("SpeechToTextListen", "Called On Partial Result")
                p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let {
                    Log.v("SpeechToTextListen", it.toString())
                    if (it.get(0).lowercase().contains(keyword.lowercase())) {
                        Log.v("SpeechToTextListen", "Recognized keyword")
                        onKeywordHeard()
                        speechRecognizer.stopListening()
                    }
                }
            }
        })
        speechRecognizer.startListening(recognizerIntent)
    }

    override fun listen(doAfterListening: (String?) -> Unit) {
        Log.v("SpeechToTextListen", "Called Listen")
        speechRecognizer.setRecognitionListener(object : SimpleListener() {
            override fun onResults(p0: Bundle?) {
                Log.v("SpeechToTextListen", "Called On Result")
                p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)
                    ?.also { Log.v("SpeechToTextListen", it) }
                    ?.let(doAfterListening)
            }
            override fun onPartialResults(p0: Bundle?) {
                // Do Nothing
                Log.v("SpeechToTextListen", "Called On Partial Result")
                p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let {
                    Log.v("SpeechToTextListen", it.toString())
                }
            }

            override fun onError(p0: Int) {
                super.onError(p0)
                doAfterListening(null)
            }
        })
        speechRecognizer.startListening(recognizerIntent)
    }

    override fun stopListening() {
        speechRecognizer.stopListening()
    }

    override fun destroy() {
       speechRecognizer.destroy()
    }
}

abstract class SimpleUtteranceDoneListener: UtteranceProgressListener() {
    override fun onStart(p0: String?) {
        // Do Nothing
        Log.v("Rahul", "Started speaking")
    }

    @Deprecated("Dont use??",
        ReplaceWith("Log.v(this::class.simpleName, \"Error ()\")", "android.util.Log")
    )
    override fun onError(utteranceId: String?) {
        // Do Nothing
        Log.v(this::class.simpleName, "Error ()")
    }

}

interface ConvoLoop {
    fun speak()
    fun listen()
    fun sendForServerResponse()
    fun exit()
}