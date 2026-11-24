package fr.studioniko.network;
import java.util.*;
final class RadioMath {
    static final int NA = Integer.MAX_VALUE;
    // LTE downlink EARFCN ranges, 3GPP TS 36.101 table 5.7.3-1, selected EU bands.
    static final int[][] LTE = {{1,0,599,21100},{3,1200,1949,18050},{7,2750,3449,26200},{8,3450,3799,9250},{20,6150,6449,7910},{28,9210,9659,7580},{32,9920,10359,14520},{38,37750,38249,25700},{40,38650,39649,23000}};
    static int band(int n){for(int[] b:LTE) if(n>=b[1]&&n<=b[2])return b[0];return NA;}
    static double lteMHz(int n){for(int[] b:LTE) if(n>=b[1]&&n<=b[2])return (b[3]+n-b[1])/10.0;return Double.NaN;}
    // Global NR raster: reference frequency, not necessarily center of carrier.
    static double nrMHz(int n){if(n<0||n>3279165)return Double.NaN;if(n<600000)return n*.005;if(n<2016667)return 3000+(n-600000)*.015;return 24250.08+(n-2016667)*.06;}
    static String fmt(double n){return Double.isFinite(n)?String.format(Locale.FRANCE,"%.3f",n).replaceAll("0+$","").replaceAll(",$",""):"—";}
    static String value(int n,String unit){return n==NA?"—":n+unit;}
    static String quality(int rsrp){if(rsrp==NA)return "Non communiqué";if(rsrp>=-85)return "Très forte";if(rsrp>=-95)return "Bonne";if(rsrp>=-105)return "Correcte";if(rsrp>=-115)return "Faible";return "Très faible";}
    static double median(List<Integer> in){if(in.isEmpty())return Double.NaN;List<Integer> a=new ArrayList<>(in);Collections.sort(a);int m=a.size()/2;return a.size()%2==1?a.get(m):((double)a.get(m-1)+a.get(m))/2;}
}
