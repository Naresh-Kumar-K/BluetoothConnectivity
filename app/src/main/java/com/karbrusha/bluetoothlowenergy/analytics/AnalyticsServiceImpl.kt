package com.karbrusha.bluetoothlowenergy.analytics

import android.util.Log
import javax.inject.Inject

class AnalyticsServiceImpl @Inject constructor() : AnalyticsService {
    override fun logEvent(event: String) {
        Log.d("Analytics", "Event: $event")
    }
}
