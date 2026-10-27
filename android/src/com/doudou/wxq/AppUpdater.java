package com.doudou.wxq;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

final class AppUpdater {
 private final Repository repo;private final ExecutorService io=Executors.newSingleThreadExecutor();
 private JSONObject manifest;private String phase="idle",message="打开应用自动检查软件版本";private int progress=0;private boolean busy=false,verified=false;
 AppUpdater(Repository r){repo=r;}
 synchronized JSONObject state(){JSONObject s=new JSONObject();Repository.put(s,"state",phase);Repository.put(s,"source","GitHub");Repository.put(s,"message",message);Repository.put(s,"installedVersion",BuildInfo.VERSION_NAME);Repository.put(s,"latestVersion",manifest==null?"":manifest.optString("versionName"));Repository.put(s,"latestVersionCode",manifest==null?0:manifest.optInt("versionCode"));Repository.put(s,"notes",manifest==null?"":manifest.optString("notes"));Repository.put(s,"progress",progress);Repository.put(s,"downloadReady",verified&&file().isFile());Repository.put(s,"available",manifest!=null&&UpdateContract.newer(manifest));return s;}
 private synchronized void emit(String p,String text,int percent){phase=p;message=text;progress=percent;JSONObject e=state();Repository.put(e,"type","softwareUpdate");repo.post(e);}
 synchronized void check(){if(busy)return;busy=true;io.execute(()->{try{emit("checking","正在检查 GitHub 新版本…",0);JSONObject m=UpdateContract.validate(new JSONObject(UpdateTransport.metadata(BuildInfo.UPDATE_URL+"?t="+System.currentTimeMillis())));synchronized(this){boolean same=manifest!=null&&manifest.optInt("versionCode")==m.optInt("versionCode")&&manifest.optString("sha256").equalsIgnoreCase(m.optString("sha256"));manifest=m;if(!same)verified=false;}boolean ready=canInstall()&&UpdateContract.newer(m);emit(ready?"ready":UpdateContract.newer(m)?"available":"current",ready?"安装包已校验完成，可安装更新":UpdateContract.newer(m)?"发现新版本 "+m.optString("versionName"):"已是最新版本 "+BuildInfo.VERSION_NAME,ready?100:0);}catch(Exception e){emit("error","软件更新检查失败："+e.getMessage(),0);}finally{synchronized(this){busy=false;}}});}
 File file(){return new File(repo.app.getCacheDir(),"update.apk");}
 synchronized void download(){
  if(busy)return;if(manifest==null||!UpdateContract.newer(manifest)){emit("error","请先检查软件新版本",0);return;}
  busy=true;final JSONObject m=manifest;verified=false;
  io.execute(()->{File part=new File(repo.app.getCacheDir(),"update.apk.part");try{
   List<String> urls=UpdateContract.downloadUrls(m);Exception failure=null;boolean downloaded=false;
   for(int i=0;i<urls.size();i++){
    emit("downloading",i==0?"正在从 GitHub 下载更新…":"正在切换 GitHub 备用下载…",0);
    try{UpdateTransport.download(urls.get(i),part,m.getLong("sizeBytes"),m.getString("sha256"),pct->emit("downloading","正在下载更新 "+pct+"%",pct));downloaded=true;break;}
    catch(Exception e){failure=e;part.delete();}
   }
   if(!downloaded)throw failure==null?new IOException("GitHub 下载暂时不可用"):failure;
   verifyPackage(part,m);if(file().exists()&&!file().delete())throw new IOException("无法替换旧安装包");if(!part.renameTo(file()))throw new IOException("无法保存安装包");
   synchronized(this){verified=true;}emit("ready","下载并校验完成，可安装更新",100);
  }catch(Exception e){part.delete();emit("error","更新下载失败："+e.getMessage(),0);}finally{synchronized(this){busy=false;}}});
 }
 @SuppressWarnings("deprecation") private void verifyPackage(File f,JSONObject m)throws Exception{
  PackageManager pm=repo.app.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
  PackageInfo apk=pm.getPackageArchiveInfo(f.getAbsolutePath(),flags),installed=pm.getPackageInfo(repo.app.getPackageName(),flags);
  if(apk==null||!repo.app.getPackageName().equals(apk.packageName))throw new IOException("安装包包名不匹配");long code=Build.VERSION.SDK_INT>=28?apk.getLongVersionCode():apk.versionCode;if(code!=m.getInt("versionCode")||code<=BuildInfo.VERSION_CODE)throw new IOException("安装包版本不匹配");
  Signature[] a=Build.VERSION.SDK_INT>=28?apk.signingInfo.getApkContentsSigners():apk.signatures,b=Build.VERSION.SDK_INT>=28?installed.signingInfo.getApkContentsSigners():installed.signatures;
  if(a==null||b==null||a.length!=b.length||a.length==0||!new HashSet<>(Arrays.asList(a)).equals(new HashSet<>(Arrays.asList(b))))throw new IOException("安装包签名不匹配");
 }
 synchronized boolean canInstall(){return verified&&file().isFile();}
}
