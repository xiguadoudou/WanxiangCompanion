package com.doudou.wxq;
import java.net.URI;
/** Public online modules are isolated from the local app's Native bridge. */
final class SiteModules {
 static final String HOME="recommendation",SIMULATOR="simulator",DATABASE="database";
 static boolean valid(String id){return HOME.equals(id)||SIMULATOR.equals(id)||DATABASE.equals(id);}
 static String title(String id){if(SIMULATOR.equals(id))return "阵容模拟";if(DATABASE.equals(id))return "万象资料库";return "阵容推荐";}
 static String url(String id){if(SIMULATOR.equals(id))return "https://www.datawxq.com/team-builder";if(DATABASE.equals(id))return "https://www.datawxq.com/h5/database";throw new IllegalArgumentException("Unknown online module");}
 static boolean allowed(String raw){try{URI u=new URI(raw);return "https".equalsIgnoreCase(u.getScheme())&&"www.datawxq.com".equalsIgnoreCase(u.getHost())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443);}catch(Exception e){return false;}}
}
