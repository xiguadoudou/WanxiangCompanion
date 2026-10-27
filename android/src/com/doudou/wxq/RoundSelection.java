package com.doudou.wxq;

import org.json.*;

/** Round choices reset independently from the user's appearance/statistics preferences. */
final class RoundSelection {
 static JSONObject reset(JSONObject previous,JSONObject catalog,boolean selectDefault){
  try{
   JSONObject ui=new JSONObject(previous.toString());
   ui.put("commanderId",selectDefault?defaultCommander(catalog):"");
   ui.put("bans",new JSONArray());
   return ui;
  }catch(JSONException e){throw new IllegalStateException(e);}
 }
 static String defaultCommander(JSONObject catalog){
  JSONArray list=catalog.optJSONArray("commanders");if(list==null)return "";
  String byName="";
  for(int i=0;i<list.length();i++){
   JSONObject c=list.optJSONObject(i);if(c==null)continue;
   String id=c.optString("id"),name=c.optString("name");
   if("commander-r11".equals(id))return id;
   if("常小娥".equals(name)||"嫦小娥".equals(name))byName=id;
  }
  return byName;
 }
}
