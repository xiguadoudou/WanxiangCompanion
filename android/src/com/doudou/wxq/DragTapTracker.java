package com.doudou.wxq;

final class DragTapTracker {
    private final float slopSquared;
    private float x,y;
    private boolean active,dragged;
    DragTapTracker(int slop){slopSquared=slop*slop;}
    void begin(float px,float py){x=px;y=py;active=true;dragged=false;}
    boolean move(float px,float py){if(active&&((px-x)*(px-x)+(py-y)*(py-y)>slopSquared))dragged=true;return active&&dragged;}
    boolean finish(float px,float py){if(!active)return false;move(px,py);boolean tap=!dragged;active=false;return tap;}
    void cancel(){active=false;dragged=false;}
}
