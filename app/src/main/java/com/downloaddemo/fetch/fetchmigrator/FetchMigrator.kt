@file:JvmName("FetchMigrator")

package com.downloaddemo.fetch.fetchmigrator

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.annotation.WorkerThread
import com.downloaddemo.fetch.fetchcore.DefaultStorageResolver
import com.downloaddemo.fetch.fetchcore.FetchLogger
import com.downloaddemo.fetch.fetchcore.getFileTempDir
import com.downloaddemo.fetch.fetchmain.database.DownloadDatabase
import com.downloaddemo.fetch.fetchmain.database.DownloadInfo
import com.downloaddemo.fetch.fetchmain.database.FetchDatabaseManagerImpl
import com.downloaddemo.fetch.fetchmain.database.FetchDatabaseManagerWrapper
import com.downloaddemo.fetch.fetchmain.fetch.LiveSettings
import com.downloaddemo.fetch.fetchmigrator.fetch1.DatabaseHelper
import com.downloaddemo.fetch.fetchmigrator.fetch1.DownloadTransferPair
import com.downloaddemo.fetch.fetchmigrator.helpers.v1CursorToV2DownloadInfo
import java.sql.SQLException

/** Migrates Downloads from Fetch version 1 to Fetch version 2. Note that the ids
 * of the transferred downloads will be different in version 2. See the list DownloadTransferPair
 * returned by this method. Note that this method must be called on a background thread or else
 * an SQLiteException error will be thrown.
 *
 * @param context context
 * @param v2Namespace Fetch 2 namespace that the downloads from version 1 will be transferred to.
 *
 * @return list of transferred downloads(DownloadTransferPair). Each download transfer pair includes the newDownload
 *          object used in Fetch version 2. downloadTransferPair.getNewDownload().getId() is
 *          the new id for the download in Fetch version 2. downloadTransferPair.getOldID()
 *          is the id for the download in Fetch version 1. Update your external references
 *          accordingly.
 *
 * @throws SQLException - If there is an issue opening or getting data from a database.
 * @throws SQLiteConstraintException - If a download in the v2 namespace already has a matching id. This
 * error will most likely be thrown when trying to insert a download from v1 that was already transferred to
 * v2.
 * */
@WorkerThread
@Throws(exceptionClasses = [SQLException::class, SQLiteConstraintException::class])
fun migrateFromV1toV2(
    context: Context, v2Namespace: String, fetchDatabaseManagerWrapper: FetchDatabaseManagerWrapper
): List<DownloadTransferPair> {
    val fetchOneDatabaseHelper = DatabaseHelper(context)
    fetchOneDatabaseHelper.clean()
    fetchOneDatabaseHelper.verifyOK()
    val downloadInfoList = mutableListOf<DownloadTransferPair>()
    val liveSettings = LiveSettings(v2Namespace)
    val cursor = fetchOneDatabaseHelper.get()
    if (cursor != null) {
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val downloadTransferPair = v1CursorToV2DownloadInfo(cursor, fetchDatabaseManagerWrapper)
            (downloadTransferPair.newDownload as DownloadInfo).namespace = v2Namespace
            downloadInfoList.add(downloadTransferPair)
            cursor.moveToNext()
        }
        cursor.close()
        val fetchTwoDatabaseManager = FetchDatabaseManagerImpl(
            context = context,
            namespace = v2Namespace,
            logger = FetchLogger(),
            migrations = DownloadDatabase.getMigrations(),
            liveSettings = liveSettings,
            fileExistChecksEnabled = false,
            defaultStorageResolver = DefaultStorageResolver(context, getFileTempDir(context))
        )
        fetchTwoDatabaseManager.insert(downloadInfoList.map { it.newDownload as DownloadInfo })
        fetchTwoDatabaseManager.close()
    }
    fetchOneDatabaseHelper.close()
    return downloadInfoList
}

/** Deletes the database for Fetch version 1
 * @param context
 * */
fun deleteFetchV1Database(context: Context) {
    context.deleteDatabase(DatabaseHelper.DB_NAME)
}

