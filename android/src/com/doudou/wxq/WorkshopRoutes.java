package com.doudou.wxq;
/** Exact packaged resource routes, also used by the offline browser checks. */
final class WorkshopRoutes {
 static String file(String path){
  if("/team-builder".equals(path)||"/simulator".equals(path)||"/h5/database".equals(path))return "index.html";
  if("/port.js".equals(path)||"/port.css".equals(path)||"/port-manifest.json".equals(path)||"/navigation-gesture.js".equals(path))return path.substring(1);
  if(path!=null&&(path.matches("/assets/[A-Za-z0-9_.-]+\\.(js|css)")||path.matches("/assets/level/[1-5]\\.png")))return path.substring(1);
  return null;
 }
 static String mime(String name){if(name.endsWith(".html"))return "text/html";if(name.endsWith(".js"))return "application/javascript";if(name.endsWith(".css"))return "text/css";if(name.endsWith(".json"))return "application/json";return "image/png";}
}
