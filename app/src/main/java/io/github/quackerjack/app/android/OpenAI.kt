package io.github.quackerjack.app.android

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

fun main() {
    println(openAITextCompletion(null, "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"))
}

fun openAITextCompletion(requestJSON: JSONObject? = null, apiKey: String): String? {
    val client = OkHttpClient()

    val mediaType = "application/json".toMediaTypeOrNull()
    val body = RequestBody.create(mediaType, "{\"model\": \"text-davinci-003\", \"prompt\": \"Say this is a test\", \"temperature\": 0, \"max_tokens\": 7}")
    val request = Request.Builder()
        .url("https://api.openai.com/v1/completions")
        .post(body)
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", "Bearer $apiKey")
        .build()

    println("Making call")

    val responseString = try {
        val response = client.newCall(request).execute()
        response.body?.string()
    } catch (e: Exception) {
        Log.v("OpenAI", e.message?:"No message in error")
        null
    }

    return responseString?.let {
        val responseJson = JSONObject(it)
        responseJson
            .getJSONArray("choices")
            .getJSONObject(0)
            .getString("text")
    }
}