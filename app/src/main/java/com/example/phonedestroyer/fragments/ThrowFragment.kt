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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.transition.Visibility
import kotlin.math.sqrt

class ThrowFragment : Fragment(R.layout.throw_layout), SensorEventListener {

    private lateinit var resultText: TextView
    private lateinit var startButton: Button
    private lateinit var sensorManager: SensorManager
    private lateinit var resultButtonsContainer: LinearLayout
    private lateinit var animalsScaleFragmentContainer: FrameLayout

    private var recording = false

    // Throw detection thresholds
    private val throwThreshold = 20.0f // m/s² (about 2g)
    private val landingThreshold = 7.0f // m/s²
    private val minThrowDuration = 200L // milliseconds

    // Throw state tracking
    private var isThrowInProgress = false
    private var throwStartTime = 0L
    private var throwDuration = 0L
    private var airTime = 0L

    // Data tracking
    private var maxAcceleration = 0f
    private var maxVelocity = 0f
    private var estimatedDistance = 0f

    // Integration for velocity and distance
    private var velocityX = 0f
    private var velocityY = 0f
    private var velocityZ = 0f
    private var distanceX = 0f
    private var distanceY = 0f
    private var distanceZ = 0f
    private var lastTimestamp = 0L

    // Time-series data for graphing
    private val timeSeriesData = mutableListOf<TimePoint>()

    data class TimePoint(
        val timeMs: Long,
        val acceleration: Float,
        val velocity: Float,
        val distance: Float
    )

    private lateinit var currentResults: Throw

    lateinit var uuid: String
    lateinit var notifier: PopupNotiHelper

    data class Throw(
        val speed: Double,
        val acceleration: Double,
        val distance: Double,
        val timeSeries: List<TimePoint>,
        val uid: String? = null,
    ){
        override fun toString(): String {
            return buildString {
                append("""{"speed": $speed, "accel": $acceleration, "distance": $distance""")

                // Add time series data
                append(""", "timeSeries": [""")
                timeSeries.forEachIndexed { index, point ->
                    append("""{"timeMs": ${point.timeMs}, "accel": ${point.acceleration}, "vel": ${point.velocity}, "dist": ${point.distance}}""")
                    if (index < timeSeries.size - 1) append(", ")
                }
                append("]")

                if (uid != null) append(""", "uid": "$uid"""")
                append("}")
            }
        }
    }

    // Data class for RecyclerView (with nullable timeSeries for lazy loading)
    data class ThrowData(
        val id: String,
        val distance: String,
        val acceleration: String,
        val speed: String,
        val distanceVal: Double = 0.0,
        val accelerationVal: Double = 0.0,
        val speedVal: Double = 0.0,
        val timeSeries: List<TimePoint>? = null
    )

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
        animalsScaleFragmentContainer = view.findViewById(R.id.animalsScaleFragmentContainer)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)

        view.findViewById<Button>(R.id.keepButton).setOnClickListener {
            // Handle "Keep" action
            saveThrow(currentResults)
            resetThrowUI()
        }

        view.findViewById<Button>(R.id.discardButton).setOnClickListener {
            resetThrowUI()
        }

