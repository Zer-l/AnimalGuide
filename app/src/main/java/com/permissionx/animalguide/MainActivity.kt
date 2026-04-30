package com.permissionx.animalguide

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.permissionx.animalguide.ui.navigation.AppNavGraph
import com.permissionx.animalguide.ui.privacy.PrivacyConsentManager
import com.permissionx.animalguide.ui.privacy.PrivacyDialog
import com.permissionx.animalguide.ui.theme.AnimalGuideTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var privacyConsentManager: PrivacyConsentManager

    private var agreementWebView: WebView? = null
    private var privacyWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        actionBar?.hide()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // SplashScreen 显示期间预热 WebView（仅首次启动需要）
        if (!privacyConsentManager.hasAgreed) {
            agreementWebView = WebView(this).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                loadUrl("file:///android_asset/user_agreement.html")
            }
            privacyWebView = WebView(this).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                loadUrl("file:///android_asset/privacy_policy.html")
            }
        }

        setContent {
            AnimalGuideTheme(darkTheme = isSystemInDarkTheme()) {
                val navController = rememberNavController()
                var showPrivacyDialog by remember {
                    mutableStateOf(!privacyConsentManager.hasAgreed)
                }

                AppNavGraph(navController = navController)

                if (showPrivacyDialog) {
                    PrivacyDialog(
                        onAgree = {
                            privacyConsentManager.setAgreed()
                            showPrivacyDialog = false
                            agreementWebView?.destroy(); agreementWebView = null
                            privacyWebView?.destroy(); privacyWebView = null
                        },
                        onDecline = { finish() },
                        agreementWebView = agreementWebView,
                        privacyWebView = privacyWebView
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agreementWebView?.destroy(); agreementWebView = null
        privacyWebView?.destroy(); privacyWebView = null
    }
}
