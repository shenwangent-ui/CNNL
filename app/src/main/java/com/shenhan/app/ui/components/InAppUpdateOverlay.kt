package com.shenhan.app.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private data class AppReleaseInfo(
    val releaseName: String,
    val downloadUrl: String,
    val fileSizeMb: Double,
    val publishedLabel: String
)

@Composable
fun InAppUpdateOverlay() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var availableRelease by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedMbText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingApkFile by remember { mutableStateOf<File?>(null) }

    fun launchApkInstaller(apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                pendingApkFile = apkFile
                Toast.makeText(
                    context,
                    "กรุณาเปิดสวิตช์ 'อนุญาตจากแหล่งที่มา' แล้วกดย้อนกลับเพื่อติดตั้ง",
                    Toast.LENGTH_LONG
                ).show()
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permIntent)
                return
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            pendingApkFile = null
        } catch (e: Exception) {
            errorMessage = "เปิดหน้าติดตั้งไม่สำเร็จ: ${e.message}"
        }
    }

    // เมื่อผู้ใช้ไปกดอนุญาตติดตั้งแอปภายนอกแล้วกดกลับมาที่แอป ให้เด้งหน้าติดตั้งต่อทันที
    DisposableEffect(lifecycleOwner, pendingApkFile) {
        val observer = LifecycleEventObserver { _, event ->
            val queued = pendingApkFile
            if (event == Lifecycle.Event.ON_RESUME && queued != null && queued.exists()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    context.packageManager.canRequestPackageInstalls()
                ) {
                    launchApkInstaller(queued)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // เช็กเวอร์ชันใหม่อัตโนมัติทันทีที่เปิดแอป
    LaunchedEffect(Unit) {
        val found = checkLatestGithubRelease(context)
        if (found != null) {
            availableRelease = found
        }
    }

    val release = availableRelease ?: return

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) {
                availableRelease = null
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "🚀 พบอัปเดตเวอร์ชันใหม่!",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "เวอร์ชัน: ${release.releaseName}\nอัปเดตเมื่อ: ${release.publishedLabel} (${String.format(Locale.US, "%.1f", release.fileSizeMb)} MB)",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )

                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Text(
                        text = "กำลังดาวน์โหลดไฟล์ติดตั้ง... $downloadedMbText (${(downloadProgress * 100).toInt()}%)",
                        color = Color(0xFF7DD3FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "กด 'อัปเดตเลย' เพื่อดาวน์โหลดและติดตั้ง เมื่อติดตั้งเสร็จกด 'เปิด' แอปจะรีสตาร์ทเข้าเวอร์ชันใหม่ทันที",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = "⚠️ $errorMessage",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isDownloading,
                onClick = {
                    errorMessage = null
                    isDownloading = true
                    downloadProgress = 0f
                    scope.launch {
                        val fileRes = downloadApkToCache(
                            context = context,
                            downloadUrl = release.downloadUrl,
                            totalBytesHint = (release.fileSizeMb * 1048576).toLong()
                        ) { progress, mbLabel ->
                            downloadProgress = progress
                            downloadedMbText = mbLabel
                        }
                        isDownloading = false
                        fileRes.onSuccess { apkFile ->
                            launchApkInstaller(apkFile)
                        }.onFailure { err ->
                            errorMessage = err.message ?: "ดาวน์โหลดไม่สำเร็จ"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isDownloading) "กำลังโหลด..." else "อัปเดตเลย",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!isDownloading) {
                OutlinedButton(
                    onClick = { availableRelease = null },
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ไว้ทีหลัง", color = Color(0xFF94A3B8))
                }
            }
        }
    )
}

private suspend fun checkLatestGithubRelease(context: Context): AppReleaseInfo? =
    withContext(Dispatchers.IO) {
        try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val installedTimeMs = pkgInfo.lastUpdateTime

            val url = URL("https://api.github.com/repos/shenwangent-ui/CNNL/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "ShenHan-InAppUpdater")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)
            val assets = json.optJSONArray("assets") ?: return@withContext null

            var apkUrl = ""
            var apkSize = 0L
            var updatedAtStr = ""

            for (i in 0 until assets.length()) {
                val item = assets.getJSONObject(i)
                val name = item.optString("name", "")
                if (name.endsWith(".apk")) {
                    apkUrl = item.optString("browser_download_url", "")
                    apkSize = item.optLong("size", 0L)
                    updatedAtStr = item.optString("updated_at", "")
                    break
                }
            }

            if (apkUrl.isBlank() || updatedAtStr.isBlank()) return@withContext null

            val utcParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val remoteTimeMs = utcParser.parse(updatedAtStr)?.time ?: return@withContext null

            // แจ้งเตือนเมื่อไฟล์ APK บน GitHub ถูกบิลด์ใหม่กว่าเวลาที่ติดตั้งแอปบนเครื่องเกิน 90 วินาที
            if (remoteTimeMs > installedTimeMs + 90_000L) {
                val localFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                AppReleaseInfo(
                    releaseName = json.optString("name", json.optString("tag_name", "Latest Build")),
                    downloadUrl = apkUrl,
                    fileSizeMb = apkSize / 1048576.0,
                    publishedLabel = localFmt.format(remoteTimeMs)
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

private suspend fun downloadApkToCache(
    context: Context,
    downloadUrl: String,
    totalBytesHint: Long,
    onProgress: (Float, String) -> Unit
): Result<File> = withContext(Dispatchers.IO) {
    try {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outFile = File(updatesDir, "ShenHan-update.apk")
        if (outFile.exists()) outFile.delete()

        val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ShenHan-InAppUpdater")
            connectTimeout = 15000
            readTimeout = 30000
        }

        if (conn.responseCode !in 200..299) {
            return@withContext Result.failure(Exception("HTTP ${conn.responseCode}"))
        }

        val totalBytes = conn.contentLengthLong.takeIf { it > 0 } ?: totalBytesHint.coerceAtLeast(1L)
        var downloadedBytes = 0L

        conn.inputStream.use { input ->
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    val pct = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    val label = String.format(
                        Locale.US,
                        "%.1f / %.1f MB",
                        downloadedBytes / 1048576.0,
                        totalBytes / 1048576.0
                    )
                    withContext(Dispatchers.Main) {
                        onProgress(pct, label)
                    }
                }
            }
        }

        Result.success(outFile)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
