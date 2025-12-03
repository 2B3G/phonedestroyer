package com.example.phonedestroyer

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.math.log

class HttpHelper private constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .build()

    companion object {
        val instance: HttpHelper by lazy { HttpHelper() }
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    fun get(url: String, headers: Map<String, String>? = null, callback: (success: Boolean, body: String?) -> Unit) {
        val builder = Request.Builder().url(url)
        headers?.forEach { (k, v) -> builder.addHeader(k, v) }
        val request = builder.get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    callback(response.isSuccessful, it.body?.string())
                }
            }
        })
    }

    fun post(url: String, json: String, headers: Map<String, String>? = null, callback: (success: Boolean, body: String?) -> Unit) {
        val body = json.toRequestBody(JSON)

        val builder = Request.Builder().url(url).post(body)
        headers?.forEach { (k, v) -> builder.addHeader(k, v) }
        val request = builder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    callback(response.isSuccessful, it.body?.string())
                }
            }
        })
    }
}
