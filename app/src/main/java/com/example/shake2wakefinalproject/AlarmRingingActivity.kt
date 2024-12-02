package com.example.shake2wakefinalproject

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AlarmRingingActivity : AppCompatActivity() {

    private var solution: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_ringing)

        // Get the math problem and solution from the intent
        val problem = intent.getStringExtra("MATH_PROBLEM") ?: ""
        solution = intent.getIntExtra("MATH_SOLUTION", 0)

        val ringingMessage: TextView = findViewById(R.id.ringingMessage)
        ringingMessage.text = "Solve: $problem"

        val stopAlarmButton: Button = findViewById(R.id.stopAlarmButton)
        stopAlarmButton.setOnClickListener {
            showMathDialog()
        }
    }

    private fun showMathDialog() {
        val inputField = EditText(this)
        inputField.hint = "Enter your answer"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Solve the problem to stop the alarm")
            .setView(inputField)
            .setPositiveButton("Submit") { _, _ ->
                val userAnswer = inputField.text.toString().toIntOrNull()

                if (userAnswer == solution) {
                    stopAlarm()
                    Toast.makeText(this, "Correct! Alarm stopped!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Wrong answer! Try again.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "Alarm is still active!", Toast.LENGTH_SHORT).show()
            }
            .create()

        dialog.show()
    }

    private fun stopAlarm() {
        finish() // Close the activity when the alarm is stopped
    }
}
