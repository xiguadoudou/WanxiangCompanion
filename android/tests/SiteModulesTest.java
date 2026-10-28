package com.doudou.wxq;
public final class SiteModulesTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args){
  check(SiteModules.url(SiteModules.SIMULATOR).equals("https://appassets.androidplatform.net/team-builder"),"Simulator must use the app asset origin");check(SiteModules.url(SiteModules.DATABASE).equals("https://appassets.androidplatform.net/h5/database"),"Use packaged mobile database");
  for(String url:new String[]{SiteModules.url(SiteModules.SIMULATOR),SiteModules.url(SiteModules.DATABASE),"https://appassets.androidplatform.net/team-builder?lineup=test","https://appassets.androidplatform.net/h5/database?tab=equipments&dbq=test","https://appassets.androidplatform.net:443/h5/database#detail"})check(SiteModules.allowed(url),"Valid site route rejected");
  check(SiteModules.owns(SiteModules.SIMULATOR,"https://appassets.androidplatform.net/team-builder?lineup=test"),"Plan queries must stay usable");
  check(SiteModules.owns(SiteModules.DATABASE,"https://appassets.androidplatform.net/h5/database?tab=talents"),"Database queries must stay usable");
  check(!SiteModules.owns(SiteModules.SIMULATOR,SiteModules.url(SiteModules.DATABASE))&&!SiteModules.owns(SiteModules.DATABASE,SiteModules.url(SiteModules.SIMULATOR)),"Only native sidebar may switch modules");
  for(String url:new String[]{"http://www.datawxq.com/database","https://www.datawxq.com.evil.test/database","https://www.datawxq.com@evil.test/","https://user@www.datawxq.com/","https://www.datawxq.com:444/","file:///android_asset/index.html","javascript:Native.command('exitApp')","intent://site","https://evil.test/?next=https://www.datawxq.com/","https://www.datawxq.com/team-builder","https://appassets.androidplatform.net/login",null})check(!SiteModules.allowed(url),"Unsafe main-frame navigation accepted: "+url);
  ModuleTabs<StringBuilder> tabs=new ModuleTabs<>();StringBuilder home=new StringBuilder("selection");tabs.home(home);final int[] creates={0};ModuleTabs.Factory<StringBuilder> f=id->{creates[0]++;return new StringBuilder(id);};
  StringBuilder simulation=tabs.select(SiteModules.SIMULATOR,f);simulation.append("/selected hero and equipment");StringBuilder db=tabs.select(SiteModules.DATABASE,f);db.append("/search text and detail");
  for(int i=0;i<200;i++){check(tabs.select(SiteModules.HOME,f)==home,"Recommendation state must survive tab switching");check(tabs.select(SiteModules.SIMULATOR,f)==simulation,"Simulator must not reload when returning");check(tabs.select(SiteModules.DATABASE,f)==db,"Database filter state must survive");}
  check(creates[0]==2,"Only two online module views may be created");check(tabs.values().size()==3,"All sections retained");
  try{tabs.select("https://evil.test",f);throw new AssertionError("Unknown module accepted");}catch(IllegalArgumentException expected){}check(tabs.selected().equals(SiteModules.DATABASE)&&creates[0]==2,"Bad selection cannot corrupt the current tab");
  tabs.clear();tabs.home(home);try{tabs.select(SiteModules.SIMULATOR,id->{throw new IllegalStateException("WebView unavailable");});throw new AssertionError();}catch(IllegalStateException expected){}check(tabs.selected().equals(SiteModules.HOME)&&tabs.values().size()==1,"Failed module creation must preserve recommendation view");
  System.out.println("Verified module routes, origin isolation, 200 state-preserving switches, lazy loading and failed creation recovery passed.");
 }
}
