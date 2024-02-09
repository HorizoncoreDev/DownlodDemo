package com.downloaddemo.fetch.fetchmain.helper

import com.downloaddemo.fetch.fetchmain.database.DownloadInfo
import com.downloaddemo.fetch.fetchmain.database.FetchDatabaseManagerWrapper


class DownloadInfoUpdater(private val fetchDatabaseManagerWrapper: FetchDatabaseManagerWrapper) {

    fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo) {
        fetchDatabaseManagerWrapper.updateFileBytesInfoAndStatusOnly(downloadInfo)
    }

    fun update(downloadInfo: DownloadInfo) {
        fetchDatabaseManagerWrapper.update(downloadInfo)
    }

    fun getNewDownloadInfoInstance(): DownloadInfo {
        return fetchDatabaseManagerWrapper.getNewDownloadInfoInstance()
    }

}