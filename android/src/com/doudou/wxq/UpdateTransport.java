package com.doudou.wxq;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Anonymous HTTPS GET for update metadata and GitHub's redirected release assets. */
final class UpdateTransport {
 interface Progress{void changed(int percent);}
 static final int MAX_REDIRECTS=5;
 static URI https(String raw)throws Exception{
  URI u=new URI(raw);if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IOException("更新地址必须使用 HTTPS");return u;
 }
 static String redirect(String from,String location)throws Exception{
  if(location==null||location.trim().isEmpty())throw new IOException("下载跳转地址为空");
  return https(https(from).resolve(location.trim()).toString()).toString();
 }
 static HttpURLConnection open(String raw)throws Exception{
  String url=https(raw).toString();Set<String> visited=new HashSet<>();
  for(int redirects=0;;redirects++){
   if(!visited.add(url))throw new IOException("更新地址出现循环跳转");
   HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
   c.setRequestProperty("User-Agent","WanxiangCompanion/"+BuildInfo.VERSION_NAME);c.setRequestProperty("Accept","application/json,application/octet-stream,*/*");c.setRequestProperty("Cache-Control","no-cache");
   try{
    int code=c.getResponseCode();
    if(code==301||code==302||code==303||code==307||code==308){if(redirects>=MAX_REDIRECTS)throw new IOException("更新地址跳转次数过多");String next=redirect(url,c.getHeaderField("Location"));c.disconnect();url=next;continue;}
    if(code!=200)throw new IOException("GitHub 响应 HTTP "+code);return c;
   }catch(Exception e){c.disconnect();if(e instanceof SocketTimeoutException)throw new IOException("GitHub 连接超时，请稍后重试",e);throw e;}
  }
 }
 static String metadata(String url)throws Exception{
  HttpURLConnection c=open(url);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>100000)throw new IOException("更新信息超过大小限制");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);
  }finally{c.disconnect();}
 }
 static void download(String url,File part,long expected,String sha256,Progress progress)throws Exception{
  if(expected<=0||expected>100*1024*1024)throw new IOException("安装包大小无效");
  HttpURLConnection c=open(url);long count=0;int last=-1;MessageDigest hash=MessageDigest.getInstance("SHA-256");
  try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(part)){
   byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){count+=n;if(count>expected)throw new IOException("安装包大小不匹配");out.write(b,0,n);hash.update(b,0,n);int pct=(int)(count*100/expected);if(pct!=last){last=pct;progress.changed(pct);}}
  }finally{c.disconnect();}
  StringBuilder hex=new StringBuilder();for(byte b:hash.digest())hex.append(String.format(Locale.ROOT,"%02x",b));
  if(count!=expected||!hex.toString().equalsIgnoreCase(sha256))throw new IOException("安装包完整性校验失败");
 }
}
