package com.k3.rtlion.handler;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.k3.rtlion.R;

import org.json.JSONException;
import org.json.JSONObject;

public class JSInterface {

    private Activity activity;
    private Context context;
    private WebView webView;
    private String jsInterfaceName;
    private int conTimeout = 9000;
    private Object[] globalParams;
    private JSONObject clientInfo, cliArgs;
    private String graphEventCommand,
            globalSocketName = "socket";
    private Boolean graphEventSet = false,
            scanEventSet = false,
            infoRecv = false;

    public enum JSCommands {
        ServerInfo("fetchServerInfo", "getClientInfo"),
        CliArgs("fetchCliArgs", "getCliArgs"),
        SetArgs("updateServerArgs", "setCliArgs"),
        GraphFFT("getGraphFromServer", "getGraph"),
        Scanner("getScanDataFromServer", "getScannedValues");
        private String serverCmd, clientCmd;
        JSCommands(String serverCmd, String clientCmd) {
            this.serverCmd = serverCmd;
            this.clientCmd = clientCmd;
        }
        public String getServerCmd() { return serverCmd; }
        public String getClientCmd() { return clientCmd; }
    }
    public interface JSOutputInterface {
        public void onInfo(JSONObject clientInfo);
        public void onArgs(JSONObject cliArgs);
        public void onConsoleMsg(ConsoleMessage msg);
        public void onData(String data);
    }
    private JSOutputInterface jsOutputInterface;
    public JSInterface(Activity activity){
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }
    public void initialize(WebView webView){
        this.webView = webView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        jsInterfaceName = this.getClass().getSimpleName();
        webView.addJavascriptInterface(this, jsInterfaceName);
        webView.setWebViewClient(new webView_client());
        webView.setWebChromeClient(new webView_chromeClient());
    }
    private class webView_client extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, final String url, Bitmap favicon) {
            if(url.split("#")[1].equals(JSCommands.ServerInfo.name())){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(conTimeout);
                            if(jsOutputInterface != null && !infoRecv)
                                jsOutputInterface.onConsoleMsg(new ConsoleMessage(context.
                                        getString(R.string.server_unreachable), "1", 1,
                                        ConsoleMessage.MessageLevel.DEBUG));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            String jsCommand = createJSCommand(JSCommands.valueOf(url.split("#")[1]).ordinal(),
                    globalParams);
            webView.loadUrl(jsCommand);
            if(url.split("#")[1].equals(JSCommands.GraphFFT.name()) && !graphEventSet) {
                webView.loadUrl(graphEventCommand);
                graphEventSet = true;
            }else if(url.split("#")[1].equals(JSCommands.Scanner.name()) && !scanEventSet) {
                webView.loadUrl(graphEventCommand);
                scanEventSet = true;
            }
        }
    }
    private class webView_chromeClient extends WebChromeClient{
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if(jsOutputInterface != null){
                jsOutputInterface.onConsoleMsg(consoleMessage);
            }
            return super.onConsoleMessage(consoleMessage);
        }
    }
    private String createJSCommand(int index, Object[] params){
        StringBuilder jsCommand = new StringBuilder();
        String jsHead = "javascript:",
                serverCmd = JSCommands.values()[index].getServerCmd(),
                clientCmd = JSCommands.values()[index].getClientCmd();
        jsCommand.append(jsHead);
        jsCommand.append(jsInterfaceName);
        jsCommand.append(".");
        jsCommand.append(serverCmd);
        jsCommand.append("(");
        jsCommand.append(clientCmd);
        jsCommand.append("(");
        if (params != null){
            for (int i = 0; i < params.length; i++){
                jsCommand.append("\'");
                jsCommand.append(String.valueOf(params[i]));
                jsCommand.append("\'");
                if (i != params.length - 1)
                    jsCommand.append(",");
            }
        }
        jsCommand.append("));");
        return jsCommand.toString();
    }
    public void getServerInfo(String url, JSOutputInterface jsOutputInterface){
        this.jsOutputInterface = jsOutputInterface;
        webView.loadUrl(url + "#" + JSCommands.ServerInfo.name());
        globalParams = null;
        infoRecv = false;
    }
    public void getServerArgs(String url, JSOutputInterface jsOutputInterface){
        this.jsOutputInterface = jsOutputInterface;
        webView.loadUrl(url + "#" + JSCommands.CliArgs.name());
        globalParams = null;
    }
    public void setServerArgs(String url, String params, JSOutputInterface jsOutputInterface){
        this.jsOutputInterface = jsOutputInterface;
        webView.loadUrl(url + "#" + JSCommands.SetArgs.name());
        globalParams = new Object[]{params};
    }
    public void getGraphFFT(String url, JSOutputInterface jsOutputInterface){
        this.jsOutputInterface = jsOutputInterface;
        webView.loadUrl(url + "#" + JSCommands.GraphFFT.name());
        graphEventCommand = "javascript:"+globalSocketName+".on('fft_data', function(msg) {"
                + jsInterfaceName + ".onServerGraph(msg.data);});";
        globalParams = null;
    }
    public void getScannedValues(String url, String params, JSOutputInterface jsOutputInterface){
        this.jsOutputInterface = jsOutputInterface;
        webView.loadUrl(url + "#" + JSCommands.Scanner.name());
        graphEventCommand = "javascript:"+globalSocketName+".on('graph_data', function(data) {"
                + jsInterfaceName + ".onServerGraph(data);});";
        globalParams = new Object[]{params};
    }
    @JavascriptInterface
    public void fetchServerInfo(String info){
        infoRecv = true;
        clientInfo = null;
        try {
            clientInfo = new JSONObject(info);
            jsOutputInterface.onInfo(clientInfo);
        }catch (JSONException e){
            e.printStackTrace();
            jsOutputInterface.onInfo(clientInfo);
        }
    }
    @JavascriptInterface
    public void fetchCliArgs(String args){
        cliArgs = null;
        try {
            cliArgs = new JSONObject(args);
            jsOutputInterface.onArgs(cliArgs);
        }catch (JSONException e){
            e.printStackTrace();
            jsOutputInterface.onArgs(cliArgs);
        }
    }
    @JavascriptInterface
    public void updateServerArgs(String upd){
        jsOutputInterface.onInfo(null);
    }

    @JavascriptInterface
    public void getGraphFromServer(String retval) {}

    @JavascriptInterface
    public void getScanDataFromServer(String retval) {}

    @JavascriptInterface
    public void onServerGraph(String data){
        jsOutputInterface.onData(data);
    }
}
