package com.example.ems

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var welcomeText: TextView
    private lateinit var loadingText: TextView
    private var progress = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        welcomeText = findViewById(R.id.welcomeText)
        loadingText = findViewById(R.id.loadingText)

        // Fade-in animation for splash elements
        val fadeInDuration = 1000L
        findViewById<android.widget.ImageView>(R.id.logo).alpha = 0f
        welcomeText.alpha = 0f
        progressBar.alpha = 0f
        progressText.alpha = 0f
        loadingText.alpha = 0f
        findViewById<android.widget.ImageView>(R.id.logo).animate().alpha(1f).setDuration(fadeInDuration).start()
        welcomeText.animate().alpha(1f).setDuration(fadeInDuration).setStartDelay(300).start()
        progressBar.animate().alpha(1f).setDuration(fadeInDuration).setStartDelay(600).start()
        progressText.animate().alpha(1f).setDuration(fadeInDuration).setStartDelay(900).start()
        loadingText.animate().alpha(1f).setDuration(fadeInDuration).setStartDelay(1200).start()

        startLoading()
    }

    private fun startLoading() {
        val loadingSteps = listOf(
            "Loading user data...",
            "Initializing modules...",
            "Syncing with server...",
            "Preparing interface...",
            "Almost done..."
        )
        Thread {
            while (progress < 100) {
                progress++
                handler.post {
                    progressBar.progress = progress
                    progressText.text = "$progress%"
                    // Update loading text at intervals
                    val step = when {
                        progress < 20 -> 0
                        progress < 40 -> 1
                        progress < 60 -> 2
                        progress < 80 -> 3
                        else -> 4
                    }
                    loadingText.text = loadingSteps[step]
                }
                Thread.sleep(30) // Adjust speed here
            }

            // Start MainActivity after loading
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }.start()
    }
}
