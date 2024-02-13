package com.downloaddemo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.downloaddemo.databinding.MultipleDownloadBinding
import com.downloaddemo.fetch.fetchcore.Downloader
import com.downloaddemo.fetch.fetchcore.FetchObserver
import com.downloaddemo.fetch.fetchcore.Reason
import com.downloaddemo.fetch.fetchmain.AbstractFetchListener
import com.downloaddemo.fetch.fetchmain.Download
import com.downloaddemo.fetch.fetchmain.Error
import com.downloaddemo.fetch.fetchmain.Fetch
import com.downloaddemo.fetch.fetchmain.FetchConfiguration
import com.downloaddemo.fetch.fetchmain.FetchListener
import com.downloaddemo.fetch.fetchmain.Request
import com.downloaddemo.fetch.fetchokhttp.OkHttpDownloader
import com.downloaddemo.room_database.AppDatabase
import com.downloaddemo.room_database.FilesDownloading
import com.downloaddemo.utils.AppConstants
import com.downloaddemo.utils.Coroutines
import com.downloaddemo.utils.getMimeType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * this class will handle multiple download with play, pause, resume, cancel functionality
 * and also it handles all this feature in background with notification that shows progress
 * and allows to pause,resume & cancel download from notification itself.
 * */
class MultipleDownloadActivity : AppCompatActivity(), FetchObserver<Download>, DownloadCallback {

    private lateinit var binding: MultipleDownloadBinding
    var fetch: Fetch? = null
    private var requestList: ArrayList<Request> = arrayListOf()
    private var downloadList: Array<DownloadData> = arrayOf()
    private val TAG = "DOWNLOAD_DEMO"
    private lateinit var multipleDownloadAdapter: MultipleDownloadAdapter
    private var downloadPosition = 0
    private val appDatabase: AppDatabase by lazy {
        AppConstants.getAppDatabase(this)
    }
    private var fileNameWithPath = ""

    private val downloadReceiver = DownloadReceiver(this)
    private val fetchListener: FetchListener = object : AbstractFetchListener() {
        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            Log.e(TAG, "onError: " + error.toString() + "....." + throwable.toString())
        }

