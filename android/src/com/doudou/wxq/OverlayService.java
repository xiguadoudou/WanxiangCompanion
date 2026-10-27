package com.doudou.wxq;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;

public final class OverlayService extends Service {
 static OverlayService instance;
 static final String START_CALLBACK="overlayStartCallback";
 private Context windowContext;private WindowManager wm;
 private WindowManager.LayoutParams iconParams,panelParams,windowParams;
 private FrameLayout root;private ImageView icon,resizeHandle;private AppView web;
 private OverlayWindowSession session;private final OverlayState state=new OverlayState();
 private final Handler handler=new Handler(Looper.getMainLooper());
 private boolean resizing=false,destroyed=false,ready=false,retryQueued=false;
 private String initError="悬浮服务尚未准备好";
 private JSONObject ui(){return Repository.json(Repository.get(this).prefs.getString("ui","{}"));}
 private float density(){return (windowContext==null?getResources():windowContext.getResources()).getDisplayMetrics().density;}
 private int dp(float n){return Math.round(n*density());}
 private Rect screenBounds(){
  try{if(Build.VERSION.SDK_INT>=30)return wm.getMaximumWindowMetrics().getBounds();}catch(RuntimeException ignored){}
  android.util.DisplayMetrics m=new android.util.DisplayMetrics();try{wm.getDefaultDisplay().getRealMetrics(m);}catch(RuntimeException e){m.setTo(getResources().getDisplayMetrics());}
  return new Rect(0,0,Math.max(1,m.widthPixels),Math.max(1,m.heightPixels));
 }
 private int screenW(){return screenBounds().width();}private int screenH(){return screenBounds().height();}
 @Override public void onCreate(){
  super.onCreate();instance=this;
  try{
   NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("overlay","悬浮阵容助手",NotificationManager.IMPORTANCE_LOW));
   PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,OverlayService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
   PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
   Notification n=new Notification.Builder(this,"overlay").setSmallIcon(R.drawable.ic_launcher).setContentTitle("万象弈伴悬浮助手已启用").setContentText("点图标展开或收起；显示异常时点此返回并重新启用").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"关闭悬浮助手",stop).build()).build();
   if(Build.VERSION.SDK_INT>=34)startForeground(7,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(7,n);
   if(!Settings.canDrawOverlays(this))throw new SecurityException("悬浮窗权限未开启");
   windowContext=this;
   if(Build.VERSION.SDK_INT>=30)try{
    android.hardware.display.DisplayManager dm=getSystemService(android.hardware.display.DisplayManager.class);Display display=dm.getDisplay(Display.DEFAULT_DISPLAY);
    windowContext=createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null);
   }catch(RuntimeException ignored){}
   wm=(WindowManager)windowContext.getSystemService(WINDOW_SERVICE);
   iconParams=params(dp(44),dp(44));panelParams=params(dp(392),dp(230));windowParams=params(dp(44),dp(44));
   iconParams.x=Repository.get(this).prefs.getInt("icon_x",dp(8));iconParams.y=Repository.get(this).prefs.getInt("icon_y",dp(76));
   root=new FrameLayout(windowContext);root.setClipChildren(true);
   icon=new ImageView(windowContext);icon.setImageResource(R.drawable.ic_floating);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);icon.setPadding(dp(2),dp(2),dp(2),dp(2));icon.setElevation(dp(8));icon.setClickable(true);icon.setFocusable(false);
   root.addView(icon,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.LEFT));
   icon.setOnClickListener(v->handler.post(()->{if(!destroyed){state.toggle();reconcile(true);}}));installIconDrag();
   session=new OverlayWindowSession(new OverlayWindowSession.Surface(){
    public boolean isAttached(){return root.getParent()!=null;}
    public void attach(boolean visible,boolean expanded){prepareWindow(visible,expanded);wm.addView(root,windowParams);}
    public void update(boolean visible,boolean expanded){prepareWindow(visible,expanded);wm.updateViewLayout(root,windowParams);}
    public void remove(){wm.removeView(root);}
   });
   state.setHostVisible(WxqApplication.isHostVisible());ready=reconcile(false);
   if(!ready)initError="系统未接受悬浮窗口，请检查悬浮权限后重试";
  }catch(RuntimeException ex){initError="悬浮窗口创建失败："+ex.getClass().getSimpleName();android.util.Log.e("WxqOverlay",initError,ex);}
 }
 private WindowManager.LayoutParams params(int w,int h){
  int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,flags,PixelFormat.TRANSLUCENT);
  if(Build.VERSION.SDK_INT>=30){p.setFitInsetsTypes(0);p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;}else if(Build.VERSION.SDK_INT>=28)p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
  p.gravity=Gravity.TOP|Gravity.LEFT;p.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;return p;
 }
 private void createPanel(){
  if(resizeHandle==null){resizeHandle=new ImageView(windowContext);resizeHandle.setImageResource(R.drawable.ic_resize);resizeHandle.setContentDescription("拖动调整悬浮窗大小");resizeHandle.setClickable(true);resizeHandle.setElevation(dp(6));root.addView(resizeHandle,new FrameLayout.LayoutParams(dp(32),dp(32),Gravity.RIGHT|Gravity.BOTTOM));installResize();}
  if(web==null){AppView candidate=new AppView(windowContext,true,failed->{if(destroyed||web!=failed)return;web=null;state.collapse();resizing=false;reconcile(true);Toast.makeText(this,"面板显示进程已重置，已保留图标，请点击重试",Toast.LENGTH_LONG).show();});web=candidate;root.addView(web,0,new FrameLayout.LayoutParams(-1,-1));}
 }
 private void prepareWindow(boolean visible,boolean expanded){
  if(expanded)createPanel();windowParams.copyFrom(expanded?panelParams:iconParams);
  if(!visible)windowParams.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;windowParams.alpha=visible?1f:0f;root.setVisibility(visible?View.VISIBLE:View.INVISIBLE);
  if(web!=null)web.setVisibility(expanded?View.VISIBLE:View.GONE);if(resizeHandle!=null)resizeHandle.setVisibility(expanded?View.VISIBLE:View.GONE);
  if(expanded){int alpha=Math.round(Math.max(30,Math.min(100,ui().optInt("opacity",88)))*2.55f);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.argb(alpha,17,29,43));bg.setCornerRadius(dp(13));bg.setStroke(dp(.5f),0x609dddc9);root.setBackground(bg);root.setClipToOutline(true);}else{root.setClipToOutline(false);root.setBackground(null);}
  icon.setVisibility(View.VISIBLE);icon.bringToFront();icon.setAlpha(expanded?1f:.90f);icon.setContentDescription(expanded?"收起阵容面板":"展开阵容面板");
 }
 private void position(){
  if(!state.showPanel()){resizing=false;iconParams.x=OverlayGeometry.clamp(iconParams.x,0,screenW()-dp(44));iconParams.y=OverlayGeometry.clamp(iconParams.y,0,screenH()-dp(44));return;}if(resizing)return;
  JSONObject p=ui();OverlayGeometry.Box b=OverlayGeometry.place(screenW(),screenH(),iconParams.x,iconParams.y,dp(OverlayGeometry.clamp(p.optInt("width",392),250,960)),dp(OverlayGeometry.clamp(p.optInt("height",230),170,900)));
  iconParams.x=b.iconX;iconParams.y=b.iconY;panelParams.x=b.x;panelParams.y=b.y;panelParams.width=b.width;panelParams.height=b.height;
 }
 private void installResize(){
  resizeHandle.setOnClickListener(v->Toast.makeText(this,"拖动右下角调整大小，松手自动保存",Toast.LENGTH_SHORT).show());
  resizeHandle.setOnTouchListener(new View.OnTouchListener(){
   float sx,sy;int width,height,x,y;final DragTapTracker gesture=new DragTapTracker(ViewConfiguration.get(OverlayService.this).getScaledTouchSlop());
   private void resize(MotionEvent e){OverlayGeometry.Box b=OverlayGeometry.resize(screenW(),screenH(),x,y,width,height,Math.round(e.getRawX()-sx),Math.round(e.getRawY()-sy),dp(250),dp(170));panelParams.width=Math.min(b.width,dp(960));panelParams.height=Math.min(b.height,dp(900));reconcile(true);}
   public boolean onTouch(View v,MotionEvent e){switch(e.getActionMasked()){
    case MotionEvent.ACTION_DOWN:sx=e.getRawX();sy=e.getRawY();width=panelParams.width;height=panelParams.height;x=panelParams.x;y=panelParams.y;gesture.begin(sx,sy);resizing=true;resizeHandle.setAlpha(1f);return true;
    case MotionEvent.ACTION_MOVE:if(resizing&&gesture.move(e.getRawX(),e.getRawY()))resize(e);return true;
    case MotionEvent.ACTION_UP:if(!resizing){gesture.cancel();return true;}boolean tap=gesture.finish(e.getRawX(),e.getRawY());if(!tap)resize(e);resizing=false;resizeHandle.setAlpha(.75f);if(tap)v.performClick();else if(state.showPanel()){JSONObject p=ui();Repository.put(p,"width",Math.round(panelParams.width/density()));Repository.put(p,"height",Math.round(panelParams.height/density()));Repository.get(OverlayService.this).saveUi(p);}return true;
    case MotionEvent.ACTION_CANCEL:gesture.cancel();resizing=false;resizeHandle.setAlpha(.75f);reconcile(true);return true;default:return true;}}
  });resizeHandle.setAlpha(.75f);
 }
 void setHostVisible(boolean visible){state.setHostVisible(visible);reconcile(true);}
 private boolean reconcile(boolean retry){
  if(destroyed||session==null)return false;
  if(!Settings.canDrawOverlays(this)){session.close();ready=false;Repository.get(this).prefs.edit().putBoolean("overlay_enabled",false).apply();initError="悬浮窗权限已关闭，请重新允许后启用";stopSelf();return false;}
  int result;try{position();result=session.present(state.showIcon(),state.showPanel());}catch(RuntimeException ex){android.util.Log.w("WxqOverlay","Window positioning failed",ex);result=OverlayWindowSession.FAILED;}
  if(result==OverlayWindowSession.OK)return true;android.util.Log.w("WxqOverlay","Window update failed; keeping compact toggle",session.lastError());state.collapse();resizing=false;
  if(result==OverlayWindowSession.RECOVERED){Toast.makeText(this,"面板暂时无法展开，已保留悬浮图标，点击可重试",Toast.LENGTH_LONG).show();return true;}
  session.close();if(retry&&!retryQueued){retryQueued=true;handler.postDelayed(()->{retryQueued=false;if(!destroyed&&!reconcile(false))Toast.makeText(this,"系统暂时拒绝悬浮显示，请返回软件重新启用并检查权限",Toast.LENGTH_LONG).show();},350);}return false;
 }
 void collapse(){handler.post(()->{state.collapse();reconcile(true);});}void updateStyle(){reconcile(true);}
 private void installIconDrag(){icon.setOnTouchListener(new View.OnTouchListener(){
  float sx,sy;int x,y;final DragTapTracker gesture=new DragTapTracker(ViewConfiguration.get(OverlayService.this).getScaledTouchSlop());
  public boolean onTouch(View v,MotionEvent e){switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:sx=e.getRawX();sy=e.getRawY();x=iconParams.x;y=iconParams.y;gesture.begin(sx,sy);return true;
   case MotionEvent.ACTION_MOVE:if(gesture.move(e.getRawX(),e.getRawY())){iconParams.x=x+Math.round(e.getRawX()-sx);iconParams.y=y+Math.round(e.getRawY()-sy);reconcile(true);}return true;
   case MotionEvent.ACTION_UP:if(gesture.finish(e.getRawX(),e.getRawY()))v.performClick();else Repository.get(OverlayService.this).prefs.edit().putInt("icon_x",iconParams.x).putInt("icon_y",iconParams.y).apply();return true;
   case MotionEvent.ACTION_CANCEL:gesture.cancel();return true;default:return true;}}
 });}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"stop".equals(i.getAction())){Repository.get(this).prefs.edit().putBoolean("overlay_enabled",false).apply();stopSelf();return START_NOT_STICKY;}
  state.setHostVisible(WxqApplication.isHostVisible());boolean ok=ready&&reconcile(false);Repository.get(this).prefs.edit().putBoolean("overlay_enabled",ok).apply();
  ResultReceiver callback=i==null?null:Build.VERSION.SDK_INT>=33?i.getParcelableExtra(START_CALLBACK,ResultReceiver.class):i.getParcelableExtra(START_CALLBACK);
  if(callback!=null){Bundle data=new Bundle();data.putString("message",ok?"悬浮图标已启用":initError);callback.send(ok?1:0,data);}else if(!ok)Toast.makeText(this,initError,Toast.LENGTH_LONG).show();if(!ok)stopSelf();return START_NOT_STICKY;
 }
 @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);resizing=false;reconcile(true);}
 void shutdown(){destroyed=true;handler.removeCallbacksAndMessages(null);if(session!=null)session.close();if(web!=null){web.dispose();web=null;}stopForeground(STOP_FOREGROUND_REMOVE);instance=null;}
 @Override public void onDestroy(){shutdown();super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
