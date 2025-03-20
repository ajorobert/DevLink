package com.klinetix.devlink

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

/**
 * A simple activity that shows a brief popup with instructions
 * and automatically dismisses after a few seconds
 */
class DialogActivity : Activity() {
    
    companion object {
        const val EXTRA_MESSAGE = "extra_message"
        private const val AUTO_DISMISS_DELAY = 4000L // 4 seconds
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple TextView
        val textView = TextView(this)
        textView.setPadding(50, 30, 50, 30)
        textView.gravity = Gravity.CENTER
        textView.textSize = 18f
        
        // Get message from intent
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Please tap on Wireless debugging"
        textView.text = message
        
        // Set the TextView as the content view
        setContentView(textView)
        
        // Make the window floating and small
        window.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window.setGravity(Gravity.CENTER)
        
        // Make the background semi-transparent
        window.setDimAmount(0.0f)
        
        // Auto-dismiss after delay
        handler.postDelayed({ finish() }, AUTO_DISMISS_DELAY)
    }
    
    override fun onBackPressed() {
        // Just dismiss the activity
        finish()
    }
} 