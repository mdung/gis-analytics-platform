package com.example.gis.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GeoJsonConverter {
    private final ObjectMapper objectMapper;
    private final WKTReader wktReader;
    private final WKTWriter wktWriter;

    public GeoJsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.wktReader = new WKTReader();
        this.wktWriter = new WKTWriter();
    }

    public ObjectNode geometryToGeoJson(Geometry geom) {
        ObjectNode geoJson = objectMapper.createObjectNode();
        geoJson.put("type", geom.getGeometryType());
        
        if (geom instanceof Point) {
            Point point = (Point) geom;
            geoJson.set("coordinates", objectMapper.valueToTree(new double[]{
                    point.getX(), point.getY()
            }));
        } else if (geom instanceof LineString) {
            LineString line = (LineString) geom;
            geoJson.set("coordinates", objectMapper.valueToTree(coordinatesToArray(line.getCoordinates())));
        } else if (geom instanceof Polygon) {
            Polygon polygon = (Polygon) geom;
            List<List<double[]>> coordinates = new ArrayList<>();
            coordinates.add(coordinatesToArray(polygon.getExteriorRing().getCoordinates()));
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                coordinates.add(coordinatesToArray(polygon.getInteriorRingN(i).getCoordinates()));
            }
            geoJson.set("coordinates", objectMapper.valueToTree(coordinates));
        }
        
        return geoJson;
    }

    public Geometry geoJsonToGeometry(Map<String, Object> geoJson) {
        String type = (String) geoJson.get("type");
        Object coords = geoJson.get("coordinates");
        
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        
        if ("Point".equals(type)) {
            List<Number> coordList = (List<Number>) coords;
            return factory.createPoint(new Coordinate(
                    coordList.get(0).doubleValue(),
                    coordList.get(1).doubleValue()
            ));
        } else if ("LineString".equals(type)) {
            return factory.createLineString(coordinatesFromArray((List<?>) coords));
        } else if ("Polygon".equals(type)) {
            List<List<?>> rings = (List<List<?>>) coords;
            LinearRing shell = factory.createLinearRing(coordinatesFromArray(rings.get(0)));
            LinearRing[] holes = new LinearRing[rings.size() - 1];
            for (int i = 1; i < rings.size(); i++) {
                holes[i - 1] = factory.createLinearRing(coordinatesFromArray(rings.get(i)));
            }
            return factory.createPolygon(shell, holes);
        }
        
        throw new IllegalArgumentException("Unsupported geometry type: " + type);
    }

    private List<double[]> coordinatesToArray(Coordinate[] coords) {
        List<double[]> result = new ArrayList<>();
        for (Coordinate coord : coords) {
            result.add(new double[]{coord.x, coord.y});
        }
        return result;
    }

    private Coordinate[] coordinatesFromArray(List<?> coords) {
        Coordinate[] result = new Coordinate[coords.size()];
        for (int i = 0; i < coords.size(); i++) {
            List<Number> coord = (List<Number>) coords.get(i);
            result[i] = new Coordinate(coord.get(0).doubleValue(), coord.get(1).doubleValue());
        }
        return result;
    }
}

