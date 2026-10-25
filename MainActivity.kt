package com.ahmed.htmltopdf

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private lateinit var ui: WebView
    private var printer: WebView? = null

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            ui.evaluateJavascript("setCode(${JSONObject.quote(text)})", null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = WebView(this)
        ui.settings.javaScriptEnabled = true
        ui.settings.domStorageEnabled = true
        ui.addJavascriptInterface(Bridge(), "Android")
        ui.loadUrl("file:///android_asset/index.html")
        setContentView(ui)
    }

    inner class Bridge {
        @JavascriptInterface fun pickFile() { runOnUiThread { picker.launch("*/*") } }
        @JavascriptInterface fun exportPdf(html: String, size: String, name: String) {
            runOnUiThread { startPrint(html, size, name) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startPrint(html: String, size: String, name: String) {
        Toast.makeText(this, "جاري التجهيز…", Toast.LENGTH_SHORT).show()
        val w = WebView(this)
        w.settings.javaScriptEnabled = true
        w.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        printer = w
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // ننتظر الخطوط والمعادلات تتحمل قبل الطباعة
                Handler(Looper.getMainLooper()).postDelayed({
                    val media = when (size) {
                        "a4l" -> PrintAttributes.MediaSize.ISO_A4.asLandscape()
                        "a4p" -> PrintAttributes.MediaSize.ISO_A4.asPortrait()
                        else -> PrintAttributes.MediaSize("slide", "Slide 16:9", 13333, 7500)
                    }
                    val attrs = PrintAttributes.Builder()
                        .setMediaSize(media)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager
                    pm.print(name, view.createPrintDocumentAdapter(name), attrs)
                }, 2500)
            }
        }
        w.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (ui.canGoBack()) ui.goBack() else super.onBackPressed()
    }
}
