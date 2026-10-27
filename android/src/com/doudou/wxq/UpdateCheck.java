package com.doudou.wxq;
import java.io.*;
import java.util.*;
import org.json.*;

final class UpdateCheck {
 interface Reader{String read(String url)throws Exception;}
 interface Progress{void source(String label,int index,int total);}
 static final class Result{final JSONObject manifest;final String source;Result(JSONObject m,String s){manifest=m;source=s;}}
 static Result load(Reader reader,Progress progress)throws Exception{
  List<UpdateRoutes.Route> routes=UpdateRoutes.metadata(System.currentTimeMillis());List<String> failures=new ArrayList<>();
  for(int i=0;i<routes.size();i++){
   UpdateRoutes.Route route=routes.get(i);progress.source(route.label,i+1,routes.size());
   try{JSONObject m=UpdateContract.validate(new JSONObject(reader.read(route.url)));if(m.getInt("versionCode")<BuildInfo.VERSION_CODE)throw new IOException("线路版本信息尚未同步");return new Result(m,route.label);}
   catch(Exception e){failures.add(route.label+"："+UpdateRoutes.reason(e));}
  }
  throw new IOException(String.join("；",failures));
 }
}
