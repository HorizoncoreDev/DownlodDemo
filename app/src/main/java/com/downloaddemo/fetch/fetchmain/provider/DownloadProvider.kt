package com.downloaddemo.fetch.fetchmain.provider

import com.downloaddemo.fetch.fetchmain.Download
import com.downloaddemo.fetch.fetchmain.PrioritySort
import com.downloaddemo.fetch.fetchmain.Status
import com.downloaddemo.fetch.fetchmain.database.FetchDatabaseManagerWrapper


class DownloadProvider(private val fetchDatabaseManagerWrapper: FetchDatabaseManagerWrapper) {

    fun getDownloads(): List<Download> {
        return fetchDatabaseManagerWrapper.get()
    }

    fun getDownload(id: Int): Download? {
        return fetchDatabaseManagerWrapper.get(id)
    }

    fun getDownloads(ids: List<Int>): List<Download?> {
        return fetchDatabaseManagerWrapper.get(ids)
    }

    fun getByGroup(group: Int): List<Download> {
        return fetchDatabaseManagerWrapper.getByGroup(group)
    }

    fun getByGroupReplace(group: Int, download: Download): List<Download> {
        val downloads = getByGroup(group) as ArrayList
        val index = downloads.indexOfFirst { it.id == download.id }
        if (index != -1) {
            downloads[index] = download
        }
        return downloads
    }

    fun getByStatus(status: Status): List<Download> {
        return fetchDatabaseManagerWrapper.getByStatus(status)
    }

    fun getPendingDownloadsSorted(prioritySort: PrioritySort): List<Download> {
        return fetchDatabaseManagerWrapper.getPendingDownloadsSorted(prioritySort)
    }

}