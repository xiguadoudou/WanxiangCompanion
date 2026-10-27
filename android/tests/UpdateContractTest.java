package com.doudou.wxq;
import org.json.*;
public final class UpdateContractTest {
 static JSONObject manifest()throws Exception{return new JSONObject().put("packageName","com.doudou.wxq").put("versionCode",11).put("versionName","0.1.10").put("apkUrl","https://updates.example/release.apk").put("sha256",String.join("",java.util.Collections.nCopies(64,"a"))).put("sizeBytes",10000);}
 static void rejects(String field,Object value)throws Exception{JSONObject m=manifest().put(field,value);try{UpdateContract.validate(m);throw new AssertionError("Accepted invalid "+field);}catch(IllegalArgumentException expected){}}
 public static void main(String[] args)throws Exception{
  if(!UpdateContract.newer(UpdateContract.validate(manifest())))throw new AssertionError();
  if(UpdateContract.newer(manifest().put("versionCode",BuildInfo.VERSION_CODE))||UpdateContract.newer(manifest().put("versionCode",1)))throw new AssertionError("Same/old version offered");
  rejects("packageName","other.app");rejects("apkUrl","http://updates.example/a.apk");rejects("apkUrl","https://user@updates.example/a.apk");rejects("sha256","invalid");rejects("sizeBytes",-1);rejects("sizeBytes",101*1024*1024);rejects("versionCode",0);
  System.out.println("Update checks passed: newer/same/older versions, package identity, HTTPS URL, hash and size validation.");
 }
}
