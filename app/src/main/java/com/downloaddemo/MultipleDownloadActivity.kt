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
import com.downloaddemo.fetch.fetchokhttp.OkHttpDownloader
import com.downloaddemo.room_database.AppDatabase
import com.downloaddemo.room_database.FilesDownloading
import com.downloaddemo.utils.AppConstants
import com.downloaddemo.utils.Coroutines
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MultipleDownloadActivity : AppCompatActivity(), FetchObserver<Download> {


    lateinit var binding: MultipleDownloadBinding
    var fetch: Fetch? = null
    private var requestList: ArrayList<Request> = arrayListOf()
    var downloadList: Array<DownloadData> = arrayOf()
    private val STORAGE_PERMISSION_CODE = 100
    private val TAG = "DOWNLOAD_DEMO"
    lateinit var multipleDownloadAdapter: MultipleDownloadAdapter
    var downloadPosition = 0
    private val appDatabase: AppDatabase by lazy {
        AppConstants.getAppDatabase(this)
    }
    var fileNameWithPath = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MultipleDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fetchConfiguration =
            FetchConfiguration.Builder(applicationContext).setDownloadConcurrentLimit(1)
                .setHttpDownloader(OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL)).build()
        if (fetch == null) {

            fetch = Fetch.getInstance(fetchConfiguration).addListener(fetchListener)
            downloadList= Data.sampleDownloadList
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
                                    this.externalCacheDir!!.path + "/Videos/" + Data.getNameFromUrl(downloadList[i].downloadUrl)

                                fetch!!.attachFetchObserversForDownload(requestList[i].id, this@MultipleDownloadActivity)
                                    .enqueue(requestList[i], { result: Request ->
                                        requestList[i] = result
                                    }) {
                                        Log.e(TAG, "enqueueDownload:Error $it")
                                    }
                                Log.e(TAG, "setDataInRoom: " + file.downloadPrg)
                                when (file.downloadPrg) {
                                    in 0..99 -> {
                                        downloadList[i].downloading = true
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
                    Log.e(TAG, "onPause: "+requestList[it].id )
                },
                onResumeDownloadClicked = {
                    fetch!!.resume(requestList[it].id)
                })
            binding.rvMultipleDownload.adapter = multipleDownloadAdapter

            val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
            binding.rvMultipleDownload.layoutManager = layoutManager

        }
    }

    private fun deleteFileFromStorage(index: Int) {
        appDatabase.filesDao().deleteFile("101")
        val videoNameWithPath = requestList[index].file
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

    override fun onChanged(data: Download, reason: Reason) {
        Log.e(TAG, "onChanged: "+data.id +"..progress.."+data.progress)
        for(i in requestList.indices) {
            if(requestList[i].id == data.id) {
                multipleDownloadAdapter.updateDownloadProgress(data, reason, i)
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