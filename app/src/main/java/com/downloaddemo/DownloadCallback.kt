package com.downloaddemo

/**
 * this interface is used to do further process after receiving notification click
 * */
interface DownloadCallback {
    fun onDownloadPaused(position:Int)
    fun onDownloadResumed(position:Int)
    fun onDownloadCanceled(position:Int)
}
