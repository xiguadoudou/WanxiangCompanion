package com.doudou.wxq;

/** Screen pixels. The toggle occupies a reserved cell inside the panel header. */
final class OverlayGeometry {
    static final int MIN_WIDTH_DP=250,MIN_HEIGHT_DP=170,MAX_WIDTH_DP=960,MAX_HEIGHT_DP=900;
    static final class Box {
        final int iconX,iconY,x,y,width,height;
        Box(int x,int y,int w,int h){iconX=x;iconY=y;this.x=x;this.y=y;width=w;height=h;}
    }
    static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(v,Math.max(lo,hi)));}
    static Box place(int sw,int sh,int ix,int iy,int wantedW,int wantedH){
        int w=clamp(wantedW,1,sw),h=clamp(wantedH,1,sh);
        return new Box(clamp(ix,0,sw-w),clamp(iy,0,sh-h),w,h);
    }
    static Box resize(int sw,int sh,int x,int y,int width,int height,int dx,int dy,int minW,int minH){
        int w=clamp(width+dx,Math.min(minW,sw-x),sw-x);
        int h=clamp(height+dy,Math.min(minH,sh-y),sh-y);
        return new Box(x,y,w,h);
    }
}
