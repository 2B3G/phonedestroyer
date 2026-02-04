package com.example.phonedestroyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.math.roundToInt

data class Animal(val name: String, val speed: Float, val imageRes: Int)

class AnimalScaleFragment : Fragment() {

    private lateinit var lowerAnimalImage: ImageView
    private lateinit var lowerAnimalName: TextView
    private lateinit var middleAnimalSpeed: TextView
    private lateinit var higherAnimalImage: ImageView
    private lateinit var higherAnimalName: TextView

    private val animals = listOf(
        Animal("Ślimak", 0.03f, R.drawable.snail),
        Animal("Żółw", 0.27f, R.drawable.turtle),
        Animal("Królik", 5f, R.drawable.rabbit),
        Animal("Pies", 10f, R.drawable.dog),
        Animal("Koń", 20f, R.drawable.horse),
        Animal("Gepard", 29f, R.drawable.cheetah),
        Animal("Sokół", 39f, R.drawable.falcon)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_animal_scale, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // find views
        lowerAnimalImage = view.findViewById(R.id.lowerAnimalImage)
        lowerAnimalName = view.findViewById(R.id.lowerAnimalName)
        middleAnimalSpeed = view.findViewById(R.id.middleAnimalSpeed)
        higherAnimalImage = view.findViewById(R.id.higherAnimalImage)
        higherAnimalName = view.findViewById(R.id.higherAnimalName)

        // get the speed value passed to the fragment
        val speed = arguments?.getFloat("speed") ?: 0f

        // find closest animals
        val sorted = animals.sortedBy { it.speed }
        val lower = sorted.lastOrNull { it.speed <= speed } ?: sorted.first()
        val higher = sorted.firstOrNull { it.speed >= speed } ?: sorted.last()

        // set views
        lowerAnimalImage.setImageResource(lower.imageRes)
        lowerAnimalName.text = lower.name

        middleAnimalSpeed.text = "${(speed * 3.6).roundToInt()} km/h"

        higherAnimalImage.setImageResource(higher.imageRes)
        higherAnimalName.text = higher.name
    }

    companion object {
        fun newInstance(speed: Float) = AnimalScaleFragment().apply {
            arguments = Bundle().apply {
                putFloat("speed", speed)
            }
        }
    }
}
