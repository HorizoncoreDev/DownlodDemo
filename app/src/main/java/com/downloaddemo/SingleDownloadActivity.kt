package com.downloaddemo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.downloaddemo.fetch.fetchcore.Extras
import com.downloaddemo.fetch.fetchcore.FetchObserver
import com.downloaddemo.fetch.fetchcore.MutableExtras
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
import com.downloaddemo.utils.isAboveAndroid12
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class SingleDownloadActivity : AppCompatActivity(), FetchObserver<Download> {


    lateinit var binding: SingleDownloadBinding
    var fetch: Fetch? = null
    private var request: Request? = null
    private val STORAGE_PERMISSION_CODE = 100
    private val TAG = "DOWNLOAD_DEMO"
    lateinit var url: String
    private val appDatabase: AppDatabase by lazy {
        AppConstants.getAppDatabase(this)
    }
    var fileNameWithPath =""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SingleDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fetchConfiguration =
            FetchConfiguration.Builder(applicationContext).setDownloadConcurrentLimit(1)
                .setHttpDownloader(OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL)).build()
        if (fetch == null) {

            fetch = Fetch.getInstance(fetchConfiguration).addListener(fetchListener)

            url = Data.sampleUrls[0]
            binding.tvUrl.text = url

            val file = appDatabase.filesDao().getFile("101")
            if(file!=null){
                 fileNameWithPath = this.externalCacheDir!!.path + "/Videos/" + Data.getNameFromUrl(url)

                request = Request(url, fileNameWithPath, 0)
                fetch!!.attachFetchObserversForDownload(request!!.id, this@SingleDownloadActivity)
                    .enqueue(request!!, { result: Request ->
                        request = result
                    }) {
                        Log.e(TAG, "enqueueDownload:Error " + it.toString())
                    }
                Log.e(TAG, "setDataInRoom: "+file.downloadPrg )
                when (file.downloadPrg) {
                    in 0..99 -> {
                        binding.startDownload.visibility = View.GONE
                        binding.pauseDownload.visibility = View.VISIBLE
                        binding.linearProgress.visibility = View.VISIBLE
                        binding.circularProgress.visibility = View.VISIBLE
                        binding.tvProgress.visibility = View.VISIBLE
                    }

                    100 -> {
                        binding.startDownload.text = "Downloaded"
                        binding.cancelDownload.visibility = View.GONE
                        binding.pauseDownload.visibility = View.GONE
                        binding.startDownload.visibility = View.VISIBLE
                        binding.linearProgress.visibility = View.VISIBLE
                        binding.circularProgress.visibility = View.VISIBLE
                        binding.tvProgress.visibility = View.VISIBLE
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
                binding.pauseDownload.visibility = View.GONE
                binding.resumeDownload.visibility = View.VISIBLE
                fetch!!.pause(request!!.id)
            }

            binding.resumeDownload.setOnClickListener {
                binding.resumeDownload.visibility = View.GONE
                binding.pauseDownload.visibility = View.VISIBLE
                fetch!!.resume(request!!.id)
            }

            binding.cancelDownload.setOnClickListener {
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

    private fun generateNotification(){
            val intent: Intent

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(fileNameWithPath)
                    type = "application/pdf"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(fileNameWithPath)
                    type = "application/pdf"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            // Push logic
            val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val builder = NotificationCompat.Builder(applicationContext, "CHANNEL_ID")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Downloaded")
                .setContentText(Data.getNameFromUrl(url))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("CHANNEL_ID", "Channel 1",
                    NotificationManager.IMPORTANCE_DEFAULT).apply {
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
        NotificationManagerCompat.from(applicationContext).notify(1, builder.build())
    }

    private fun deleteFileFromStorage() {
        appDatabase.filesDao().deleteFile("101")
        val videoNameWithPath = request!!.file
        val fileNew = File(videoNameWithPath)
        if (fileNew.exists()) {
            fileNew.delete()
        }
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

    private fun openSettings() {
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.parse("package:" + applicationContext.packageName)
        settingsIntent.data = uri
        settingsLauncher.launch(settingsIntent)
    }

    private fun checkAndGetPermission() {
        if (checkHasPermissions()) {
            enqueueDownload()
        } else {
            storagePermissionLauncher.launch(reqStoragePermission)
        }
    }

    private fun checkHasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, readStoragePermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enqueueDownload() {

        val filePath: String = Data.getSaveDir(this) + "/movies/" + Data.getNameFromUrl(url)
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

    private fun setDataInRoom(){
        val file = appDatabase.filesDao().getFile("101")
        if(file==null) {
            Log.e(TAG, "setDataInRoom: File Not found" )
            val c = Calendar.getInstance().time
            // exp: Wed, Feb 02, 2023
            val df = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
            val formattedDate: String = df.format(c)

            val fileDownloading = FilesDownloading(
                vId = 0,
                downloadId = request!!.id.toString(),
                fileId = "101",
                fileName = Data.getNameFromUrl(url)!!,
                downloadStatus = AppConstants.MATERIAL_DOWNLOADING,
                downloadPrg = 0,
                downloadDate = formattedDate
            )

            Coroutines.main {
                val x: Long? = appDatabase.filesDao().insertFile(fileDownloading)
                Log.e("VideoPlayAdapter", "insertVideo: $x")
            }
        }

    }

    private fun getExtrasForRequest(request: Request): Extras {
        val extras = MutableExtras()
        extras.putBoolean("testBoolean", true)
        extras.putString("testString", "test")
        extras.putFloat("testFloat", Float.MIN_VALUE)
        extras.putDouble("testDouble", Double.MIN_VALUE)
        extras.putInt("testInt", Int.MAX_VALUE)
        extras.putLong("testLong", Long.MAX_VALUE)
        return extras
    }


    override fun onChanged(data: Download, reason: Reason) {
        var downloadProgress = data.progress
        binding.linearProgress.setProgress(downloadProgress, true)
        binding.circularProgress.setProgress(downloadProgress, true)



        if (downloadProgress > -1) {
            binding.tvProgress.text = downloadProgress.toString()
            appDatabase.filesDao().updateFilePrg(downloadProgress,"101")
        }
        else
            binding.tvProgress.text = "0"
        request!!.id = data.id
        if (reason == Reason.DOWNLOAD_ERROR) {
            // resetDownloadTimer()
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

                generateNotification()

                fetch!!.removeFetchObserversForDownload(
                    request!!.id, this@SingleDownloadActivity
                )


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getAvailableInternalMemorySize(): Long {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize: Long = stat.blockSizeLong
        val availableBlocks: Long = stat.availableBlocksLong
        return availableBlocks * blockSize
    }


}