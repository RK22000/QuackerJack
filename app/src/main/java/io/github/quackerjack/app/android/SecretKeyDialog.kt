package io.github.quackerjack.app.android

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.quackerjack.app.android.ui.theme.Cerulean


@Composable
fun SecretKeyDialog(
    onKeyEntered: (String) -> Unit
) {
    val model: Model = viewModel()
    var dialogOpen by model.dialogOpenState

    dialogOpen.takeIf { it }?.let {
        var openAIKEY by remember {
            mutableStateOf(model.secretKey)
        }
        with(LocalContext.current) {
            AlertDialog(
                onDismissRequest = { dialogOpen = false },
                confirmButton = {
                    Button(onClick = {
                        onKeyEntered(openAIKEY)
                        dialogOpen = false
                    }) {
                        Text(text = "Confirm Key")
                    }
                },
                dismissButton = {
                    Button(onClick = { dialogOpen = false }) {
                        Text(text = "Cancel")
                    }
                },
                title = { Text(text = "Enter your OpenAI Key") },
                text = {
                    Column {
                        val focusManager = LocalFocusManager.current
                        TextField(
                            value = openAIKEY,
                            onValueChange = { openAIKEY = it },
                            label = { Text(text = "Open AI KEY") },
                            singleLine = true,
                            keyboardActions = KeyboardActions { focusManager.clearFocus() }
                        )
                        Text(
                            text = "Get API Key",
                            modifier = Modifier.clickable {
                                val webIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://platform.openai.com/account/api-keys")
                                )
                                startActivity(webIntent)
                            },
                            color = Cerulean,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }

//            properties = DialogProperties()//dismissOnBackPress = true, dismissOnClickOutside = true)
            )
        }
    }
}