package com.doudou.wxq;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import java.util.function.BooleanSupplier;

/** Native public-data client. No webpage loading, JS execution or game/account access. */
final class SiteDataClient {
    interface Progress {void stage(String message);}
    interface Cache {String read(String key)throws Exception;void write(String key,String value)throws Exception;}
    static final String ORIGIN="https://www.datawxq.com";
    static final String USER_AGENT="Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 WanxiangCompanion/0.1.8";
    private final Cache cache;private final String deviceId;
    SiteDataClient(Cache cache,String deviceId){this.cache=cache;this.deviceId=deviceId;}
    static String match(String source,String regex,int group){Matcher m=Pattern.compile(regex).matcher(source);if(!m.find())throw new IllegalArgumentException("数据网站结构变化，无法识别资料入口");return m.group(group);}
    static String http(String url,String body,Map<String,String> headers,int limit)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent",USER_AGENT);c.setRequestProperty("Accept","application/json,text/html,*/*");
        for(Map.Entry<String,String> e:headers.entrySet())c.setRequestProperty(e.getKey(),e.getValue());
        try{
            if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}
            int status=c.getResponseCode();if(status<200||status>=300)throw new IOException("服务器响应 HTTP "+status);
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("请求已取消");if(out.size()+n>limit)throw new IOException("响应超过大小限制");out.write(b,0,n);}return out.toString("UTF-8");
            }
        }catch(SocketTimeoutException e){throw new IOException("网络连接或读取超时，请检查网络后重试",e);}finally{c.disconnect();}
    }
    private String asset(String name)throws Exception{
        if(!name.matches("[A-Za-z0-9_.-]+\\.js"))throw new IOException("资料文件名无效");
        String old=cache.read(name);if(old!=null&&!old.isEmpty())return old;
        String s=http(ORIGIN+"/assets/"+name,null,Collections.emptyMap(),6000000);cache.write(name,s);return s;
    }
    JSONObject refresh(Progress progress)throws Exception{
        progress.stage("数据检查 1/4：连接资料网站");
        String home=http(ORIGIN+"/",null,Collections.singletonMap("Cache-Control","no-cache"),300000);
        String entry=match(home,"src=[\"'](/assets/index-[\\w-]+\\.js)[\"']",1);
        progress.stage("数据检查 2/4：检查入口版本");String e=asset(entry.substring(8));
        String desktop=match(e,"desktop-main-[\\w-]+\\.js",0);
        progress.stage("数据检查 3/4：检查统计版本");String d=asset(desktop);
        String shared=match(d,"seo-[\\w-]+\\.js",0);
        progress.stage("数据检查 4/4：读取英雄与棋手资料");String s=asset(shared);
        return parseCatalog(d,s,desktop+"|"+shared);
    }
    static JSONObject parseCatalog(String desktop,String source,String build) throws Exception{
        JSONArray heroes=null,items=null,commanders=null;
        Matcher arrays=Pattern.compile("(?:\\b(?:const|let|var)\\s+|,)([A-Za-z_$][\\w$]*)\\s*=\\s*(\\[\\{id:)").matcher(source);
        while(arrays.find()){
            try{
                JSONArray a=(JSONArray)new JsDataReader(source,arrays.start(2)).read();JSONObject first=a.optJSONObject(0);if(first==null)continue;
                if(first.has("lordId")&&first.has("sections")&&a.length()>5)commanders=a;
                if("wanxiang".equals(first.optString("gameType"))&&"hero".equals(first.optString("type"))&&a.length()>20)heroes=a;
                if("wanxiang".equals(first.optString("gameType"))&&"equipment".equals(first.optString("type"))&&a.length()>20)items=a;
            }catch(Exception ignored){}
        }
        if(heroes==null||commanders==null||items==null)throw new IllegalArgumentException("网站资料格式变化，已保留内置资料，可稍后重试");
        String saltName=match(source,"\\.concat\\((\\w+)\\)\\)\\.toString\\(\\)",1);
        String salt=match(source,"\\b"+Pattern.quote(saltName)+"=\"([^\"]+)\"",1);
        String version=match(desktop,"\\{value:\"(v\\d{6})\",label:\"v\\d{6}\"\\}",1);
        JSONArray hs=new JSONArray(),cs=new JSONArray(),es=new JSONArray();
        for(int i=0;i<heroes.length();i++){JSONObject h=heroes.getJSONObject(i);hs.put(new JSONObject().put("id",String.valueOf(h.get("id"))).put("name",h.optString("name")).put("faction",h.optString("relationName","无阵营")).put("image",image(h)));}
        for(int i=0;i<items.length();i++){JSONObject h=items.getJSONObject(i);es.put(new JSONObject().put("id",String.valueOf(h.get("id"))).put("name",h.optString("name")).put("image",image(h)));}
        for(int i=0;i<commanders.length();i++){
            JSONObject c=commanders.getJSONObject(i),assets=c.optJSONObject("assets");JSONArray sections=c.optJSONArray("sections"),skills=new JSONArray();String skillName="",skillText="";
            if(sections!=null)for(int j=0;j<sections.length();j++){JSONObject section=sections.getJSONObject(j);JSONArray entries=section.optJSONArray("entries");if(entries==null||entries.length()==0)continue;JSONObject first=entries.getJSONObject(0);skills.put(new JSONObject().put("type",section.optString("type")).put("name",first.optString("name")).put("text",first.optString("description")));if(skillName.isEmpty()){skillName=first.optString("name");skillText=first.optString("description");}}
            cs.put(new JSONObject().put("id",c.optString("id")).put("name",c.optString("name")).put("avatar",assets==null?"":assets.optString("avatar")).put("skillName",skillName).put("skillText",skillText).put("skills",skills));
        }
        return new JSONObject().put("version",version).put("sourceBuild",build).put("checkedAt",System.currentTimeMillis()).put("commanders",cs).put("heroes",hs).put("items",es).put("factions",catalogFactions(hs,source)).put("apiBase","https://api.datatft.com").put("publicSignatureSalt",salt).put("_workshopData",WorkshopData.extract(source,build));
    }
    static JSONArray catalogFactions(JSONArray hs,String source)throws Exception{
        LinkedHashSet<String> names=new LinkedHashSet<>();for(int i=0;i<hs.length();i++){String name=hs.getJSONObject(i).optString("faction");if(!name.isEmpty()&&!name.equals("无阵营"))names.add(name);}
        JSONObject icons=new JSONObject();int iconAt=source.indexOf("/factions/");
        if(iconAt>=0)try{Object map=new JsDataReader(source,source.lastIndexOf('{',iconAt)).read();if(map instanceof JSONObject)icons=(JSONObject)map;}catch(Exception ignored){}
        JSONArray fs=new JSONArray();for(String name:orderedNames(names)){String url=icons.optString(name);fs.put(new JSONObject().put("name",name).put("image",url.startsWith("https://")?url:""));}
        return fs;
    }
    private static String image(JSONObject x){JSONObject a=x.optJSONObject("assets");if(a==null)return "";for(String k:new String[]{"avatar","thumbnail","listImage","image"})if(!a.optString(k).isEmpty())return a.optString(k);return "";}
    private static Map<String,JSONObject> index(JSONArray a) throws Exception{Map<String,JSONObject> m=new HashMap<>();if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);m.put(x.optString("id"),x);}return m;}
    static List<String> orderedNames(Collection<String> names){
        List<String> out=new ArrayList<>();for(String name:new String[]{"河洛","逐鹿","日落海","三分之地","大河流域"})if(names.contains(name))out.add(name);
        TreeSet<String> rest=new TreeSet<>(names);rest.removeAll(out);out.addAll(rest);return out;
    }
    static JSONArray bans(JSONObject q) throws Exception{
        Set<String> names=new HashSet<>();JSONArray a=q.optJSONArray("bans");if(a!=null)for(int i=0;i<a.length();i++){String name=a.optString(i).trim();if(!name.isEmpty()&&name.length()<=40&&!name.equals("无阵营"))names.add(name);}
        return new JSONArray(orderedNames(names));
    }
    static JSONObject payload(JSONObject q,String version,int page) throws Exception{
        JSONArray filters=new JSONArray().put(new JSONObject().put("type","commander").put("id",q.optString("commanderId")).put("commanderName",q.optString("commanderName")).put("switchVal",true));JSONArray b=bans(q);
        if(b.length()==1)filters.put(new JSONObject().put("type","faction").put("id",b.getString(0)).put("banned",true).put("switchVal",true));
        return new JSONObject().put("version",version).put("gameType","2").put("time",q.optInt("days",7)).put("operator","AND").put("advancedMode",false).put("filters",filters).put("exclusions",new JSONArray()).put("page",page).put("pageSize",12).put("minimumCount",Math.max(1,q.optInt("minCount",10))).put("sortBy",q.optString("sort","avgPlacement"));
    }
    private JSONObject api(JSONObject payload,JSONObject catalog)throws Exception{
        for(int attempt=0;attempt<2;attempt++){
        String t=String.valueOf(System.currentTimeMillis()),nonce=UUID.randomUUID().toString().replace("-","").substring(0,16);
        Map<String,String> h=new HashMap<>();h.put("t",t);h.put("did",deviceId);h.put("nonce",nonce);h.put("signature",digest("MD5",t+nonce+deviceId+catalog.getString("publicSignatureSalt")));h.put("Accept-Language","CN");h.put("Browser-Language","ZH-CN");h.put("App-Platform","wzwxq");h.put("Origin",ORIGIN);h.put("Referer",ORIGIN+"/");
            JSONObject result=new JSONObject(http("https://api.datatft.com/wzwxq/lineups/search",payload.toString(),h,8000000));
            if(result.has("code")&&!Arrays.asList(0,1,200).contains(result.optInt("code"))){
                String message=result.optString("message",result.optString("msg","资料查询失败"));
                if(message.contains("系统异常")){if(attempt==0)continue;throw new IOException("数据服务器暂时异常，请稍后重试");}
                throw new IOException(message);
            }
            return result.optJSONObject("data")!=null?result.getJSONObject("data"):result;
        }
        throw new IOException("数据服务器暂时不可用");
    }
    static String digest(String algorithm,String text)throws Exception{byte[] b=MessageDigest.getInstance(algorithm).digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte x:b)out.append(String.format(Locale.ROOT,"%02x",x));return out.toString();}
    private static JSONObject find(JSONArray a,String key,String value) throws Exception{if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&value.equals(x.optString(key)))return x;}return null;}
    static JSONObject normalize(JSONObject row,JSONObject catalog) throws Exception{
        Map<String,JSONObject> hs=index(catalog.optJSONArray("heroes")),items=index(catalog.optJSONArray("items"));List<JSONObject> units=new ArrayList<>();Set<String> carry=new HashSet<>();JSONArray ca=row.optJSONArray("carryHeroes");if(ca!=null)for(int i=0;i<ca.length();i++)carry.add(ca.getJSONObject(i).optString("id"));
        JSONArray heroes=row.optJSONArray("heroes");if(heroes==null)heroes=new JSONArray();
        for(int i=0;i<heroes.length();i++){
            JSONObject u=heroes.getJSONObject(i);String id=u.optString("id");JSONObject h=hs.get(id);JSONObject rec=find(row.optJSONArray("heroEquipment"),"heroId",id);if(rec==null)rec=find(row.optJSONArray("recommendedUnits"),"heroId",id);JSONArray eq=new JSONArray();
            if(rec!=null){JSONArray ids=rec.optJSONArray("itemIds"),names=rec.optJSONArray("itemNames");if(ids!=null)for(int j=0;j<ids.length();j++){String iid=ids.optString(j);JSONObject item=items.get(iid);eq.put(new JSONObject().put("id",iid).put("name",item!=null?item.optString("name"):names!=null?names.optString(j,"装备 "+iid):"装备 "+iid).put("image",item==null?"":item.optString("image")));}}
            units.add(new JSONObject().put("id",id).put("name",h==null?u.optString("name","未知英雄"):h.optString("name")).put("faction",h==null?"":h.optString("faction")).put("image",h==null?"":h.optString("image")).put("core",carry.contains(id)).put("level",rec==null?0:rec.optDouble("avgLevel",0)).put("appearanceRate",rec==null?0:rec.optDouble("appearanceRate",0)).put("items",eq));
        }
        if(ca==null){List<JSONObject> ranked=new ArrayList<>(units);ranked.sort((a,b)->Double.compare(b.optDouble("level"),a.optDouble("level")));double max=ranked.isEmpty()?0:ranked.get(0).optDouble("level");int picked=0;for(JSONObject u:ranked)if(picked<2&&max>0&&u.optDouble("level")>=max*.5&&u.optDouble("appearanceRate")>.5){u.put("core",true);picked++;}}
        units.sort((a,b)->a.optBoolean("core")!=b.optBoolean("core")?a.optBoolean("core")?-1:1:Double.compare(b.optDouble("level"),a.optDouble("level")));
        Map<String,Integer> factions=new LinkedHashMap<>();for(JSONObject h:units){String f=h.optString("faction");if(!f.isEmpty()&&!f.equals("无阵营"))factions.put(f,factions.getOrDefault(f,0)+1);}
        String faction="";int max=0;for(Map.Entry<String,Integer> f:factions.entrySet())if(f.getValue()>max){max=f.getValue();faction=f.getKey();}
        List<String> names=new ArrayList<>();if(!faction.isEmpty())names.add(faction);int selected=0;for(JSONObject h:units)if(h.optBoolean("core")&&selected++<3)names.add(h.optString("name"));if(names.size()<2)for(int i=0;i<Math.min(2,units.size());i++)names.add(units.get(i).optString("name"));
        String title=String.join(" · ",names);JSONObject play=row.optJSONObject("playstyle");if(play!=null&&!play.optString("name").isEmpty())title=play.optString("name");
        JSONObject out=new JSONObject().put("key",row.optString("lineupKey")).put("title",title).put("heroes",new JSONArray(units)).put("size",row.optInt("lineupSize",units.size())).put("count",row.optInt("count")).put("rosterCount",row.opt("rosterCount")).put("lineupCode",row.opt("lineupCode") instanceof String?row.optString("lineupCode"):"");
        for(String k:new String[]{"avgPlacement","firstRate","top3Rate","appearanceRate"})out.put(k,row.optDouble(k,0));return out;
    }
    JSONObject query(JSONObject q,JSONObject catalog,BooleanSupplier cancelled,Progress progress)throws Exception{
        JSONArray blocked=bans(q);Set<String> block=new HashSet<>();for(int i=0;i<blocked.length();i++)block.add(blocked.getString(i));List<JSONObject> rows=new ArrayList<>();Set<String> seen=new HashSet<>();int total=0,samples=0,scanned=0,page=0,maxPages=blocked.length()>1?4:2;
        for(int p=1;p<=maxPages;p++){
            if(cancelled.getAsBoolean())throw new InterruptedIOException("查询已取消");progress.stage("查询阵容 "+p+"/"+maxPages);
            JSONObject r=api(payload(q,catalog.optString("version"),p),catalog);JSONArray a=r.optJSONArray("lineups");if(a==null)throw new IOException("阵容响应格式变化");total=r.optInt("total");samples=r.optInt("sampleCount");page=p;
            for(int i=0;i<a.length();i++){scanned++;JSONObject row=normalize(a.getJSONObject(i),catalog);boolean valid=true;JSONArray units=row.getJSONArray("heroes");if(!block.isEmpty())for(int j=0;j<units.length();j++){String f=units.getJSONObject(j).optString("faction");if(f.isEmpty()||block.contains(f)){valid=false;break;}}if(valid&&seen.add(row.optString("key")))rows.add(row);}
            if(rows.size()>=8||a.length()==0||p*12>=total)break;
        }
        String sort=q.optString("sort","avgPlacement");rows.sort((a,b)->{double diff=sort.equals("avgPlacement")?a.optDouble(sort)-b.optDouble(sort):b.optDouble(sort)-a.optDouble(sort);return diff==0?b.optInt("count")-a.optInt("count"):diff>0?1:-1;});
        return new JSONObject().put("rows",new JSONArray(rows)).put("version",catalog.optString("version")).put("query",q).put("fetchedAt",System.currentTimeMillis()).put("total",total).put("sampleCount",samples).put("scanned",scanned).put("limited",page*12<total).put("scope",blocked.length()==1?"single-ban":blocked.length()>1?"multi-exclude":"commander");
    }
}
