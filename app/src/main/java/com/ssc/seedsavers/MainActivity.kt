package com.ssc.seedsavers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import android.webkit.WebChromeClient
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager

import androidx.core.content.ContextCompat
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ClipData
import java.net.URI


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.qualifiedName
    private lateinit var webView: WebView
    lateinit var progressBar: ProgressBar
    lateinit var root:View
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var url:String
    private lateinit var context: Context
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        webView = findViewById(R.id.webview)
        root = webView.rootView
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                1
            )
        }
        webView.settings.javaScriptEnabled=true
        webView.settings.allowFileAccess=true
        webView.settings.mixedContentMode=0
        webView.settings.domStorageEnabled = true
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        val versionCode = BuildConfig.VERSION_CODE
        webView.settings.userAgentString= "Seed Savers Club-$versionCode"
        context=this
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if(URI(url).getHost()?.contains("seedsaversclub.com") == true) {
                    progressBar.visibility=View.VISIBLE
                    view?.loadUrl(url.toString())
                    return true
                }else{
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))

                    // The following flags launch the app outside the current app

                    // The following flags launch the app outside the current app
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP

                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }else{
                        val text = R.string.no_app
                        val duration = Toast.LENGTH_SHORT

                        val toast = Toast.makeText(applicationContext, text, duration)
                        toast.show()
                    }

                    return true
                }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().acceptCookie()
                CookieManager.getInstance().flush()
            }
            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                webView.visibility=View.VISIBLE
                progressBar.visibility = View.GONE
                root.setBackgroundColor(Color.WHITE)
            }
        }
        webView.webChromeClient = object : WebChromeClient(){
            override fun onShowFileChooser(mWebView:WebView,
                                           filePathCallback:ValueCallback<Array<Uri>>,
                                           fileChooserParams:FileChooserParams):Boolean {
                if (mUploadMessage != null) {
                    mUploadMessage!!.onReceiveValue(null)
                    mUploadMessage = null
                }
                mUploadMessage = filePathCallback
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
                val intent = Intent(Intent.ACTION_CHOOSER)
                intent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                intent.putExtra(Intent.EXTRA_TITLE, "File Chooser")
                try {
                    getFileResultLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    mUploadMessage = null
                    Toast.makeText(getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show()
                    return false
                }
                return true
            }
        }
        var uri: Uri?
        fun isDynamicLink():Boolean{
            return intent.hasExtra("com.google.firebase.dynamiclinks.DYNAMIC_LINK_DATA")||intent.data.toString().contains("page.link")
        }
        when {
            intent.extras==null&&intent.data==null->{webView.loadUrl("https://app.seedsaversclub.com")}
            isDynamicLink() -> {
                Firebase.dynamicLinks
                    .getDynamicLink(intent)
                    .addOnSuccessListener(this) { pendingDynamicLinkData ->
                        // Get deep link from result (may be null if no link is found)
                        uri = pendingDynamicLinkData.link
                        url = uri?.toString() ?: "https://app.seedsaversclub.com"
                        webView.loadUrl(url)
                    }
                    .addOnFailureListener(this) { e -> Log.w(TAG, "getDynamicLink:onFailure", e) }
            }
            intent.hasExtra("url") -> {
                webView.loadUrl(intent.extras?.getString("url")?:"https://app.seedsaversclub.com")
            }
            else -> {
                uri = intent.data
                url = uri?.toString() ?: "https://app.seedsaversclub.com"
                webView.loadUrl(url)
            }
        }
        analytics = FirebaseAnalytics.getInstance(this)
        createNotificationChannel()

    }
    val getFileResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
            ar: ActivityResult ->
        val intent: Intent? = ar.data
        Log.w(TAG,"result from launcher ${intent}")
        val clipData=intent?.clipData
        val result:Array<Uri>?
        if (intent == null || ar.resultCode != RESULT_OK){
            result = null
        }
        else if(clipData!=null){
            fun ClipData.convertToList(): List<Uri> = 0.until(itemCount).map { getItemAt(it).uri }
            result=clipData.convertToList().toTypedArray()
        }else{
            result = arrayOf(Uri.parse(intent.dataString))
        }
        mUploadMessage!!.onReceiveValue(result)
        mUploadMessage = null
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
