package com.downloaddemo.utils

import android.annotation.SuppressLint
import android.content.Context
import com.downloaddemo.fetch.fetchmain.Fetch
import com.downloaddemo.fetch.fetchmain.Request
import com.downloaddemo.room_database.AppDatabase

class AppConstants {

    companion object {

        @JvmField
        var isLiveVideo = false

        var fetch: Fetch? = null
        var appDatabase: AppDatabase? = null

        @SuppressLint("StaticFieldLeak")
        var requestList = mutableListOf<Request>()
        var downloadingIdList: java.util.ArrayList<String>? = java.util.ArrayList()

        fun getAppDatabase(context: Context): AppDatabase {
            if (appDatabase == null) {
//                synchronized(Object()) {
                appDatabase = AppDatabase.DatabaseBuilder.getInstance(context)
//                }
            }
            return appDatabase as AppDatabase
        }

        const val MATERIAL_DOWNLOADING = "m_downloading"
        const val IS_FOR_MY_DOWNLOADS = "is_for_my_downLoads"


    }
}