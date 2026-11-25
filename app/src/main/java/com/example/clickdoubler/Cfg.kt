package com.example.clickdoubler

import android.content.Context
import android.content.SharedPreferences

object Cfg {
    fun p(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences("cfg", Context.MODE_PRIVATE)
}
