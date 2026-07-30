package com.tollscan.app

import android.app.Application
import com.tollscan.app.data.TollRepository

class TollScanApp : Application() {

    lateinit var repository: TollRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TollRepository(this)
    }
}
