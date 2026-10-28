package com.doudou.wxq;
import org.json.*;import java.util.regex.*;
/** Extracts literal game records only; downloaded code is never evaluated. */
final class WorkshopData {
 static String key(JSONObject first){String game=first.optString("gameType"),type=first.optString("type");if(!game.isEmpty()&&!type.isEmpty())return game+"_"+type;if(first.has("lordId")&&first.has("sections"))return "commanders";if(first.has("sourceLordId"))return "specialHeroes";if(first.has("cards")&&first.has("id"))return "packs";if(first.has("typeName")&&first.has("description"))return "extraEffects";return "";}
 static JSONObject extract(String source,String build)throws Exception{
  JSONObject arrays=new JSONObject();Matcher m=Pattern.compile("(?:\\b(?:const|let|var)\\s+|,)([A-Za-z_$][\\w$]*)\\s*=\\s*(\\[\\{id:)").matcher(source);
  while(m.find())try{JSONArray a=(JSONArray)new JsDataReader(source,m.start(2)).read();JSONObject first=a.optJSONObject(0);if(first!=null){String key=key(first);if(!key.isEmpty())arrays.put(key,a);}}catch(Exception ignored){}
  for(String key:new String[]{"diamond_equipment","diamond_effect","diamond_talent","extraEffects","specialHeroes","packs"})if(!arrays.has(key))arrays.put(key,new JSONArray());
  JSONObject out=new JSONObject().put("schema",1).put("sourceBuild",build).put("arrays",arrays);validate(out);return out;
 }
 static void validate(JSONObject data){JSONObject a=data.optJSONObject("arrays");if(data.optInt("schema")!=1||a==null||count(a,"wanxiang_hero")<50||count(a,"wanxiang_equipment")<30||count(a,"commanders")<10||count(a,"wanxiang_talent")<50||count(a,"wanxiang_effect")<30)throw new IllegalArgumentException("完整资料不齐全，继续使用已保存版本");}
 private static int count(JSONObject a,String key){JSONArray v=a.optJSONArray(key);return v==null?0:v.length();}
}
