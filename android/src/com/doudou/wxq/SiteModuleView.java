package com.doudou.wxq;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.http.SslError;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.Collections;

/** Online website view with native navigation, retry and the shared image cache. No JS bridge. */
final class SiteModuleView extends LinearLayout {
 private final Activity activity;private final String module;private final Repository repo;
 private final FrameLayout stage;private final ProgressBar progress;private final TextView status;private WebView web;
 private View error;private boolean disposed=false,failed=false,rendererGone=false;private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());private Runnable timeout;
 SiteModuleView(Activity a,String id,Runnable navigation,Runnable home){
  super(a);activity=a;module=id;repo=Repository.get(a);setOrientation(VERTICAL);setBackgroundColor(0xff101724);
  LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),dp(6),dp(8),dp(6));bar.setBackgroundColor(0xff162630);
  bar.addView(button("☰","打开功能侧边栏",navigation),new LayoutParams(dp(40),dp(40)));
  LinearLayout caption=new LinearLayout(a);caption.setOrientation(VERTICAL);TextView title=text(SiteModules.title(id),16,0xffedf5f1);status=text("万象棋大数据 · 在线模块",10,0xff92b2b6);caption.addView(title);caption.addView(status);bar.addView(caption,new LayoutParams(0,-2,1));
  bar.addView(button("↻","重新加载当前页面",this::retry),new LayoutParams(dp(40),dp(40)));bar.addView(button("‹","返回阵容推荐",home),new LayoutParams(dp(40),dp(40)));addView(bar,new LayoutParams(-1,-2));
  progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);addView(progress,new LayoutParams(-1,dp(2)));stage=new FrameLayout(a);addView(stage,new LayoutParams(-1,0,1));createWeb();
 }
 private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private TextView text(String value,int size,int color){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
 private Button button(String label,String description,Runnable action){Button b=new Button(activity);b.setText(label);b.setTextSize(20);b.setTextColor(0xffa2e8d0);b.setContentDescription(description);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setBackgroundColor(Color.TRANSPARENT);b.setOnClickListener(v->action.run());return b;}
 private void createWeb(){
  if(disposed)return;rendererGone=false;
  try{
   web=new WebView(activity);web.setBackgroundColor(0xff101724);WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);settings.setCacheMode(WebSettings.LOAD_DEFAULT);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
   // Deliberately no addJavascriptInterface: remote content cannot invoke updates or overlays.
   web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int n){if(disposed||v!=web)return;progress.setProgress(n);progress.setVisibility(n==100?View.GONE:View.VISIBLE);}});
   web.setWebViewClient(new WebViewClient(){
    @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(!r.isForMainFrame())return false;if(SiteModules.allowed(r.getUrl().toString()))return false;if(r.hasGesture())Toast.makeText(activity,"此链接不属于内置数据模块",Toast.LENGTH_SHORT).show();return true;}
    @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){String url=r.getUrl().toString();if(!"GET".equals(r.getMethod())||!ImageDiskCache.accepts(url))return null;try{ImageDiskCache.Entry e=repo.images.get(url);return new WebResourceResponse(e.mime,null,200,"OK",Collections.singletonMap("Cache-Control","no-store"),new java.io.ByteArrayInputStream(e.bytes));}catch(Exception e){return null;}}
    @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){if(disposed||v!=web)return;failed=false;progress.setVisibility(View.VISIBLE);status.setText("正在加载 · "+SiteModules.title(module));if(error!=null)error.setVisibility(View.GONE);scheduleTimeout();}
    @Override public void onPageFinished(WebView v,String url){if(disposed||v!=web)return;cancelTimeout();progress.setVisibility(View.GONE);if(!failed)status.setText(SiteModules.SIMULATOR.equals(module)?"在线模块 · 横屏操作更方便":"万象棋大数据 · 在线模块");}
    @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())showError("页面暂时无法连接，请检查网络后重试。");}
    @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame()&&e.getStatusCode()>=400)showError("网站返回 "+e.getStatusCode()+"，可稍后重试。");}
    @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();showError("网站 HTTPS 校验未通过，请检查设备时间与网络后重试。");}
    @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){if(v!=web)return true;rendererGone=true;releaseWeb();showError("页面显示进程已重置，点击重试重新打开。");return true;}
   });
   stage.addView(web,new FrameLayout.LayoutParams(-1,-1));loadRoot();
  }catch(RuntimeException e){releaseWeb();showError("页面未能启动，请更新系统 WebView 后重试。");}
 }
 private void scheduleTimeout(){cancelTimeout();timeout=()->{if(!disposed&&web!=null){web.stopLoading();showError("页面加载超时，点击重试。已缓存的图片仍会保留。");}};handler.postDelayed(timeout,30000);}
 private void cancelTimeout(){if(timeout!=null)handler.removeCallbacks(timeout);timeout=null;}
 private void loadRoot(){failed=false;web.loadUrl(SiteModules.url(module));}
 private void showError(String message){if(disposed)return;failed=true;cancelTimeout();progress.setVisibility(View.GONE);status.setText("加载未完成");if(error!=null)stage.removeView(error);LinearLayout box=new LinearLayout(activity);box.setGravity(Gravity.CENTER);box.setOrientation(VERTICAL);box.setPadding(dp(24),dp(20),dp(24),dp(20));box.setBackgroundColor(0xff101724);TextView t=text(message,14,0xffb7cbd0);t.setGravity(Gravity.CENTER);box.addView(t);Button retry=new Button(activity);retry.setText("重新加载");retry.setTextColor(0xff173a3b);GradientDrawable bg=new GradientDrawable();bg.setColor(0xffa2e8d0);bg.setCornerRadius(dp(12));retry.setBackground(bg);retry.setOnClickListener(v->retry());LayoutParams p=new LayoutParams(dp(150),dp(46));p.topMargin=dp(18);box.addView(retry,p);error=box;stage.addView(error,new FrameLayout.LayoutParams(-1,-1));}
 private void retry(){if(disposed)return;if(error!=null)error.setVisibility(View.GONE);if(web==null)createWeb();else{failed=false;if(SiteModules.allowed(web.getUrl()))web.reload();else loadRoot();}}
 boolean back(){if(web!=null&&!failed&&web.canGoBack()){web.goBack();return true;}return false;}
 private void releaseWeb(){cancelTimeout();if(web!=null){WebView old=web;web=null;stage.removeView(old);try{if(!rendererGone)old.stopLoading();old.destroy();}catch(RuntimeException ignored){}}}
 void dispose(){disposed=true;handler.removeCallbacksAndMessages(null);releaseWeb();}
}
