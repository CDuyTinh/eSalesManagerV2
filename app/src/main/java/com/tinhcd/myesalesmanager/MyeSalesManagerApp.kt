package com.tinhcd.myesalesmanager

import android.app.Application
import com.tinhcd.myesalesmanager.push.PushNotifications
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyeSalesManagerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Before any push can arrive: FCM shows background ones on this channel.
        PushNotifications.createChannel(this)
    }
}
