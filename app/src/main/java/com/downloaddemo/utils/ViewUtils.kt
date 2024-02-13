package com.downloaddemo.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import java.util.Formatter
import java.util.Locale
import java.util.concurrent.TimeUnit


/**
 * this function is used to check device version if it ie above 12
 * */
fun isAboveAndroid12(): Boolean {
    return Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2
}

/**
 * this function is to get time in milliseconds
 * */
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

/**
 * this function will convert h,m&s to milliseconds
 * */
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

/**
 * this function is used to get mime type of file with giving URL
 * */
fun getMimeType(url: String?): String? {
    var type: String? = null
    val extension = MimeTypeMap.getFileExtensionFromUrl(url)
    if (extension != null) {
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    return type
}

/**
 * this function will return duration of media file for given URL
 * */
fun getMediaDuration(mContext: Context, url: String): String {
    val uri = Uri.parse(url)
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