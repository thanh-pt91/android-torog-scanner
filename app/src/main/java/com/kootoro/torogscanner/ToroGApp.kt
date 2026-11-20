package com.kootoro.torogscanner

import android.app.Application

class ToroGApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ToroGApp
            private set
    }
}
