package com.echoesapp.audiobooks.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner ad component for Echoes.
 * Uses Google's test ad unit ID for development.
 * 
 * TODO: Replace with production ad unit ID before release:
 * - Create ad unit in AdMob console
 * - Replace TEST_BANNER_AD_UNIT_ID with real ID
 */

// Test ad unit ID - shows test ads that are safe to click
private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_BANNER_AD_UNIT_ID,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // Reload ad if needed (e.g., on configuration change)
            // adView.loadAd(AdRequest.Builder().build())
        },
    )
}

/**
 * Adaptive banner that adjusts to screen width.
 * Better for different device sizes.
 */
@Composable
fun AdaptiveAdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_BANNER_AD_UNIT_ID,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                // Get adaptive banner size based on screen width
                val displayMetrics = context.resources.displayMetrics
                val adWidthPixels = displayMetrics.widthPixels.toFloat()
                val density = displayMetrics.density
                val adWidth = (adWidthPixels / density).toInt()
                
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth))
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
