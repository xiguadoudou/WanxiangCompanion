package com.doudou.wxq;

/** Pure state used by the real service and JVM transition tests. */
final class OverlayState {
    private boolean hostVisible=true;
    private boolean expanded=false;
    private boolean strip=false;
    private boolean locked=false;
    void setHostVisible(boolean visible){hostVisible=visible;if(visible)collapse();}
    void toggle(){if(!hostVisible&&!locked){if(strip){strip=false;return;}expanded=!expanded;}}
    void collapse(){expanded=false;strip=false;locked=false;}
    void setStrip(boolean enabled){strip=enabled&&showPanel();if(!strip)locked=false;}
    void setLocked(boolean enabled){locked=enabled&&showStrip();}
    boolean isLocked(){return showStrip()&&locked;}
    boolean showStrip(){return showPanel()&&strip;}
    boolean showIcon(){return !hostVisible;}
    boolean showPanel(){return !hostVisible&&expanded;}
}
