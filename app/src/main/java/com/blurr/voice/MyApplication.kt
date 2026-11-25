package com.blurr.voice

import android.app.Application
import android.content.Context
import android.content.Intent
import com.blurr.voice.utilities.FirebaseHelper
import com.blurr.voice.utilities.Logger
import com.android.billingclient.api.*
import com.blurr.voice.intents.IntentRegistry
import com.blurr.voice.intents.impl.DialIntent
import com.blurr.voice.intents.impl.EmailComposeIntent
import com.blurr.voice.intents.impl.ShareTextIntent
import com.blurr.voice.intents.impl.ViewUrlIntent
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow

class MyApplication : Application(), PurchasesUpdatedListener {

    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val initialReconnectDelayMs = 1000L

    enum class BillingStatus {
        CONNECTING,
        AVAILABLE,
        UNAVAILABLE
    }

    companion object {
        lateinit var appContext: Context
            private set

        lateinit var billingClient: BillingClient
            private set

        private val _billingStatus = MutableStateFlow(BillingStatus.CONNECTING)
        val billingStatus: StateFlow<BillingStatus> = _billingStatus.asStateFlow()

        private val _isBillingClientReady = MutableStateFlow(false)
        val isBillingClientReady: StateFlow<Boolean> = _isBillingClientReady.asStateFlow()

        val isBillingAvailable: Boolean
            get() = _billingStatus.value == BillingStatus.AVAILABLE
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Initialize Firebase Remote Config only if google-services.json configured Firebase
        if (FirebaseHelper.isConfigured(this)) {
            val remoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 1L else 3L
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        } else {
            Logger.w("MyApplication", "Firebase is not configured (google-services.json missing); running in local mode.")
        }


        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        connectToBillingService()

        IntentRegistry.register(DialIntent())
        IntentRegistry.register(ViewUrlIntent())
        IntentRegistry.register(ShareTextIntent())
        IntentRegistry.register(EmailComposeIntent())
        IntentRegistry.init(this)
    }

    private fun disableBillingCleanly(reason: String) {
        Logger.w("MyApplication", "$reason Disabling Play Billing and enabling unlimited local tasks.")
        _isBillingClientReady.value = false
        _billingStatus.value = BillingStatus.UNAVAILABLE
        try {
            billingClient.endConnection()
        } catch (_: Exception) {
        }
    }

    private fun connectToBillingService() {
        if (_billingStatus.value == BillingStatus.UNAVAILABLE) {
            return
        }
        if (billingClient.isReady) {
            Logger.d("MyApplication", "BillingClient is already connected.")
            _isBillingClientReady.value = true
            _billingStatus.value = BillingStatus.AVAILABLE
            return
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            Logger.d("MyApplication", "BillingClient setup successfully.")
                            _isBillingClientReady.value = true
                            _billingStatus.value = BillingStatus.AVAILABLE
                            reconnectAttempts = 0
                        }
                        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
                        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                            disableBillingCleanly(
                                "Play Billing unavailable on device (${billingResult.responseCode}: ${billingResult.debugMessage})."
                            )
                        }
                        else -> {
                            Logger.w(
                                "MyApplication",
                                "BillingClient transient setup failure (${billingResult.responseCode}): ${billingResult.debugMessage}"
                            )
                            _isBillingClientReady.value = false
                            retryConnectionWithBackoff()
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (_billingStatus.value == BillingStatus.UNAVAILABLE) {
                        return
                    }
                    Logger.w("MyApplication", "Billing service disconnected. Retrying...")
                    _isBillingClientReady.value = false
                    retryConnectionWithBackoff()
                }
            })
        } catch (e: Exception) {
            disableBillingCleanly("Play Billing initialization failed: ${e.message}")
        }
    }

    private fun retryConnectionWithBackoff() {
        if (_billingStatus.value == BillingStatus.UNAVAILABLE) {
            return
        }
        if (reconnectAttempts < maxReconnectAttempts) {
            val delay = initialReconnectDelayMs * (2.0.pow(reconnectAttempts)).toLong()
            applicationScope.launch {
                delay(delay)
                if (_billingStatus.value != BillingStatus.UNAVAILABLE) {
                    reconnectAttempts++
                    Logger.d("MyApplication", "Retrying connection, attempt #$reconnectAttempts")
                    connectToBillingService()
                }
            }
        } else {
            disableBillingCleanly("Max billing reconnect attempts reached.")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Logger.d("MyApplication", "Purchase update received")
        val intent = Intent("com.blurr.voice.PURCHASE_UPDATED")
        intent.putExtra("response_code", billingResult.responseCode)
        intent.putExtra("debug_message", billingResult.debugMessage)
        appContext.sendBroadcast(intent)
    }
}