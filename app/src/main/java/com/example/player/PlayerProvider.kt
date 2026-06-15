package com.example.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object PlayerProvider {
    @Volatile
    private var playerHandler: MusicPlayerHandler? = null

    fun getPlayerHandler(context: Context): MusicPlayerHandler {
        return playerHandler ?: synchronized(this) {
            val current = playerHandler
            if (current != null) {
                current
            } else {
                val instance = MusicPlayerHandler(context.applicationContext)
                playerHandler = instance
                instance
            }
        }
    }

    fun getPlayer(context: Context): ExoPlayer {
        return getPlayerHandler(context).exoPlayer
    }
}
