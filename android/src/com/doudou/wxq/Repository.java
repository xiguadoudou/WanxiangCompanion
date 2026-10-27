package com.doudou.wxq;

import android.content.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

final class Repository {
 interface Listener{void onEvent(JSONObject e);}
 private static Repository instance;
 static Repository get(Context c){if(instance==null)instance=new Repository(c.getApplicationContext());return instance;}
 final Context app;final Handler main=new Handler(Looper.getMainLooper());final SharedPreferences prefs;
 private final List<Listener> listeners=new ArrayList<>();private final ExecutorService io=Executors.newFixedThreadPool(2);
 private final SiteDataClient client;private volatile long requestId=0;private boolean loading=false,inFlight=false;private String activeKey="",activeCatalogBuild="";
 JSONObject catalog,result;final AppUpdater updater;final ImageDiskCache images;
 private final ExecutorService imageWorker=Executors.newSingleThreadExecutor();private volatile int imageGeneration=0;private String warmSignature="";private volatile String cacheMessage="准备图片缓存…";
 private Repository(Context c){
  app=c;images=new ImageDiskCache(new File(c.getFilesDir(),"image-cache"));prefs=c.getSharedPreferences("wxq_companion_v1",Context.MODE_PRIVATE);
  JSONObject ui=json(prefs.getString("ui","{}"));if(ui.optInt("compactOverlayVersion")<3){put(ui,"width",392);put(ui,"height",230);put(ui,"compactOverlayVersion",3);prefs.edit().putString("ui",ui.toString()).apply();}
  if(ui.optInt("minimumDefaultRevision")<1){if(!ui.has("minCount")||ui.optInt("minCount")==50)put(ui,"minCount",10);put(ui,"minimumDefaultRevision",1);prefs.edit().putString("ui",ui.toString()).apply();}
  catalog=json(prefs.getString("catalog","{}"));if(!valid(catalog))try{catalog=json(asset("catalog-snapshot.json"));}catch(Exception e){catalog=new JSONObject();}
  result=json(prefs.getString("last_result","{}"));
  String did=prefs.getString("data_device_id","");if(!did.startsWith("did")||did.length()!=26){did="did"+UUID.randomUUID().toString().replace("-","").substring(0,23);prefs.edit().putString("data_device_id",did).apply();}
  final File sourceDir=new File(app.getFilesDir(),"public-data");sourceDir.mkdirs();
  client=new SiteDataClient(new SiteDataClient.Cache(){
   public String read(String key)throws Exception{File f=new File(sourceDir,key);return f.isFile()?readFile(f):null;}
   public void write(String key,String value)throws Exception{try(FileOutputStream out=new FileOutputStream(new File(sourceDir,key))){out.write(value.getBytes(StandardCharsets.UTF_8));}}
  },did);
  updater=new AppUpdater(this);main.post(()->warmImages(catalog));
 }
 private void warmImages(JSONObject source){
  LinkedHashSet<String> urls=new LinkedHashSet<>();for(String kind:new String[]{"commanders","heroes","items","factions"}){JSONArray list=source.optJSONArray(kind);if(list!=null)for(int i=0;i<list.length();i++){JSONObject row=list.optJSONObject(i);if(row==null)continue;for(String key:new String[]{"avatar","image"}){String url=row.optString(key);if(ImageDiskCache.accepts(url))urls.add(url);}}}
  String signature=urls.toString();if(signature.equals(warmSignature)||urls.isEmpty())return;warmSignature=signature;final int generation=++imageGeneration;
  imageWorker.execute(()->{int available=0,done=0;for(String url:urls){if(generation!=imageGeneration)return;try{images.get(url);available++;}catch(IOException ignored){}done++;if(done%10==0||done==urls.size()){String message=done<urls.size()?"图片缓存准备中 "+done+"/"+urls.size():"图片缓存 "+available+"/"+urls.size()+" · "+String.format(java.util.Locale.ROOT,"%.1f",images.sizeBytes()/1048576.0)+" MB"+(available<urls.size()?" · 未加载图片将在联网后补齐":" · 刷新优先使用本地");cacheMessage=message;JSONObject event=event("cacheStatus");put(event,"message",message);post(event);}}if(available<urls.size())main.post(()->{if(generation==imageGeneration)warmSignature="";});});
 }
 static boolean valid(JSONObject c){return c.optJSONArray("commanders")!=null&&c.optJSONArray("heroes")!=null&&!c.optString("publicSignatureSalt").isEmpty();}
 static JSONObject json(String s){try{return new JSONObject(s);}catch(Exception e){return new JSONObject();}}
 static JSONObject event(String type){JSONObject e=new JSONObject();put(e,"type",type);return e;}
 static void put(JSONObject e,String k,Object v){try{e.put(k,v);}catch(Exception ignored){}}
 static String readFile(File f)throws Exception{try(InputStream in=new FileInputStream(f)){return read(in);}}
 private static String read(InputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}
 String asset(String name)throws Exception{try(InputStream in=app.getAssets().open(name)){return read(in);}}
 void add(Listener l){if(!listeners.contains(l))listeners.add(l);}void remove(Listener l){listeners.remove(l);}
 void emit(JSONObject e){for(Listener l:new ArrayList<>(listeners))l.onEvent(e);}
 void post(JSONObject e){main.post(()->emit(e));}
 JSONObject state(boolean overlay){JSONObject e=event("init");put(e,"overlay",overlay);put(e,"catalog",catalog);put(e,"data",result);put(e,"prefs",json(prefs.getString("ui","{}")));put(e,"ready",valid(catalog));put(e,"updater",updater.state());put(e,"imageCacheStatus",cacheMessage);return e;}
 void checkUpdates(){
  if(loading)return;loading=true;
  io.execute(()->{
   try{
    JSONObject c=client.refresh(s->{JSONObject e=event("dataStatus");put(e,"state","checking");put(e,"message",s);post(e);});
    main.post(()->{String previous=catalog.optString("sourceBuild");catalog=c;warmImages(catalog);loading=false;prefs.edit().putString("catalog",c.toString()).apply();JSONObject e=event("catalog");put(e,"catalog",catalog);put(e,"changed",!previous.equals(c.optString("sourceBuild")));emit(e);JSONObject done=event("dataStatus");put(done,"state","current");put(done,"message","数据资料已检查 · "+c.optString("version"));emit(done);});
   }catch(Exception ex){main.post(()->{loading=false;JSONObject e=event("dataStatus");put(e,"state","failed");put(e,"message","资料检查失败，仍可用内置/缓存资料查询："+ex.getMessage());emit(e);});}
  });
 }
 static String key(JSONObject q){JSONArray a=new JSONArray(),b=new JSONArray();try{b=SiteDataClient.bans(q);}catch(Exception ignored){}a.put(q.optString("commanderId")).put(b).put(q.optString("sort","avgPlacement")).put(q.optInt("days",7)).put(q.optInt("minCount",10));return a.toString();}
 void query(JSONObject q){
  if(q==null||q.optString("commanderId").isEmpty())return;
  String k=key(q),build=catalog.optString("sourceBuild")+"|"+catalog.optString("version");if(inFlight&&k.equals(activeKey)&&build.equals(activeCatalogBuild))return;
  final long id=++requestId;activeKey=k;activeCatalogBuild=build;inFlight=true;
  JSONObject cached=json(prefs.getString("cache_"+k,"{}"));if(cached.has("rows")){put(cached,"type","result");put(cached,"cached",true);emit(cached);}
  JSONObject start=event("queryStart");put(start,"queryKey",k);emit(start);final JSONObject snapshot=catalog;
  io.execute(()->{
   try{
    if(!valid(snapshot))throw new IOException("内置资料不完整，请检查数据更新");
    JSONObject answer=client.query(q,snapshot,()->id!=requestId||Thread.currentThread().isInterrupted(),s->{if(id==requestId){JSONObject e=event("queryProgress");put(e,"queryKey",k);put(e,"message",s);post(e);}});
    put(answer,"queryKey",k);
    main.post(()->{if(id!=requestId)return;inFlight=false;put(answer,"type","result");put(answer,"id",id);put(answer,"queryKey",k);put(answer,"cached",false);result=answer;saveResult(k,answer);emit(answer);});
   }catch(Exception ex){main.post(()->{if(id!=requestId)return;inFlight=false;JSONObject e=event("queryError");put(e,"id",id);put(e,"queryKey",k);put(e,"message","阵容查询失败："+ex.getMessage());emit(e);});}
  });
 }
 private void saveResult(String key,JSONObject result){
  SharedPreferences.Editor edit=prefs.edit().putString("last_result",result.toString()).putString("cache_"+key,result.toString());
  LinkedHashSet<String> keys=new LinkedHashSet<>();try{JSONArray old=new JSONArray(prefs.getString("cache_keys","[]"));for(int i=0;i<old.length();i++)keys.add(old.getString(i));}catch(Exception ignored){}
  keys.remove(key);keys.add(key);while(keys.size()>30){String first=keys.iterator().next();keys.remove(first);edit.remove("cache_"+first);}edit.putString("cache_keys",new JSONArray(keys).toString()).apply();
 }
 void resetRound(){resetRound(false);}
 void resetRound(boolean selectDefault){
  ++requestId;inFlight=false;activeKey="";activeCatalogBuild="";result=new JSONObject();
  JSONObject ui=RoundSelection.reset(json(prefs.getString("ui","{}")),catalog,selectDefault);
  prefs.edit().putString("ui",ui.toString()).remove("last_result").apply();
  JSONObject e=event("roundReset");put(e,"prefs",ui);emit(e);
 }
 void detail(JSONObject q,String key){JSONObject e=event("detailError");put(e,"message","请在阵容卡片中查看当前英雄与装备");emit(e);}
 void saveUi(JSONObject ui){prefs.edit().putString("ui",ui.toString()).apply();JSONObject e=event("preferences");put(e,"prefs",ui);emit(e);if(OverlayService.instance!=null)OverlayService.instance.updateStyle();}
}
