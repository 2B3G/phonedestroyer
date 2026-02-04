package com.example.phonedestroyer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class ThrowDataAdapter(
    private val onThrowClicked: (String) -> Unit
) : RecyclerView.Adapter<ThrowDataAdapter.ViewHolder>() {

    private var throws = listOf<ThrowFragment.ThrowData>()
    private var expandedPosition = -1 // track expanded item

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val distance: TextView = view.findViewById(R.id.tvDistance)
        val acceleration: TextView = view.findViewById(R.id.tvAcceleration)
        val speed: TextView = view.findViewById(R.id.tvSpeed)
        val chartContainer: LinearLayout = view.findViewById(R.id.chartContainer)
        val chartProgress: ProgressBar = view.findViewById(R.id.chartProgressBar)
        val velocityChart: LineChart = view.findViewById(R.id.velocityChart)
        val accelerationChart: LineChart = view.findViewById(R.id.accelerationChart)
        val distanceChart: LineChart = view.findViewById(R.id.distanceChart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.throw_data_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = throws.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val throwData = throws[position]
        holder.distance.text = throwData.distance
        holder.acceleration.text = throwData.acceleration
        holder.speed.text = throwData.speed

        // expand logic
        val isExpanded = position == expandedPosition

        if (isExpanded) {
            // Show chart container
            holder.chartContainer.visibility = View.VISIBLE

            if (throwData.timeSeries != null && throwData.timeSeries.isNotEmpty()) {
                // Data is loaded, show charts
                holder.chartProgress.visibility = View.GONE
                setupCharts(holder, throwData.timeSeries)
            } else {
                // Data not loaded yet, show progress and trigger loading
                holder.chartProgress.visibility = View.VISIBLE
                holder.velocityChart.visibility = View.GONE
                holder.accelerationChart.visibility = View.GONE
                holder.distanceChart.visibility = View.GONE
                onThrowClicked(throwData.id)
            }
        } else {
            holder.chartContainer.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val previousExpandedPosition = expandedPosition
            expandedPosition = if (isExpanded) -1 else position

            // Notify both positions to update
            if (previousExpandedPosition != -1) {
                notifyItemChanged(previousExpandedPosition)
            }
            notifyItemChanged(position)
        }
    }

    fun submitList(newThrows: List<ThrowFragment.ThrowData>) {
        throws = newThrows
        notifyDataSetChanged()
    }

    fun updateTimeSeries(throwId: String, timeSeries: List<ThrowFragment.TimePoint>) {
        val index = throws.indexOfFirst { it.id == throwId }
        if (index != -1) {
            val updatedThrows = throws.toMutableList()
            updatedThrows[index] = updatedThrows[index].copy(timeSeries = timeSeries)
            throws = updatedThrows
            notifyItemChanged(index)
        }
    }

    private fun setupCharts(holder: ViewHolder, timeSeries: List<ThrowFragment.TimePoint>) {
        holder.chartProgress.visibility = View.GONE
        holder.velocityChart.visibility = View.VISIBLE
        holder.accelerationChart.visibility = View.VISIBLE
        holder.distanceChart.visibility = View.VISIBLE

        // Prepare data entries
        val velocityEntries = mutableListOf<Entry>()
        val accelerationEntries = mutableListOf<Entry>()
        val distanceEntries = mutableListOf<Entry>()

        for (point in timeSeries) {
            val timeSeconds = point.timeMs / 1000f
            velocityEntries.add(Entry(timeSeconds, point.velocity))
            accelerationEntries.add(Entry(timeSeconds, point.acceleration))
            distanceEntries.add(Entry(timeSeconds, point.distance))
        }

        // Setup velocity chart
        setupChart(
            holder.velocityChart,
            velocityEntries,
            "Velocity (m/s)",
            Color.rgb(0, 150, 255)
        )

        // Setup acceleration chart
        setupChart(
            holder.accelerationChart,
            accelerationEntries,
            "Acceleration (m/s²)",
            Color.rgb(255, 100, 0)
        )

        // Setup distance chart
        setupChart(
            holder.distanceChart,
            distanceEntries,
            "Distance (m)",
            Color.rgb(0, 200, 100)
        )
    }

    private fun setupChart(chart: LineChart, entries: List<Entry>, label: String, color: Int) {
        val dataSet = LineDataSet(entries, label)
        dataSet.color = color
        dataSet.lineWidth = 2f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(dataSet)
        chart.data = lineData

        // Customize chart appearance
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)

        // X axis (time)
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 0.1f
        xAxis.textColor = Color.BLACK

        // Y axis
        chart.axisLeft.textColor = Color.BLACK
        chart.axisLeft.setDrawGridLines(true)
        chart.axisRight.isEnabled = false

        chart.invalidate() // refresh
    }
}