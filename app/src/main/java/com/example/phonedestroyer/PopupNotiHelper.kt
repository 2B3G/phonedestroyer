package com.example.phonedestroyer

import android.content.Context
import android.graphics.Color
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.core.graphics.toColorInt

class PopupNotiHelper(val rootView: View) {
    enum class Severity {
        SUCCESS,
        WARNING,
        ERROR
    }

    fun showMessage(message: String, severity: Severity) {
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)

        val color = when (severity) {
            Severity.SUCCESS -> "#4CAF50"
            Severity.WARNING -> "#FFC107"
            Severity.ERROR -> "#D32F2F"
        }

        snackbar.setBackgroundTint(color.toColorInt())
            .setTextColor(Color.WHITE).show()
    }
}