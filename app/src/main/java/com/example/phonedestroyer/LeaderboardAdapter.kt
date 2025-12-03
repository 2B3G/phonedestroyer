import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.phonedestroyer.R
import com.example.phonedestroyer.fragments.LeaderboardEntry

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    private var entries = listOf<LeaderboardEntry>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardView)
        val position: TextView = view.findViewById(R.id.tvPosition)
        val playerName: TextView = view.findViewById(R.id.tvPlayerName)
        val score: TextView = view.findViewById(R.id.tvScore)
        val medal: TextView = view.findViewById(R.id.tvMedal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        holder.playerName.text = entry.playerName
        holder.score.text = "${entry.score} pkt"

        // special styling for top 3
        when (entry.position) {
            1 -> {
                holder.medal.visibility = View.VISIBLE
                holder.medal.text = "🥇"
                holder.position.visibility = View.GONE
                holder.card.setCardBackgroundColor(Color.parseColor("#FFF9E6"))
            }
            2 -> {
                holder.medal.visibility = View.VISIBLE
                holder.medal.text = "🥈"
                holder.position.visibility = View.GONE
                holder.card.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
            }
            3 -> {
                holder.medal.visibility = View.VISIBLE
                holder.medal.text = "🥉"
                holder.position.visibility = View.GONE
                holder.card.setCardBackgroundColor(Color.parseColor("#FFF5E6"))
            }
            else -> {
                holder.medal.visibility = View.GONE
                holder.position.visibility = View.VISIBLE
                holder.position.text = "${entry.position}"
                holder.card.setCardBackgroundColor(Color.WHITE)
            }
        }
    }

    override fun getItemCount() = entries.size

    fun submitList(newEntries: List<LeaderboardEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}