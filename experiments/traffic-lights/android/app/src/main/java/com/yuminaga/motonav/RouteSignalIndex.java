package com.yuminaga.motonav;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Projects SDK signal-bearing link endpoints onto the user's unchanged web route.
 * A marked link, matching approach geometry and named road are all mandatory.
 */
public final class RouteSignalIndex {
    public static final class Node {
        public final double lat,lng,backLat,backLng;
        public final String road;
        public Node(double lat,double lng,double backLat,double backLng,String road) {
            this.lat=lat;this.lng=lng;this.backLat=backLat;this.backLng=backLng;this.road=road;
        }
    }
    public static final class Signal {
        public final double progress,lat,lng;
        Signal(double progress,Node n){this.progress=progress;lat=n.lat;lng=n.lng;}
    }
    public final List<Signal> signals=new ArrayList<>();
    public RouteSignalIndex(MotorTrack track,String[] roads,List<Node> nodes) {
        for(Node n:nodes) {
            if(Thread.currentThread().isInterrupted())return;
            double back=CarSignalAlignment.metres(n.lat,n.lng,n.backLat,n.backLng);
            if(back<8 || back>100 || n.road==null || n.road.trim().isEmpty() || n.road.equals("无名道路"))continue;
            double best=Double.POSITIVE_INFINITY,progress=-1;
            List<Double> matches=new ArrayList<>();
            for(int i=1;i<track.points.length;i++) {
                if((i&4095)==0 && Thread.currentThread().isInterrupted())return;
                double[] a=track.points[i-1],b=track.points[i];
                double scale=Math.cos(Math.toRadians(n.lat))*111320;
                double px=(n.lng-a[1])*scale,py=(n.lat-a[0])*111320;
                double bx=(b[1]-a[1])*scale,by=(b[0]-a[0])*111320;
                double square=bx*bx+by*by;
                if(square<.01)continue;
                double t=Math.max(0,Math.min(1,(px*bx+py*by)/square));
                double offset=Math.hypot(px-t*bx,py-t*by);
                if(offset>18 || roads==null || i-1>=roads.length || !n.road.trim().equals(roads[i-1]))continue;
                double p=track.cum[i-1]+(track.cum[i]-track.cum[i-1])*t;
                if(p<back)continue;
                double[] before=track.at(p-back);
                if(CarSignalAlignment.metres(before[0],before[1],n.backLat,n.backLng)>18)continue;
                float incoming=MotorTrack.bearing(n.backLat,n.backLng,n.lat,n.lng);
                float projected=MotorTrack.bearing(before[0],before[1],n.lat,n.lng);
                double angle=Math.abs(incoming-projected);angle=Math.min(angle,360-angle);
                if(angle>30)continue;
                matches.add(p);
                if(offset<best){best=offset;progress=p;}
            }
            if(progress<0)continue;
            boolean ambiguous=false;
            for(double p:matches)if(Math.abs(p-progress)>80){ambiguous=true;break;}
            if(!ambiguous)signals.add(new Signal(progress,n));
        }
        signals.sort(Comparator.comparingDouble(s->s.progress));
        // Repeated SDK link endpoints describe one signal, not extra intersections.
        for(int i=signals.size()-1;i>0;i--) {
            Signal a=signals.get(i-1),b=signals.get(i);
            if(b.progress-a.progress<8 && CarSignalAlignment.metres(a.lat,a.lng,b.lat,b.lng)<3)signals.remove(i);
        }
    }
    public int next(double progress) {
        if(!Double.isFinite(progress))return -1;
        for(int i=0;i<signals.size();i++)if(signals.get(i).progress>=progress)return i;
        return -1;
    }
    public int distance(double progress) {
        int i=next(progress);
        if(i<0)return -1;
        // With no upstream junction ID, tightly spaced signals are ambiguous.
        if(i>0 && signals.get(i).progress-signals.get(i-1).progress<80)return -1;
        if(i+1<signals.size() && signals.get(i+1).progress-signals.get(i).progress<80)return -1;
        double d=signals.get(i).progress-progress;
        return d>=0 && d<=300 ? (int)Math.round(d) : -1;
    }
}
