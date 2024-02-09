package com.downloaddemo.fetch.fetchfileserver

import com.downloaddemo.fetch.fetchcore.FetchLogger


/** Fetch File Server Default Logger*/
open class FetchFileServerLogger(
    enableLogging: Boolean = true, tag: String = "FetchFileServerLogger"
) : FetchLogger(enableLogging, tag)