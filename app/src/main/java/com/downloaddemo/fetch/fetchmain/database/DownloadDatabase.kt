package com.downloaddemo.fetch.fetchmain.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.downloaddemo.fetch.fetchmain.EnqueueAction
import com.downloaddemo.fetch.fetchmain.database.DownloadDatabase.Companion.DATABASE_VERSION
import com.downloaddemo.fetch.fetchmain.util.DEFAULT_AUTO_RETRY_ATTEMPTS
import com.downloaddemo.fetch.fetchmain.util.DEFAULT_UNIQUE_IDENTIFIER
import com.downloaddemo.fetch.fetchmain.util.EMPTY_JSON_OBJECT_STRING

@Database(entities = [DownloadInfo::class], version = DATABASE_VERSION, exportSchema = false)
@TypeConverters(value = [Converter::class])
abstract class DownloadDatabase : RoomDatabase() {

    abstract fun requestDao(): DownloadDao

    fun wasRowInserted(row: Long): Boolean {
        return row != (-1).toLong()
    }

    companion object {
        const val TABLE_NAME = "requests"
        const val COLUMN_ID = "_id"
        const val COLUMN_NAMESPACE = "_namespace"
        const val COLUMN_URL = "_url"
        const val COLUMN_FILE = "_file"
        const val COLUMN_GROUP = "_group"
        const val COLUMN_PRIORITY = "_priority"
        const val COLUMN_HEADERS = "_headers"
        const val COLUMN_DOWNLOADED = "_written_bytes"
        const val COLUMN_TOTAL = "_total_bytes"
        const val COLUMN_STATUS = "_status"
        const val COLUMN_ERROR = "_error"
        const val COLUMN_NETWORK_TYPE = "_network_type"
        const val COLUMN_CREATED = "_created"
        const val COLUMN_TAG = "_tag"
        const val COLUMN_ENQUEUE_ACTION = "_enqueue_action"
        const val COLUMN_IDENTIFIER = "_identifier"
        const val COLUMN_DOWNLOAD_ON_ENQUEUE = "_download_on_enqueue"
        const val COLUMN_EXTRAS = "_extras"
        const val COLUMN_AUTO_RETRY_MAX_ATTEMPTS = "_auto_retry_max_attempts"
        const val COLUMN_AUTO_RETRY_ATTEMPTS = "_auto_retry_attempts"
        const val OLD_DATABASE_VERSION = 6
        const val DATABASE_VERSION = 7

        val migrationOneToTwo = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE '$TABLE_NAME' " + "ADD COLUMN '$COLUMN_TAG' TEXT NULL DEFAULT NULL"
                )
            }
        }

        val migrationTwoToThree = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE '${TABLE_NAME}' " + "ADD COLUMN '${COLUMN_ENQUEUE_ACTION}' INTEGER NOT NULL DEFAULT ${EnqueueAction.REPLACE_EXISTING.value}"
                )
            }
        }

        val migrationThreeToFour = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE '${TABLE_NAME}' " + "ADD COLUMN '${COLUMN_IDENTIFIER}' INTEGER NOT NULL DEFAULT $DEFAULT_UNIQUE_IDENTIFIER"
                )
            }
        }

        val migrationFourToFive = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE '${TABLE_NAME}' " + "ADD COLUMN '${COLUMN_DOWNLOAD_ON_ENQUEUE}' INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val migrationFiveToSix = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE '${TABLE_NAME}' " + "ADD COLUMN '${COLUMN_EXTRAS}' TEXT NOT NULL DEFAULT '$EMPTY_JSON_OBJECT_STRING'"
                )
            }
        }

        val migrationSixToSeven = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE '${TABLE_NAME}' " + "ADD COLUMN '${COLUMN_AUTO_RETRY_MAX_ATTEMPTS}' INTEGER NOT NULL DEFAULT '$DEFAULT_AUTO_RETRY_ATTEMPTS'"
                )
                database.execSQL(
                    "ALTER TABLE '${TABLE_NAME}' " + "ADD COLUMN '${COLUMN_AUTO_RETRY_ATTEMPTS}' INTEGER NOT NULL DEFAULT '$DEFAULT_AUTO_RETRY_ATTEMPTS'"
                )
            }
        }

        @JvmStatic
        fun getMigrations(): Array<Migration> {
            return arrayOf(
                migrationOneToTwo,
                migrationTwoToThree,
                migrationThreeToFour,
                migrationFourToFive,
                migrationFiveToSix,
                migrationSixToSeven
            )
        }

    }

}