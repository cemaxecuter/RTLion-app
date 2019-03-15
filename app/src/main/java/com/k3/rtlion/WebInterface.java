package com.k3.rtlion;

import android.app.Activity;
import android.content.Context;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebInterface {

    private Activity activity;
    private Context context;
    private WebView webView;
    private String jsInterfaceName = "JSInterface";

    public WebInterface(Activity activity, WebView webView){
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.webView = webView;
    }
    private WebView getWebView(){
        return webView;
    }
    public void fetchPage(String url){
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(url);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {

            }
        });
    }
    private class JSInterface {
    }
}
