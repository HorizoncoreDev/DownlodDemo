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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.downloaddemo.databinding.ItemMultipleDownloadBinding
import com.downloaddemo.fetch.fetchcore.Reason
import com.downloaddemo.fetch.fetchmain.Download
import com.downloaddemo.fetch.fetchmain.Fetch
import com.downloaddemo.fetch.fetchmain.Request
import com.downloaddemo.room_database.AppDatabase
import com.downloaddemo.utils.isAboveAndroid12
import java.io.File

class MultipleDownloadAdapter(
    var context: Context,
    private var downloadList: Array<DownloadData>,
    private var requestList: ArrayList<Request>,
    var fetch: Fetch,
    var appDatabase: AppDatabase,
    val onStartDownloadClicked: (position: Int) -> Unit,
    val onPauseDownloadClicked: (position: Int) -> Unit,
    val onResumeDownloadClicked: (position: Int) -> Unit,
    val onCancelDownloadClicked: (position: Int) -> Unit,
    val onPermissionNotGranted: (position: Int) -> Unit,
) : RecyclerView.Adapter<MultipleDownloadAdapter.ViewHolder>() {

    class ViewHolder(
        val binding: ItemMultipleDownloadBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = DataBindingUtil.inflate<ItemMultipleDownloadBinding>(
            LayoutInflater.from(parent.context), R.layout.item_multiple_download, parent, false
        )
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = downloadList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        var url = downloadList[position].downloadUrl.split("/")
        holder.binding.tvUrl.text = downloadList[position].downloadUrl

        holder.binding.linearProgress.setProgress(downloadList[position].downloadProgress, true)
        holder.binding.circularProgress.setProgress(downloadList[position].downloadProgress, true)

        if (downloadList[position].downloadProgress == 100) {
                holder.binding.startDownload.text = "Downloaded"
                holder.binding.cancelDownload.visibility = View.GONE
                holder.binding.pauseDownload.visibility = View.GONE
                holder.binding.resumeDownload.visibility = View.GONE
                holder.binding.startDownload.visibility = View.VISIBLE
                holder.binding.startDownload.isClickable = false
        }



        if( downloadList[position].downloading) {
            if (downloadList[position].downloadProgress > -1) {
                holder.binding.tvProgress.text = downloadList[position].downloadProgress.toString()
                appDatabase.filesDao().updateFilePrg(
                    downloadList[position].downloadProgress,
                    downloadList[position].downloadId
                )
                if (downloadList[position].downloadProgress == 100) {
                    downloadList[position].downloading = false
                    try {
                        holder.binding.startDownload.text = "Downloaded"
                        holder.binding.cancelDownload.visibility = View.GONE
                        holder.binding.pauseDownload.visibility = View.GONE
                        holder.binding.startDownload.visibility = View.VISIBLE
                        holder.binding.startDownload.isClickable = false

                        generateNotification(position)

                        fetch.removeFetchObserversForDownload(
                            requestList[position].id,
                        )


                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }else{
                    if(downloadList[position].cancelled){
                        holder.binding.cancelDownload.visibility = View.GONE
                        holder.binding.pauseDownload.visibility = View.GONE
                        holder.binding.resumeDownload.visibility = View.GONE
                        holder.binding.startDownload.visibility = View.VISIBLE
                    }else {
                        if (downloadList[position].paused) {
                            holder.binding.pauseDownload.visibility = View.GONE
                            holder.binding.resumeDownload.visibility = View.VISIBLE
                        } else {
                            holder.binding.pauseDownload.visibility = View.VISIBLE
                            holder.binding.resumeDownload.visibility = View.GONE
                        }

                        holder.binding.startDownload.visibility = View.GONE
                        holder.binding.cancelDownload.visibility = View.VISIBLE
                        holder.binding.linearProgress.visibility = View.VISIBLE
                        holder.binding.circularProgress.visibility = View.VISIBLE
                        holder.binding.tvProgress.visibility = View.VISIBLE
                    }
                }
            } else {
                holder.binding.tvProgress.text = "0"
            }

            if (downloadList[position].downloadError) {
                downloadList[position].downloading = false
                // resetDownloadTimer()
                Toast.makeText(context, "Error while downloading", Toast.LENGTH_SHORT).show()
                holder.binding.resumeDownload.visibility = View.GONE
                holder.binding.pauseDownload.visibility = View.GONE
                holder.binding.linearProgress.visibility = View.GONE
                holder.binding.circularProgress.visibility = View.GONE
                holder.binding.tvProgress.visibility = View.GONE
                holder.binding.startDownload.visibility = View.VISIBLE
                holder.binding.startDownload.text = "Start"
                holder.binding.linearProgress.progress = 0
                holder.binding.circularProgress.progress = 0
                holder.binding.tvProgress.text = ""
                appDatabase.filesDao().deleteFile(downloadList[position].downloadId)
            }
        }
        holder.binding.startDownload.setOnClickListener {

            if (!isAboveAndroid12()) {
                val readPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                )
                if (readPermission != PackageManager.PERMISSION_GRANTED) {
                    onPermissionNotGranted(position)
                    return@setOnClickListener
                } else {
                    holder.binding.startDownload.visibility = View.GONE
                    holder.binding.pauseDownload.visibility = View.VISIBLE
                    holder.binding.cancelDownload.visibility = View.VISIBLE
                    holder.binding.linearProgress.visibility = View.VISIBLE
                    holder.binding.circularProgress.visibility = View.VISIBLE
                    holder.binding.tvProgress.visibility = View.VISIBLE
                    onStartDownloadClicked(position)
                }
            }

        }

        holder.binding.pauseDownload.setOnClickListener {
            downloadList[position].paused = true
            downloadList[position].downloading = false
            downloadList[position].cancelled = false
            holder.binding.pauseDownload.visibility = View.GONE
            holder.binding.resumeDownload.visibility = View.VISIBLE
            onPauseDownloadClicked(position)
        }

        holder.binding.resumeDownload.setOnClickListener {
            downloadList[position].paused = false
            downloadList[position].cancelled = false
            holder.binding.resumeDownload.visibility = View.GONE
            holder.binding.pauseDownload.visibility = View.VISIBLE
            onResumeDownloadClicked(position)
        }

        holder.binding.cancelDownload.setOnClickListener {
            downloadList[position].cancelled = true
            downloadList[position].downloading = false
            holder.binding.cancelDownload.visibility = View.GONE
            holder.binding.resumeDownload.visibility = View.GONE
            holder.binding.pauseDownload.visibility = View.GONE
            holder.binding.linearProgress.visibility = View.GONE
            holder.binding.circularProgress.visibility = View.GONE
            holder.binding.tvProgress.visibility = View.GONE
            holder.binding.startDownload.visibility = View.VISIBLE
            holder.binding.startDownload.text = "Start"
            holder.binding.linearProgress.progress = 0
            holder.binding.circularProgress.progress = 0
            holder.binding.tvProgress.text = ""
            onCancelDownloadClicked(position)
        }

    }



    fun updateDownloadProgress(data: Download, reason: Reason, downloadPosition: Int) {
        downloadList[downloadPosition].downloadProgress = data.progress
        downloadList[downloadPosition].downloading = true
        requestList[downloadPosition].id = data.id
        downloadList[downloadPosition].downloadError = reason == Reason.DOWNLOAD_ERROR
        notifyItemChanged(downloadPosition)
    }

    private fun generateNotification(position: Int) {
        val intent: Intent
        var fileNameWithPath =
            context.externalCacheDir!!.path + "/Videos/" + Data.getNameFromUrl(downloadList[position].downloadUrl)

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
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, "CHANNEL_ID")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloaded")
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
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        if (ActivityCompat.checkSelfPermission(
                context,
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
        NotificationManagerCompat.from(context).notify(1, builder.build())
    }

}