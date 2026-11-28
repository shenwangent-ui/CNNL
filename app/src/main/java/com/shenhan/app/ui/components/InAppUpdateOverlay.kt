package com.shenhan.app.ui.components

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shenhan.app.data.InAppAutoUpdater
import com.shenhan.app.data.UpdateInstallStatusReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
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
    var autoInstallConsent by remember {
        mutableStateOf(InAppAutoUpdater.isAutoInstallConsented(context))
    }
    var isDownloading by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedMbText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingApkFile by remember { mutableStateOf<File?>(null) }

    fun executeInstall(apkFile: File, releaseName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                pendingApkFile = apkFile
                isInstalling = false
                Toast.makeText(
                    context,
                    "กรุณาเปิดสวิตช์ 'อนุญาตจากแหล่งที่มา' แล้วกดย้อนกลับเพื่อติดตั้งอัตโนมัติ",
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

            if (autoInstallConsent) {
                isInstalling = true
                InAppAutoUpdater.markPendingRelaunch(context, releaseName, true)

                val packageInstaller = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(context.packageName)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }

                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)

                FileInputStream(apkFile).use { apkIn ->
                    session.openWrite("shenhan_base_apk", 0, apkFile.length()).use { sessionOut ->
                        apkIn.copyTo(sessionOut)
                        session.fsync(sessionOut)
                    }
                }

                val statusIntent = Intent(context, UpdateInstallStatusReceiver::class.java)
                val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val statusPendingIntent = PendingIntent.getBroadcast(context, sessionId, statusIntent, piFlags)
                session.commit(statusPendingIntent.intentSender)
                session.close()
                pendingApkFile = null
            } else {
                InAppAutoUpdater.markPendingRelaunch(context, "", false)
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
                isInstalling = false
            }
        } catch (e: Exception) {
            isInstalling = false
            InAppAutoUpdater.markPendingRelaunch(context, "", false)
            errorMessage = "เปิดหน้าติดตั้งไม่สำเร็จ: ${e.message}"
        }
    }

    DisposableEffect(lifecycleOwner, pendingApkFile) {
        val observer = LifecycleEventObserver { _, event ->
            val queued = pendingApkFile
            val relName = availableRelease?.releaseName ?: "เวอร์ชันใหม่"
            if (event == Lifecycle.Event.ON_RESUME && queued != null && queued.exists()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    context.packageManager.canRequestPackageInstalls()
                ) {
                    executeInstall(queued, relName)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val found = checkLatestGithubRelease(context)
        if (found != null) {
            availableRelease = found
        }
    }

    val release = availableRelease ?: return
    val isBusy = isDownloading || isInstalling

    AlertDialog(
        onDismissRequest = {
            if (!isBusy) {
                availableRelease = null
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (isInstalling) "⚙️ กำลังติดตั้งอัปเดต..." else "🚀 พบอัปเดตเวอร์ชันใหม่!",
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

                if (isInstalling) {
                    LinearProgressIndicator(
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "กำลังติดตั้งลงในเครื่อง แอปจะปิดตัวลงชั่วคราวและจะเด้งเปิดกลับมาอัตโนมัติทันทีที่ติดตั้งเสร็จสิ้นครับ",
                        color = Color(0xFF7DD3FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "กำลังดาวน์โหลดไฟล์ติดตั้ง... $downloadedMbText (${(downloadProgress * 100).toInt()}%)",
                        color = Color(0xFF7DD3FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .clickable {
                                val nextVal = !autoInstallConsent
                                autoInstallConsent = nextVal
                                InAppAutoUpdater.setAutoInstallConsented(context, nextVal)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = autoInstallConsent,
                            onCheckedChange = { checked ->
                                autoInstallConsent = checked
                                InAppAutoUpdater.setAutoInstallConsented(context, checked)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF2563EB),
                                uncheckedColor = Color(0xFF94A3B8),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "อนุญาตให้ติดตั้งและเปิดแอปกลับอัตโนมัติ (จำค่านี้ไว้ทุกครั้ง)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = if (autoInstallConsent) {
                            "กด 'อัปเดตเลย' ระบบจะโหลดและติดตั้งให้เอง แอปจะปิดชั่วคราวแล้วเด้งเปิดกลับมาเมื่อเสร็จครับ"
                        } else {
                            "กด 'อัปเดตเลย' เพื่อดาวน์โหลดไฟล์ จากนั้นกดยืนยันติดตั้งด้วยตนเอง"
                        },
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
                enabled = !isBusy,
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
                            executeInstall(apkFile, release.releaseName)
                        }.onFailure { err ->
                            errorMessage = err.message ?: "ดาวน์โหลดไม่สำเร็จ"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        isInstalling -> "กำลังติดตั้ง..."
                        isDownloading -> "กำลังโหลด..."
                        else -> "อัปเดตเลย"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!isBusy) {
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
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
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
        val outFile = File(updatesDir, "shenhan-update.apk")
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
