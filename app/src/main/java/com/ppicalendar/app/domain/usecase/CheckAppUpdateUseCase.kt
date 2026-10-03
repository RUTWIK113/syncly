package com.ppicalendar.app.domain.usecase

import android.content.Context
import android.os.Build
import android.util.Log
import com.ppicalendar.app.domain.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class CheckAppUpdateUseCase(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "CheckAppUpdateUseCase"
        private const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/RUTWIK113/syncly/main/version.json"
    }

    suspend operator fun invoke(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UPDATE_JSON_URL)
                .header("Cache-Control", "no-cache")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.d(TAG, "Update check HTTP response unsuccessful: ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val updateInfo = json.decodeFromString<AppUpdateInfo>(body)

            val currentVersionCode = getInstalledVersionCode()
            Log.d(TAG, "Current versionCode: $currentVersionCode, Remote versionCode: ${updateInfo.versionCode}")

            if (updateInfo.versionCode > currentVersionCode) {
                return@withContext updateInfo
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check for updates: ${e.message}")
        }
        null
    }

    private fun getInstalledVersionCode(): Long {
        return try {
            com.ppicalendar.app.BuildConfig.VERSION_CODE.toLong()
        } catch (e: Exception) {
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            } catch (e2: Exception) {
                Long.MAX_VALUE
            }
        }
    }
}
