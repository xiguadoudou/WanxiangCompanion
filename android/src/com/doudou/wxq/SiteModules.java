package com.doudou.wxq;
import java.net.URI;
/** Application-local modules; no website page is loaded. */
final class SiteModules {
 static final String HOST="appassets.androidplatform.net";
 static final String HOME="recommendation",SIMULATOR="simulator",DATABASE="database";
 static boolean valid(String id){return HOME.equals(id)||SIMULATOR.equals(id)||DATABASE.equals(id);}
 static String title(String id){if(SIMULATOR.equals(id))return "阵容模拟";if(DATABASE.equals(id))return "万象资料库";return "阵容推荐";}
 static String url(String id){if(SIMULATOR.equals(id))return "https://appassets.androidplatform.net/team-builder";if(DATABASE.equals(id))return "https://appassets.androidplatform.net/h5/database";throw new IllegalArgumentException("Unknown online module");}
 static boolean allowed(String raw){try{URI u=new URI(raw);return "https".equalsIgnoreCase(u.getScheme())&&HOST.equalsIgnoreCase(u.getHost())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&java.util.Arrays.asList("/team-builder","/simulator","/h5/database").contains(u.getPath());}catch(Exception e){return false;}}
}
