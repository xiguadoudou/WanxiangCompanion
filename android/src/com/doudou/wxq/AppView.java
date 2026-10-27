package com.doudou.wxq;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;

final class AppView extends WebView implements Repository.Listener {
    interface FailureListener{void failed(AppView view);}
    private final Repository repo;
    private final boolean overlay;
    private volatile boolean loaded=false,disposed=false;private boolean rendererGone=false;
    AppView(Context context,boolean isOverlay){this(context,isOverlay,null);}
    AppView(Context context,boolean isOverlay,FailureListener failure){
        super(context);overlay=isOverlay;repo=Repository.get(context);
        setBackgroundColor(Color.TRANSPARENT);
        setOverScrollMode(OVER_SCROLL_NEVER);
        getSettings().setJavaScriptEnabled(true);getSettings().setDomStorageEnabled(true);
        if(overlay)getSettings().setTextZoom(100);
        getSettings().setAllowFileAccess(false);getSettings().setAllowContentAccess(false);
        getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        addJavascriptInterface(new Object(){
            @JavascriptInterface public String initialState(){loaded=true;return repo.state(overlay).toString();}
            @JavascriptInterface public void command(String raw){
                if(disposed||raw==null||raw.length()>50000)return;
                repo.main.post(()->{if(!disposed)dispatch(Repository.json(raw));});
            }
        },"Native");
        setWebViewClient(new WebViewClient(){
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){
                String url=request.getUrl().toString();if(!"GET".equals(request.getMethod())||!ImageDiskCache.accepts(url))return null;
                try{ImageDiskCache.Entry entry=repo.images.get(url);return new android.webkit.WebResourceResponse(entry.mime,null,200,"OK",java.util.Collections.singletonMap("Cache-Control","no-store"),new java.io.ByteArrayInputStream(entry.bytes));}
                catch(java.io.IOException e){return new android.webkit.WebResourceResponse("text/plain","UTF-8",404,"Image unavailable",java.util.Collections.emptyMap(),new java.io.ByteArrayInputStream(new byte[0]));}
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !r.getUrl().toString().equals("file:///android_asset/"+(overlay?"overlay.html":"index.html"));}
            @Override public void onPageFinished(WebView v,String u){if(!disposed){loaded=true;onEvent(repo.state(overlay));}}
            @Override public boolean onRenderProcessGone(WebView v,android.webkit.RenderProcessGoneDetail detail){rendererGone=true;dispose();if(failure!=null)repo.main.post(()->failure.failed(AppView.this));return true;}
        });
        repo.add(this);try{loadUrl("file:///android_asset/"+(overlay?"overlay.html":"index.html"));}catch(RuntimeException e){dispose();throw e;}
    }
    private void dispatch(JSONObject c){
        String action=c.optString("action");JSONObject data=c.optJSONObject("data");if(data==null)data=new JSONObject();
        switch(action){
            case "query":repo.query(data);break;
            case "detail":repo.detail(data.optJSONObject("query"),data.optString("key"));break;
            case "save":repo.saveUi(data);break;
            case "resetRound":repo.resetRound();break;
            case "refresh":repo.checkUpdates();break;
            case "checkSoftwareUpdate":if(!overlay)repo.updater.check();break;
            case "downloadSoftwareUpdate":if(!overlay)repo.updater.download();break;
            case "installSoftwareUpdate":if(!overlay&&getContext() instanceof MainActivity)((MainActivity)getContext()).installUpdate();break;
            case "start":{
                if(getContext() instanceof MainActivity){((MainActivity)getContext()).requestOverlay();break;}
                if(!Settings.canDrawOverlays(getContext())){
                    Intent i=new Intent(getContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("startOverlay",true);getContext().startActivity(i);
                }else{
                    Intent i=new Intent(getContext(),OverlayService.class);getContext().startForegroundService(i);
                    if(getContext() instanceof MainActivity)((MainActivity)getContext()).moveTaskToBack(true);
                }
                break;
            }
            case "stop":if(getContext() instanceof MainActivity)((MainActivity)getContext()).cancelOverlayStart();repo.prefs.edit().putBoolean("overlay_enabled",false).apply();getContext().stopService(new Intent(getContext(),OverlayService.class));break;
            case "minimize":if(OverlayService.instance!=null)OverlayService.instance.collapse();break;
            case "background":if(getContext() instanceof MainActivity)((MainActivity)getContext()).moveTaskToBack(true);break;
            case "source":getContext().startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.datawxq.com/lineups")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));break;
            case "copy":{
                JSONObject response=Repository.event("copyResult");Repository.put(response,"token",data.optString("token"));
                try{
                    String code=data.optString("text","");
                    if(code.trim().isEmpty())throw new IllegalArgumentException("数据源暂未提供此阵容码");
                    android.content.ClipboardManager cm=(android.content.ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("阵容码",code));
                    Repository.put(response,"ok",true);Repository.put(response,"message","阵容码已复制");
                }catch(Exception ex){Repository.put(response,"ok",false);Repository.put(response,"message",ex instanceof IllegalArgumentException?ex.getMessage():"复制失败，请重试");}
                onEvent(response);
                android.widget.Toast.makeText(getContext(),response.optString("message"),android.widget.Toast.LENGTH_SHORT).show();break;
            }
        }
    }
    @Override public void onEvent(JSONObject e){if(loaded&&!disposed)evaluateJavascript("window.App&&window.App.receive("+e+")",null);}
    void dispose(){if(disposed)return;disposed=true;loaded=false;repo.remove(this);if(getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)getParent()).removeView(this);try{if(!rendererGone){removeJavascriptInterface("Native");stopLoading();}destroy();}catch(RuntimeException ignored){}}
}
