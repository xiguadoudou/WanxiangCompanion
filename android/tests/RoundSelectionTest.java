package com.doudou.wxq;
import java.nio.file.*;
import org.json.*;

public final class RoundSelectionTest {
 public static void main(String[] args)throws Exception{
  JSONObject c=new JSONObject(new String(Files.readAllBytes(Paths.get("assets/catalog-snapshot.json")),"UTF-8"));
  JSONObject old=new JSONObject().put("commanderId","commander-r6").put("bans",new JSONArray().put("河洛")).put("opacity",61).put("minCount",100).put("width",420).put("minimumDefaultRevision",1);
  JSONObject entering=RoundSelection.reset(old,c,true);
  if(!entering.getString("commanderId").equals("commander-r11")||entering.getJSONArray("bans").length()!=0)throw new AssertionError("Entry defaults not applied");
  if(entering.getInt("opacity")!=61||entering.getInt("minCount")!=100||entering.getInt("width")!=420||entering.getInt("minimumDefaultRevision")!=1)throw new AssertionError("Preferences lost");
  if(old.getJSONArray("bans").length()!=1)throw new AssertionError("Input mutated");
  if(!RoundSelection.reset(old,c,false).getString("commanderId").isEmpty())throw new AssertionError("Manual new round should wait for a chess player");
  JSONObject renamed=new JSONObject().put("commanders",new JSONArray().put(new JSONObject().put("id","future-id").put("name","嫦小娥")));
  if(!RoundSelection.defaultCommander(renamed).equals("future-id")||!RoundSelection.defaultCommander(new JSONObject()).isEmpty())throw new AssertionError("Catalog fallback broken");
  System.out.println("Main entry defaults to 常小娥, bans clear, preferences preserved, manual round reset and catalog fallback passed.");
 }
}
