package com.doudou.wxq;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;
public final class UpdateRoutesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static JSONObject manifest()throws Exception{return new JSONObject().put("packageName","com.doudou.wxq").put("versionCode",BuildInfo.VERSION_CODE+1).put("versionName","0.1.12").put("apkUrl","https://github.com/xiguadoudou/WanxiangCompanion/releases/download/v0.1.12/WanxiangCompanion-0.1.12.apk").put("apkFallbackUrls",new JSONArray().put("https://raw.githubusercontent.com/xiguadoudou/WanxiangCompanion/main/updates/WanxiangCompanion-0.1.12.apk")).put("sha256",String.join("",Collections.nCopies(64,"a"))).put("sizeBytes",10000);}
 public static void main(String[] args)throws Exception{
  JSONObject m=manifest();List<UpdateRoutes.Route> routes=UpdateRoutes.downloads(m);check(routes.get(0).url.startsWith(UpdateRoutes.PROXIES[0]),"Proxy must be first");check(routes.get(routes.size()-1).url.startsWith("https://raw.githubusercontent.com/"),"Official fallback missing");check(new HashSet<>(UpdateContract.downloadUrls(m)).size()==2,"Canonical URLs must stay unchanged");
  for(String bad:new String[]{"http://github.com/xiguadoudou/WanxiangCompanion/a.apk","https://github.com.evil.example/xiguadoudou/WanxiangCompanion/releases/download/a.apk","https://github.com/other/repo/releases/download/a.apk","https://raw.githubusercontent.com/xiguadoudou/WanxiangCompanion/main/updates/../secret","https://github.com/xiguadoudou/WanxiangCompanion/releases/download/%2e%2e/x","https://user:password@github.com/xiguadoudou/WanxiangCompanion/releases/download/a.apk"})check(!UpdateRoutes.proxyable(bad),"Unsafe or unrelated resource proxied: "+bad);
  AtomicInteger calls=new AtomicInteger();UpdateCheck.Result answer=UpdateCheck.load(url->{if(calls.getAndIncrement()==0)throw new SocketTimeoutException();return m.toString();},(s,i,total)->{});check(calls.get()==2&&answer.manifest.getInt("versionCode")==m.getInt("versionCode"),"Timeout fallback failed");
  calls.set(0);answer=UpdateCheck.load(url->calls.getAndIncrement()==0?"<html>wrong page</html>":m.toString(),(s,i,total)->{});check(calls.get()==2,"Invalid JSON must switch sources");
  calls.set(0);JSONObject old=new JSONObject(m.toString()).put("versionCode",BuildInfo.VERSION_CODE-1);answer=UpdateCheck.load(url->calls.getAndIncrement()==0?old.toString():m.toString(),(s,i,total)->{});check(calls.get()==2,"Old proxy cache cannot claim up-to-date");
  calls.set(0);JSONObject tampered=new JSONObject(m.toString()).put("sha256","bad");answer=UpdateCheck.load(url->calls.getAndIncrement()==0?tampered.toString():m.toString(),(s,i,total)->{});check(calls.get()==2,"Invalid manifest must switch sources");
  try{UpdateCheck.load(url->{throw new UnknownHostException();},(s,i,total)->{});throw new AssertionError("All-failed check accepted");}catch(IOException e){check(e.getMessage().contains("域名解析失败")&&e.getMessage().contains("GitHub Pages"),"Actionable source errors missing");}
  check(UpdateRoutes.reason(new javax.net.ssl.SSLHandshakeException("bad cert")).contains("HTTPS"),"TLS failures need an accurate message");
  if(args.length>0){UpdateCheck.Result live=UpdateCheck.load(UpdateTransport::metadata,(s,i,total)->System.out.println("Checking "+s));System.out.println("Live update source: "+live.source+" / "+live.manifest.getString("versionName"));}
  System.out.println("Proxy ordering/scope, canonical URLs, timeout/HTML/old-cache/invalid-manifest fallback and DNS/TLS errors passed.");
 }
}
