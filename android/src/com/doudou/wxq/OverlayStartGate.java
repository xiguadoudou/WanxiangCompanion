package com.doudou.wxq;
/** Ignore cancelled, timed-out and superseded startup acknowledgements. */
final class OverlayStartGate {
 private long serial;private boolean waiting;
 long begin(){waiting=true;return ++serial;}
 boolean complete(long ticket){if(!waiting||ticket!=serial)return false;waiting=false;return true;}
 boolean waiting(){return waiting;}
 void cancel(){waiting=false;++serial;}
}
