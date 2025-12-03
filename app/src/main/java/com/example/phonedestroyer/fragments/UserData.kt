package com.example.phonedestroyer.fragments

import ThrowDataAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phonedestroyer.BuildConfig
import com.example.phonedestroyer.HttpHelper
import com.example.phonedestroyer.R
import com.example.phonedestroyer.LoginActivity
import com.example.phonedestroyer.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

data class ThrowData(
    val distance: String,
    val acceleration: String,
    val speed: String
)

class UserData : Fragment(R.layout.user_data_layout) {

    private lateinit var tvUsername: TextView
    private lateinit var btnLogout: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var adapter: ThrowDataAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvUsername = view.findViewById(R.id.tvUsername)
        btnLogout = view.findViewById(R.id.btnLogout)
        recyclerView = view.findViewById(R.id.recyclerViewThrows)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        // Load username from SharedPreferences
        val sp = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val username = sp.getString("username", "Użytkownik") ?: "Użytkownik"
        tvUsername.text = username

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ThrowDataAdapter()
        recyclerView.adapter = adapter

        // Logout button click listener
        btnLogout.setOnClickListener {
            logout()
        }

        loadThrowData()
    }

    private fun logout() {
        // Clear SharedPreferences
        val sp = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        sp.edit().clear().apply()

        // Navigate to LoginActivity
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    fun loadThrowData() {
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        recyclerView.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sp = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
                val uid = sp.getString("uid", "").toString()
                HttpHelper.instance.get("${BuildConfig.API_BASE_URL}/user/${uid}/data", mapOf("token" to uid)) {
                    ok, body ->
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            if (ok) {
                                val jsonArray = JSONArray(body)

                                val throws = mutableListOf<ThrowData>()
                                for (i in 0 until jsonArray.length()) {
                                    val item = jsonArray.getJSONObject(i)
                                    throws.add(
                                        ThrowData(
                                            distance = item.getString("distance"),
                                            acceleration = item.getString("acceleration"),
                                            speed = item.getString("speed")
                                        )
                                    )
                                }

                                adapter.submitList(throws)
                                progressBar.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                            } else {
                                throw Exception("API call failed, ok is false.")
                            }
                        } catch (e: Exception) {
                            // This catch block handles JSON parsing errors or the explicit throw
                            progressBar.visibility = View.GONE
                            errorText.visibility = View.VISIBLE
                            errorText.text = "Nie udało się załadować tabeli wyników"
                        }
                        finally {
                            (activity as? MainActivity)?.stopRefreshing()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Nie udało się załadować danych"

                    (activity as? MainActivity)?.stopRefreshing()
                }
            }
        }
    }
}
