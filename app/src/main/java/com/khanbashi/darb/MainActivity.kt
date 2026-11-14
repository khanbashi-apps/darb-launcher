package com.khanbashi.darb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Darb launcher. The whole interface is the web page in assets/web (same as the live prototype);
 * this activity hosts it full screen and gives it native powers through the DarbNative bridge:
 * GPS speed, installed apps, opening apps and maps, and network requests for mp3quran.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private val io = Executors.newFixedThreadPool(4)
    private val main = Handler(Looper.getMainLooper())
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    @Volatile private var appsCache: String? = null

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        fileCallback = null
    }

    private val askLocation =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startLocation() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideBars()

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this)
        web.setBackgroundColor(0xFF000000.toInt())
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (u.host == "appassets.androidplatform.net") return false
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback
                return try {
                    pickFile.launch("image/*")
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }
        }
        web.addJavascriptInterface(Bridge(), "DarbNative")
        setContentView(web)
        web.loadUrl("https://appassets.androidplatform.net/assets/web/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                js("window.onDarbBack&&onDarbBack()")
            }
        })

        askLocation.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        js("window.onDarbHome&&onDarbHome()")
    }

    override fun onResume() {
        super.onResume()
        hideBars()
        appsCache = null
        js("window.onDarbResume&&onDarbResume()")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    override fun onDestroy() {
        runCatching { locationManager?.removeUpdates(locationListener) }
        super.onDestroy()
    }

    private fun hideBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun js(code: String) {
        main.post { if (::web.isInitialized) web.evaluateJavascript(code, null) }
    }

    // ------------------------------------------------------------------ GPS

    private var locationManager: LocationManager? = null

    // All four methods are overridden: on Android 8–10 they are abstract in the platform interface.
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = push(location)
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun startLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager = lm
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener, Looper.getMainLooper())
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 0f, locationListener, Looper.getMainLooper())
            }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun push(l: Location) {
        val kmh = if (l.hasSpeed()) l.speed * 3.6 else 0.0
        val s = String.format(Locale.US, "%.2f,%.1f,%.6f,%.6f", kmh, l.bearing, l.latitude, l.longitude)
        js("window.onDarbLoc&&onDarbLoc($s)")
    }

    // ------------------------------------------------------------------ bridge

    inner class Bridge {
        @JavascriptInterface
        fun version(): String = BuildConfig.VERSION_NAME

        /** Async GET for the page (mp3quran API); result comes back through __darbCb(id, ok, text). */
        @JavascriptInterface
        fun get(url: String, id: String) {
            io.execute {
                val q = JSONObject.quote(id)
                try {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.connectTimeout = 15_000
                    c.readTimeout = 30_000
                    c.setRequestProperty("Accept", "application/json")
                    val text = c.inputStream.bufferedReader().use { it.readText() }
                    c.disconnect()
                    js("__darbCb($q,true,${JSONObject.quote(text)})")
                } catch (e: Exception) {
                    js("__darbCb($q,false,${JSONObject.quote(e.toString())})")
                }
            }
        }

        /** Installed launchable apps: [{l: label, p: package, i: icon data URI}] */
        @JavascriptInterface
        fun apps(): String {
            appsCache?.let { return it }
            val pm = packageManager
            val list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            val arr = JSONArray()
            val seen = HashSet<String>()
            for (ri in list.sortedBy { it.loadLabel(pm).toString().lowercase() }) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || !seen.add(pkg)) continue
                val icon = runCatching {
                    val d = ri.loadIcon(pm)
                    val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                    d.setBounds(0, 0, 96, 96)
                    d.draw(Canvas(bmp))
                    val bo = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, bo)
                    "data:image/png;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP)
                }.getOrDefault("")
                arr.put(JSONObject().put("l", ri.loadLabel(pm).toString()).put("p", pkg).put("i", icon))
            }
            return arr.toString().also { appsCache = it }
        }

        @JavascriptInterface
        fun launch(pkg: String) {
            main.post {
                packageManager.getLaunchIntentForPackage(pkg)?.let {
                    runCatching { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            }
        }

        /** Opens the maps app searching near the driver, e.g. «محطة وقود». */
        @JavascriptInterface
        fun openMap(query: String) {
            main.post {
                val uri = if (query.isBlank()) Uri.parse("geo:0,0") else Uri.parse("geo:0,0?q=" + Uri.encode(query))
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }

        @JavascriptInterface
        fun navigate(lat: Double, lng: Double) {
            main.post {
                val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val ok = runCatching { startActivity(nav) }.isSuccess
                if (!ok) {
                    runCatching {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        }
    }
}
