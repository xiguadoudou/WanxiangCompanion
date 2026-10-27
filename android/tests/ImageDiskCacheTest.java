package com.doudou.wxq;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public final class ImageDiskCacheTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static final String URL="https://example.com/a.png";
 static byte[] image(){byte[] b=new byte[100];b[0]=(byte)137;b[1]=80;return b;}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("wxq-image-test").toFile();AtomicLong now=new AtomicLong(System.currentTimeMillis());AtomicInteger calls=new AtomicInteger();
  ImageDiskCache.Fetcher fetch=(url,etag,last)->{calls.incrementAndGet();if(!etag.isEmpty()){check(etag.equals("v1"),"ETag lost");return new ImageDiskCache.Response(304,null,null,null,null);}return new ImageDiskCache.Response(200,"image/png","v1","date",image());};
  ImageDiskCache c=new ImageDiskCache(root,1000,1000,fetch,now::get);check(!c.get(URL).cached,"First fetch should download");check(c.get(URL).cached&&calls.get()==1,"Refresh must reuse the file");
  c=new ImageDiskCache(root,1000,1000,fetch,now::get);check(c.get(URL).cached&&calls.get()==1,"Restart must retain cached image");now.addAndGet(1001);check(c.get(URL).cached&&calls.get()==2,"Stale images should use ETag/304");
  now.addAndGet(1001);ImageDiskCache offline=new ImageDiskCache(root,1000,1000,(u,e,l)->{calls.incrementAndGet();throw new IOException("offline");},now::get);check(offline.get(URL).cached,"Offline must retain existing image");int before=calls.get();offline.get(URL);check(calls.get()==before,"Offline retries need a backoff");
  File[] files=root.listFiles((d,n)->n.endsWith(".img"));Files.write(files[0].toPath(),new byte[]{1});ImageDiskCache repaired=new ImageDiskCache(root,1000,1000,fetch,now::get);check(repaired.get(URL).bytes.length==100,"Corrupt image should be downloaded again");
  File concurrent=Files.createTempDirectory("wxq-image-concurrent").toFile();AtomicInteger downloads=new AtomicInteger();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  ImageDiskCache shared=new ImageDiskCache(concurrent,1000,1000,(u,e,l)->{downloads.incrementAndGet();entered.countDown();try{release.await();}catch(InterruptedException ex){throw new IOException(ex);}return new ImageDiskCache.Response(200,"image/png",null,null,image());},now::get);
  ExecutorService pool=Executors.newFixedThreadPool(6);List<Future<?>> tasks=new ArrayList<>();for(int i=0;i<6;i++)tasks.add(pool.submit(()->{try{shared.get(URL);}catch(IOException e){throw new RuntimeException(e);}}));entered.await();release.countDown();for(Future<?> f:tasks)f.get();pool.shutdown();check(downloads.get()==1,"Prefetch and two WebViews must share one download");
  File limited=Files.createTempDirectory("wxq-image-limit").toFile();ImageDiskCache lru=new ImageDiskCache(limited,200,1000,fetch,now::get);for(int i=0;i<4;i++){now.addAndGet(1);lru.get("https://example.com/"+i+".png");}check(lru.sizeBytes()<=200,"Disk limit exceeded");
  check(!ImageDiskCache.accepts("http://example.com/a.png")&&!ImageDiskCache.accepts("https://user:pass@example.com/a.png")&&!ImageDiskCache.accepts("https://example.com/script.js"),"Only HTTPS image requests may be intercepted");
  ImageDiskCache invalid=new ImageDiskCache(Files.createTempDirectory("wxq-image-invalid").toFile(),1000,1000,(u,e,l)->new ImageDiskCache.Response(200,"text/html",null,null,image()),now::get);try{invalid.get(URL);throw new AssertionError("HTML cached as image");}catch(IOException expected){}
  System.out.println("Image cache: first load, refresh/restart hits, ETag/304, offline/backoff, corruption, concurrent deduplication, bounded disk and image-only validation passed.");
 }
}
