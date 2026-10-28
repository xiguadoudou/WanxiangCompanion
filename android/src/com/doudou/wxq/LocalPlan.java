package com.doudou.wxq;
import org.json.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.Base64;
/** App plan format, distinct from game lineup import codes. */
final class LocalPlan {
 static String extract(String input)throws Exception{String s=input.trim();if(s.startsWith("WXQ1:"))s=s.substring(5);else if(s.startsWith("https://")){URI u=new URI(s);if(!("www.datawxq.com".equals(u.getHost())||SiteModules.HOST.equals(u.getHost())))throw new IllegalArgumentException("不是支持的阵容方案");if(u.getRawQuery()==null)throw new IllegalArgumentException("链接中没有阵容方案");s="";for(String p:u.getRawQuery().split("&"))if(p.startsWith("lineup="))s=URLDecoder.decode(p.substring(7),"UTF-8");}
  if(s.length()>100000||s.isEmpty())throw new IllegalArgumentException("方案码为空或过长");JSONObject o;try{o=new JSONObject(new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalArgumentException("方案码格式不正确");}JSONArray b=o.optJSONArray("b");if(o.optInt("v")!=1||b==null||b.length()<1||b.length()>112)throw new IllegalArgumentException("方案版本或棋盘不受支持");for(int i=0;i<b.length();i++){if(b.isNull(i))continue;JSONArray h=b.optJSONArray(i);if(h==null||h.length()>12||h.optString(0).length()>50)throw new IllegalArgumentException("英雄资料无效");if(h.length()<3||h.optInt(2,1)<1||h.optInt(2,1)>3)throw new IllegalArgumentException("英雄星级必须为 1 至 3 星");JSONArray items=h.optJSONArray(1);if(items!=null&&items.length()>3)throw new IllegalArgumentException("每个英雄最多三件装备");}return Base64.getUrlEncoder().withoutPadding().encodeToString(o.toString().getBytes(StandardCharsets.UTF_8));
 }
 static String code(String value)throws Exception{return "WXQ1:"+extract(value);}
 static String url(String value)throws Exception{return SiteModules.url(SiteModules.SIMULATOR)+"?lineup="+extract(value);}
}
