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
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.downloaddemo.databinding.SingleDownloadBinding
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
import com.downloaddemo.fetch.fetchmain.Status
import com.downloaddemo.fetch.fetchokhttp.OkHttpDownloader
import com.downloaddemo.room_database.AppDatabase
import com.downloaddemo.room_database.FilesDownloading
import com.downloaddemo.utils.AppConstants
import com.downloaddemo.utils.Coroutines
import com.downloaddemo.utils.getMimeType
import com.downloaddemo.utils.isAboveAndroid12
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * this class will handle single download with play, pause, resume, cancel functionality
 * and also it handles all this feature in background with notification that shows progress
 * and allows to pause,resume & cancel download from notification itself.
 * */
class SingleDownloadActivity : AppCompatActivity(), FetchObserver<Download>, DownloadCallback {


    private lateinit var binding: SingleDownloadBinding
    var fetch: Fetch? = null
    private var request: Request? = null
    private val TAG = "DOWNLOAD_DEMO"
    lateinit var url: String
    private var isPause = false
    private var isCancel = false
    private var fileNameWithPath = ""

    private val downloadReceiver = DownloadReceiver(this)

    private val appDatabase: AppDatabase by lazy {
        AppConstants.getAppDatabase(this)
    }

    private val fetchListener: FetchListener = object : AbstractFetchListener() {
        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            Log.e(TAG, "onError: " + error.toString() + "....." + throwable.toString())
        }