        startButton.setOnClickListener {
            startThrowDetection()
        }
    }

    private fun startThrowDetection() {
        recording = true
        isThrowInProgress = false
        resetThrowData()
        resultText.text = "Rzuć telefonem !"

        // Dismiss AnimalScaleFragment if it exists
        val animalScaleFragment = childFragmentManager.findFragmentByTag("AnimalScaleFragment")
        if (animalScaleFragment != null) {
            childFragmentManager.beginTransaction()
                .remove(animalScaleFragment)
                .commit()
        }
    }

    private fun resetThrowData() {
        maxAcceleration = 0f
        maxVelocity = 0f
        estimatedDistance = 0f
        throwStartTime = 0L
        throwDuration = 0L
        airTime = 0L
        velocityX = 0f
        velocityY = 0f
        velocityZ = 0f
        distanceX = 0f
        distanceY = 0f
        distanceZ = 0f
        lastTimestamp = 0L
        timeSeriesData.clear()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!recording || event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate total acceleration (removing gravity)
        val acceleration = sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH

        // Detect throw start
        if (!isThrowInProgress && kotlin.math.abs(acceleration) > throwThreshold) {
            isThrowInProgress = true
            throwStartTime = System.currentTimeMillis()
            lastTimestamp = event.timestamp
            resultText.text = "Wykryto rzut! Śledzenie..."
        }

        // Track throw data
        if (isThrowInProgress) {
            // Calculate time delta in seconds
            val dt = if (lastTimestamp > 0) {
                (event.timestamp - lastTimestamp) / 1000000000.0f
            } else {
                0f
            }
            lastTimestamp = event.timestamp

            // Update max acceleration
            if (kotlin.math.abs(acceleration) > maxAcceleration) {
                maxAcceleration = kotlin.math.abs(acceleration)
            }

            // Integrate acceleration to get velocity (simple integration)
            if (dt > 0 && dt < 0.1f) { // Sanity check on dt
                velocityX += x * dt
                velocityY += y * dt
                velocityZ += z * dt

                // Calculate total velocity
                val currentVelocity = sqrt(velocityX*velocityX + velocityY*velocityY + velocityZ*velocityZ)
                if (currentVelocity > maxVelocity) {
                    maxVelocity = currentVelocity
                }

                // Integrate velocity to get distance
                distanceX += velocityX * dt
                distanceY += velocityY * dt
                distanceZ += velocityZ * dt

                val currentDistance = sqrt(distanceX*distanceX + distanceY*distanceY + distanceZ*distanceZ)

                // Collect time series data
                val currentTime = System.currentTimeMillis()
                timeSeriesData.add(
                    TimePoint(
                        timeMs = currentTime - throwStartTime,
                        acceleration = kotlin.math.abs(acceleration),
                        velocity = currentVelocity,
                        distance = currentDistance
                    )
                )
            }

            // Detect throw end (acceleration returns to near gravity)
            if (kotlin.math.abs(acceleration) < landingThreshold) {
                val currentTime = System.currentTimeMillis()
                throwDuration = currentTime - throwStartTime

                // Only count as valid throw if duration is long enough
                if (throwDuration > minThrowDuration) {
                    airTime = throwDuration
                    estimatedDistance = sqrt(distanceX*distanceX + distanceY*distanceY + distanceZ*distanceZ)

                    // Stop detection and show results
                    stopDetection()
                    displayResults()
                } else {
                    // Too short, might be a false positive
                    isThrowInProgress = false
                    resetThrowData()
                }
            }
        }
    }

    private fun stopDetection() {
        recording = false
        isThrowInProgress = false
    }

    private fun displayResults() {
        val speedKmh = maxVelocity * 3.6 // Convert m/s to km/h

        currentResults = Throw(
            speedKmh.toDouble(),
            maxAcceleration.toDouble(),
            estimatedDistance.toDouble(),
            timeSeriesData.toList(), // Create a copy of the list
            uuid
        )

        showThrowResults("""
            Wykryto rzut!

            Maksymalne Przyśpieszenie: %.2f m/s²
            Prędkość: %.2f km/h
            Dystans: %.2f m
        """.trimIndent().format(
            maxAcceleration,
            speedKmh,
            estimatedDistance
        ))

        // Show AnimalScaleFragment with the speed (in m/s)
        showAnimalComparison(maxVelocity)
    }

    private fun showAnimalComparison(speedMps: Float) {
        val animalScaleFragment = AnimalScaleFragment.newInstance(speedMps)

        animalsScaleFragmentContainer.visibility = View.VISIBLE

        // Add fragment below the results
        childFragmentManager.beginTransaction()
            .replace(R.id.animalsScaleFragmentContainer, animalScaleFragment)
            .commit()
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
        animalsScaleFragmentContainer.visibility = View.GONE
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
