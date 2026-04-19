package com.example.lordflix

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class LordFlixPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(LordFlixProvider())
    }
}
