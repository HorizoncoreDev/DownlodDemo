package com.downloaddemo.utils

import android.annotation.SuppressLint
import android.content.Context
import com.downloaddemo.fetch.fetchmain.Fetch
import com.downloaddemo.fetch.fetchmain.Request
import com.downloaddemo.room_database.AppDatabase

/**
 * this class have all the constants method or variable required
 * */
class AppConstants {
    companion object {
        var appDatabase: AppDatabase? = null
        fun getAppDatabase(context: Context): AppDatabase {
            if (appDatabase == null) {
                appDatabase = AppDatabase.DatabaseBuilder.getInstance(context)
            }
            return appDatabase as AppDatabase
        }

        const val File_DOWNLOADING = "file_downloading"
        const val File_DOWNLOAD_PAUSED = "file_download_paused"

    }
}