package com.example.shake2wakefinalproject

import android.app.AlertDialog
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import android.media.MediaPlayer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AlarmRingingActivity : AppCompatActivity(), SensorEventListener {

    private var solution: Int = 0
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 2.5f // Adjusted for realistic shake detection
    private var lastUpdate: Long = 0
    private var mathSolved = false

    private lateinit var mediaPlayer: MediaPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_ringing)

        // Initialize the accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Get the math problem and solution from the intent
        val problem = intent.getStringExtra("MATH_PROBLEM") ?: ""
        solution = intent.getIntExtra("MATH_SOLUTION", 0)

        val ringingMessage: TextView = findViewById(R.id.ringingMessage)
        ringingMessage.text = "Solve: $problem"

        // Initialize and start playing alarm sound
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound) // Make sure alarm_sound.mp3 is in res/raw
        mediaPlayer.isLooping = true
        mediaPlayer.start()

        // Show the math problem dialog immediately
        showMathDialog(problem)
    }

    private fun showMathDialog(problem: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inputField = android.widget.EditText(this)
        inputField.hint = "Enter your answer"

        dialogBuilder.setTitle("Solve the problem")
            .setMessage("What is $problem?")
            .setView(inputField)
            .setPositiveButton("Submit") { _, _ ->
                val userAnswer = inputField.text.toString().toIntOrNull()

                if (userAnswer == solution) {
                    mathSolved = true
                    val ringingMessage: TextView = findViewById(R.id.ringingMessage)
                    ringingMessage.text = "Correct! Now shake your phone!"
                } else {
                    Toast.makeText(this, "Wrong answer! Try again.", Toast.LENGTH_SHORT).show()
                    showMathDialog(problem) // Show the dialog again
                }
            }
            .setCancelable(false) // Prevent user from dismissing the dialog
            .create()
            .show()
    }

    private fun stopAlarm() {

        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        sensorManager.unregisterListener(this) // Unregister the accelerometer listener
        finish() // Close the activity when the alarm is stopped
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && mathSolved) {
            val values = event.values
            val x = values[0]
            val y = values[1]
            val z = values[2]

            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > 100) {
                val diffTime = currentTime - lastUpdate
                lastUpdate = currentTime

                // Gravity compensation and shake detection
                val gX = x / SensorManager.GRAVITY_EARTH
                val gY = y / SensorManager.GRAVITY_EARTH
                val gZ = z / SensorManager.GRAVITY_EARTH
                val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

                if (gForce > shakeThreshold) {
                    Toast.makeText(this, "Shake detected! Alarm stopped!", Toast.LENGTH_SHORT).show()
                    stopAlarm()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
