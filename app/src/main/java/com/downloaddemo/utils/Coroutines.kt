package com.downloaddemo.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * this object to used to do long running task in worker(IO)/main thread
 * */
object Coroutines {

    fun main(work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Main).launch {
            work()
        }

    fun io(work: suspend () -> Unit) =
        CoroutineScope(Dispatchers.IO).launch {
            work()
        }

}