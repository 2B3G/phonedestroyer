package com.example.phonedestroyer

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log.e
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.TransitionBuilder.validate
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.jvm.java
import androidx.core.content.edit
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {
    lateinit var usernameInput: EditText
    lateinit var passwordInput: EditText

    lateinit var notifier: PopupNotiHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_layout)

        notifier = PopupNotiHelper(findViewById(android.R.id.content))

        usernameInput = findViewById<EditText>(R.id.usernameInput)
        passwordInput = findViewById<EditText>(R.id.passwordInput)

        findViewById<Button>(R.id.loginBtn).setOnClickListener {sendRequest("login")}
        findViewById<Button>(R.id.registerBtn).setOnClickListener {sendRequest("register")}
    }

    private fun sendRequest(type: String){
        try{
            val (usr, pass) = validate()

            val json = JSONObject()
            json.put("username", usr)
            json.put("hash", sha256(pass))

            HttpHelper.instance.post("${BuildConfig.API_BASE_URL}/$type", json.toString()){
                success, body ->

                val resp = JSONObject(body)

                if(success) {
                    proceed(usr, resp.getString("uid"))
                }
                else{
                    // TODO: show an error message
                    notifier.showMessage(resp.getString("message"), PopupNotiHelper.Severity.ERROR)
                }
            }
        }catch (e: Error){
            notifier.showMessage(e.message.toString(), PopupNotiHelper.Severity.ERROR)
        }
    }

    private fun validate(): Pair<String, String>{
        val usr = usernameInput.text.toString()
        val pass = passwordInput.text.toString()

        if(usr.isNullOrEmpty() || pass.isNullOrEmpty()) throw Error("Nazwa użytkownika i hasło nie mogą być puste")

        return Pair(usr, pass)
    }

    fun sha256(input: String): String {
        val bytes = input.toByteArray() // convert string to bytes
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes) // hash the bytes

        // Convert bytes to hex string
        return digest.joinToString("") { "%02x".format(it) }
    }

    // TODO : remove the default uid when api is done
    private fun proceed(username: String, uid: String){
        val sp = getSharedPreferences("user_data", MODE_PRIVATE);

        sp.edit {
            putString("username", username)
            putString("uid", uid)
        }

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}