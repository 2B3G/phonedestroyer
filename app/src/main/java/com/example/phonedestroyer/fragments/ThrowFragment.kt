package com.example.phonedestroyer

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.math.sin
import kotlin.math.sqrt

class ThrowFragment : Fragment(R.layout.throw_layout), SensorEventListener {

    private lateinit var resultText: TextView
    private lateinit var startButton: Button
    private lateinit var sensorManager: SensorManager
    private lateinit var resultButtonsContainer: LinearLayout

    private var recording = false

    // Throw detection constants
    private val THROW_THRESHOLD = 5.0
    private val WINDOW_MS = 200
    private val NANOS_IN_MS = 1_000_000L

    // Window data
    private var windowActive = false
    private var windowStartTime = 0L
    private val accelWindow = mutableListOf<Pair<Long, Double>>()

    // Gyroscope → release angle
    private var currentPitch = 0.0
    private var lastGyroTimestamp = 0L

    private lateinit var currentResults: Throw

    lateinit var uuid: String
    lateinit var notifier: PopupNotiHelper

    data class Throw(
        val speed: Double,
        val acceleration: Double,
        val distance: Double,
        val uid: String? = null,
    ){
        override fun toString(): String {
            return buildString {
                append("""{"speed": $speed, "accel": $acceleration, "distance": $distance""")
                if (uid != null) append(""", "uid": "$uid"""")
                append("}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uuid =
            requireActivity().getSharedPreferences("user_data", MODE_PRIVATE).getString("uid", null)
                .toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notifier = PopupNotiHelper(view)

        startButton = view.findViewById(R.id.startButton)
        resultButtonsContainer = view.findViewById(R.id.resultButtonsContainer)
        resultText = view.findViewById(R.id.resultText)


        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)

        view.findViewById<Button>(R.id.keepButton).setOnClickListener {
            // Handle "Keep" action
            saveThrow(currentResults)
            resetThrowUI()
        }

        view.findViewById<Button>(R.id.discardButton).setOnClickListener {
            resetThrowUI()
        }

        startButton.setOnClickListener {
            accelWindow.clear()
            recording = true
            windowActive = false
            currentPitch = 0.0
            lastGyroTimestamp = 0L
            resultText.text = "Rzuć telefonem !"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // Gyroscope → calculate pitch
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val wx = event.values[0]
            val now = event.timestamp
            if (lastGyroTimestamp != 0L) {
                val dt = (now - lastGyroTimestamp) / 1_000_000_000.0
                currentPitch += wx * dt
            }
            lastGyroTimestamp = now
            return
        }

        // Accelerometer → detect throw
        if (!recording || event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        val ax = event.values[0].toDouble()
        val ay = event.values[1].toDouble()
        val az = event.values[2].toDouble()

        val aMag = sqrt(ax * ax + ay * ay + az * az)
        val now = event.timestamp

        // Detect start of throw
        if (!windowActive && aMag > THROW_THRESHOLD) {
            windowActive = true
            windowStartTime = now
            accelWindow.clear()
        }

        if (windowActive) {
            accelWindow.add(now to aMag)

            if ((now - windowStartTime) / NANOS_IN_MS > WINDOW_MS) {
                processThrowWindow()
                windowActive = false
                recording = false
            }
        }
    }

    private fun processThrowWindow() {
        if (accelWindow.isEmpty()) return

        // Peak acceleration
        val peakAccel = accelWindow.maxOf { it.second }

        // Integrate acceleration → velocity
        var velocity = 0.0
        for (i in 1 until accelWindow.size) {
            val dt = (accelWindow[i].first - accelWindow[i - 1].first) / 1_000_000_000.0
            val a = accelWindow[i].second
            velocity += a * dt
        }

        val speedMps = velocity
        val speedKmh = speedMps * 3.6
        val angleRad = currentPitch

        // Projectile distance
        val distance = (speedMps * speedMps * sin(2 * angleRad)) / 9.81

        // Update UI after delay
        Handler(Looper.getMainLooper()).postDelayed({
            currentResults = Throw(speedKmh, peakAccel, distance, uuid)

            showThrowResults("""
                Wykryto rzut!

                Maksymalne Przyśpieszenie: %.2f m/s²
                Prędkość: %.2f km/h
                Dystans: %.2f m
            """.trimIndent().format(
                peakAccel,
                speedKmh,
                distance
            ))
        }, 2000)
    }

    private fun showThrowResults(result: String) {
        resultText.text = result
        startButton.visibility = View.GONE
        resultButtonsContainer.visibility = View.VISIBLE
    }

    private fun resetThrowUI() {
        resultText.text = ""
        startButton.visibility = View.VISIBLE
        resultButtonsContainer.visibility = View.GONE
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }

    private fun saveThrow(throwObj: Throw){
        Log.d("MYAPP", throwObj.toString())
        HttpHelper.instance.post("${BuildConfig.API_BASE_URL}/user/${uuid}/throw", throwObj.toString(), mapOf("token" to uuid)) {
            ok, body ->
            if (!ok) {
                notifier.showMessage("Nie udało się zapisać rzutu. Spróbuj ponownie", PopupNotiHelper.Severity.ERROR)
            }
        }
    }
}