        override fun onDeleted(download: Download) {
            Log.e(TAG, "onDeleted: " + download.toString())
        }
    }


    private val readStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE

    //    private val writeStoragePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
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
     * this method will handle click listeners of start, pause, resume & cancel
     * also will create instance for fetch
     * */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SingleDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fetchConfiguration =
            FetchConfiguration.Builder(applicationContext)
                .setDownloadConcurrentLimit(1)
                .setHttpDownloader(OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL)).build()
        if (fetch == null) {

            fetch = Fetch.getInstance(fetchConfiguration).addListener(fetchListener)

            url = Data.sampleUrls[2]
            binding.tvUrl.text = url

            val file = appDatabase.filesDao().getFile("101")
            if (file != null) {
                fileNameWithPath =
                    this.externalCacheDir!!.path + "/Videos/" + Data.getNameFromUrl(url)

                request = Request(url, fileNameWithPath, 0)

                Log.e(TAG, "setDataInRoom: " + file.downloadPrg)
                when (file.downloadPrg) {
                    in 0..99 -> {
                        binding.startDownload.visibility = View.GONE
                        binding.pauseDownload.visibility = View.VISIBLE
                        binding.cancelDownload.visibility = View.VISIBLE
                        binding.linearProgress.visibility = View.VISIBLE
                        binding.circularProgress.visibility = View.VISIBLE
                        binding.tvProgress.visibility = View.VISIBLE
                        fetch!!.attachFetchObserversForDownload(
                            request!!.id,
                            this@SingleDownloadActivity
                        )
                            .enqueue(request!!, { result: Request ->
                                request = result
                            }) {
                                Log.e(TAG, "enqueueDownload:Error " + it.toString())
                            }
                        if (file.downloadStatus == AppConstants.File_DOWNLOAD_PAUSED) {
                            fetch!!.pause(request!!.id)
                            isPause = true
                            binding.pauseDownload.visibility = View.GONE
                            binding.resumeDownload.visibility = View.VISIBLE
                        }
                    }

                    100 -> {
                        binding.startDownload.text = "Downloaded"
                        binding.cancelDownload.visibility = View.GONE
                        binding.pauseDownload.visibility = View.GONE
                        binding.startDownload.visibility = View.VISIBLE
                        binding.linearProgress.visibility = View.GONE
                        binding.circularProgress.visibility = View.GONE
                        binding.tvProgress.visibility = View.GONE
                        binding.startDownload.isClickable = false
                    }

                    else -> {
                    }
                }
            }

            binding.startDownload.setOnClickListener {
                if (!isAboveAndroid12()) {
                    val readPermission = ContextCompat.checkSelfPermission(
                        applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    if (readPermission != PackageManager.PERMISSION_GRANTED) {
                        checkAndGetPermission()
                    } else {
                        enqueueDownload()
                    }
                }
            }

            binding.pauseDownload.setOnClickListener {
                appDatabase.filesDao()
                    .updateFileDownloadStatus(AppConstants.File_DOWNLOAD_PAUSED, "101")
                isPause = true
                isCancel = false
                binding.pauseDownload.visibility = View.GONE
                binding.resumeDownload.visibility = View.VISIBLE
                fetch!!.pause(request!!.id)
            }

            binding.resumeDownload.setOnClickListener {
                appDatabase.filesDao()
                    .updateFileDownloadStatus(AppConstants.File_DOWNLOADING, "101")
                isPause = false
                isCancel = false
                binding.resumeDownload.visibility = View.GONE
                binding.pauseDownload.visibility = View.VISIBLE
                fetch!!.resume(request!!.id)
            }

            binding.cancelDownload.setOnClickListener {
                isPause = false
                isCancel = true
                binding.cancelDownload.visibility = View.GONE
                binding.resumeDownload.visibility = View.GONE
                binding.pauseDownload.visibility = View.GONE
                binding.linearProgress.visibility = View.GONE
                binding.circularProgress.visibility = View.GONE
                binding.tvProgress.visibility = View.GONE
                binding.startDownload.visibility = View.VISIBLE
                binding.startDownload.text = "Start"
                binding.linearProgress.progress = 0
                binding.circularProgress.progress = 0
                binding.tvProgress.text = ""
                deleteFileFromStorage()
                fetch!!.delete(request!!.id)
            }

        }

    }

    /**
     * this method will be called on click of pause from notification
     * */
    override fun onDownloadPaused(position: Int) {
        appDatabase.filesDao().updateFileDownloadStatus(AppConstants.File_DOWNLOAD_PAUSED, "101")
        isPause = true
        isCancel = false
        binding.pauseDownload.visibility = View.GONE
        binding.resumeDownload.visibility = View.VISIBLE
        fetch!!.pause(request!!.id)
    }

    /**
     * this method will be called on click of resume from notification
     * */
    override fun onDownloadResumed(position: Int) {
        appDatabase.filesDao().updateFileDownloadStatus(AppConstants.File_DOWNLOADING, "101")
        isPause = false
        isCancel = false
        binding.resumeDownload.visibility = View.GONE
        binding.pauseDownload.visibility = View.VISIBLE
        fetch!!.resume(request!!.id)
    }

    /**
     * this method will be called on click of cancel from notification
     * */
    override fun onDownloadCanceled(position: Int) {
        isPause = false
        isCancel = true
        binding.cancelDownload.visibility = View.GONE
        binding.resumeDownload.visibility = View.GONE
        binding.pauseDownload.visibility = View.GONE
        binding.linearProgress.visibility = View.GONE
        binding.circularProgress.visibility = View.GONE
        binding.tvProgress.visibility = View.GONE
        binding.startDownload.visibility = View.VISIBLE
        binding.startDownload.text = "Start"
        binding.linearProgress.progress = 0
        binding.circularProgress.progress = 0
        binding.tvProgress.text = ""
        deleteFileFromStorage()
        fetch!!.delete(request!!.id)
        generateNotification(0, false)
    }

    /**
     * this method will delete file from storage in case of cancel download or download error
     * */
    private fun deleteFileFromStorage() {
        appDatabase.filesDao().deleteFile("101")
        val videoNameWithPath = request!!.file
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

        //  val filePath: String = Data.getSaveDir(this) + "/movies/" + Data.getNameFromUrl(url)
        val fileNameWithPath = this.externalCacheDir!!.path + "/Videos/" + Data.getNameFromUrl(url)
        Log.e("TAG", "newFilePath: $fileNameWithPath")
        request = Request(url, fileNameWithPath, 0)
        //   request!!.extras = getExtrasForRequest(request!!)
        setDataInRoom()
        fetch!!.attachFetchObserversForDownload(request!!.id, this@SingleDownloadActivity)
            .enqueue(request!!, { result: Request ->
                request = result
                binding.startDownload.visibility = View.GONE
                binding.pauseDownload.visibility = View.VISIBLE
                binding.cancelDownload.visibility = View.VISIBLE
                binding.linearProgress.visibility = View.VISIBLE
                binding.circularProgress.visibility = View.VISIBLE
                binding.tvProgress.visibility = View.VISIBLE
            }) {
                Log.e(TAG, "enqueueDownload:Error " + it.toString())
            }

    }

    /**
     * this method is used to set data in room, when download starts it will insert data in local db if not already exist
     * */
    private fun setDataInRoom() {
        val file = appDatabase.filesDao().getFile("101")
        if (file == null) {
            Log.e(TAG, "setDataInRoom: File Not found")
            val c = Calendar.getInstance().time
            // exp: Wed, Feb 02, 2023
            val df = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
            val formattedDate: String = df.format(c)

            val fileDownloading = FilesDownloading(
                vId = 0,
                downloadId = request!!.id.toString(),
                fileId = "101",
                fileName = Data.getNameFromUrl(url)!!,
                downloadStatus = AppConstants.File_DOWNLOADING,
                downloadPrg = 0,
                downloadDate = formattedDate
            )

            Coroutines.main {
                val x: Long = appDatabase.filesDao().insertFile(fileDownloading)
                Log.e("VideoPlayAdapter", "insertVideo: $x")
            }
        }

    }

    /**
     * this method will call when download gets started, it will return download progress,
     * it will also return state of the download request
     * */
    override fun onChanged(data: Download, reason: Reason) {
        var downloadProgress = data.progress
        binding.linearProgress.setProgress(downloadProgress, true)
        binding.circularProgress.setProgress(downloadProgress, true)

        if (downloadProgress > -1) {
            binding.tvProgress.text = downloadProgress.toString()
            appDatabase.filesDao().updateFilePrg(downloadProgress, "101")
            generateNotification(downloadProgress, false)
        } else
            binding.tvProgress.text = "0"
        request!!.id = data.id
        if (reason == Reason.DOWNLOAD_ERROR) {
            // resetDownloadTimer()
            generateNotification(downloadProgress, true)
            Toast.makeText(applicationContext, "Error while downloading", Toast.LENGTH_SHORT).show()
            binding.resumeDownload.visibility = View.GONE
            binding.pauseDownload.visibility = View.GONE
            binding.linearProgress.visibility = View.GONE
            binding.circularProgress.visibility = View.GONE
            binding.tvProgress.visibility = View.GONE
            binding.startDownload.visibility = View.VISIBLE
            binding.startDownload.text = "Start"
            binding.linearProgress.progress = 0
            binding.circularProgress.progress = 0
            binding.tvProgress.text = ""
            appDatabase.filesDao().deleteFile("101")
        }

        if (data.total < getAvailableInternalMemorySize()) {
            if (data.progress == 100) {
                // resetDownloadTimer()
            }
        }

        if (data.status == Status.COMPLETED) {
            try {
                binding.startDownload.text = "Downloaded"
                binding.cancelDownload.visibility = View.GONE
                binding.pauseDownload.visibility = View.GONE
                binding.startDownload.visibility = View.VISIBLE
                binding.startDownload.isClickable = false

                generateNotification(downloadProgress, false)

                fetch!!.removeFetchObserversForDownload(
                    request!!.id, this@SingleDownloadActivity
                )


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * this method is used to generate notification while downloading
     * */
    private fun generateNotification(downloadProgress: Int, downloadError: Boolean) {
        val intent: Intent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(fileNameWithPath)
                type = getMimeType(url)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(fileNameWithPath)
                type = getMimeType(url)
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
            .setContentText(Data.getNameFromUrl(url))
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
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
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

        val pauseIntent = Intent("PAUSE_DOWNLOAD_ACTION")
        val pausePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent("RESUME_DOWNLOAD_ACTION")
        val resumePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            1,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent("CANCEL_DOWNLOAD_ACTION")
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (isPause) {
            builder.setContentTitle("Download Paused")
            builder.setSmallIcon(R.drawable.fetch_notification_resume)
            builder.setProgress(100, downloadProgress, false)
            builder.addAction(R.drawable.fetch_notification_pause, "Resume", resumePendingIntent)
            builder.addAction(
                com.google.android.material.R.drawable.mtrl_ic_error,
                "Cancel",
                cancelPendingIntent
            )
        } else if (isCancel) {
            builder.setContentTitle("Download Cancelled")
            builder.setSmallIcon(com.google.android.material.R.drawable.mtrl_ic_error)
            builder.setProgress(0, 0, false)
        } else if (downloadError) {
            builder.setContentTitle("Error occurred while downloading")
            builder.setSmallIcon(com.google.android.material.R.drawable.mtrl_ic_error)
            builder.setProgress(0, 0, false)
        } else if (downloadProgress < 100) {
            builder.setContentTitle("Downloading in progress")
            builder.setProgress(100, downloadProgress, false)
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
        NotificationManagerCompat.from(applicationContext).notify(1, builder.build())

    }

    /**
     * this method is used to check if there is any space available in mobile to download
     * */
    private fun getAvailableInternalMemorySize(): Long {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize: Long = stat.blockSizeLong
        val availableBlocks: Long = stat.availableBlocksLong
        return availableBlocks * blockSize
    }


}


