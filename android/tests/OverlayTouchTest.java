package com.doudou.wxq;
import org.json.JSONObject;
public final class OverlayTouchTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  for(float max:new float[]{0f,.3f,.5f,.8f,1f}){
   float alpha=OverlayTouchPolicy.windowAlpha(true,max);check(alpha>=0f&&alpha<=max&&alpha<1f,"Locked alpha must permit Android touch delivery");
   check(OverlayTouchPolicy.windowAlpha(false,max)==1f,"Unlock restores full text opacity");
   if(alpha>=.5f)check(Math.abs(OverlayTouchPolicy.backgroundAlpha(50,alpha)/255f*alpha-.5f)<.005f,"Lock preserves requested 50% background appearance");
  }
  check(OverlayTouchPolicy.windowAlpha(true,Float.NaN)<.8f,"Invalid system limit falls back safely");
  OverlayState s=new OverlayState();s.setLocked(true);check(!s.isLocked(),"Cannot lock hidden helper");s.setHostVisible(false);s.toggle();s.setLocked(true);check(!s.isLocked(),"Only simple mode can be locked");s.setStrip(true);s.setLocked(true);check(s.isLocked(),"Simple mode locks");s.toggle();check(s.isLocked()&&s.showStrip(),"Locked content cannot restore accidentally");s.setLocked(false);s.toggle();check(s.showPanel()&&!s.showStrip(),"Unlock permits normal restore");s.setStrip(true);s.setLocked(true);s.setHostVisible(true);check(!s.isLocked()&&!s.showIcon(),"Main clears lock and all floating controls");s.setHostVisible(false);s.toggle();s.setStrip(true);s.setLocked(true);s.collapse();check(!s.isLocked(),"Window failure recovery clears lock");
  for(int old:new int[]{-1,88,37,50}){JSONObject ui=new JSONObject();if(old>=0)ui.put("opacity",old);OverlayAppearance.migrate(ui);check(ui.getInt("opacity")==((old==-1||old==88)?50:old),"Fresh/default migration and custom preservation");ui.put("opacity",88);OverlayAppearance.migrate(ui);check(ui.getInt("opacity")==88,"Later explicit opacity choice must not be migrated again");}
  int count=0;
  for(int density:new int[]{1,2,3})for(int[] screen:new int[][]{{392,800},{430,900},{800,392}}){
   int sw=screen[0]*density,sh=screen[1]*density,top=sw<sh?40*density:0,bottom=sw<sh?24*density:0;
   for(int x:new int[]{-100,0,sw-44*density,sw+100})for(int y:new int[]{-500,0,sh/2,sh+500}){
    OverlayGeometry.Box icon=OverlayGeometry.placeSafe(sw,sh,x,y,44*density,44*density,top,bottom);
    check(icon.y>=top&&icon.y+icon.height<=sh-bottom,"Portrait icon cannot enter status/navigation bars");
    OverlayGeometry.Box panel=OverlayGeometry.placeSafe(sw,sh,x,y,513*density,86*density,top,bottom);
    check(panel.y>=top&&panel.y+panel.height<=sh-bottom,"Simple panel remains within safe vertical bounds");
    OverlayGeometry.Box u=OverlayGeometry.unlock(sw,sh-top-bottom,new OverlayGeometry.Box(panel.x,panel.y-top,panel.width,panel.height),28*density,3*density);
    check(u.x>=0&&u.y>=0&&u.x+u.width<=sw&&u.y+u.height<=sh-top-bottom,"Unlock button remains accessible after rotation");
    check(u.x+u.width<=panel.x||u.x>=panel.x+panel.width||u.y+u.height<=panel.y-top||u.y>=panel.y-top+panel.height,"Unlock button stays outside the click-through panel");count++;
   }
   check(OverlayGeometry.placeSafe(sw,sh,0,0,250*density,170*density,top,bottom).x==0,"Status-bar protection retains physical left edge");
  }
  System.out.println("Lock/unlock lifecycle, safe window alpha, opacity migration and "+count+" portrait/rotation/unlock placements passed.");
 }
}
