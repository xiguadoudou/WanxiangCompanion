package com.doudou.wxq;
import java.net.*;
import java.util.*;
import org.json.*;

/** Only anonymous, public files in this project's GitHub repository use proxies. */
final class UpdateRoutes {
 static final String[] PROXIES={"https://ghfast.top/"};
 static final String DOWNLOAD_PAGE="https://xiguadoudou.github.io/WanxiangCompanion/";
 static final class Route{final String url,label;Route(String u,String l){url=u;label=l;}}
 static boolean proxyable(String raw){try{URI u=UpdateTransport.https(raw);String host=u.getHost(),path=u.getRawPath();return u.getPort()==-1&&u.getRawQuery()==null&&u.getRawFragment()==null&&(("github.com".equals(host)&&path.startsWith("/xiguadoudou/WanxiangCompanion/releases/download/"))||("raw.githubusercontent.com".equals(host)&&path.startsWith("/xiguadoudou/WanxiangCompanion/main/updates/")))&&!path.contains("/../")&&!path.toLowerCase(Locale.ROOT).contains("%2e");}catch(Exception e){return false;}}
 static List<Route> metadata(long time){List<Route> out=new ArrayList<>();for(int i=0;i<PROXIES.length;i++)out.add(new Route(PROXIES[i]+BuildInfo.UPDATE_URL+"?t="+time,"加速线路 "+(i+1)));out.add(new Route(BuildInfo.UPDATE_URL+"?t="+time,"GitHub 官方"));out.add(new Route(DOWNLOAD_PAGE+"latest.json?t="+time,"GitHub Pages"));return out;}
 static List<Route> downloads(JSONObject manifest)throws Exception{
  List<String> canonical=UpdateContract.downloadUrls(manifest);List<Route> out=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(int i=0;i<PROXIES.length;i++)for(int j=0;j<canonical.size();j++){String url=canonical.get(j);if(proxyable(url)&&seen.add(PROXIES[i]+url))out.add(new Route(PROXIES[i]+url,"加速线路 "+(i+1)+(j==0?"":" · 仓库备用")));}
  for(int j=0;j<canonical.size();j++)if(seen.add(canonical.get(j)))out.add(new Route(canonical.get(j),j==0?"GitHub 官方":"GitHub 仓库备用"));return out;
 }
 static String reason(Exception e){
  for(Throwable t=e;t!=null;t=t.getCause()){
   if(t instanceof UnknownHostException)return "域名解析失败";
   if(t instanceof SocketTimeoutException)return "连接或读取超时";
   if(t instanceof javax.net.ssl.SSLException||t instanceof java.security.cert.CertificateException)return "HTTPS 校验失败，请检查手机日期和网络";
   if(t instanceof ConnectException)return "网络连接失败";
  }
  String message=e.getMessage();if(message!=null&&message.contains("HTTP "))return message;
  if(e instanceof org.json.JSONException)return "返回的不是有效版本资料";
  if(message!=null&&message.contains("尚未同步"))return message;
  if(e instanceof IllegalArgumentException)return "版本资料校验失败";
  return "线路暂时不可用";
 }
}
