package com.downloaddemo.fetch.fetchmain.util

import android.content.Context
import android.content.Intent
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_CANCEL
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_CANCEL_ALL
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_DELETE
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_DELETE_ALL
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_INVALID
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_PAUSE
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_PAUSE_ALL
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_RESUME
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_RESUME_ALL
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_RETRY
import com.downloaddemo.fetch.fetchmain.ACTION_TYPE_RETRY_ALL
import com.downloaddemo.fetch.fetchmain.DOWNLOAD_ID_INVALID
import com.downloaddemo.fetch.fetchmain.DownloadNotification
import com.downloaddemo.fetch.fetchmain.EXTRA_ACTION_TYPE
import com.downloaddemo.fetch.fetchmain.EXTRA_DOWNLOAD_ID
import com.downloaddemo.fetch.fetchmain.EXTRA_DOWNLOAD_NOTIFICATIONS
import com.downloaddemo.fetch.fetchmain.EXTRA_GROUP_ACTION
import com.downloaddemo.fetch.fetchmain.EXTRA_NAMESPACE
import com.downloaddemo.fetch.fetchmain.EXTRA_NOTIFICATION_GROUP_ID
import com.downloaddemo.fetch.fetchmain.EXTRA_NOTIFICATION_ID
import com.downloaddemo.fetch.fetchmain.FetchNotificationManager
import com.downloaddemo.fetch.fetchmain.NOTIFICATION_GROUP_ID_INVALID
import com.downloaddemo.fetch.fetchmain.NOTIFICATION_ID_INVALID

fun onDownloadNotificationActionTriggered(
    context: Context?, intent: Intent?, fetchNotificationManager: FetchNotificationManager
) {
    if (context != null && intent != null) {
        val namespace = intent.getStringExtra(EXTRA_NAMESPACE)
        val downloadId = intent.getIntExtra(EXTRA_DOWNLOAD_ID, DOWNLOAD_ID_INVALID)
        val actionType = intent.getIntExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_INVALID)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_INVALID)
        val notificationGroupId =
            intent.getIntExtra(EXTRA_NOTIFICATION_GROUP_ID, NOTIFICATION_GROUP_ID_INVALID)
        val isGroupAction = intent.getBooleanExtra(EXTRA_GROUP_ACTION, false)
        val downloadNotifications = intent.getParcelableArrayListExtra(EXTRA_DOWNLOAD_NOTIFICATIONS)
            ?: emptyList<DownloadNotification>()
        if (!isGroupAction) {
            if (!namespace.isNullOrEmpty() && downloadId != DOWNLOAD_ID_INVALID && actionType != ACTION_TYPE_INVALID) {
                val fetch = fetchNotificationManager.getFetchInstanceForNamespace(namespace)
                if (!fetch.isClosed) {
                    when (actionType) {
                        ACTION_TYPE_CANCEL -> fetch.cancel(downloadId)
                        ACTION_TYPE_DELETE -> fetch.delete(downloadId)
                        ACTION_TYPE_PAUSE -> fetch.pause(downloadId)
                        ACTION_TYPE_RESUME -> fetch.resume(downloadId)
                        ACTION_TYPE_RETRY -> fetch.retry(downloadId)
                        else -> {
                            //do nothing
                        }
                    }
                }
            }
        } else {
            if (notificationGroupId != NOTIFICATION_GROUP_ID_INVALID && downloadNotifications.isNotEmpty()) {
                downloadNotifications.groupBy { it.namespace }.forEach { entry ->
                    val fetchNamespace = entry.key
                    val downloadIds = entry.value.map { downloadNotification ->
                        downloadNotification.notificationId
                    }
                    val fetch =
                        fetchNotificationManager.getFetchInstanceForNamespace(fetchNamespace)
                    if (!fetch.isClosed) {
                        when (actionType) {
                            ACTION_TYPE_CANCEL_ALL -> fetch.cancel(downloadIds)
                            ACTION_TYPE_DELETE_ALL -> fetch.delete(downloadIds)
                            ACTION_TYPE_PAUSE_ALL -> fetch.pause(downloadIds)
                            ACTION_TYPE_RESUME_ALL -> fetch.resume(downloadIds)
                            ACTION_TYPE_RETRY_ALL -> fetch.retry(downloadIds)
                            else -> {
                                //do nothing
                            }
                        }
                    }
                }
            }
        }
    }
}