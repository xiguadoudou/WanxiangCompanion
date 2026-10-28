package com.doudou.wxq;
import org.json.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.Base64;
public final class WorkshopModelTest {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);}
 static void rejects(String code)throws Exception{try{LocalPlan.extract(code);throw new AssertionError("Invalid plan accepted");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args)throws Exception{
  JSONObject data=new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8"));WorkshopData.validate(data);JSONObject arrays=data.getJSONObject("arrays");check(arrays.getJSONArray("wanxiang_hero").length()>=85,"Complete hero records must ship locally");check(arrays.getJSONArray("wanxiang_talent").length()>=261,"Talents must ship locally");
  StringBuilder source=new StringBuilder("/* untrusted code is never executed */window.invalid();");int i=0;for(String key:arrays.keySet()){JSONArray a=arrays.getJSONArray(key);String literal=a.toString();source.append("const a").append(i++).append("=[{id:0,").append(literal.substring(2)).append(';');}
  JSONObject refreshed=WorkshopData.extract(source.toString(),"updated-build");check(refreshed.getJSONObject("arrays").getJSONArray("wanxiang_hero").length()==arrays.getJSONArray("wanxiang_hero").length(),"Refreshing literal records preserves full data");check(refreshed.getString("sourceBuild").equals("updated-build"),"Build marker updates");
  JSONArray board=new JSONArray();for(i=0;i<56;i++)board.put(JSONObject.NULL);board.put(8,new JSONArray().put("1941").put(new JSONArray().put("643100")).put(1).put(0).put(JSONObject.NULL));JSONObject plan=new JSONObject().put("v",1).put("b",board).put("p",new JSONArray()).put("t",new JSONArray()).put("s",new JSONObject());String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(plan.toString().getBytes(StandardCharsets.UTF_8)),code=LocalPlan.code(raw);
  check(code.startsWith("WXQ1:"),"Software plan codes need an explicit prefix");check(LocalPlan.extract(code).equals(raw),"Plan round-trip must preserve placements and equipment");check(LocalPlan.url(code).startsWith(SiteModules.url(SiteModules.SIMULATOR)+"?lineup="),"Imported plans open only local modules");check(LocalPlan.extract("https://www.datawxq.com/team-builder?lineup="+raw).equals(raw),"Authorized public share data can be imported without visiting the webpage");
  rejects("https://evil.test/?lineup="+raw);rejects("https://www.datawxq.com/team-builder");rejects("WXQ1:invalid");rejects("WXQ1:"+Base64.getUrlEncoder().encodeToString("{\"v\":999,\"b\":[]}".getBytes(StandardCharsets.UTF_8)));board.getJSONArray(8).put(1,new JSONArray().put("1").put("2").put("3").put("4"));rejects("WXQ1:"+Base64.getUrlEncoder().encodeToString(plan.toString().getBytes(StandardCharsets.UTF_8)));
  System.out.println("Bundled full records, safe data refresh, local plan round-trip, import origins and equipment constraints passed.");
 }
}
