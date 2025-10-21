package com.team19.musuimsa.shelter.util;

import com.team19.musuimsa.shelter.dto.map.ClusterFeature;
import com.team19.musuimsa.shelter.dto.map.MapShelterResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Clusterer {

    public static List<ClusterFeature> byGeohash(List<MapShelterResponse> points, int precision) {
        Map<String, List<MapShelterResponse>> groups = new HashMap<String, List<MapShelterResponse>>();
        for (MapShelterResponse s : points) {
            String gh = GeoHashUtil.geohash(s.lat(), s.lng(), precision);
            List<MapShelterResponse> bucket = groups.get(gh);
            if (bucket == null) {
                bucket = new ArrayList<MapShelterResponse>();
                groups.put(gh, bucket);
            }
            bucket.add(s);
        }
        List<ClusterFeature> out = new ArrayList<ClusterFeature>(groups.size());
        for (Map.Entry<String, List<MapShelterResponse>> e : groups.entrySet()) {
            List<MapShelterResponse> list = e.getValue();
            double latAvg = list.stream().mapToDouble(MapShelterResponse::lat).average().orElse(0.0);
            double lngAvg = list.stream().mapToDouble(MapShelterResponse::lng).average().orElse(0.0);
            out.add(new ClusterFeature(e.getKey(), latAvg, lngAvg, list.size()));
        }
        return out;
    }
}
