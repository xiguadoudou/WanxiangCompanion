package com.doudou.wxq;
import org.json.JSONObject;
/** Defaults migrate once; subsequent custom opacity choices are preserved. */
final class OverlayAppearance {
 static JSONObject migrate(JSONObject ui){
  if(ui.optInt("opacityDefaultRevision")<1){
   if(!ui.has("opacity")||ui.optInt("opacity")==88)try{ui.put("opacity",50);}catch(Exception e){throw new IllegalStateException(e);}
   try{ui.put("opacityDefaultRevision",1);}catch(Exception e){throw new IllegalStateException(e);}
  }
  return ui;
 }
}
