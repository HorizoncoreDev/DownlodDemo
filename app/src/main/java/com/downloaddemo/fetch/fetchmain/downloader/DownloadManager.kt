package com.downloaddemo.fetch.fetchmain.downloader

import com.downloaddemo.fetch.fetchmain.Download
import java.io.Closeable

interface DownloadManager : Closeable {

    val isClosed: Boolean
    var concurrentLimit: Int

    fun start(download: Download): Boolean
    fun cancel(downloadId: Int): Boolean
    fun cancelAll()
    fun contains(downloadId: Int): Boolean
    fun canAccommodateNewDownload(): Boolean
    fun getActiveDownloadCount(): Int
    fun getActiveDownloads(): List<Download>
    fun getActiveDownloadsIds(): List<Int>
    fun getNewFileDownloaderForDownload(download: Download): FileDownloader?
    fun getFileDownloaderDelegate(): FileDownloader.Delegate
    fun getDownloadFileTempDir(download: Download): String

}