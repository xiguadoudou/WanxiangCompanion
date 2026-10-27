package com.doudou.wxq;

/** Pure state used by the real service and JVM transition tests. */
final class OverlayState {
    private boolean hostVisible=true;
    private boolean expanded=false;
    void setHostVisible(boolean visible){hostVisible=visible;if(visible)expanded=false;}
    void toggle(){if(!hostVisible)expanded=!expanded;}
    void collapse(){expanded=false;}
    boolean showIcon(){return !hostVisible;}
    boolean showPanel(){return !hostVisible&&expanded;}
}
