package com.example.phonedestroyer.fragments// Leaderboard.kt
import LeaderboardAdapter
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phonedestroyer.BuildConfig
import com.example.phonedestroyer.HttpHelper
import com.example.phonedestroyer.MainActivity
import com.example.phonedestroyer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class LeaderboardEntry(
    val position: Int,
    val playerName: String,
    val score: Int
)

class Leaderboard : Fragment(R.layout.leaderboard_layout) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var adapter: LeaderboardAdapter

    private lateinit var uuid: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uuid = requireActivity().getSharedPreferences("user_data", MODE_PRIVATE).getString("uid", null).toString()

        recyclerView = view.findViewById(R.id.recyclerViewLeaderboard)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = LeaderboardAdapter()
        recyclerView.adapter = adapter

        loadLeaderboardData()
    }

     fun loadLeaderboardData() {
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        recyclerView.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                HttpHelper.instance.get(
                    "${BuildConfig.API_BASE_URL}/leaderboard",
                    mapOf("token" to uuid)
                ) { ok, body ->
                    // Since the callback runs on the main thread, we need to switch contexts for UI updates.
                    // However, we must ensure all paths in the callback are handled.
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            if (ok) {
                                val jsonArray = JSONArray(body)

                                val entries = mutableListOf<LeaderboardEntry>()
                                for (i in 0 until jsonArray.length()) {
                                    val item = jsonArray.getJSONObject(i)
                                    entries.add(
                                        LeaderboardEntry(
                                            position = i + 1,
                                            playerName = item.getString("username"),
                                            score = item.getInt("score")
                                        )
                                    )
                                }
                                adapter.submitList(entries)
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
            } catch (e: Exception) { // This will now primarily catch IOExceptions from HttpHelper
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Nie udało się załadować tabeli wyników"

                    (activity as? MainActivity)?.stopRefreshing()
                }
            }
        }
    }
}