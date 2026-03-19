package com.connect.medium

import android.app.Application
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.MemoryCategory
import com.bumptech.glide.load.engine.cache.LruResourceCache

class ConnectApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ExoPlayer.Builder(this)
        val glideBuilder = GlideBuilder()
        glideBuilder.setMemoryCache(LruResourceCache(10 * 1024 * 1024)) // 10MB max
        Glide.get(this).apply{
            setMemoryCategory(MemoryCategory.LOW)
        }
    }
}