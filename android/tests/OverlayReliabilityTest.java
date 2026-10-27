package com.doudou.wxq;
public final class OverlayReliabilityTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static final class Fake implements OverlayWindowSession.Surface {
  int adds,updates,removes;boolean attached,visible,expanded,failPanel,failAll;
  public boolean isAttached(){return attached;}
  public void attach(boolean v,boolean e){if(failAll||e&&failPanel)throw new IllegalStateException("OEM window rejected");check(!attached,"Duplicate system window");adds++;attached=true;visible=v;expanded=e;}
  public void update(boolean v,boolean e){if(failAll||e&&failPanel)throw new IllegalStateException("OEM window rejected");check(attached,"Update detached window");updates++;visible=v;expanded=e;}
  public void remove(){removes++;attached=false;}
 }
 public static void main(String[] args){
  Fake f=new Fake();OverlayWindowSession s=new OverlayWindowSession(f);OverlayState state=new OverlayState();
  check(s.present(false,false)==0&&!f.visible&&f.attached,"Prepare while main is visible must be hidden");state.setHostVisible(false);
  for(int i=0;i<200;i++){state.toggle();check(s.present(state.showIcon(),state.showPanel())==0,"Toggle failed");}
  check(f.adds==1&&f.removes==0,"Toggling must neither detach nor reparent the system toggle");
  f.failPanel=true;state.toggle();check(s.present(true,true)==OverlayWindowSession.RECOVERED,"A failed expansion should fall back");state.collapse();check(f.visible&&!f.expanded&&f.attached,"Compact icon must survive a panel exception");
  f.failPanel=false;check(s.present(true,true)==0&&f.expanded,"Retry can open the panel");check(s.present(false,false)==0&&!f.visible,"Main hides both icon and panel without detach");
  f.attached=false;check(s.present(true,false)==0&&f.adds==2,"Unexpected system detach should be reattached");
  f.failAll=true;check(s.present(true,true)==OverlayWindowSession.FAILED,"Total rejection must report failure");s.close();check(!f.attached,"Failed large window must be removed");f.failAll=false;check(s.present(true,false)==0&&f.visible,"Explicit retry restores compact toggle");
  OverlayStartGate g=new OverlayStartGate();long a=g.begin(),b=g.begin();check(!g.complete(a)&&g.waiting(),"Old callback must not go Home");check(g.complete(b)&&!g.complete(b),"Only one accepted acknowledgement");a=g.begin();g.cancel();check(!g.complete(a),"Cancel/stop must prevent later Home navigation");a=g.begin();check(g.complete(a)&&!g.waiting(),"Timeout closes pending request");check(!g.complete(a),"Success arriving after timeout must not go Home");
  System.out.println("200 in-place toggles, failed-panel icon recovery, retry, hidden preparation, unexpected detach and startup acknowledgements passed.");
 }
}
