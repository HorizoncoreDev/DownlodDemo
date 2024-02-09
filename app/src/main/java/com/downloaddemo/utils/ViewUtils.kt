package com.downloaddemo.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import java.util.Formatter
import java.util.Locale
import java.util.concurrent.TimeUnit

fun isAboveAndroid12(): Boolean {
    return Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2
}

fun getTimeFromMillis(timeMs: Int): String {
    try {
        if (timeMs == 0) {
            return "00:00"
        }
        val mFormatBuilder = StringBuilder()
        val mFormatter = Formatter(mFormatBuilder, Locale.getDefault())
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        mFormatBuilder.setLength(0)
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    } catch (e: Exception) {
        return "00:00"
    }
}

fun convertMillieToHMmSs(millie: Long): String? {
    val seconds = millie / 1000
    val second = seconds % 60
    val minute = seconds / 60 % 60
    val hour = seconds / (60 * 60) % 24
    val result = ""
    return if (hour > 0) {
        String.format("%02d:%02d:%02d", hour, minute, second)
    } else {
        String.format("%02d:%02d", minute, second)
    }
}

fun getMediaDuration(mContext: Context, uri: String): String {
    val uri = Uri.parse(uri)
    var minutes = ""
    var seconds = ""

    MediaPlayer.create(mContext, uri).also {
        val millis = it.duration.toLong()
        minutes = TimeUnit.MILLISECONDS.toMinutes(millis).toString()
        seconds = TimeUnit.MILLISECONDS.toSeconds(millis).toString()

        it.reset()
        it.release()
    }

    return "$minutes:$seconds"
}