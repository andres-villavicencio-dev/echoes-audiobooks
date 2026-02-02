package com.echoesapp.audiobooks

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class EchoesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Mobile Ads SDK on background thread
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@EchoesApplication)
        }
    }
}
