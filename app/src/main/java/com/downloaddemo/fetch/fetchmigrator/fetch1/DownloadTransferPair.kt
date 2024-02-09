package com.downloaddemo.fetch.fetchmigrator.fetch1

import com.downloaddemo.fetch.fetchmain.Download

data class DownloadTransferPair(val newDownload: Download, val oldID: Long)