package com.doudou.wxq;
import java.util.*;import java.util.zip.*;import java.nio.charset.StandardCharsets;import java.util.regex.*;
public final class WorkshopRoutesTest {
 public static void main(String[] args)throws Exception{
  try(ZipFile zip=new ZipFile(args[0])){
   String file=WorkshopRoutes.file("/port-manifest.json");if(!"port-manifest.json".equals(file)||!"application/json".equals(WorkshopRoutes.mime(file)))throw new AssertionError("Export manifest must be reachable through the actual Android resource policy");
   String manifest=new String(read(zip,file),StandardCharsets.UTF_8);Matcher desktop=Pattern.compile("\"desktop\"\\s*:\\s*\"([^\"]+)\"").matcher(manifest);if(!desktop.find())throw new AssertionError("Missing desktop capture module");String capture="assets/"+desktop.group(1);if(!capture.equals(WorkshopRoutes.file("/"+capture)))throw new AssertionError("Capture library is blocked");read(zip,capture);
   if(WorkshopRoutes.file("/library-data.json")!=null||WorkshopRoutes.file("/../port-manifest.json")!=null||WorkshopRoutes.file("/login")!=null)throw new AssertionError("Unexpected local resource exposure");
   if(args.length>1&&"--list".equals(args[1])){for(String path:new String[]{"/team-builder","/simulator","/h5/database"})System.out.println(path);Enumeration<? extends ZipEntry> entries=zip.entries();while(entries.hasMoreElements()){String name=entries.nextElement().getName();if(name.equals(WorkshopRoutes.file("/"+name)))System.out.println("/"+name);}}
   else System.out.println("Android resource routing, JSON export manifest, packaged capture module and blocked paths passed.");
  }
 }
 private static byte[] read(ZipFile zip,String name)throws Exception{ZipEntry e=zip.getEntry(name);if(e==null)throw new AssertionError("Missing packaged resource: "+name);try(java.io.InputStream in=zip.getInputStream(e);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
}
