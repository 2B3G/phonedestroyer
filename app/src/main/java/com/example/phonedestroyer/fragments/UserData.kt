package com.example.phonedestroyer.fragments

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
import com.example.phonedestroyer.ThrowDataAdapter
import com.example.phonedestroyer.ThrowFragment
import com.example.phonedestroyer.ThrowFragment.TimePoint
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.collections.mutableListOf

class UserData : Fragment(R.layout.user_data_layout) {

    private lateinit var tvUsername: TextView
    private lateinit var btnLogout: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var adapter: ThrowDataAdapter
    private lateinit var chipGroupFilters: ChipGroup

    private var allThrows = listOf<ThrowFragment.ThrowData>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvUsername = view.findViewById(R.id.tvUsername)
        btnLogout = view.findViewById(R.id.btnLogout)
        recyclerView = view.findViewById(R.id.recyclerViewThrows)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters)

        // Load username from SharedPreferences
        val sp = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val username = sp.getString("username", "Użytkownik") ?: "Użytkownik"
        tvUsername.text = username

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Pass callback to adapter for loading time series data
        adapter = ThrowDataAdapter { throwId ->
            loadTimeSeriesData(throwId)
        }
        recyclerView.adapter = adapter

        // Logout button click listener
        btnLogout.setOnClickListener {
            logout()
        }

        // Filter/Sort selection listener
        chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            applyFilter(checkedIds.firstOrNull())
        }

        loadThrowData()
    }

    private fun applyFilter(checkedId: Int?) {
        if (allThrows.isEmpty()) return

        val sortedList = when (checkedId) {
            R.id.chipSpeed -> allThrows.sortedByDescending { it.speedVal }
            R.id.chipAccel -> allThrows.sortedByDescending { it.accelerationVal }
            R.id.chipDistance -> allThrows.sortedByDescending { it.distanceVal }
            else -> allThrows // R.id.chipAll or nothing - original order (likely chronological)
        }
        adapter.submitList(sortedList)
        recyclerView.scrollToPosition(0)
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

                Log.d("UserData", "Loading throw data for uid: $uid")

                HttpHelper.instance.get("${BuildConfig.API_BASE_URL}/user/${uid}/data", mapOf("token" to uid)) {
                        ok, body ->
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            if (ok) {
                                Log.d("UserData", "Received response: $body")
                                val jsonArray = JSONArray(body)
                                Log.d("UserData", "JSON Array length: ${jsonArray.length()}")

                                val throws = mutableListOf<ThrowFragment.ThrowData>()
                                for (i in 0 until jsonArray.length()) {
                                    val item = jsonArray.getJSONObject(i)
                                    Log.d("UserData", "Item $i: $item")

                                    val throwId = when {
                                        item.has("id") -> item.get("id").toString()
                                        item.has("_id") -> item.getString("_id")
                                        else -> i.toString()
                                    }

                                    val dVal = if (item.has("distance")) item.getDouble("distance") else 0.0
                                    val aVal = when {
                                        item.has("acceleration") -> item.getDouble("acceleration")
                                        item.has("accel") -> item.getDouble("accel")
                                        else -> 0.0
                                    }
                                    val sVal = if (item.has("speed")) item.getDouble("speed") else 0.0

                                    val distanceStr = "%.2f m".format(dVal)
                                    val accelerationStr = "%.2f m/s²".format(aVal)
                                    val speedStr = "%.2f km/h".format(sVal)

                                    throws.add(
                                        ThrowFragment.ThrowData(
                                            id = throwId,
                                            distance = distanceStr,
                                            acceleration = accelerationStr,
                                            speed = speedStr,
                                            distanceVal = dVal,
                                            accelerationVal = aVal,
                                            speedVal = sVal,
                                            timeSeries = null
                                        )
                                    )
                                }

                                allThrows = throws
                                
                                // Apply current filter if any
                                applyFilter(chipGroupFilters.checkedChipId)
                                
                                progressBar.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                            } else {
                                Log.e("UserData", "API call failed")
                                throw Exception("API call failed")
                            }
                        } catch (e: Exception) {
                            Log.e("UserData", "Error parsing data: ${e.message}")
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
                Log.e("UserData", "Error in loadThrowData: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Nie udało się załadować danych"

                    (activity as? MainActivity)?.stopRefreshing()
                }
            }
        }
    }

    private fun loadTimeSeriesData(throwId: String) {
        Log.d("UserData", "Loading time series for throw: $throwId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sp = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
                val uid = sp.getString("uid", "").toString()

                HttpHelper.instance.get("${BuildConfig.API_BASE_URL}/user/${uid}/throw/${throwId}", mapOf("token" to uid)) {
                        ok, body ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (ok) {
                            try {
                                val points = JSONArray(body)
                                val parsed = mutableListOf<TimePoint>()

                                for (j in 0 until points.length()) {
                                    val point = points.getJSONObject(j)
                                    parsed.add(TimePoint(
                                        timeMs = point.getLong("timeMs"),
                                        acceleration = point.getDouble("accel").toFloat(),
                                        distance = point.getDouble("dist").toFloat(),
                                        velocity = point.getDouble("vel").toFloat(),
                                    ))
                                }

                                adapter.updateTimeSeries(throwId, parsed)
                                
                                // Also update in allThrows to keep it in sync if needed later
                                val index = allThrows.indexOfFirst { it.id == throwId }
                                if (index != -1) {
                                    val updatedList = allThrows.toMutableList()
                                    updatedList[index] = updatedList[index].copy(timeSeries = parsed)
                                    allThrows = updatedList
                                }
                                
                            } catch (e: Exception) {
                                Log.e("UserData", "Error parsing time series: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("UserData", "Error loading time series: ${e.message}")
                }
            }
        }
    }
}
