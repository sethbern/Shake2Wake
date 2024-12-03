package com.example.shake2wakefinalproject

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 15.0f
    private var lastUpdate: Long = 0

    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private var isAlarmSet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val setAlarmButton: Button = findViewById(R.id.setAlarmButton)

        //Data Extraction: Initialize sensor and set up alarm functionality
        setAlarmButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                alarmHour = selectedHour
                alarmMinute = selectedMinute
                isAlarmSet = true
                Toast.makeText(this, "Alarm set for $selectedHour:$selectedMinute", Toast.LENGTH_SHORT).show()

                //Start monitoring alarm time
                monitorAlarmTime()
            }, hour, minute, true)

            timePickerDialog.show()
        }
    }

    private fun monitorAlarmTime() {
        Thread {
            while (isAlarmSet) {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                // Preprocessing: Compare current time to alarm time
                if (currentHour == alarmHour && currentMinute == alarmMinute) {
                    runOnUiThread {
                        val intent = Intent(this, AlarmRingingActivity::class.java)
                        val (problem, solution) = generateMathProblem()
                        intent.putExtra("MATH_PROBLEM", problem)
                        intent.putExtra("MATH_SOLUTION", solution)
                        startActivity(intent)
                    }
                    isAlarmSet = false
                    break
                }

                Thread.sleep(1000) //Preprocessing: Polling frequency for alarm check
            }
        }.start()
    }

    private fun generateMathProblem(): Pair<String, Int> {
        // Feature Extraction: Generate math problem to present as a challenge
        val num1 = (1..10).random()
        val num2 = (1..10).random()
        val operation = listOf("+", "-").random()

        val problem = "$num1 $operation $num2"
        val solution = when (operation) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            else -> 0
        }

        return Pair(problem, solution)
    }

    override fun onSensorChanged(event: SensorEvent?) {}

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
