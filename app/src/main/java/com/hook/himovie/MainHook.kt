package com.hook.himovie

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedHelpers
import java.util.Base64

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.huawei.himovie") return

        try {
            val cls = XposedHelpers.findClass(
                "okhttp3.RequestBody",
                lpparam.classLoader
            )
            XposedHelpers.findAndHookMethod(
                cls,
                "writeTo",
                okio.BufferedSink::class.java,
                object : de.robv.android.xposed.XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Log.i("HIMOVIEHOOK", "Request intercepted")
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e("HIMOVIEHOOK", "Hook error", e)
        }
    }
}
