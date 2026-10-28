package com.doudou.wxq;
import android.content.Context;import android.webkit.WebResourceResponse;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;import java.util.zip.*;
/** Serves the authorized component port and literal records from the APK, never remote HTML/JS. */
final class LocalWorkshop {
 static final String HOST=SiteModules.HOST;private static LocalWorkshop instance;
 private final Context app;private final ZipFile zip;private final JSONObject images;
 static synchronized LocalWorkshop get(Context c)throws Exception{if(instance==null)instance=new LocalWorkshop(c.getApplicationContext());return instance;}
 private LocalWorkshop(Context c)throws Exception{app=c;JSONObject m=new JSONObject(readAsset("workshop/bundle.json"));File target=new File(app.getCacheDir(),"workshop-"+BuildInfo.VERSION_NAME+".zip");if(!target.isFile()||target.length()!=m.getLong("sizeBytes")||!sha(target).equals(m.getString("sha256"))){File tmp=new File(target.getPath()+".tmp");try(InputStream in=app.getAssets().open("workshop/site.zip");FileOutputStream out=new FileOutputStream(tmp)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.getFD().sync();}if(!sha(tmp).equals(m.getString("sha256"))||!tmp.renameTo(target))throw new IOException("本地模块校验失败");}zip=new ZipFile(target);images=new JSONObject(text("image-map.json"));}
 private String readAsset(String name)throws Exception{try(InputStream in=app.getAssets().open(name)){return new String(bytes(in),StandardCharsets.UTF_8);}}
 private static byte[] bytes(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
 private static String sha(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(Locale.ROOT,"%02x",b));return s.toString();}
 private byte[] file(String name)throws IOException{ZipEntry e=zip.getEntry(name);if(e==null)throw new FileNotFoundException(name);try(InputStream in=zip.getInputStream(e)){return bytes(in);}}
 private String text(String name)throws IOException{return new String(file(name),StandardCharsets.UTF_8);}
 JSONObject data()throws Exception{File f=new File(app.getFilesDir(),"workshop/library-data.json");if(f.isFile())try{JSONObject d=new JSONObject(Repository.readFile(f));WorkshopData.validate(d);return d;}catch(Exception ignored){}return new JSONObject(text("library-data.json"));}
 static void saveData(Context c,JSONObject data)throws Exception{WorkshopData.validate(data);File dir=new File(c.getFilesDir(),"workshop");dir.mkdirs();File tmp=new File(dir,"library-data.json.tmp"),f=new File(dir,"library-data.json");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(data.toString().getBytes(StandardCharsets.UTF_8));out.getFD().sync();}if(!tmp.renameTo(f))throw new IOException("资料缓存未能保存");}
 private static WebResourceResponse response(String mime,byte[] data){Map<String,String> h=new HashMap<>();h.put("Cache-Control","no-store");h.put("Access-Control-Allow-Origin","*");return new WebResourceResponse(mime,mime.startsWith("image/")?null:"UTF-8",200,"OK",h,new ByteArrayInputStream(data));}
 WebResourceResponse resource(String path)throws Exception{
  if(path.equals("/workshop-data.js"))return response("application/javascript",("window.__WXQ_DATA__="+data().toString()+";").getBytes(StandardCharsets.UTF_8));
  if(path.equals("/team-builder")||path.equals("/simulator")||path.equals("/h5/database"))return response("text/html",file("index.html"));
  if(path.equals("/port.js")||path.equals("/port.css"))return response(path.endsWith(".js")?"application/javascript":"text/css",file(path.substring(1)));
  if(path.matches("/assets/[A-Za-z0-9_.-]+\\.(js|css)"))return response(path.endsWith(".js")?"application/javascript":"text/css",file(path.substring(1)));
  if(path.matches("/assets/level/[1-5]\\.png"))return response("image/png",file(path.substring(1)));
  return blocked();
 }
 WebResourceResponse image(String url)throws Exception{String name=images.optString(url);return name.isEmpty()?null:response(name.endsWith(".jpg")?"image/jpeg":"image/png",file(name));}
 static WebResourceResponse blocked(){return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",Collections.singletonMap("Cache-Control","no-store"),new ByteArrayInputStream(new byte[0]));}
}
