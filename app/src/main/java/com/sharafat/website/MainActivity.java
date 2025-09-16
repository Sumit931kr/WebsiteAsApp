package com.sharafat.website;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String TAG = "MainActivity";
    private String mainDomain;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable fullscreen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Hide system bars for immersive fullscreen
        hideSystemBars();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        webView = findViewById(R.id.webView);
        setupWebViewSettings();
        
        // Extract domain for ad blocking purposes
        String url = getString(R.string.website_url);
        try {
            Uri uri = Uri.parse(url);
            mainDomain = uri.getHost();
            Log.d(TAG, "Main domain: " + mainDomain);
        } catch (Exception e) {
            mainDomain = "freemovieswatch-cc.lol";
            Log.e(TAG, "Error parsing URL: " + e.getMessage());
        }

        // Restore WebView state if available
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(url);
        }

        ProgressBar progressBar = findViewById(R.id.showProgress);
        ImageView imageView = findViewById(R.id.imageView);
        TextView textView = findViewById(R.id.textView);
        ConstraintLayout constraintLayout = findViewById(R.id.constraint);

        imageView.setVisibility(ImageView.VISIBLE);
        textView.setVisibility(TextView.VISIBLE);

        webView.setWebViewClient(new AdBlockWebViewClient(progressBar, imageView, textView, constraintLayout));
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the WebView state
        webView.saveState(outState);
        Log.d(TAG, "WebView state saved");
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Configuration changed: " + 
              (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? "Landscape" : "Portrait"));
        
        // Maintain WebView state during orientation changes
        // The WebView should automatically handle this with state saving enabled
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebViewSettings() {
        WebSettings webSettings = webView.getSettings();
        
        // Essential settings
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        // Performance enhancements
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        
        // Block redirects and popups
        webSettings.setSupportMultipleWindows(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        
        // Handle media better
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        
        // Additional settings for performance
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        
        // Set zoom controls
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
    }
    
    // Inner class to handle ad blocking
    private class AdBlockWebViewClient extends WebViewClient {
        private final ProgressBar progressBar;
        private final ImageView imageView;
        private final TextView textView;
        private final ConstraintLayout constraintLayout;
        private final String[] AD_HOSTS = {
            "google-analytics", "googleads", "googlesyndication", "googletagmanager",
            "facebook.net", "facebook.com/tr", "adservice", "analytics", "doubleclick",
            "cloudfront.net", "effectivemeasure", "scorecardresearch", "amazon-adsystem",
            "ad", "ads", "banner", "pop", "stat", "track", "click", "log", "affiliate", "promo",
            "count", "analytic", "pixel", "monitor", "mediaad", "advert", "tracking", "prebid",
            "imasdk.googleapis.com", "moatads", "outbrain", "taboola", "gemini", "adnxs",
            "bidswitch", "revcontent", "pubmatic", "serving-sys", "openx", "criteo", "yieldmo"
        };
        
        public AdBlockWebViewClient(ProgressBar progressBar, ImageView imageView, TextView textView, ConstraintLayout constraintLayout) {
            this.progressBar = progressBar;
            this.imageView = imageView;
            this.textView = textView;
            this.constraintLayout = constraintLayout;
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                
                // If it's the main domain or its subdomain, load in WebView
                if (host != null && (host.equals(mainDomain) || host.endsWith("." + mainDomain))) {
                    return false; // Let WebView handle it
                }
                
                // Check for ad domains and block
                for (String adHost : AD_HOSTS) {
                    if (host != null && host.contains(adHost)) {
                        Log.d(TAG, "Blocked ad URL: " + url);
                        return true; // Block the request
                    }
                }
                
                // Handle potential redirect or popup
                if (url.contains("http") && !url.contains(mainDomain)) {
                    Log.d(TAG, "Blocked redirect to: " + url);
                    Toast.makeText(MainActivity.this, "Blocked redirect to: " + host, Toast.LENGTH_SHORT).show();
                    return true; // Block the request
                }
                
                // Handle external URLs
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                view.getContext().startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in shouldOverrideUrlLoading: " + e.getMessage());
                return false;
            }
        }
        
        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Uri uri = request.getUrl();
            String host = uri.getHost();
            
            if (host != null) {
                // Block common ad resources
                for (String adHost : AD_HOSTS) {
                    if (host.contains(adHost)) {
                        Log.d(TAG, "Intercepted and blocked ad request: " + url);
                        return new WebResourceResponse("text/plain", "utf-8", null);
                    }
                }
                
                // Block common ad file types and scripts
                if (url.endsWith(".js") && containsAdKeyword(url)) {
                    Log.d(TAG, "Blocked ad script: " + url);
                    return new WebResourceResponse("text/plain", "utf-8", null);
                }
            }
            
            return super.shouldInterceptRequest(view, request);
        }
        
        private boolean containsAdKeyword(String url) {
            String urlLower = url.toLowerCase();
            String[] adKeywords = {"ad", "ads", "analytics", "track", "pixel", "banner", 
                                 "pop", "stat", "click", "promo", "log", "google"};
            
            for (String keyword : adKeywords) {
                if (urlLower.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            super.onPageStarted(view, url, favicon);
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(ProgressBar.GONE);
            imageView.setVisibility(ImageView.GONE);
            textView.setVisibility(TextView.GONE);
            constraintLayout.setVisibility(ConstraintLayout.GONE);
            
            // Inject CSS to hide any remaining ads
            String css = "javascript:(function() {" +
                         "var elements = document.querySelectorAll('div[id*=\\'ad\\'], div[class*=\\'ad\\'], " + 
                         "iframe, ins, .banner, [class*=\\'banner\\'], [id*=\\'banner\\'], " +
                         "[class*=\\'popup\\'], [id*=\\'popup\\'], [class*=\\'sponsor\\'], " +
                         "[class*=\\'modal\\'], .modal, .overlay, #overlay, .ads-popup')" +
                         "for(var i=0; i<elements.length; i++) {" +
                         "   elements[i].style.display='none';" +
                         "}" +
                         "})()";
            view.evaluateJavascript(css, null);
            
            super.onPageFinished(view, url);
        }
    }
    
    private void hideSystemBars() {
        WindowInsetsControllerCompat windowInsetsController =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController == null) {
            return;
        }
        // Configure the behavior of the hidden system bars
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }
}
