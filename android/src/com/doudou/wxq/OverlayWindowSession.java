package com.doudou.wxq;
/** A toggle stays in one system window, even when opening the panel fails. */
final class OverlayWindowSession {
 interface Surface{boolean isAttached();void attach(boolean visible,boolean expanded);void update(boolean visible,boolean expanded);void remove();}
 static final int OK=0,RECOVERED=1,FAILED=2;
 private final Surface surface;private boolean attached;private RuntimeException error;
 OverlayWindowSession(Surface surface){this.surface=surface;}
 private void apply(boolean visible,boolean expanded){if(attached&&!surface.isAttached())attached=false;if(attached)surface.update(visible,expanded);else{surface.attach(visible,expanded);attached=true;}}
 int present(boolean visible,boolean expanded){error=null;try{apply(visible,visible&&expanded);return OK;}catch(RuntimeException e){error=e;}if(visible&&expanded)try{apply(true,false);return RECOVERED;}catch(RuntimeException e){error=e;}return FAILED;}
 RuntimeException lastError(){return error;}
 void close(){if(attached){attached=false;try{surface.remove();}catch(RuntimeException ignored){}}}
}
