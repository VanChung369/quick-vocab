package com.chung.lexis

import android.app.Application
import com.chung.lexis.core.AppContainer
import com.chung.lexis.core.DefaultAppContainer

class LexisApplication :
    Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        appContainer = DefaultAppContainer(
            context = this
        )
    }
}