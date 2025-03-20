package com.klinetix.devlink

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity() {
    
    private val wifiConnectionTimeoutMs = 30000L // 30 seconds timeout for WiFi connection
    private val checkIntervalMs = 1000L // Check connection status every second
    
    private val wifiManager by lazy { 
        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    private val connectivityManager by lazy {
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var elapsedTime = 0L
    private val isWifiConnected = AtomicBoolean(false)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the main process
        processWifiAndDebugging()
    }
    
    private fun processWifiAndDebugging() {
        // Check if WiFi is enabled
        if (!wifiManager.isWifiEnabled) {
            // Enable WiFi
            wifiManager.isWifiEnabled = true
            // Wait for WiFi to be enabled and connected
            startConnectionMonitoring()
            waitForWifiConnection()
        } else if (!isWifiConnected()) {
            // WiFi is already enabled but not connected
            startConnectionMonitoring()
            checkWifiConnection()
        } else {
            // WiFi is already enabled and connected
            // Open developer options and show guidance
            openDeveloperOptions()
        }
    }
    
    private fun startConnectionMonitoring() {
        // For Android 11+, use the new NetworkCallback API for monitoring
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isWifiConnected.set(true)
                handler.post { 
                    openDeveloperOptions()
                }
            }
            
            override fun onLost(network: Network) {
                isWifiConnected.set(false)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }
    
    private fun stopConnectionMonitoring() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // Ignore errors during unregister
            }
        }
    }
    
    private fun isWifiConnected(): Boolean {
        // First check our callback flag
        if (isWifiConnected.get()) return true
        
        // Fallback to traditional check
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    private fun waitForWifiConnection() {
        elapsedTime = 0L
        
        val runnable = object : Runnable {
            override fun run() {
                if (wifiManager.isWifiEnabled) {
                    checkWifiConnection()
                } else {
                    elapsedTime += checkIntervalMs
                    if (elapsedTime >= wifiConnectionTimeoutMs) {
                        // Timeout occurred, show error and exit
                        stopConnectionMonitoring()
                        showError("Failed to enable WiFi within the timeout period")
                        finish()
                    } else {
                        // Check again after interval
                        handler.postDelayed(this, checkIntervalMs)
                    }
                }
            }
        }
        
        // Start checking
        handler.post(runnable)
    }
    
    private fun checkWifiConnection() {
        elapsedTime = 0L
        
        val runnable = object : Runnable {
            override fun run() {
                // Check if a WiFi network is connected
                if (isWifiConnected()) {
                    // WiFi is connected, open wireless debugging settings
                    stopConnectionMonitoring()
                    openDeveloperOptions()
                } else {
                    elapsedTime += checkIntervalMs
                    if (elapsedTime >= wifiConnectionTimeoutMs) {
                        // Timeout occurred, show error and exit
                        stopConnectionMonitoring()
                        showError("Failed to connect to WiFi within the timeout period")
                        finish()
                    } else {
                        // Check again after interval
                        handler.postDelayed(this, checkIntervalMs)
                    }
                }
            }
        }
        
        // Start checking
        handler.post(runnable)
    }
    
    private fun openDeveloperOptions() {
        try {
            // Focus on directly opening wireless debugging on Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Use a more comprehensive approach to target the wireless debugging screen directly
                // Try multiple methods in sequence until one works
                
                // Method 1: Try the standard intent action (most devices)
                val intent1 = Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS")
                
                // Method 2: Try different spelling/capitalization variations
                val intent2 = Intent("android.settings.WIRELESS_ADB_SETTINGS")
                
                // Method 3: Try direct component name (specific to some devices)
                val intent3 = Intent()
                intent3.setClassName("com.android.settings", 
                    "com.android.settings.Settings\$DevelopmentSettingsWirelessDebuggingActivity")
                
                // Method 4: Try a different component path
                val intent4 = Intent()
                intent4.setClassName("com.android.settings", 
                    "com.android.settings.development.WirelessDebuggingFragment")
                
                // Method 5: Try content URI approach
                val intent5 = Intent(Intent.ACTION_VIEW)
                intent5.setData(android.net.Uri.parse("android:settings:development:wireless_debugging"))
                
                // Method 6: Try a package manager approach with fragment specification
                val intent6 = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                intent6.putExtra(":settings:show_fragment", "com.android.settings.development.WirelessDebuggingFragment")
                
                // Try each intent in order, keeping track of whether we succeeded
                var navigated = false
                
                // List of intents to try
                val intents = arrayOf(intent1, intent2, intent3, intent4, intent5, intent6)
                
                for (intent in intents) {
                    try {
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                            navigated = true
                            android.util.Log.i("DevLink", "Successfully navigated using intent: ${intent}")
                            break
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DevLink", "Failed to navigate with intent: ${e.message}")
                    }
                }
                
                // If all direct methods failed, fall back to developer options with a "tap on this" popup
                if (!navigated) {
                    // Fallback: Open developer options
                    val developerOptionsIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    startActivity(developerOptionsIntent)
                    
                    // Show a minimal dialog explaining what to tap on
                    startDialogActivity("Scroll down and tap on \"Wireless debugging\"")
                }
            } else {
                // For older Android versions, just open developer options
                val developerOptionsIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                startActivity(developerOptionsIntent)
                
                // Show a minimal message for older versions
                startDialogActivity("Wireless debugging requires Android 11+")
            }
            
            // Finish this activity immediately
            finish()
        } catch (e: Exception) {
            showError("Failed to open settings: ${e.message}")
            finish()
        }
    }
    
    private fun startDialogActivity(message: String) {
        // Start DialogActivity to show the guidance
        val intent = Intent(this, DialogActivity::class.java)
        intent.putExtra(DialogActivity.EXTRA_MESSAGE, message)
        startActivity(intent)
    }
    
    private fun showError(message: String) {
        // Show the error message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Log the error for debugging
        android.util.Log.e("DevLink", "Error: $message")
    }
    
    override fun onDestroy() {
        stopConnectionMonitoring()
        super.onDestroy()
    }
} 