package com.downloaddemo.fetch.fetchfileserver.provider

import com.downloaddemo.fetch.fetchcore.FileResource

interface FileResourceProvider {

    val id: String

    fun execute()

    fun interrupt()

    fun isServingFileResource(fileResource: FileResource): Boolean

}