package com.doudou.wxq;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;

public final class MainActivity extends Activity {
 private AppView view;private FrameLayout root,mainPane;private android.view.View navigation;private final ModuleTabs<android.view.View> pages=new ModuleTabs<>();
 private boolean startPending=false,installPending=false,permissionPending=false,notificationPending=false,notificationPrompted=false,destroyed=false;
 private final Handler handler=new Handler(Looper.getMainLooper());private final OverlayStartGate startGate=new OverlayStartGate();private Runnable timeout;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);Repository.get(this).resetRound(true);root=new FrameLayout(this);root.setBackgroundColor(0xff101724);
  root.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=i.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});setContentView(root);mainPane=new FrameLayout(this);root.addView(mainPane,new FrameLayout.LayoutParams(-1,-1));pages.home(mainPane);loadMainView();Repository.get(this).checkUpdates();
  if(Settings.canDrawOverlays(this)&&OverlayService.instance==null&&Repository.get(this).prefs.getBoolean("overlay_enabled",true))try{startForegroundService(new Intent(this,OverlayService.class));}catch(RuntimeException e){Toast.makeText(this,"自动启用未完成，请点击启用悬浮图标重试",Toast.LENGTH_LONG).show();}
  if(getIntent().getBooleanExtra("startOverlay",false)){getIntent().removeExtra("startOverlay");requestOverlay();}
 }
 private void loadMainView(){if(destroyed)return;if(view!=null){view.dispose();view=null;}mainPane.removeAllViews();try{view=new AppView(this,false,failed->{if(view==failed){view=null;showReload();}});mainPane.addView(view,new FrameLayout.LayoutParams(-1,-1));}catch(RuntimeException e){showReload();}}
 private void showReload(){if(destroyed)return;mainPane.removeAllViews();Button retry=new Button(this);retry.setText("界面暂时不可用，点击重新加载");retry.setOnClickListener(v->loadMainView());mainPane.addView(retry,new FrameLayout.LayoutParams(-1,-2,android.view.Gravity.CENTER));}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private android.graphics.drawable.GradientDrawable surface(int color,int stroke){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
 private TextView navText(String text,int size,int color){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);return v;}
 void showSection(String id){
  if(destroyed||!SiteModules.valid(id))return;closeNavigation();
  try{android.view.View target=pages.select(id,key->{SiteModuleView module=new SiteModuleView(this,key,this::openNavigation,()->showSection(SiteModules.HOME));try{root.addView(module,new FrameLayout.LayoutParams(-1,-1));}catch(RuntimeException e){module.dispose();throw e;}return module;});for(android.view.View page:pages.values())page.setVisibility(page==target?android.view.View.VISIBLE:android.view.View.GONE);}
  catch(RuntimeException ex){Toast.makeText(this,"模块未能打开，请重试",Toast.LENGTH_LONG).show();}
 }
 private void closeNavigation(){if(navigation!=null){root.removeView(navigation);navigation=null;}}
 void openNavigation(){
  if(destroyed||navigation!=null)return;
  FrameLayout shade=new FrameLayout(this);shade.setBackgroundColor(0x99000000);shade.setOnClickListener(v->closeNavigation());
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackground(surface(0xff172a36,0x449ed6c5));scroll.setOnClickListener(v->{});
  LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(18),dp(20),dp(18),dp(18));column.setClickable(true);scroll.addView(column,new ScrollView.LayoutParams(-1,-1));
  LinearLayout heading=new LinearLayout(this);heading.setGravity(android.view.Gravity.CENTER_VERTICAL);heading.addView(navText("◇ 万象弈伴",21,0xffedf5f1),new LinearLayout.LayoutParams(0,-2,1));Button close=new Button(this);close.setText("×");close.setTextColor(0xffa2e8d0);close.setContentDescription("关闭功能侧边栏");close.setPadding(0,0,0,0);close.setBackgroundColor(android.graphics.Color.TRANSPARENT);close.setOnClickListener(v->closeNavigation());heading.addView(close,new LinearLayout.LayoutParams(dp(36),dp(40)));column.addView(heading);
  TextView subtitle=navText("功能导航",11,0xff8eafb5);LinearLayout.LayoutParams sub=new LinearLayout.LayoutParams(-1,-2);sub.topMargin=dp(8);sub.bottomMargin=dp(23);column.addView(subtitle,sub);
  String[] ids={SiteModules.HOME,SiteModules.SIMULATOR,SiteModules.DATABASE};String[] symbols={"◇","▦","▤"};String[] descriptions={"按棋手与本局禁用匹配", "英雄、装备与站位编排", "英雄、装备、棋手与更多资料"};
  for(int i=0;i<ids.length;i++){final String id=ids[i];boolean selected=id.equals(pages.selected());LinearLayout row=new LinearLayout(this);row.setGravity(android.view.Gravity.CENTER_VERTICAL);row.setPadding(dp(13),dp(13),dp(10),dp(13));row.setBackground(surface(selected?0x209ee1cc:0x08ffffff,selected?0x669ee1cc:0x18ffffff));TextView icon=navText(symbols[i],24,selected?0xffa2e8d0:0xff9cb8bd);row.addView(icon,new LinearLayout.LayoutParams(dp(37),-2));LinearLayout label=new LinearLayout(this);label.setOrientation(LinearLayout.VERTICAL);label.addView(navText(SiteModules.title(id),15,selected?0xffa2e8d0:0xffedf5f1));TextView note=navText(descriptions[i],10,0xff94b0b8);note.setPadding(0,dp(5),0,0);label.addView(note);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));row.setContentDescription(SiteModules.title(id)+(selected?"，当前页面":""));row.setOnClickListener(v->showSection(id));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(10);column.addView(row,rp);}
  TextView note=navText("模拟与资料库由万象棋大数据提供，联网加载。切换模块会保留当前页面，刷新将重新加载。",11,0xff8eafb5);note.setLineSpacing(dp(4),1f);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(20);column.addView(note,np);TextView credit=navText("软件由 ChatGPT 构建 · "+BuildInfo.VERSION_NAME,10,0xff73919c);credit.setPadding(0,dp(25),0,0);column.addView(credit);
  int width=Math.min(dp(300),Math.max(dp(220),getResources().getDisplayMetrics().widthPixels-dp(48)));shade.addView(scroll,new FrameLayout.LayoutParams(width,-1,android.view.Gravity.LEFT));navigation=shade;root.addView(navigation,new FrameLayout.LayoutParams(-1,-1));
 }
 @Override protected void onStart(){super.onStart();if(!startPending&&!installPending){Repository.get(this).resetRound(true);Repository.get(this).checkUpdates();Repository.get(this).updater.check();}}
 void requestOverlay(){if(startPending)return;startPending=true;if(Settings.canDrawOverlays(this)){notificationThenStart();return;}permissionPending=true;Toast.makeText(this,"请允许显示在其他应用上层，然后返回软件",Toast.LENGTH_LONG).show();try{startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())),42);}catch(RuntimeException e){permissionPending=false;startPending=false;Toast.makeText(this,"请在系统设置中手动开启悬浮窗权限",Toast.LENGTH_LONG).show();}}
 private void notificationThenStart(){if(Build.VERSION.SDK_INT>=33&&!notificationPrompted&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){notificationPrompted=true;notificationPending=true;try{requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},4);return;}catch(RuntimeException ignored){notificationPending=false;}}startPanel();}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==42&&permissionPending){permissionPending=false;if(Settings.canDrawOverlays(this))notificationThenStart();else{startPending=false;Toast.makeText(this,"未获得悬浮权限，尚未启用",Toast.LENGTH_LONG).show();}}}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==4&&notificationPending){notificationPending=false;startPanel();}}
 private void startPanel(){
  if(destroyed)return;final long ticket=startGate.begin();
  ResultReceiver callback=new ResultReceiver(handler){protected void onReceiveResult(int code,Bundle data){if(destroyed||!startGate.complete(ticket))return;if(timeout!=null)handler.removeCallbacks(timeout);startPending=false;if(code==1){Toast.makeText(MainActivity.this,"悬浮图标已启用，正在返回桌面",Toast.LENGTH_SHORT).show();try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException e){moveTaskToBack(true);}}else Toast.makeText(MainActivity.this,data==null?"悬浮窗启用失败，请检查权限后重试":data.getString("message","悬浮窗启用失败"),Toast.LENGTH_LONG).show();}};
  timeout=()->{if(startGate.complete(ticket)){startPending=false;Toast.makeText(this,"尚未收到系统启用回执，请点击重试或检查权限",Toast.LENGTH_LONG).show();}};handler.postDelayed(timeout,8000);
  try{startForegroundService(new Intent(this,OverlayService.class).putExtra(OverlayService.START_CALLBACK,callback));}catch(RuntimeException e){cancelOverlayStart();Toast.makeText(this,"悬浮服务未能启动："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
 }
 void cancelOverlayStart(){startGate.cancel();startPending=false;if(timeout!=null)handler.removeCallbacks(timeout);}
 @Override protected void onStop(){super.onStop();if(startGate.waiting()&&!permissionPending&&!notificationPending)cancelOverlayStart();}
 @Override protected void onResume(){super.onResume();if(installPending){installPending=false;if(getPackageManager().canRequestPackageInstalls())installUpdate();}}
 void installUpdate(){if(!Repository.get(this).updater.canInstall()){Toast.makeText(this,"请先下载并校验更新",Toast.LENGTH_SHORT).show();return;}try{if(!getPackageManager().canRequestPackageInstalls()){installPending=true;Toast.makeText(this,"请允许本应用安装更新，然后返回",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));return;}Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://"+getPackageName()+".updates/update.apk"),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(android.content.ClipData.newRawUri("软件更新",i.getData()));startActivity(i);}catch(Exception e){installPending=false;Toast.makeText(this,"无法打开系统安装器："+e.getMessage(),Toast.LENGTH_LONG).show();}}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("startOverlay",false)){i.removeExtra("startOverlay");requestOverlay();}}
 @Override public void onConfigurationChanged(android.content.res.Configuration c){super.onConfigurationChanged(c);if(navigation!=null){closeNavigation();root.post(this::openNavigation);}}
 @Override protected void onDestroy(){destroyed=true;cancelOverlayStart();handler.removeCallbacksAndMessages(null);closeNavigation();for(android.view.View page:pages.values())if(page instanceof SiteModuleView)((SiteModuleView)page).dispose();pages.clear();if(view!=null){view.dispose();view=null;}super.onDestroy();}
 @Override public void onBackPressed(){if(navigation!=null){closeNavigation();return;}if(!SiteModules.HOME.equals(pages.selected())){android.view.View current=pages.select(pages.selected(),key->null);if(current instanceof SiteModuleView&&((SiteModuleView)current).back())return;showSection(SiteModules.HOME);return;}if(view!=null)view.evaluateJavascript("window.App.back()",null);else super.onBackPressed();}
}
