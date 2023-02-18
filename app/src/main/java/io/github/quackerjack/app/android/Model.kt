package io.github.quackerjack.app.android

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.github.quackerjack.app.android.Moods.*

enum class Moods {
    MEAN,
    HELPFUL,
    TALKATIVE
}

class Model(
    val moodState: MutableState<Moods> = mutableStateOf(MEAN),
): ViewModel() {

}