package com.shenhan.app.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
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
    const val LINE_CHANNEL_ID = "2012749518"
    const val LINE_CALLBACK_URL = "https://murqzekjjrvoabibzjql.supabase.co/functions/v1/line-auth"
    const val FIREBASE_WEB_CLIENT_ID = "461634645448-j40vrgt7n3o1pu9coo51sn8r0ojvisrv.apps.googleusercontent.com"
    private const val FIREBASE_APP_ID = "1:461634645448:android:a79d516c0b80c25dbe24fd"
    private const val FIREBASE_API_KEY = "AIzaSyA_IsZw0SloF0MAXjz9AJbvUvrUAyOjxj0"
    private const val FIREBASE_PROJECT_ID = "loginsingup-8ca9c"
    private const val FIREBASE_STORAGE_BUCKET = "loginsingup-8ca9c.firebasestorage.app"
    private const val FIREBASE_GCM_SENDER_ID = "461634645448"

    var SUPABASE_URL = "https://YOUR_PROJECT_REF.supabase.co"
    var SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"
    const val REDIRECT_URI = "shenhan://auth/callback"
    var pendingDeepLinkUri: Uri? = null

    fun ensureFirebaseInitialized(context: Context): FirebaseApp {
        val appContext = context.applicationContext
        val existing = FirebaseApp.getApps(appContext)
        if (existing.isNotEmpty()) {
            return existing.first()
        }
        FirebaseApp.initializeApp(appContext)?.let { return it }

        val options = FirebaseOptions.Builder()
            .setApplicationId(FIREBASE_APP_ID)
            .setApiKey(FIREBASE_API_KEY)
            .setProjectId(FIREBASE_PROJECT_ID)
            .setStorageBucket(FIREBASE_STORAGE_BUCKET)
            .setGcmSenderId(FIREBASE_GCM_SENDER_ID)
            .build()
        return FirebaseApp.initializeApp(appContext, options)
    }

    private fun getAuth(context: Context? = null): FirebaseAuth {
        if (context != null) {
            ensureFirebaseInitialized(context)
        }
        return FirebaseAuth.getInstance()
    }

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
        ensureFirebaseInitialized(context)
        val activity = context.findActivity()
            ?: return Result.failure(IllegalStateException("ไม่พบหน้าต่าง Activity สำหรับเปิดหน้าล็อกอิน Google"))
        val credentialManager = CredentialManager.create(activity)

        // ชั้นที่ 1: เรียก Google One-Tap Credential ด้วย Activity จริง
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(FIREBASE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(context = activity, request = request)
            return handleGoogleCredential(activity, result.credential, initialHskLevel, dailyGoalMinutes)
        } catch (_: Exception) {
            // ข้ามไปชั้นที่ 2 ทันที
        }

        // ชั้นที่ 2: เรียกหน้าต่างเลือกบัญชี Google เต็มรูปแบบด้วย Activity จริง
        try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(FIREBASE_WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()
            val result = credentialManager.getCredential(context = activity, request = request)
            return handleGoogleCredential(activity, result.credential, initialHskLevel, dailyGoalMinutes)
        } catch (_: GetCredentialCancellationException) {
            return signInWithGoogleWebFallback(activity, initialHskLevel, dailyGoalMinutes)
        } catch (_: GetCredentialException) {
            return signInWithGoogleWebFallback(activity, initialHskLevel, dailyGoalMinutes)
        } catch (_: Exception) {
            return signInWithGoogleWebFallback(activity, initialHskLevel, dailyGoalMinutes)
        }
    }

    private suspend fun handleGoogleCredential(
        context: Context,
        credential: Credential,
        initialHskLevel: HskLevel,
        dailyGoalMinutes: Int
    ): Result<UserProfile> {
        return try {
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = getAuth(context).signInWithCredential(firebaseCred).awaitResult()
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
        activity: Activity,
        initialHskLevel: HskLevel,
        dailyGoalMinutes: Int
    ): Result<UserProfile> {
        return try {
            val provider = OAuthProvider.newBuilder("google.com").build()
            val authResult = getAuth(activity).startActivityForSignInWithProvider(activity, provider).awaitResult()
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
                IllegalStateException("Google Sign-In: ${e.localizedMessage ?: "กรุณาเช็กการเปิดใช้ Google ใน Firebase Authentication"}")
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
            val auth = FirebaseAuth.getInstance()
            val authResult = if (isRegister) {
                auth.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
            } else {
                auth.signInWithEmailAndPassword(email.trim(), password).awaitResult()
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
        val authUrl = if (provider == AuthProvider.LINE) {
            Uri.parse("https://access.line.me/oauth2/v2.1/authorize")
                .buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", LINE_CHANNEL_ID)
                .appendQueryParameter("redirect_uri", LINE_CALLBACK_URL)
                .appendQueryParameter("state", "shenhan_${System.currentTimeMillis()}")
                .appendQueryParameter("scope", "profile openid email")
                .build()
        } else {
            Uri.parse("$SUPABASE_URL/auth/v1/authorize")
                .buildUpon()
                .appendQueryParameter("provider", providerSlug)
                .appendQueryParameter("redirect_to", REDIRECT_URI)
                .build()
        }

        val customTabs = CustomTabsIntent.Builder().setShowTitle(true).build()
        customTabs.launchUrl(context, authUrl)
        return Result.success(Unit)
    }

    suspend fun completeFirebaseCustomTokenCallback(
        uri: Uri,
        initialHskLevel: HskLevel = HskLevel.HSK1,
        dailyGoalMinutes: Int = 15
    ): Result<UserProfile>? {
        if (uri.scheme != "shenhan" || uri.host != "auth") return null
        val firebaseToken = uri.getQueryParameter("firebase_token") ?: return null
        val displayName = uri.getQueryParameter("name") ?: "LINE Scholar"
        return try {
            val authResult = firebaseAuth.signInWithCustomToken(firebaseToken).awaitResult()
            val fbUser = authResult.user
                ?: return Result.failure(IllegalStateException("ยืนยันตัวตน LINE ผ่าน Firebase ไม่สำเร็จ"))
            Result.success(
                UserProfile(
                    id = fbUser.uid,
                    displayName = displayName,
                    email = fbUser.email ?: "${fbUser.uid}@line.shenhan.app",
                    avatarEmoji = "🟢",
                    provider = AuthProvider.LINE,
                    linkedProviders = listOf(AuthProvider.LINE),
                    startingHskLevel = initialHskLevel,
                    dailyGoalMinutes = dailyGoalMinutes,
                    isGuest = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
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
