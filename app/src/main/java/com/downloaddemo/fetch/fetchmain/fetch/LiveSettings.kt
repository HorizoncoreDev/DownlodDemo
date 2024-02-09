package com.downloaddemo.fetch.fetchmain.fetch

class LiveSettings(val namespace: String) {

    private val lock = Any()

    @Volatile
    var didSanitizeDatabaseOnFirstEntry: Boolean = false

    fun execute(func: (LiveSettings) -> Unit) {
        synchronized(lock) {
            func(this)
        }
    }

}