        override fun onDeleted(download: Download) {
            Log.e(TAG, "onDeleted: " + download.toString())
        }
    }

    private val readStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE

    private val reqStoragePermission = arrayOf(readStoragePermission/*, writeStoragePermission*/)

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value
            }
            if (granted) {
                enqueueDownload()
            } else {
                // show custom alert
                //Previously Permission Request was cancelled with 'Don't Ask Again',
                // Redirect to Settings after showing Information about why you need the permission
                showSettingsDialog()
            }
        }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkAndGetPermission()
        }

    /**
     * This method will create intent filter for handling notification actions
     * Also it will register receiver
     * */
    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter().apply {
            addAction("PAUSE_DOWNLOAD_ACTION")
            addAction("RESUME_DOWNLOAD_ACTION")
            addAction("CANCEL_DOWNLOAD_ACTION")
        }
        registerReceiver(downloadReceiver, intentFilter)
    }

    /**
     * this method will set recyclerview with list
     * also will create instance for fetch
     * */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MultipleDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fetchConfiguration =
            FetchConfiguration.Builder(applicationContext).setDownloadConcurrentLimit(1)
                .setHttpDownloader(OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL)).build()
        if (fetch == null) {

            fetch = Fetch.getInstance(fetchConfiguration).addListener(fetchListener)
            downloadList = Data.sampleDownloadList
            for (i in downloadList) {
                val fileNameWithPath =
                    this.externalCacheDir!!.path + "/Videos/" + Data.getNameFromUrl(i.downloadUrl)
                requestList.add(Request(i.downloadUrl, fileNameWithPath, 0))
            }

            (binding.rvMultipleDownload.itemAnimator as SimpleItemAnimator).supportsChangeAnimations =
                false

            val downloadedFileList: List<FilesDownloading>? = appDatabase.filesDao().getFilesList()
            for (i in downloadList.indices) {
                for (j in downloadedFileList!!.indices) {
                    if (downloadList[i].downloadId == downloadedFileList[j].fileId) {
                        downloadList[i].downloadProgress = downloadedFileList[j].downloadPrg
                        val file = appDatabase.filesDao().getFile(downloadList[i].downloadId)
                        if (file != null) {
                            fileNameWithPath =
                                this.externalCacheDir!!.path + "/Videos/" + Data.getNameFromUrl(
                                    downloadList[i].downloadUrl
                                )


                            when (file.downloadPrg) {
                                in 0..99 -> {
                                    downloadList[i].downloading = true
                                    fetch!!.attachFetchObserversForDownload(
                                        requestList[i].id,
                                        this@MultipleDownloadActivity
                                    )
                                        .enqueue(requestList[i], { result: Request ->
                                            requestList[i] = result
                                        }) {
                                            Log.e(TAG, "enqueueDownload:Error $it")
                                        }
                                    if (file.downloadStatus == AppConstants.File_DOWNLOAD_PAUSED) {
                                        fetch!!.pause(requestList[i].id)
                                        downloadList[i].paused = true
                                    }
                                }

                                100 -> {
                                    downloadList[i].downloading = false
                                }

                                else -> {
                                }
                            }
                        }

                    }
                }
            }

            multipleDownloadAdapter = MultipleDownloadAdapter(
                context = applicationContext,
                downloadList = downloadList,
                fetch = fetch!!,
                appDatabase = appDatabase,
                requestList = requestList,
                onPermissionNotGranted = {
                    downloadPosition = it
                    checkAndGetPermission()
                },
                onStartDownloadClicked = {
                    downloadPosition = it
                    enqueueDownload()
                },
                onCancelDownloadClicked = {
                    deleteFileFromStorage(it)
                    fetch!!.delete(requestList[it].id)
                },
                onPauseDownloadClicked = {
                    fetch!!.pause(requestList[it].id)
                },
                onResumeDownloadClicked = {
                    fetch!!.resume(requestList[it].id)
                })
            binding.rvMultipleDownload.adapter = multipleDownloadAdapter

            val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
            binding.rvMultipleDownload.layoutManager = layoutManager

        }
    }

    /**
     * this method will be called on click of pause from notification
     * */
    override fun onDownloadPaused(position: Int) {
        fetch!!.pause(requestList[position].id)
        downloadList[position].paused = true
        multipleDownloadAdapter.notifyItemChanged(position)
    }

    /**
     * this method will be called on click of resume from notification
     * */
    override fun onDownloadResumed(position: Int) {
        fetch!!.resume(requestList[position].id)
        downloadList[position].paused = false
        multipleDownloadAdapter.notifyItemChanged(position)
    }

    /**
     * this method will be called on click of cancel from notification
     * */
    override fun onDownloadCanceled(position: Int) {
        deleteFileFromStorage(position)
        fetch!!.delete(requestList[position].id)
        downloadList[position].cancelled = true
        downloadList[position].paused = false
        downloadList[position].downloading = true
        multipleDownloadAdapter.notifyItemChanged(position)
        generateNotification(position,false)
    }

    /**
     * this method will delete particular file from storage in case of cancel download or download error
     * */
    private fun deleteFileFromStorage(index: Int) {
        appDatabase.filesDao().deleteFile(downloadList[index].downloadId)
        val videoNameWithPath = requestList[index].file
        val fileNew = File(videoNameWithPath)
        if (fileNew.exists()) {
            fileNew.delete()
        }
    }

    /**
     * this method will ask to open setting page to grant the permission
     * */
    private fun showSettingsDialog() {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_permission_title))
        builder.setMessage(getString(R.string.dialog_storage_permission_message))
        builder.setPositiveButton(getString(R.string.go_to_settings)) { dialog, which ->
            dialog.cancel()
            openSettings()
        }
        builder.setNegativeButton(getString(android.R.string.cancel)) { dialog, which ->
            dialog.cancel()
        }
        if (!isFinishing) {
            builder.show()
        }
    }

    /**
     * this method will open the settings page of device if permission is denied multiple times
     * */
    private fun openSettings() {
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.parse("package:" + applicationContext.packageName)
        settingsIntent.data = uri
        settingsLauncher.launch(settingsIntent)
    }

    /**
     * this method is used to ask for the permission if not granted or it will start download in case of granted
     * */
    private fun checkAndGetPermission() {
        if (checkHasPermissions()) {
            enqueueDownload()
        } else {
            storagePermissionLauncher.launch(reqStoragePermission)
        }
    }

    /**
     * this method will check and return boolean according to the permission status
     * */
    private fun checkHasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, readStoragePermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * this method is start the download and it also register observer for download request
     * */
    private fun enqueueDownload() {

        //   request!!.extras = getExtrasForRequest(request!!)
        setDataInRoom(downloadPosition)
        fetch!!.attachFetchObserversForDownload(
            requestList[downloadPosition].id,
            this@MultipleDownloadActivity
        )
            .enqueue(requestList[downloadPosition], { result: Request ->
                requestList[downloadPosition] = result
                /*  binding.startDownload.visibility = View.GONE
                  binding.pauseDownload.visibility = View.VISIBLE
                  binding.linearProgress.visibility = View.VISIBLE
                  binding.circularProgress.visibility = View.VISIBLE
                  binding.tvProgress.visibility = View.VISIBLE*/
            }) {
                Log.e(TAG, "enqueueDownload:Error $it")
            }

    }

    /**
     * this method is used to set data in room, when download starts it will insert data in local db if not already exist
     * */
    private fun setDataInRoom(index: Int) {
        val file = appDatabase.filesDao().getFile(downloadList[downloadPosition].downloadId)
        if (file == null) {
            Log.e(TAG, "setDataInRoom: File Not found")
            val c = Calendar.getInstance().time
            // exp: Wed, Feb 02, 2023
            val df = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
            val formattedDate: String = df.format(c)

            val fileDownloading = FilesDownloading(
                vId = 0,
                downloadId = requestList[index].id.toString(),
                fileId = downloadList[downloadPosition].downloadId,
                fileName = Data.getNameFromUrl(downloadList[downloadPosition].downloadUrl)!!,
                downloadStatus = AppConstants.File_DOWNLOADING,
                downloadPrg = 0,
                downloadDate = formattedDate
            )

            Coroutines.main {
                val x: Long? = appDatabase.filesDao().insertFile(fileDownloading)
                Log.e("VideoPlayAdapter", "insertVideo: $x")
            }
        }

    }

    /**
     * this method will call when download gets started, it will return download progress,
     * it will also return state of the download request
     * so according to the download data it will update adapter
     * */
    override fun onChanged(data: Download, reason: Reason) {
        Log.e(TAG, "onChanged: " + data.id + "..progress.." + data.progress)
        for (i in requestList.indices) {
            if (requestList[i].id == data.id) {
                multipleDownloadAdapter.updateDownloadProgress(data, reason, i)
                if (downloadList[i].downloading) {
                    generateNotification(i, false)
                }
                if (reason == Reason.DOWNLOAD_ERROR) {
                    downloadList[i].downloading = false
                    downloadList[i].downloadError = true
                    // resetDownloadTimer()
                    Toast.makeText(this, "Error while downloading", Toast.LENGTH_SHORT).show()
                    generateNotification(i, true)
                }
            }
        }
    }

    /**
     * this method is used to check if there is any space available in mobile to download
     * */
    private fun generateNotification(position: Int, downloadError: Boolean) {
        val intent: Intent
        val fileNameWithPath =
            externalCacheDir!!.path + "/Videos/" + Data.getNameFromUrl(downloadList[position].downloadUrl)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(fileNameWithPath)
                type = getMimeType(downloadList[position].downloadUrl)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(fileNameWithPath)
                type = getMimeType(downloadList[position].downloadUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // Push logic
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(applicationContext, "CHANNEL_ID")
        builder.setSmallIcon(R.drawable.fetch_notification_pause)
            .setContentText(Data.getNameFromUrl(Data.sampleDownloadList[position].downloadUrl))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHANNEL_ID", "Channel 1",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ""
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        val pauseRequestCode = "0${position}"
        val pauseIntent = Intent("PAUSE_DOWNLOAD_ACTION")
            .putExtra("position", position)
        val pausePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            pauseRequestCode.toInt(),
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeRequestCode = "1${position}"
        val resumeIntent = Intent("RESUME_DOWNLOAD_ACTION").putExtra("position", position)
        val resumePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            resumeRequestCode.toInt(),
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelRequestCode = "2${position}"
        val cancelIntent = Intent("CANCEL_DOWNLOAD_ACTION").putExtra("position", position)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            cancelRequestCode.toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (downloadList[position].paused) {
            builder.setContentTitle("Download Paused")
            builder.setSmallIcon(R.drawable.fetch_notification_resume)
            builder.setProgress(100, downloadList[position].downloadProgress, false)
            builder.addAction(R.drawable.fetch_notification_pause, "Resume", resumePendingIntent)
            builder.addAction(
                com.google.android.material.R.drawable.mtrl_ic_error,
                "Cancel",
                cancelPendingIntent
            )
        } else if (downloadList[position].cancelled) {
            builder.setContentTitle("Download Cancelled")
            builder.setSmallIcon(com.google.android.material.R.drawable.mtrl_ic_error)
            builder.setProgress(0, 0, false)
        } else if (downloadError) {
            builder.setContentTitle("Error occurred while downloading")
            builder.setSmallIcon(com.google.android.material.R.drawable.mtrl_ic_error)
            builder.setProgress(0, 0, false)
        } else if (downloadList[position].downloadProgress < 100) {
            builder.setContentTitle("Downloading in progress")
            builder.setProgress(100, downloadList[position].downloadProgress, false)
            builder.addAction(R.drawable.fetch_notification_resume, "Pause", pausePendingIntent)
            builder.addAction(
                com.google.android.material.R.drawable.mtrl_ic_error,
                "Cancel",
                cancelPendingIntent
            )
        } else {
            builder.setContentTitle("Download Complete")
            builder.setSmallIcon(com.google.android.material.R.drawable.ic_m3_chip_check)
            builder.setProgress(0, 0, false)
        }
        NotificationManagerCompat.from(applicationContext)
            .notify(downloadList[position].downloadId.toInt(), builder.build())
    }

}