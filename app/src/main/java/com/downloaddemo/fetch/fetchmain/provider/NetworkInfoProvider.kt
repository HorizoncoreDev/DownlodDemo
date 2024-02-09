package com.downloaddemo.fetch.fetchmain.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.downloaddemo.fetch.fetchcore.isNetworkAvailable
import com.downloaddemo.fetch.fetchcore.isOnMeteredConnection
import com.downloaddemo.fetch.fetchcore.isOnWiFi
import com.downloaddemo.fetch.fetchmain.NetworkType
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection
import java.net.URL


class NetworkInfoProvider constructor(
    private val context: Context, private val internetCheckUrl: String?
) {

    private val lock = Any()
    private val networkChangeListenerSet = hashSetOf<NetworkChangeListener>()
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val networkChangeBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            notifyNetworkChangeListeners()
        }
    }
    private var broadcastRegistered = false
    private var networkCallback: Any? = null

    init {
        if (connectivityManager != null) {
            val networkRequest =
                NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build()
            val networkCallback: ConnectivityManager.NetworkCallback =
                object : ConnectivityManager.NetworkCallback() {

                    override fun onLost(network: Network) {
                        notifyNetworkChangeListeners()
                    }

                    override fun onAvailable(network: Network) {
                        notifyNetworkChangeListeners()
                    }

                }
            this.networkCallback = networkCallback
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } else {
            try {
                context.registerReceiver(
                    networkChangeBroadcastReceiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                )
                broadcastRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun notifyNetworkChangeListeners() {
        synchronized(lock) {
            networkChangeListenerSet.iterator().forEach { listener ->
                listener.onNetworkChanged()
            }
        }
    }

    fun registerNetworkChangeListener(networkChangeListener: NetworkChangeListener) {
        synchronized(lock) {
            networkChangeListenerSet.add(networkChangeListener)
        }
    }

    fun unregisterNetworkChangeListener(networkChangeListener: NetworkChangeListener) {
        synchronized(lock) {
            networkChangeListenerSet.remove(networkChangeListener)
        }
    }

    fun unregisterAllNetworkChangeListeners() {
        synchronized(lock) {
            networkChangeListenerSet.clear()
            if (broadcastRegistered) {
                try {
                    context.unregisterReceiver(networkChangeBroadcastReceiver)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (connectivityManager != null) {
                val networkCallback = this.networkCallback
                if (networkCallback is ConnectivityManager.NetworkCallback) {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
            }
        }
    }

    fun isOnAllowedNetwork(networkType: NetworkType): Boolean {
        if (networkType == NetworkType.WIFI_ONLY && context.isOnWiFi()) {
            return true
        }
        if (networkType == NetworkType.UNMETERED && !context.isOnMeteredConnection()) {
            return true
        }
        if (networkType == NetworkType.ALL && context.isNetworkAvailable()) {
            return true
        }
        return false
    }

    val isNetworkAvailable: Boolean
        get() {
            val url = internetCheckUrl
            return if (url != null) {
                var connected = false
                try {
                    val urlConnection = URL(url)
                    val connection = urlConnection.openConnection() as HttpsURLConnection
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 20_000
                    connection.instanceFollowRedirects = true
                    connection.useCaches = false
                    connection.defaultUseCaches = false
                    connection.setRequestProperty("Connection", "close")
                    connection.connect()
                    connected = connection.responseCode != -1
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                connected
            } else {
                context.isNetworkAvailable()
            }
        }

    interface NetworkChangeListener {
        fun onNetworkChanged()
    }

}