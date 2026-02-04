package com.example.phonedestroyer

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class HttpHelper private constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder().build()

    companion object {
        val instance: HttpHelper by lazy { HttpHelper() }
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val TAG = "HTTP"
    }

    fun get(
        url: String,
        headers: Map<String, String>? = null,
        callback: (success: Boolean, body: String?) -> Unit
    ) {
        Log.d(TAG, "➡️ GET $url")
        headers?.forEach { (k, v) ->
            Log.d(TAG, "Header: $k = $v")
        }

        val builder = Request.Builder().url(url)
        headers?.forEach { (k, v) -> builder.addHeader(k, v) }
        val request = builder.get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "❌ GET failed: ${e.message}")
                callback(false, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                Log.d(TAG, "⬅️ GET ${response.code} $url")
                Log.d(TAG, "Response body: $body")

                response.close()
                callback(response.isSuccessful, body)
            }
        })
    }

    fun post(
        url: String,
        json: String,
        headers: Map<String, String>? = null,
        callback: (success: Boolean, body: String?) -> Unit
    ) {
        Log.d(TAG, "➡️ POST $url")
        Log.d(TAG, "Request body: $json")
        headers?.forEach { (k, v) ->
            Log.d(TAG, "Header: $k = $v")
        }

        val body = json.toRequestBody(JSON)

        val builder = Request.Builder().url(url).post(body)
        headers?.forEach { (k, v) -> builder.addHeader(k, v) }
        val request = builder.build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "❌ POST failed: ${e.message}")
                callback(false, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                Log.d(TAG, "⬅️ POST ${response.code} $url")
                Log.d(TAG, "Response body: $responseBody")

                response.close()
                callback(response.isSuccessful, responseBody)
            }
        })
    }
}
