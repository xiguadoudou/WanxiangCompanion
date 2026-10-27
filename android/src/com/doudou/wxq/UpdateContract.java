package com.doudou.wxq;
import org.json.*;
import java.net.URI;
import java.util.*;
final class UpdateContract {
 static JSONObject validate(JSONObject m)throws Exception{
  if(!"com.doudou.wxq".equals(m.optString("packageName")))throw new IllegalArgumentException("更新包名称不匹配");
  if(m.optInt("versionCode")<1||m.optString("versionName").isEmpty())throw new IllegalArgumentException("版本信息不完整");
  URI u=new URI(m.optString("apkUrl"));if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IllegalArgumentException("更新地址无效");
  if(!m.optString("sha256").matches("[a-fA-F0-9]{64}")||m.optLong("sizeBytes")<=0||m.optLong("sizeBytes")>100*1024*1024)throw new IllegalArgumentException("更新校验信息无效");downloadUrls(m);return m;
 }
 static List<String> downloadUrls(JSONObject m)throws Exception{
  LinkedHashSet<String> urls=new LinkedHashSet<>();urls.add(UpdateTransport.https(m.getString("apkUrl")).toString());JSONArray fallbacks=m.optJSONArray("apkFallbackUrls");
  if(fallbacks!=null){if(fallbacks.length()>2)throw new IllegalArgumentException("备用下载地址过多");for(int i=0;i<fallbacks.length();i++)urls.add(UpdateTransport.https(fallbacks.getString(i)).toString());}
  return new ArrayList<>(urls);
 }
 static boolean newer(JSONObject m){return m.optInt("versionCode")>BuildInfo.VERSION_CODE;}
}
