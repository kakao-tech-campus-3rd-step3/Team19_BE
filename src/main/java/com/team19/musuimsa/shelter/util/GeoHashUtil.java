package com.team19.musuimsa.shelter.util;

public final class GeoHashUtil {
    private GeoHashUtil() {
    }

    public static int prefixForZoom(int z) {
        if (z < 13) {
            return 5;
        }
        if (z < 16) {
            return 6;
        }
        return 7;
    }

    public static String geohash(double lat, double lng, int precision) {
        double scale = Math.pow(10, precision - 2);
        long la = Math.round(lat * scale);
        long ln = Math.round(lng * scale);
        return la + "_" + ln + "_p" + precision;
    }

    public static String snapBbox(double minLat, double minLng, double maxLat, double maxLng, int p) {
        String a = geohash(minLat, minLng, p);
        String b = geohash(maxLat, maxLng, p);
        return a + "__" + b;
    }
}
