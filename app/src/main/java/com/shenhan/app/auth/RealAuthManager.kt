package com.shenhan.app.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.shenhan.app.model.AuthProvider
import com.shenhan.app.model.HskLevel
import com.shenhan.app.model.UserProfile
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RealAuthManager {
    const val FIREBASE_WEB_CLIENT_ID = "461634645448-j40vrgt7n3o1pu9coo51sn8r0ojvisrv.apps.googleusercontent.com"

    var SUPABASE_URL = "https://YOUR_PROJECT_REF.supabase.co"
    var SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"
    const val REDIRECT_URI = "shenhan://auth/callback"
    var pendingDeepLinkUri: Uri? = null

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cont.resume(task.result)
                } else {
                    cont.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase authentication failed")
                    )
                }
            }
        }

    suspend fun signInWithGoogle(
        context: Context,
        initialHskLevel: HskLevel,
        dailyGoalMinutes: Int
    ): Result<UserProfile> {
        val credentialManager = CredentialManager.create(context)

        // ชั้นที่ 1: ลองเรียก Google One-Tap Credential
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(FIREBASE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(context = context, request = request)
            return handleGoogleCredential(result.credential, initialHskLevel, dailyGoalMinutes)
        } catch (e: GetCredentialCancellationException) {
            return Result.failure(IllegalStateException("ยกเลิกการเลือกบัญชี Google"))
        } catch (_: Exception) {
            // ข้ามไปชั้นที่ 2 ทันทีหาก One-Tap ไม่มี credential พร้อมใช้
        }

        // ชั้นที่ 2: บังคับเด้งหน้าต่างเลือกบัญชี Google ในเครื่องเต็มรูปแบบ (Sign in with Google bottom sheet)
        try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(FIREBASE_WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()
            val result = credentialManager.getCredential(context = context, request = request)
            return handleGoogleCredential(result.credential, initialHskLevel, dailyGoalMinutes)
        } catch (e: GetCredentialCancellationException) {
            return Result.failure(IllegalStateException("ยกเลิกการเลือกบัญชี Google"))
        } catch (e: NoCredentialException) {
            // ชั้นที่ 3: เปิดหน้าล็อกอิน Google OAuth ผ่าน Firebase โดยตรง (ใช้ได้แม้ Play Services ยังไม่อัปเดต SHA-1)
            return signInWithGoogleWebFallback(context, initialHskLevel, dailyGoalMinutes)
        } catch (e: Exception) {
            return signInWithGoogleWebFallback(context, initialHskLevel, dailyGoalMinutes)
        }
    }

    private suspend fun handleGoogleCredential(
        credential: androidx.credentials.Credential,
        initialHskLevel: HskLevel,
        dailyGoalMinutes: Int
    ): Result<UserProfile> {
        return try {
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCred).awaitResult()
                val fbUser = authResult.user
                    ?: return Result.failure(IllegalStateException("ไม่พบข้อมูลผู้ใช้จาก Firebase"))

                Result.success(
                    UserProfile(
                        id = fbUser.uid,
                        displayName = fbUser.displayName ?: googleIdTokenCredential.displayName ?: "Google Scholar",
                        email = fbUser.email ?: googleIdTokenCredential.id,
                        avatarEmoji = "🌐",
                        provider = AuthProvider.GOOGLE,
                        linkedProviders = listOf(AuthProvider.GOOGLE),
                        startingHskLevel = initialHskLevel,
                        dailyGoalMinutes = dailyGoalMinutes,
                        isGuest = false
                    )
                )
            } else {
                Result.failure(IllegalStateException("รูปแบบข้อมูลรับรอง Google ไม่ถูกต้อง"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun signInWithGoogleWebFallback(
        context: Context,
        initialHskLevel: HskLevel,
        dailyGoalMinutes: Int
    ): Result<UserProfile> {
        return try {
            val activity = context.findActivity()
                ?: return Result.failure(IllegalStateException("ไม่พบหน้าต่าง Activity สำหรับเปิดหน้าล็อกอิน Google"))
            val provider = OAuthProvider.newBuilder("google.com").build()
            val authResult = firebaseAuth.startActivityForSignInWithProvider(activity, provider).awaitResult()
            val fbUser = authResult.user
                ?: return Result.failure(IllegalStateException("ล็อกอิน Google ไม่สำเร็จ"))

            Result.success(
                UserProfile(
                    id = fbUser.uid,
                    displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Google Scholar",
                    email = fbUser.email ?: "",
                    avatarEmoji = "🌐",
                    provider = AuthProvider.GOOGLE,
                    linkedProviders = listOf(AuthProvider.GOOGLE),
                    startingHskLevel = initialHskLevel,
                    dailyGoalMinutes = dailyGoalMinutes,
                    isGuest = false
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("กรุณาเปิด Enable Google ใน Firebase Console -> Authentication (${e.localizedMessage})")
            )
        }
    }

    suspend fun signInWithEmailPassword(
        email: String,
        password: String,
        displayName: String,
        isRegister: Boolean,
        initialHskLevel: HskLevel,
        dailyGoalMinutes: Int
    ): Result<UserProfile> {
        return try {
            val authResult = if (isRegister) {
                firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
            } else {
                firebaseAuth.signInWithEmailAndPassword(email.trim(), password).awaitResult()
            }
            val fbUser = authResult.user
                ?: return Result.failure(IllegalStateException("ยืนยันตัวตนอีเมลไม่สำเร็จ"))

            Result.success(
                UserProfile(
                    id = fbUser.uid,
                    displayName = displayName.ifBlank { fbUser.displayName ?: email.substringBefore("@") },
                    email = fbUser.email ?: email.trim(),
                    avatarEmoji = "✉️",
                    provider = AuthProvider.EMAIL,
                    linkedProviders = listOf(AuthProvider.EMAIL),
                    startingHskLevel = initialHskLevel,
                    dailyGoalMinutes = dailyGoalMinutes,
                    isGuest = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun launchSupabaseOAuth(context: Context, provider: AuthProvider): Result<Unit> {
        if (SUPABASE_URL.contains("YOUR_PROJECT_REF")) {
            return Result.failure(
                IllegalStateException("กรุณาใส่ SUPABASE_URL และ SUPABASE_ANON_KEY ใน RealAuthManager.kt ก่อนใช้งาน ${provider.name}")
            )
        }
        val providerSlug = when (provider) {
            AuthProvider.APPLE -> "apple"
            AuthProvider.LINE -> "line"
            AuthProvider.WECHAT -> "wechat"
            else -> "google"
        }
        val authUrl = Uri.parse("$SUPABASE_URL/auth/v1/authorize")
            .buildUpon()
            .appendQueryParameter("provider", providerSlug)
            .appendQueryParameter("redirect_to", REDIRECT_URI)
            .build()

        val customTabs = CustomTabsIntent.Builder().setShowTitle(true).build()
        customTabs.launchUrl(context, authUrl)
        return Result.success(Unit)
    }

    fun parseSupabaseCallbackUri(
        uri: Uri,
        initialHskLevel: HskLevel = HskLevel.HSK1,
        dailyGoalMinutes: Int = 15
    ): UserProfile? {
        if (uri.scheme != "shenhan" || uri.host != "auth") return null
        val fragment = uri.fragment ?: uri.query ?: return null
        val params = fragment.split("&").mapNotNull {
            val idx = it.indexOf('=')
            if (idx > 0) it.substring(0, idx) to Uri.decode(it.substring(idx + 1)) else null
        }.toMap()

        val accessToken = params["access_token"] ?: return null
        return try {
            val parts = accessToken.split(".")
            val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
            val json = JSONObject(payloadJson)
            val sub = json.optString("sub", "sb_${System.currentTimeMillis()}")
            val email = json.optString("email", "member@shenhan.app")
            val meta = json.optJSONObject("user_metadata")
            val appMeta = json.optJSONObject("app_metadata")
            val rawProvider = appMeta?.optString("provider", "")?.lowercase() ?: ""
            val mappedProvider = when {
                rawProvider.contains("apple") -> AuthProvider.APPLE
                rawProvider.contains("line") -> AuthProvider.LINE
                rawProvider.contains("wechat") -> AuthProvider.WECHAT
                else -> AuthProvider.LINE
            }
            val name = meta?.optString("full_name")?.takeIf { it.isNotBlank() }
                ?: meta?.optString("name")?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@")

            UserProfile(
                id = sub,
                displayName = name,
                email = email,
                avatarEmoji = when (mappedProvider) {
                    AuthProvider.APPLE -> "🍏"
                    AuthProvider.LINE -> "🟢"
                    AuthProvider.WECHAT -> "💬"
                    else -> "✨"
                },
                provider = mappedProvider,
                linkedProviders = listOf(mappedProvider),
                startingHskLevel = initialHskLevel,
                dailyGoalMinutes = dailyGoalMinutes,
                isGuest = false
            )
        } catch (e: Exception) {
            null
        }
    }
}
