package com.downloaddemo.room_database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.downloaddemo.fetch.fetchmain.Status

/**
 * this interface used for querying data from Room DB
 * */
@Dao
interface FilesDao {

    /**
     * this query will fetch list of download files
     * */
    @Query("Select * FROM files")
            fun getFilesList(): List<FilesDownloading>?

    /**
     * this query will fetch particular file according to the given fileId
     * */
    @Query("Select * FROM files WHERE fileId = :id")
            fun getFile(id: String): FilesDownloading?

    /**
     * this query will insert file or new record
     * */
    @Insert
           fun insertFile(video: FilesDownloading): Long

    /**
     * this query will update download progress of particular file according to the given fileId
     * */
    @Query("UPDATE files SET downloadPrg = :downloadPrg WHERE fileId = :id")
            fun updateFilePrg(downloadPrg: Int, id: String): Int


    /**
     * this query will update download status of particular file according to the given fileId
     * */
    @Query("UPDATE files SET downloadStatus = :downloadStatus WHERE fileId = :id")
            fun updateFileDownloadStatus(downloadStatus: String, id: String): Int


    /**
     * this query will delete file according to the given fileId
     * */
    @Query("DELETE FROM files WHERE fileId = :id")
           fun deleteFile(id: String): Int

    /**
     * this query will delete all files from table
     * */
    @Query("DELETE FROM files WHERE fileId IN (:ids)")
    fun deleteAll(ids: List<String>)

}