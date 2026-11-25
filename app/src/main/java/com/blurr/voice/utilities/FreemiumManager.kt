package com.blurr.voice.utilities

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import com.blurr.voice.MyApplication
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

class FreemiumManager {

    private val db: FirebaseFirestore?
        get() = FirebaseHelper.firestoreOrNull()
    private val auth: FirebaseAuth?
        get() = FirebaseHelper.authOrNull()
    private val billingClient: BillingClient = MyApplication.billingClient

    companion object {
        const val DAILY_TASK_LIMIT = 15 // Set your daily task limit here
        private const val PRO_SKU = "pro" // The SKU for the pro subscription
    }

    fun isBillingUnavailable(): Boolean {
        return MyApplication.billingStatus.value == MyApplication.BillingStatus.UNAVAILABLE
    }

    suspend fun getDeveloperMessage(): String {
        val firestore = db ?: return ""
        return try {
            val document = firestore.collection("settings").document("freemium").get().await()
            document.getString("developerMessage") ?: ""
        } catch (e: Exception) {
            Log.e("FreemiumManager", "Error fetching developer message from Firestore.", e)
            ""
        }
    }
    suspend fun isUserSubscribed(): Boolean {
        if (isBillingUnavailable()) return false
        val firestore = db ?: return false
        val currentUser = auth?.currentUser ?: return false // If not logged in, not pro
        return try {
            val document = firestore.collection("users").document(currentUser.uid).get().await()
            if (document.exists()) {
                val plan = document.getString("plan")
                plan == "pro"
            } else {
                false // Document doesn't exist, so user can't be pro
            }
        } catch (e: Exception) {
            Logger.e("FreemiumManager", "Error checking user plan from Firestore", e)
            false // In case of error, default to not pro
        }
    }



    suspend fun provisionUserIfNeeded() {
        val firestore = db ?: return
        val currentUser = auth?.currentUser ?: return
        val userDocRef = firestore.collection("users").document(currentUser.uid)

        try {
            val document = userDocRef.get().await()
            if (!document.exists()) {
                Logger.d("FreemiumManager", "Provisioning new user: ${currentUser.uid}")
                val newUser = hashMapOf(
                    "email" to currentUser.email,
                    "plan" to "free",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(newUser).await()
            }
        } catch (e: Exception) {
            Logger.e("FreemiumManager", "Error provisioning user", e)
        }
    }

    suspend fun getTasksRemaining(): Long? {
        if (isBillingUnavailable() || isUserSubscribed()) return Long.MAX_VALUE
        val firestore = db ?: return Long.MAX_VALUE
        val currentUser = auth?.currentUser ?: return Long.MAX_VALUE
        return try {
            val document = firestore.collection("users").document(currentUser.uid).get().await()
            document.getLong("tasksRemaining")
        } catch (e: Exception) {
            Logger.e("FreemiumManager", "Error fetching tasks remaining", e)
            null
        }
    }

    suspend fun canPerformTask(): Boolean {
        if (isBillingUnavailable() || isUserSubscribed()) return true
        val firestore = db ?: return true
        val currentUser = auth?.currentUser ?: return false

        return try {
            val document = firestore.collection("users").document(currentUser.uid).get().await()
            val tasksRemaining = document.getLong("tasksRemaining") ?: 0
            Logger.d("FreemiumManager", "User has $tasksRemaining tasks remaining today.")
            tasksRemaining > 0
        } catch (e: Exception) {
            Logger.e("FreemiumManager", "Error fetching user task count", e)
            false
        }
    }

    suspend fun decrementTaskCount() {
        if (isBillingUnavailable() || isUserSubscribed()) return
        val firestore = db ?: return
        val currentUser = auth?.currentUser ?: return

        val userDocRef = firestore.collection("users").document(currentUser.uid)
        
        try {
            userDocRef.update("tasksRemaining", FieldValue.increment(-1)).await()
            Logger.d("FreemiumManager", "Successfully decremented task count for user ${currentUser.uid}.")
        } catch (e: Exception) {
            Logger.e("FreemiumManager", "Failed to decrement task count.", e)
        }
    }
}