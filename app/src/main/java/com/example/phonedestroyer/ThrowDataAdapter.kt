import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.phonedestroyer.R
import com.example.phonedestroyer.fragments.ThrowData

class ThrowDataAdapter : RecyclerView.Adapter<ThrowDataAdapter.ViewHolder>() {

    private var throws = listOf<ThrowData>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val distance: TextView = view.findViewById(R.id.tvDistance)
        val acceleration: TextView = view.findViewById(R.id.tvAcceleration)
        val speed: TextView = view.findViewById(R.id.tvSpeed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.throw_data_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val throwData = throws[position]
        holder.distance.text = throwData.distance
        holder.acceleration.text = throwData.acceleration
        holder.speed.text = throwData.speed
    }

    override fun getItemCount() = throws.size

    fun submitList(newThrows: List<ThrowData>) {
        throws = newThrows
        notifyDataSetChanged()
    }
}