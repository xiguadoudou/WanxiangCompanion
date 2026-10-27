package com.doudou.wxq;
import java.io.*;
import org.json.*;
public final class UpdateTransportTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 static void reject(String from,String to)throws Exception{try{UpdateTransport.redirect(from,to);throw new AssertionError("Unsafe redirect accepted");}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
  String from="https://github.com/x/y/releases/download/v1/a.apk";
  check(UpdateTransport.redirect(from,"https://release-assets.githubusercontent.com/a?sig=a%2Bb").equals("https://release-assets.githubusercontent.com/a?sig=a%2Bb"));
  check(UpdateTransport.redirect(from,"/next.apk").equals("https://github.com/next.apk"));
  reject(from,"http://example.com/a.apk");reject(from,"https://user:pass@example.com/a.apk");reject(from,"");
  JSONObject m=new JSONObject().put("apkUrl",from).put("apkFallbackUrls",new JSONArray().put(from).put("https://raw.githubusercontent.com/x/y/main/a.apk"));check(UpdateContract.downloadUrls(m).size()==2);
  m.put("apkFallbackUrls",new JSONArray().put("http://example.com/a.apk"));try{UpdateContract.downloadUrls(m);throw new AssertionError();}catch(IOException expected){}
  System.out.println("HTTPS redirect resolution and GitHub fallback validation passed.");
  if(args.length>0){JSONObject live=UpdateContract.validate(new JSONObject(UpdateTransport.metadata(BuildInfo.UPDATE_URL+"?t="+System.currentTimeMillis())));File f=new File("build/github-live-download.apk");UpdateTransport.download(live.getString("apkUrl"),f,live.getLong("sizeBytes"),live.getString("sha256"),p->{});for(String url:UpdateContract.downloadUrls(live)){UpdateTransport.download(url,new File("build/github-fallback-download.apk"),live.getLong("sizeBytes"),live.getString("sha256"),p->{});}
   System.out.println("Live GitHub metadata, release redirect, fallback download and SHA-256 passed: "+live.getString("versionName"));}
 }
}
