package com.doudou.wxq;
/** Android 12+ uses window alpha, not background alpha, for pass-through safety. */
final class OverlayTouchPolicy {
 static float windowAlpha(boolean locked,float maximum){
  if(!locked)return 1f;
  if(Float.isNaN(maximum)||maximum<0f||maximum>1f)maximum=.8f;
  return Math.max(0f,Math.min(.79f,maximum-.01f));
 }
 static int backgroundAlpha(int opacity,float windowAlpha){
  float desired=Math.max(30,Math.min(100,opacity))/100f;
  return Math.round(255f*Math.min(1f,desired/Math.max(.01f,windowAlpha)));
 }
}
