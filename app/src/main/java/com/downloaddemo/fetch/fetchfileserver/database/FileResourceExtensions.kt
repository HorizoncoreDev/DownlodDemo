package com.downloaddemo.fetch.fetchfileserver.database

import com.downloaddemo.fetch.fetchcore.Extras
import com.downloaddemo.fetch.fetchcore.FileResource
import org.json.JSONObject

fun FileResource.toFileResourceInfo(): FileResourceInfo {
    val fileResourceInfo = FileResourceInfo()
    fileResourceInfo.id = id
    fileResourceInfo.file = file
    fileResourceInfo.length = length
    fileResourceInfo.name = name
    fileResourceInfo.md5 = md5
    fileResourceInfo.extras = extras.toJSONString()
    return fileResourceInfo
}

fun FileResourceInfo.toFileResource(): FileResource {
    val fileResource = FileResource()
    fileResource.id = id
    fileResource.file = file
    fileResource.length = length
    fileResource.name = name
    fileResource.md5 = md5
    fileResource.extras = try {
        val map = mutableMapOf<String, String>()
        val json = JSONObject(extras)
        json.keys().forEach {
            map[it] = json.getString(it)
        }
        Extras(map)
    } catch (e: Exception) {
        Extras.emptyExtras
    }
    return fileResource
}