package com.blurr.voice.utilities

import android.content.Context
import com.blurr.voice.MyApplication
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig

object FirebaseHelper {
    fun isConfigured(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun isInitialized(): Boolean {
        return try {
            FirebaseApp.getApps(MyApplication.appContext).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun authOrNull(): FirebaseAuth? = if (isInitialized()) Firebase.auth else null
    fun firestoreOrNull(): FirebaseFirestore? = if (isInitialized()) Firebase.firestore else null
    fun remoteConfigOrNull(): FirebaseRemoteConfig? = if (isInitialized()) Firebase.remoteConfig else null
    fun analyticsOrNull(): FirebaseAnalytics? = if (isInitialized()) Firebase.analytics else null
    fun functionsOrNull(region: String = "us-central1"): FirebaseFunctions? =
        if (isInitialized()) Firebase.functions(region) else null
}
