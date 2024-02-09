package com.downloaddemo.room_database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FilesDao {

    @Query("Select * FROM videos")
            fun getFilesList(): List<FilesDownloading>?

    @Query("Select * FROM videos WHERE fileId = :id")
            fun getFile(id: String): FilesDownloading?

    @Insert
           fun insertFile(video: FilesDownloading): Long

    @Query("UPDATE videos SET downloadPrg = :downloadPrg WHERE fileId = :id")
            fun updateFilePrg(downloadPrg: Int, id: String): Int

    @Query("UPDATE videos SET downloadPrg = :downloadPrg WHERE fileId = :id")
    suspend fun updateFilePrgInBg(downloadPrg: Int, id: String): Int

    @Query("UPDATE videos SET downloadId = :downloadId WHERE fileId = :id")
           fun updateDownloadId(downloadId: Int, id: String): Int

    @Query("DELETE FROM videos WHERE fileId = :id")
           fun deleteFile(id: String): Int

    @Query("DELETE FROM videos WHERE fileId IN (:ids)")
    fun deleteAll(ids: List<String>)

}