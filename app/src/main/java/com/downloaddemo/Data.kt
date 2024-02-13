package com.downloaddemo

import android.content.Context
import android.net.Uri
import android.os.Environment

class Data {
    companion object {

        /**
         * this array used for single download
        * */
        val sampleUrls = arrayOf(
            "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            "http://speedtest.ftp.otenet.gr/files/test100Mb.db",
            "https://download.blender.org/peach/bigbuckbunny_movies/big_buck_bunny_720p_stereo.avi",
            "http://media.mongodb.org/zips.json",
            "http://www.exampletonyotest/some/unknown/123/Errorlink.txt",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Android_logo_2019.svg/687px-Android_logo_2019.svg.png",
            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        )

        fun getSaveDir(context: Context): String? {
            return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/fetch"
        }

        /**
         * this method used to get the filename from full URL
         * */
        fun getNameFromUrl(url: String?): String? {
            return Uri.parse(url).lastPathSegment
        }

        /**
         * this array used for multiple download
         * */
        val sampleDownloadList:Array<DownloadData> = arrayOf(
            DownloadData(
                "1",
                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
            ),
            DownloadData("2", "http://speedtest.ftp.otenet.gr/files/test100Mb.db"),
            DownloadData(
                "3",
                "https://download.blender.org/peach/bigbuckbunny_movies/big_buck_bunny_720p_stereo.avi"
            ),
            DownloadData("4", "http://media.mongodb.org/zips.json"),
            DownloadData("5", "http://www.exampletonyotest/some/unknown/123/Errorlink.txt"),
            DownloadData(
                "6",
                "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Android_logo_2019.svg/687px-Android_logo_2019.svg.png"
            ),
            DownloadData(
                "7",
                "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            DownloadData(
                "8",
                "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            )
        )

    }


}