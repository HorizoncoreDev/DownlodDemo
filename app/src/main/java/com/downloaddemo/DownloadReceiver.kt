package com.downloaddemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * this class is used to do to receive/detect notification click
 * */
class DownloadReceiver(private val callback: DownloadCallback) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        val position = intent?.getIntExtra("position",0)

        when (action) {
            "PAUSE_DOWNLOAD_ACTION" -> {
                callback.onDownloadPaused(position!!)
            }
            "RESUME_DOWNLOAD_ACTION" -> {
                callback.onDownloadResumed(position!!)
            }
            "CANCEL_DOWNLOAD_ACTION" -> {
                callback.onDownloadCanceled(position!!)
            }
        }
    }
}
