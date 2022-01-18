package com.ssc.seedsavers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.qualifiedName
    private lateinit var webView: WebView
    lateinit var progressBar: ProgressBar
    lateinit var root:View
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var url:String
    private lateinit var context: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectAll() // Checks for all violations
                .penaltyLog() // Output violations via logging
                .build()
        )

        setContentView(R.layout.activity_main)
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        analytics = FirebaseAnalytics.getInstance(this)
        webView = findViewById(R.id.webview)
        root = webView.rootView
        webView.settings.javaScriptEnabled=true
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.userAgentString="Seed Savers Club"
        context=this
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if(url?.startsWith("http") == true) {
                    view?.loadUrl(url.toString())
                    return true
                }else{
                    return false
                }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().acceptCookie()
                CookieManager.getInstance().flush()
            }
            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                progressBar.visibility = View.GONE
                root.setBackgroundColor(Color.WHITE)
            }
        }
        var uri: Uri?
        fun isDynamicLink():Boolean{
            return intent.hasExtra("com.google.firebase.dynamiclinks.DYNAMIC_LINK_DATA")||intent.data.toString().contains("page.link")
        }
        if (isDynamicLink()){
            Firebase.dynamicLinks
                .getDynamicLink(intent)
                .addOnSuccessListener(this) { pendingDynamicLinkData ->
                    // Get deep link from result (may be null if no link is found)
                    uri = pendingDynamicLinkData.link
                    url = uri?.toString() ?: "https://app.seedsaversclub.com"
                    webView.loadUrl(url)
                }
                .addOnFailureListener(this) { e -> Log.w(TAG, "getDynamicLink:onFailure", e) }
        }else if(intent.hasExtra("url")){
            webView.loadUrl(intent.extras?.getString("url")?:"https://app.seedsaversclub.com")
        }else{
            uri = intent.data
            url = uri?.toString() ?: "https://app.seedsaversclub.com"
            webView.loadUrl(url)
        }
        createNotificationChannel()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event)
    }
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("SSC", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
