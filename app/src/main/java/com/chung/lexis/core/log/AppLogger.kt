package com.chung.lexis.core.log

import android.util.Log

/*
 * ViewModel log qua interface này thay vì gọi thẳng
 * android.util.Log: unit test chạy trên JVM inject
 * được fake, không bị "Method e in android.util.Log
 * not mocked".
 */
interface AppLogger {

    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}

object AndroidLogger : AppLogger {

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.e(
            tag,
            message,
            throwable,
        )
    }
}
