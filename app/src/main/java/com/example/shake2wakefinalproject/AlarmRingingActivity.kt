package com.example.shake2wakefinalproject

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.media.MediaPlayer

class AlarmRingingActivity : AppCompatActivity(), SensorEventListener {

    private var solution: Int = 0
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 2.5f
    private var lastUpdate: Long = 0
    private var mathSolved = false
    private var shakeDetected = false

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionIntent: Intent
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_ringing)

        //Data Extraction: Initialize accelerometer and speech recognizer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }

        //Initialize accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        //Initialize speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
        setupSpeechRecognitionListener()

        //Preprocessing: Retrieve math problem and display it
        val problem = intent.getStringExtra("MATH_PROBLEM") ?: ""
        solution = intent.getIntExtra("MATH_SOLUTION", 0)

        val ringingMessage: TextView = findViewById(R.id.ringingMessage)
        ringingMessage.text = "Solve: $problem"

        // Initialize and start playing alarm sound
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound) // Make sure alarm_sound.mp3 is in res/raw
        mediaPlayer.isLooping = true
        mediaPlayer.start()


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
                    accelerometer?.let {
                        //Data Extraction: Register accelerometer listener

                        sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                    }
                } else {
                    Toast.makeText(this, "Wrong answer! Try again.", Toast.LENGTH_SHORT).show()
                    showMathDialog(problem) // Show the dialog again
                }
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun startListening() {
        //Pause alarm sound while listening - fix from demo
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }

        // Data Extraction: Start listening for audio input
        speechRecognizer.startListening(recognitionIntent)
    }

    private fun setupSpeechRecognitionListener() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(this@AlarmRingingActivity, "Listening for sound...", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Toast.makeText(this@AlarmRingingActivity, "Sound detected! Alarm stopped.", Toast.LENGTH_SHORT).show()
                    stopAlarm()
                } else {
                    Toast.makeText(this@AlarmRingingActivity, "No sound detected. Try again.", Toast.LENGTH_SHORT).show()
                    // Resume alarm sound if no match is found - fix from demo
                    if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                        mediaPlayer.start()
                    }
                }
            }


            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error"
                }
                Toast.makeText(this@AlarmRingingActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
    }

    private fun stopAlarm() {
        if (::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }

        // Cleanup: Unregister listeners and release resources
        sensorManager.unregisterListener(this)
        speechRecognizer.destroy()
        finish()
    }


    override fun onSensorChanged(event: SensorEvent?) {
        // Data Extraction: Capture accelerometer readings
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && mathSolved) {
            val values = event.values
            val x = values[0]
            val y = values[1]
            val z = values[2]

            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > 100) {
                val diffTime = currentTime - lastUpdate
                lastUpdate = currentTime
                // Feature Extraction: Calculate g-force
                val gX = x / SensorManager.GRAVITY_EARTH
                val gY = y / SensorManager.GRAVITY_EARTH
                val gZ = z / SensorManager.GRAVITY_EARTH
                val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
                //Classification: Detect shake based on g-force threshold
                if (gForce > shakeThreshold) {
                    shakeDetected = true
                    val ringingMessage: TextView = findViewById(R.id.ringingMessage)
                    ringingMessage.text = "Shake detected! Now speak into the mic!"
                    sensorManager.unregisterListener(this)
                    startListening()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission denied. App functionality may be limited.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        speechRecognizer.destroy()
    }
}
