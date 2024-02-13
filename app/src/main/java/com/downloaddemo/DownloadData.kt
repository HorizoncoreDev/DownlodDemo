package com.downloaddemo

/**
 * this class used for Multiple download list
 * */
class DownloadData (
      var downloadId:String,
      var downloadUrl:String,
      var downloadProgress:Int=-1,
      var downloadError:Boolean = false,
      var downloading:Boolean = false,
      var paused:Boolean = false,
      var cancelled:Boolean = false,
)