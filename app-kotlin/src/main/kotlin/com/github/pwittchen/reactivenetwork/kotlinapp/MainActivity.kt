/*
 * Copyright (C) 2016 Piotr Wittchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pwittchen.reactivenetwork.kotlinapp

import android.app.Activity
import android.net.wifi.ScanResult
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import com.github.pwittchen.reactivenetwork.library.ConnectivityStatus
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork
import com.github.pwittchen.reactivenetwork.library.WifiSignalLevel
import kotlinx.android.synthetic.main.activity_main.*

import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.ArrayList

class MainActivity : Activity() {
  private var wifiSub: Subscription? = null
  private var connectivitySub: Subscription? = null
  private var internetSub: Subscription? = null
  private var signalLevelSub: Subscription? = null

  companion object {
    private val TAG = "ReactiveNetwork"
    private val WIFI_SIGNAL_LEVEL_MESSAGE = "WiFi signal level: ";
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  override fun onResume() {
    super.onResume()
    val reactiveNetwork: ReactiveNetwork = ReactiveNetwork()

    connectivitySub = reactiveNetwork.observeNetworkConnectivity(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { connectivityStatus ->
          Log.d(TAG, connectivityStatus.toString())
          connectivity_status.text = connectivityStatus.description;

          val isOffline = connectivityStatus == ConnectivityStatus.OFFLINE
          val isMobileConnected = connectivityStatus == ConnectivityStatus.MOBILE_CONNECTED

          if (isOffline || isMobileConnected) {
            val description = WifiSignalLevel.NO_SIGNAL.description
            wifi_signal_level.text = WIFI_SIGNAL_LEVEL_MESSAGE + description
          }
        }

    internetSub = reactiveNetwork.observeInternetConnectivity()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { isConnectedToInternet ->
          internet_status.text = isConnectedToInternet.toString()
        }

    signalLevelSub = reactiveNetwork.observeWifiSignalLevel(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { wifiSignalLevel ->
          Log.d(TAG, wifiSignalLevel.toString())
          wifi_signal_level.text = WIFI_SIGNAL_LEVEL_MESSAGE + wifiSignalLevel.description;
        }

    wifiSub = reactiveNetwork.observeWifiAccessPoints(applicationContext)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { scanResults -> displayAccessPoints(scanResults) }
  }

  private fun displayAccessPoints(scanResults: List<ScanResult>) {
    val ssids = ArrayList<String>()

    for (scanResult in scanResults) {
      ssids.add(scanResult.SSID)
    }

    access_points.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ssids)
  }

  override fun onPause() {
    super.onPause()
    safelyUnsubscribe(connectivitySub)
    safelyUnsubscribe(internetSub)
    safelyUnsubscribe(signalLevelSub)
    safelyUnsubscribe(wifiSub)
  }

  private fun safelyUnsubscribe(subscription: Subscription?) {
    if (subscription != null && !subscription.isUnsubscribed) {
      subscription.unsubscribe()
    }
  }
}
