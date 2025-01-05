package com.example.typemaster6000

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlin.math.roundToInt
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.os.Handler
import android.os.Looper
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.formatter.ValueFormatter
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var textToType: TextView
    private lateinit var typingInput: EditText
    private lateinit var wpmLabel: TextView
    private lateinit var tryAgainButton: Button
    private lateinit var typingProgress: LinearProgressIndicator
    private lateinit var timeLabel: TextView
    private lateinit var accuracyLabel: TextView
    
    private var startTime: Long = 0
    private var isTyping = false
    
    private val sampleTexts = listOf(
        "The quick brown fox jumps over the lazy dog.",
        "Pack my box with five dozen liquor jugs.",
        "How vexingly quick daft zebras jump!",
        "The five boxing wizards jump quickly."
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundHandler = Handler(Looper.getMainLooper())

    private var statsUpdateHandler = Handler(Looper.getMainLooper())
    private var statsUpdateRunnable = object : Runnable {
        override fun run() {
            if (isTyping) {
                updateStats()
                calculateWPM(typingInput.text.toString())
                statsUpdateHandler.postDelayed(this, 2000)
            }
        }
    }

    private val performanceData = mutableListOf<Pair<Long, Int>>() // time, wpm

    private var completionDialog: AlertDialog? = null

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d("MainActivity", "Starting onCreate")
            
            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences("TypeMaster6000Prefs", Context.MODE_PRIVATE)
            Log.d("MainActivity", "Initialized SharedPreferences")
            
            // Apply saved theme
            val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
            AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            Log.d("MainActivity", "Applied theme")
            
            setContentView(R.layout.activity_main)
            Log.d("MainActivity", "Set content view")

            // Initialize each view with error checking
            try {
                textToType = findViewById(R.id.textToType) ?: throw Exception("textToType not found")
                typingInput = findViewById(R.id.typingInput) ?: throw Exception("typingInput not found")
                wpmLabel = findViewById(R.id.wpm_label) ?: throw Exception("wpmLabel not found")
                tryAgainButton = findViewById(R.id.tryAgainButton) ?: throw Exception("tryAgainButton not found")
                typingProgress = findViewById(R.id.typingProgress) ?: throw Exception("typingProgress not found")
                timeLabel = findViewById(R.id.timeLabel) ?: throw Exception("timeLabel not found")
                accuracyLabel = findViewById(R.id.accuracyLabel) ?: throw Exception("accuracyLabel not found")
                Log.d("MainActivity", "All views initialized")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing views", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Set up toolbar
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            // Set welcome message - simplified for guest-only mode
            findViewById<TextView>(R.id.welcomeTextView).text = "Welcome to TypeMaster6000"

            // Initialize typing test
            setupTypingTest()

            // Set up try again button
            tryAgainButton.setOnClickListener {
                setupTypingTest()
            }

            // Add IME action listener for Enter key
            typingInput.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    if (isTyping) {
                        val typedText = typingInput.text.toString()
                        val targetText = textToType.text.toString()
                        if (typedText == targetText) {
                            showCompletionDialog()
                        } else {
                            setupTypingTest()
                        }
                    }
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupTypingTest() {
        // Reset state
        isTyping = false
        startTime = 0
        wpmLabel.text = "WPM: 0"
        
        // Set random text
        textToType.text = sampleTexts.random()
        typingInput.text.clear()
        typingInput.isEnabled = true  // Make sure input is enabled
        
        // Set up typing listener
        typingInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isTyping) {
                    startTime = System.currentTimeMillis()
                    isTyping = true
                    startStatsUpdates()  // Start updating stats
                }
                
                val targetText = textToType.text.toString()
                updateProgress(s?.length ?: 0, targetText.length)
                
                if (isTyping) {
                    calculateWPM(s.toString())
                }
                
                // Check if typing is complete
                if (s.toString() == textToType.text.toString()) {
                    showCompletionAnimation()
                    showCompletionDialog()
                    typingInput.isEnabled = false
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun calculateWPM(typedText: String) {
        val words = typedText.length / 5.0
        val minutes = (System.currentTimeMillis() - startTime) / 60000.0
        val wpm = if (minutes > 0) (words / minutes).roundToInt() else 0
        
        wpmLabel.text = "WPM: $wpm"
        updatePerformanceData(wpm)
    }

    private fun showCompletionDialog() {
        // Dismiss any existing dialog
        completionDialog?.dismiss()
        
        stopStatsUpdates()
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_typing_results, null)
        completionDialog = AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setView(dialogView)
            .create()

        // Set final values
        val finalWpm = calculateFinalWPM()
        val finalAccuracy = calculateAccuracy(typingInput.text.toString(), textToType.text.toString())
        
        dialogView.findViewById<TextView>(R.id.finalWpmValue).text = finalWpm.toString()
        dialogView.findViewById<TextView>(R.id.finalAccuracyValue).text = 
            String.format("%.1f%%", finalAccuracy)

        // Setup chart
        val chart = dialogView.findViewById<LineChart>(R.id.performanceChart)
        setupPerformanceChart(chart)

        // Setup buttons
        dialogView.findViewById<Button>(R.id.tryAgainButton).setOnClickListener {
            completionDialog?.dismiss()
            completionDialog = null
            setupTypingTest()
        }
        
        dialogView.findViewById<Button>(R.id.closeButton).setOnClickListener {
            completionDialog?.dismiss()
            completionDialog = null
        }

        completionDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        completionDialog?.show()
    }

    private fun calculateFinalWPM(): Int {
        val timeElapsed = (System.currentTimeMillis() - startTime) / 1000.0 // in seconds
        val typedText = typingInput.text.toString()
        val words = typedText.length / 5.0
        val minutes = timeElapsed / 60.0
        return (words / minutes).roundToInt()
    }

    // Add typing indicator animation
    private fun animateTypingIndicator() {
        val fadeAnimation = AlphaAnimation(1.0f, 0.0f)
        fadeAnimation.duration = 500
        fadeAnimation.repeatCount = Animation.INFINITE
        fadeAnimation.repeatMode = Animation.REVERSE
        typingInput.startAnimation(fadeAnimation)
    }

    // Add smooth WPM counter animation
    private fun animateWPMChange(newWPM: Int) {
        val currentWPM = wpmLabel.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
        val valueAnimator = ValueAnimator.ofInt(currentWPM, newWPM)
        valueAnimator.duration = 300
        valueAnimator.addUpdateListener { animation ->
            wpmLabel.text = "WPM: ${animation.animatedValue}"
        }
        valueAnimator.start()
    }

    // Add completion celebration animation
    private fun showCompletionAnimation() {
        val scaleUp = ScaleAnimation(
            1f, 1.2f, 1f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleUp.duration = 200
        scaleUp.repeatMode = Animation.REVERSE
        scaleUp.repeatCount = 1
        
        wpmLabel.startAnimation(scaleUp)
    }

    private fun updateProgress(current: Int, total: Int) {
        val progress = ((current.toFloat() / total.toFloat()) * 100).toInt()
        typingProgress.progress = progress
    }

    private fun provideHapticFeedback(isCorrect: Boolean) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                if (isCorrect) 10 else 20,
                if (isCorrect) VibrationEffect.DEFAULT_AMPLITUDE else VibrationEffect.DEFAULT_AMPLITUDE
            ))
        }
    }

    // Update the stats display
    private fun updateStats() {
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
        val minutes = elapsedTime / 60
        val seconds = elapsedTime % 60
        timeLabel.text = String.format("Time: %d:%02d", minutes, seconds)
        
        val typedText = typingInput.text.toString()
        val targetText = textToType.text.toString()
        val accuracy = calculateAccuracy(typedText, targetText)
        accuracyLabel.text = String.format("Accuracy: %.1f%%", accuracy)
    }

    private fun calculateAccuracy(typed: String, target: String): Float {
        if (typed.isEmpty()) return 0f
        var correct = 0
        val length = minOf(typed.length, target.length)
        
        for (i in 0 until length) {
            if (typed[i] == target[i]) correct++
        }
        
        return (correct.toFloat() / target.length.toFloat()) * 100
    }

    private fun startStatsUpdates() {
        statsUpdateHandler.post(statsUpdateRunnable)
    }

    private fun stopStatsUpdates() {
        statsUpdateHandler.removeCallbacks(statsUpdateRunnable)
    }

    private fun setupPerformanceChart(chart: LineChart) {
        val entries = performanceData.mapIndexed { index, pair ->
            Entry(pair.first.toFloat(), pair.second.toFloat())
        }

        val dataSet = LineDataSet(entries, "WPM").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.accent_primary)
            setDrawCircles(true)
            circleRadius = 4f
            circleHoleRadius = 2f
            setCircleColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@MainActivity, R.color.accent_primary)
            fillAlpha = 50
            lineWidth = 2f
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (entries.indexOf(Entry(value, value)) % 2 == 0) {
                        "${value.toInt()}"
                    } else ""
                }
            }
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            isDoubleTapToZoomEnabled = false
            isDragEnabled = true
            setScaleEnabled(true)
            setViewPortOffsets(50f, 20f, 30f, 50f)
            
            // Disable animations completely to prevent flickering
            setNoDataText("")
            animateX(0)  // Set to 0 to disable animation
            
            xAxis.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(context, R.color.text_secondary)
                setGridLineWidth(0.5f)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value.toInt() % 5 == 0) "${value.toInt()}s" else ""
                    }
                }
                setLabelCount(6, true)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.argb(51, 255, 255, 255)
                setGridLineWidth(0.5f)
                textColor = Color.WHITE
                axisMinimum = 0f
                axisMaximum = 200f
                setLabelCount(6, true)
                spaceTop = 15f
            }
            
            axisRight.isEnabled = false
            
            setExtraOffsets(8f, 16f, 8f, 16f)
        }
    }

    // Add this function to update performance data
    private fun updatePerformanceData(wpm: Int) {
        if (wpm in 0..200) {
            val currentTime = (System.currentTimeMillis() - startTime) / 1000
            if (performanceData.isEmpty() || 
                currentTime - (performanceData.lastOrNull()?.first ?: 0) >= 2) {
                performanceData.add(Pair(currentTime, wpm))
                
                if (performanceData.size > 15) {
                    performanceData.removeAt(0)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                backgroundHandler.post {
                    toggleTheme()
                }
                true
            }
            R.id.action_about -> {
                backgroundHandler.post {
                    showAboutDialog()
                }
                true
            }
            R.id.action_exit -> {
                backgroundHandler.post {
                    showExitDialog()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleTheme() {
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        val newMode = !isDarkMode
        
        mainHandler.post {
            sharedPreferences.edit().putBoolean("dark_mode", newMode).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (newMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("About TypeMaster6000")
            .setMessage("TypeMaster6000 is a typing speed test application designed to help users improve their typing skills. " +
                    "Track your WPM, accuracy, and progress over time!\n\n" +
                    "Version 1.0\n" +
                    "© 2024 TypeMaster Team")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Exit Application")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateTypingStats() {
        backgroundHandler.post {
            // Move the calculation to background
            val currentText = typingInput.text.toString()
            val targetText = textToType.text.toString()
            
            // Calculate stats
            val accuracy = calculateAccuracy(currentText, targetText)
            val wpm = calculateWPM(currentText)
            
            // Update UI on main thread
            mainHandler.post {
                accuracyLabel.text = "${accuracy}%"
                wpmLabel.text = "WPM: $wpm"
                // Update progress indicator
                typingProgress.progress = ((currentText.length.toFloat() / targetText.length) * 100).toInt()
            }
        }
    }
} 
