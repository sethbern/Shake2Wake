package com.example.shake2wakefinalproject
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 12.0f
    private var lastUpdate: Long = 0

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find buttons
        val setAlarmButton: Button = findViewById(R.id.setAlarmButton)
        val startAlarmButton: Button = findViewById(R.id.startAlarmButton)
        val stopAlarmButton: Button = findViewById(R.id.stopAlarmButton)

        // Button listeners
        setAlarmButton.setOnClickListener {
            Toast.makeText(this, "Set Alarm button clicked!", Toast.LENGTH_SHORT).show()
        }

        startAlarmButton.setOnClickListener {
            speechRecognizer.startListening(recognitionIntent)
        }

        stopAlarmButton.setOnClickListener {
            Toast.makeText(this, "Stop Alarm button clicked!", Toast.LENGTH_SHORT).show()
        }

        // Initialize accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Initialize speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(this@MainActivity, "Listening...", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.get(0) ?: ""
                checkMathAnswer(spokenText)
            }

            override fun onError(error: Int) {
                Toast.makeText(this@MainActivity, "Error recognizing speech!", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
    }

    private fun checkMathAnswer(spokenText: String) {
        val correctAnswer = 42 // Example answer
        if (spokenText.trim() == correctAnswer.toString()) {
            Toast.makeText(this, "Correct! Alarm disabled.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Wrong answer. Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val values = event.values
            val x = values[0]
            val y = values[1]
            val z = values[2]

            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > 100) {
                val diffTime = currentTime - lastUpdate
                lastUpdate = currentTime

                val speed = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat() / diffTime * 10000

                if (speed > shakeThreshold) {
                    Toast.makeText(this, "Shake detected!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
