package com.downloaddemo.room_database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * this table used for files storage in Room db
 * */
@Entity(tableName = "files")
data class FilesDownloading(
    @PrimaryKey(autoGenerate = true) val vId: Int,
    @ColumnInfo(name = "downloadId") val downloadId: String,
    @ColumnInfo(name = "fileId") val fileId: String,
    @ColumnInfo(name = "fileName") val fileName: String,
    @ColumnInfo(name = "downloadStatus") val downloadStatus: String,
    @ColumnInfo(name = "downloadPrg") var downloadPrg: Int,
    @ColumnInfo(name = "downloadDate") val downloadDate: String
)
