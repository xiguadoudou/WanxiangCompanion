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
    private WindowManager wm;
    private WindowManager.LayoutParams iconParams,panelParams;
    private FrameLayout panel;
    private ImageView icon,resizeHandle;
    private boolean resizing=false;
    private AppView web;
    private final OverlayState state=new OverlayState();
    private boolean iconAttached=false,panelAttached=false;
    private JSONObject ui(){return Repository.json(Repository.get(this).prefs.getString("ui","{}"));}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private Rect screenBounds(){
        if(Build.VERSION.SDK_INT>=30)return wm.getMaximumWindowMetrics().getBounds();
        android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);return new Rect(0,0,m.widthPixels,m.heightPixels);
    }
    private int screenW(){return screenBounds().width();}
    private int screenH(){return screenBounds().height();}
    @Override public void onCreate(){
        super.onCreate();instance=this;
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("overlay","悬浮阵容助手",NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,OverlayService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,"overlay").setSmallIcon(R.drawable.ic_launcher).setContentTitle("万象弈伴悬浮助手已启用").setContentText("应用外显示图标；点图标展开或收起；点此返回设置").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"关闭悬浮助手",stop).build()).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(7,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(7,n);
        if(!Settings.canDrawOverlays(this)){stopSelf();return;}
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        iconParams=params(dp(44),dp(44));panelParams=params(dp(392),dp(230));
        iconParams.x=Repository.get(this).prefs.getInt("icon_x",dp(8));iconParams.y=Repository.get(this).prefs.getInt("icon_y",dp(76));
        icon=new ImageView(this);icon.setImageResource(R.drawable.ic_floating);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);icon.setPadding(dp(2),dp(2),dp(2),dp(2));icon.setElevation(dp(5));
        icon.setContentDescription("展开阵容面板");icon.setClickable(true);icon.setFocusable(false);
        icon.setOnClickListener(v->{state.toggle();reconcile();});
        installIconDrag();
        setHostVisible(WxqApplication.isHostVisible());
        Repository repo=Repository.get(this);if(!repo.state(true).optBoolean("ready"))repo.checkUpdates();
    }
    private WindowManager.LayoutParams params(int width,int height){
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        if(Build.VERSION.SDK_INT>=30){p.setFitInsetsTypes(0);p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;}
        else if(Build.VERSION.SDK_INT>=28)p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        p.gravity=Gravity.TOP|Gravity.LEFT;p.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;return p;
    }
    private void createPanel(){
        panel=new FrameLayout(this);panel.setPadding(dp(1),dp(1),dp(1),dp(1));panel.setClipToOutline(true);panel.setElevation(dp(7));
        web=new AppView(this,true);panel.addView(web,new FrameLayout.LayoutParams(-1,-1));
        resizeHandle=new ImageView(this);resizeHandle.setImageResource(R.drawable.ic_resize);resizeHandle.setContentDescription("拖动调整悬浮窗大小");resizeHandle.setClickable(true);
        FrameLayout.LayoutParams grip=new FrameLayout.LayoutParams(dp(32),dp(32),Gravity.RIGHT|Gravity.BOTTOM);panel.addView(resizeHandle,grip);installResize();
        paintPanel();
    }
    private void paintPanel(){
        if(panel==null)return;
        int alpha=Math.round(Math.max(30,Math.min(100,ui().optInt("opacity",88)))*2.55f);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.argb(alpha,17,29,43));bg.setCornerRadius(dp(13));bg.setStroke(dp(.5f),0x609dddc9);panel.setBackground(bg);
    }
    private void position(){
        if(!state.showPanel()){
            resizing=false;
            iconParams.x=OverlayGeometry.clamp(iconParams.x,0,screenW()-dp(44));
            iconParams.y=OverlayGeometry.clamp(iconParams.y,0,screenH()-dp(44));
            return;
        }
        if(resizing)return;
        JSONObject p=ui();
        int w=dp(OverlayGeometry.clamp(p.optInt("width",392),OverlayGeometry.MIN_WIDTH_DP,OverlayGeometry.MAX_WIDTH_DP));
        int h=dp(OverlayGeometry.clamp(p.optInt("height",230),OverlayGeometry.MIN_HEIGHT_DP,OverlayGeometry.MAX_HEIGHT_DP));
        OverlayGeometry.Box box=OverlayGeometry.place(screenW(),screenH(),iconParams.x,iconParams.y,w,h);
        iconParams.x=box.iconX;iconParams.y=box.iconY;
        panelParams.x=box.x;panelParams.y=box.y;panelParams.width=box.width;panelParams.height=box.height;
    }
    private void installResize(){
        resizeHandle.setOnClickListener(v->Toast.makeText(this,"拖动右下角调整大小，松手自动保存",Toast.LENGTH_SHORT).show());
        resizeHandle.setOnTouchListener(new View.OnTouchListener(){
            float sx,sy;int width,height,x,y;
            final DragTapTracker gesture=new DragTapTracker(ViewConfiguration.get(OverlayService.this).getScaledTouchSlop());
            private void resize(MotionEvent e){
                OverlayGeometry.Box b=OverlayGeometry.resize(screenW(),screenH(),x,y,width,height,Math.round(e.getRawX()-sx),Math.round(e.getRawY()-sy),dp(OverlayGeometry.MIN_WIDTH_DP),dp(OverlayGeometry.MIN_HEIGHT_DP));
                panelParams.width=Math.min(b.width,dp(OverlayGeometry.MAX_WIDTH_DP));panelParams.height=Math.min(b.height,dp(OverlayGeometry.MAX_HEIGHT_DP));
                if(panelAttached)wm.updateViewLayout(panel,panelParams);
            }
            @Override public boolean onTouch(View v,MotionEvent e){
                switch(e.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:sx=e.getRawX();sy=e.getRawY();width=panelParams.width;height=panelParams.height;x=panelParams.x;y=panelParams.y;gesture.begin(sx,sy);resizing=true;resizeHandle.setAlpha(1f);return true;
                    case MotionEvent.ACTION_MOVE:if(resizing&&gesture.move(e.getRawX(),e.getRawY()))resize(e);return true;
                    case MotionEvent.ACTION_UP:
                        if(!resizing){gesture.cancel();return true;}
                        boolean tap=gesture.finish(e.getRawX(),e.getRawY());if(!tap)resize(e);resizing=false;resizeHandle.setAlpha(.75f);
                        if(tap)v.performClick();else{JSONObject p=ui();Repository.put(p,"width",Math.round(panelParams.width/getResources().getDisplayMetrics().density));Repository.put(p,"height",Math.round(panelParams.height/getResources().getDisplayMetrics().density));Repository.get(OverlayService.this).saveUi(p);}return true;
                    case MotionEvent.ACTION_CANCEL:gesture.cancel();resizing=false;resizeHandle.setAlpha(.75f);reconcile();return true;
                    default:return true;
                }
            }
        });resizeHandle.setAlpha(.75f);
    }
    void setHostVisible(boolean visible){state.setHostVisible(visible);reconcile();}
    private void reconcile(){
        if(wm==null||icon==null)return;
        if(!Settings.canDrawOverlays(this)){detachAll();stopSelf();return;}
        if(!state.showIcon()){resizing=false;detachAll();return;}
        position();
        try{
            if(state.showPanel()){
                // Reuse the same toggle as a panel child, above the WebView, so it cannot be covered by window Z order.
                if(iconAttached){wm.removeView(icon);iconAttached=false;}
                if(panel==null)createPanel();
                if(icon.getParent()!=panel)panel.addView(icon,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.LEFT));
                if(!panelAttached){wm.addView(panel,panelParams);panelAttached=true;}else wm.updateViewLayout(panel,panelParams);
            }else{
                if(panelAttached){wm.removeView(panel);panelAttached=false;}
                if(panel!=null&&icon.getParent()==panel)panel.removeView(icon);
                if(!iconAttached){wm.addView(icon,iconParams);iconAttached=true;}else wm.updateViewLayout(icon,iconParams);
            }
            icon.setContentDescription(state.showPanel()?"收起阵容面板":"展开阵容面板");
            icon.setAlpha(state.showPanel()?1f:.90f);
        }catch(Exception ex){detachAll();Toast.makeText(this,"悬浮显示失败，请检查悬浮权限后重新启用",Toast.LENGTH_LONG).show();stopSelf();}
    }
    private void detachAll(){
        if(wm==null)return;
        if(panelAttached){try{wm.removeView(panel);}catch(Exception ignored){}panelAttached=false;}
        if(iconAttached){try{wm.removeView(icon);}catch(Exception ignored){}iconAttached=false;}
    }
    void collapse(){state.collapse();reconcile();}
    void updateStyle(){paintPanel();reconcile();}
    private void installIconDrag(){
        final int slop=ViewConfiguration.get(this).getScaledTouchSlop();
        icon.setOnTouchListener(new View.OnTouchListener(){
            float sx,sy;int x,y;final DragTapTracker gesture=new DragTapTracker(slop);
            @Override public boolean onTouch(View v,MotionEvent e){
                switch(e.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:sx=e.getRawX();sy=e.getRawY();x=iconParams.x;y=iconParams.y;gesture.begin(sx,sy);return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx,dy=e.getRawY()-sy;
                        if(gesture.move(e.getRawX(),e.getRawY())){iconParams.x=x+Math.round(dx);iconParams.y=y+Math.round(dy);reconcile();}return true;
                    case MotionEvent.ACTION_UP:
                        if(gesture.finish(e.getRawX(),e.getRawY()))v.performClick();
                        else Repository.get(OverlayService.this).prefs.edit().putInt("icon_x",iconParams.x).putInt("icon_y",iconParams.y).apply();return true;
                    case MotionEvent.ACTION_CANCEL:gesture.cancel();return true;
                    default:return true;
                }
            }
        });
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i!=null&&"stop".equals(i.getAction())){Repository.get(this).prefs.edit().putBoolean("overlay_enabled",false).apply();stopSelf();return START_NOT_STICKY;}
        setHostVisible(WxqApplication.isHostVisible());return START_NOT_STICKY;
    }
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);resizing=false;reconcile();}
    @Override public void onDestroy(){detachAll();if(web!=null)web.dispose();instance=null;super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
