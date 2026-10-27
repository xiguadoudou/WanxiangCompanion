package com.doudou.wxq;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.LongSupplier;
import javax.net.ssl.HttpsURLConnection;

/** Shared by preloading and both WebViews. All network/disk work runs off the UI thread. */
final class ImageDiskCache {
 static final long FRESH_MS=7L*24*60*60*1000,RETRY_MS=60L*60*1000;
 static final int MAX_IMAGE_BYTES=2*1024*1024;
 static final long DEFAULT_LIMIT=48L*1024*1024;
 static final class Entry {final byte[] bytes;final String mime;final boolean cached;Entry(byte[] b,String m,boolean c){bytes=b;mime=m;cached=c;}}
 static final class Response {final int status;final String mime,etag,modified;final byte[] bytes;Response(int s,String m,String e,String l,byte[] b){status=s;mime=m;etag=e;modified=l;bytes=b;}}
 interface Fetcher{Response fetch(String url,String etag,String modified)throws IOException;}
 private final File directory;private final long limit,fresh;private final Fetcher fetcher;private final LongSupplier clock;
 private final Object[] locks=new Object[32];private final Object trimLock=new Object();
 ImageDiskCache(File dir){this(dir,DEFAULT_LIMIT,FRESH_MS,ImageDiskCache::fetchHttps,System::currentTimeMillis);}
 ImageDiskCache(File dir,long limit,long fresh,Fetcher fetcher,LongSupplier clock){directory=dir;this.limit=limit;this.fresh=fresh;this.fetcher=fetcher;this.clock=clock;for(int i=0;i<locks.length;i++)locks[i]=new Object();}
 static boolean accepts(String url){try{URI u=new URI(url);String path=u.getPath();return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null&&path!=null&&path.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpe?g|webp|gif|avif|svg)$");}catch(Exception e){return false;}}
 static String hash(byte[] bytes){try{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 Entry get(String url)throws IOException{
  if(!accepts(url))throw new IOException("Not a public HTTPS image");String key=hash(url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  synchronized(locks[(key.hashCode()&0x7fffffff)%locks.length]){
   File data=new File(directory,key+".img"),meta=new File(directory,key+".meta");Properties p=new Properties();byte[] old=null;long now=clock.getAsLong();
   try{if(meta.isFile()&&data.isFile()&&data.length()>0&&data.length()<=MAX_IMAGE_BYTES){try(InputStream in=new FileInputStream(meta)){p.load(in);}old=Files.readAllBytes(data.toPath());if(!url.equals(p.getProperty("url"))||!hash(old).equals(p.getProperty("sha256"))||!p.getProperty("mime","").startsWith("image/"))old=null;}}catch(IOException|IllegalArgumentException e){old=null;p.clear();}
   if(old!=null&&(now-number(p,"checkedAt")<fresh||(number(p,"failedAt")>0&&now-number(p,"failedAt")<RETRY_MS))){data.setLastModified(now);return new Entry(old,p.getProperty("mime"),true);}
   try{
    Response response=fetcher.fetch(url,old==null?"":p.getProperty("etag",""),old==null?"":p.getProperty("modified",""));
    if(response.status==304&&old!=null){p.setProperty("checkedAt",Long.toString(now));p.remove("failedAt");saveProperties(meta,p);data.setLastModified(now);return new Entry(old,p.getProperty("mime"),true);}
    if(response.status!=200||response.bytes==null||response.bytes.length==0||response.bytes.length>MAX_IMAGE_BYTES||response.mime==null||!response.mime.startsWith("image/"))throw new IOException("Invalid image response: "+response.status);
    Properties next=new Properties();next.setProperty("url",url);next.setProperty("mime",response.mime);next.setProperty("sha256",hash(response.bytes));next.setProperty("checkedAt",Long.toString(now));next.setProperty("etag",response.etag==null?"":response.etag);next.setProperty("modified",response.modified==null?"":response.modified);
    if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create image cache");
    atomic(data,response.bytes);saveProperties(meta,next);data.setLastModified(now);trim();return new Entry(response.bytes,response.mime,false);
   }catch(IOException e){if(old==null)throw e;p.setProperty("failedAt",Long.toString(now));try{saveProperties(meta,p);}catch(IOException ignored){}return new Entry(old,p.getProperty("mime"),true);}
  }
 }
 private static long number(Properties p,String key){try{return Long.parseLong(p.getProperty(key,"0"));}catch(NumberFormatException e){return 0;}}
 private static void saveProperties(File file,Properties p)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();p.store(bytes,"Wanxiang image cache");atomic(file,bytes.toByteArray());}
 private static void atomic(File file,byte[] bytes)throws IOException{
  File tmp=new File(file.getParentFile(),file.getName()+".tmp");try{try(FileOutputStream out=new FileOutputStream(tmp)){out.write(bytes);}try{Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException|UnsupportedOperationException e){Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}finally{tmp.delete();}
 }
 long sizeBytes(){long sum=0;File[] files=directory.listFiles((d,n)->n.endsWith(".img"));if(files!=null)for(File f:files)sum+=f.length();return sum;}
 private void trim(){synchronized(trimLock){File[] files=directory.listFiles((d,n)->n.endsWith(".img"));if(files==null)return;long sum=0;for(File f:files)sum+=f.length();Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(File f:files){if(sum<=limit)break;long bytes=f.length();if(f.delete()){sum-=bytes;new File(directory,f.getName().replace(".img",".meta")).delete();}}}}
 private static Response fetchHttps(String url,String etag,String modified)throws IOException{
  URL target=new URL(url);
  for(int hop=0;hop<=5;hop++){
   if(!"https".equalsIgnoreCase(target.getProtocol())||target.getUserInfo()!=null)throw new IOException("Unsafe image redirect");
   HttpsURLConnection connection=(HttpsURLConnection)target.openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(7000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("User-Agent","WanxiangCompanion/"+BuildInfo.VERSION_NAME);connection.setRequestProperty("Accept","image/*");connection.setRequestProperty("Accept-Encoding","identity");
   if(!etag.isEmpty())connection.setRequestProperty("If-None-Match",etag);if(!modified.isEmpty())connection.setRequestProperty("If-Modified-Since",modified);
   try{
    int status=connection.getResponseCode();
    if(status==301||status==302||status==303||status==307||status==308){String location=connection.getHeaderField("Location");if(location==null)throw new IOException("Image redirect lacks Location");target=new URL(target,location);continue;}
    String mime=connection.getContentType();if(mime!=null)mime=mime.split(";",2)[0].trim().toLowerCase(Locale.ROOT);
    if(status==304)return new Response(status,mime,connection.getHeaderField("ETag"),connection.getHeaderField("Last-Modified"),null);
    if(status!=200||mime==null||!mime.startsWith("image/")||connection.getContentLengthLong()>MAX_IMAGE_BYTES)throw new IOException("Image HTTP "+status);
    ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>MAX_IMAGE_BYTES)throw new IOException("Image too large");out.write(buffer,0,n);}}
    if(connection.getContentLengthLong()>0&&connection.getContentLengthLong()!=out.size())throw new IOException("Incomplete image");
    return new Response(status,mime,connection.getHeaderField("ETag"),connection.getHeaderField("Last-Modified"),out.toByteArray());
   }finally{connection.disconnect();}
  }
  throw new IOException("Too many image redirects");
 }
}
