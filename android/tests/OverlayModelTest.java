package com.doudou.wxq;

public final class OverlayModelTest {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args){
        OverlayState s=new OverlayState();
        check(!s.showIcon()&&!s.showPanel(),"No windows inside the app at startup");
        s.toggle();check(!s.showPanel(),"Host foreground must reject icon toggling");
        s.setHostVisible(false);check(s.showIcon()&&!s.showPanel(),"Leaving app shows only icon");
        s.toggle();check(s.showIcon()&&s.showPanel(),"Icon remains visible with expanded panel");
        s.toggle();check(s.showIcon()&&!s.showPanel(),"Same icon closes panel without stopping helper");
        for(int i=0;i<20;i++){s.toggle();check(s.showIcon(),"Icon must stay available after every toggle");}
        s.toggle();s.setHostVisible(true);check(!s.showIcon()&&!s.showPanel(),"Return to own app hides both windows");
        s.setHostVisible(false);check(s.showIcon()&&!s.showPanel(),"Leaving own app must not reopen a panel unexpectedly");
        s.toggle();s.collapse();check(s.showIcon()&&!s.showPanel(),"Back collapses panel while preserving icon");
        int layouts=0;
        for(int[] size:new int[][]{{800,364},{640,320},{392,800},{1536,698},{2400,1080}}){
            int w=size[0],h=size[1];
            for(int x:new int[]{0,w/2,w-44})for(int y:new int[]{0,h/2,h-44}){
                OverlayGeometry.Box b=OverlayGeometry.place(w,h,x,y,392,230);
                check(b.x>=0&&b.y>=0&&b.x+b.width<=w&&b.y+b.height<=h,"Panel stays on screen");
                check(b.iconX>=0&&b.iconY>=0&&b.iconX+44<=w&&b.iconY+44<=h,"Icon stays on screen");
                check(b.iconX==b.x&&b.iconY==b.y,"Icon must occupy the reserved panel header cell, never a left-side column");
                check(b.width<=392&&b.height<=230,"Default dimensions cannot grow unexpectedly");layouts++;
            }
        }
        OverlayGeometry.Box edge=OverlayGeometry.place(800,364,0,40,392,230);
        check(edge.x==0&&edge.iconX==0,"Landscape panel must reach the physical left edge");
        check(OverlayGeometry.place(800,364,999,999,392,230).x==408,"Panel can reach the right edge too");
        int resizes=0;
        for(int[] screen:new int[][]{{800,364},{392,800},{1536,698},{2400,1080}}){
            for(int density:new int[]{1,2,3}){
                int w=screen[0],h=screen[1];OverlayGeometry.Box b=OverlayGeometry.place(w,h,0,0,392*density,230*density);
                for(int dx:new int[]{-5000,-50,0,100,5000})for(int dy:new int[]{-5000,-50,0,100,5000}){
                    OverlayGeometry.Box r=OverlayGeometry.resize(w,h,b.x,b.y,b.width,b.height,dx,dy,250*density,170*density);
                    check(r.x==b.x&&r.y==b.y,"Bottom-right resize cannot move the top-left anchor");
                    check(r.width>0&&r.height>0&&r.x+r.width<=w&&r.y+r.height<=h,"Resizing stays within screen");
                    check(r.width>=Math.min(250*density,w)&&r.height>=Math.min(170*density,h),"Minimum touch-readable size enforced");resizes++;
                }
            }
        }
        OverlayGeometry.Box offset=OverlayGeometry.resize(800,364,150,80,392,230,900,900,250,170);
        check(offset.x==150&&offset.y==80&&offset.width==650&&offset.height==284,"Resize stops at screen edge without moving the anchor");
        OverlayGeometry.Box rotated=OverlayGeometry.place(392,800,650,250,760,210);
        check(rotated.x==0&&rotated.width==392,"Orientation change fits saved wide layout to the new screen");
        check(392*230<560*340*.5,"Default panel must occupy under half of previous area");
        DragTapTracker g=new DragTapTracker(8);
        g.begin(100,100);check(!g.move(103,104),"Small finger jitter must remain a tap");check(g.finish(104,103),"Tap must toggle the panel");
        g.begin(100,100);check(g.move(109,100),"Movement beyond system slop starts dragging");check(!g.finish(100,100),"A drag returning to its start cannot accidentally toggle");
        g.begin(100,100);check(!g.finish(120,100),"Large displacement on release cannot count as a tap");
        g.begin(100,100);g.cancel();check(!g.finish(100,100),"Cancelled touches cannot toggle");
        System.out.println("Overlay foreground/toggle transitions and "+layouts+" edge-to-edge placements and "+resizes+" resize cases passed");
    }
}
