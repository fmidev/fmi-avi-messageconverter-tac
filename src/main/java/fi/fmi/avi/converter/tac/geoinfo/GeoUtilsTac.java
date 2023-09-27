package fi.fmi.avi.converter.tac.geoinfo;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LINE2_POINT1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LINE2_POINT2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LINE2_POINT3;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LINE2_POINT4;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LINE_POINT1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LINE_POINT2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LINE_POINT3;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LINE_POINT4;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATEDLINE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATEDLINE2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONTYPE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONTYPE2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RDOACT_LAT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RDOACT_LON;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RDOACT_RADIUS;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RDOACT_RADIUS_UNIT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.Feature;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.operation.polygonize.Polygonizer;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.Winding;
import fi.fmi.avi.model.immutable.CircleByCenterPointImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.util.JtsTools;
import fi.fmi.avi.util.JtsToolsException;


public class GeoUtilsTac {

	private static GeometryFactory gf;
	private static ObjectMapper om;

	private static GeometryFactory getGeometryFactory() {
		if (gf==null) {
			gf=new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
		}
		return gf;
	}

	private static ObjectMapper getObjectMapper() {
		if (om==null) {
			om=new ObjectMapper();
		}
		return om;
	}

    public static PolygonGeometry jts2PolygonGeometry(Geometry geom) {
        PolygonGeometryImpl.Builder bldr = PolygonGeometryImpl.builder();
        bldr.setCrs(CoordinateReferenceSystemImpl.wgs84());
        List<Double> coords = new ArrayList<>();
        for (Coordinate c: geom.getCoordinates()) {
            coords.add(c.getY());
            coords.add(c.getX());
        }
        bldr.addAllExteriorRingPositions(coords);
        return bldr.build();
    }

    public static PolygonGeometry getPolygonOutside(Lexeme lexeme, String firName, FirInfoStore firInfo) {
        org.locationtech.jts.geom.Geometry fir = firInfo.getFirGeometry(firName, true);
        org.locationtech.jts.geom.Geometry firEnvelope = fir.getEnvelope();

        GeometryFactory fact = getGeometryFactory();

        String relationOperator = lexeme.getParsedValue(RELATIONTYPE, String.class);
        String relationValue = lexeme.getParsedValue(RELATEDLINE, String.class);
        String relationOperator2 = lexeme.getParsedValue(RELATIONTYPE2, String.class);
        String relationValue2 = lexeme.getParsedValue(RELATEDLINE2, String.class);
        if (relationOperator2==null) {
            double limit = getLatLon(relationValue);
            Coordinate[] coords = firEnvelope.getCoordinates();
            switch (relationOperator) {
            case "N":
                // box with lower edge at limit
                coords[0].setY(limit);
                coords[3].setY(limit);
                coords[4].setY(limit);
                org.locationtech.jts.geom.Geometry box = fact.createPolygon(coords);
                org.locationtech.jts.geom.Geometry selected = box.intersection(fir);
                return jts2PolygonGeometry(selected);
            case "S":
                // box with upper edge at limit
                coords[1].setY(limit);
                coords[2].setY(limit);
                box = fact.createPolygon(coords);
                selected = box.intersection(fir);
                return jts2PolygonGeometry(selected);
            case "E":
                // box with left edge at limit
                coords[0].setX(limit);
                coords[1].setX(limit);
                coords[4].setX(limit);
                box = fact.createPolygon(coords);
                selected = box.intersection(fir);
                return jts2PolygonGeometry(selected);
            case "W":
                 // box with right edge at limit
                 coords[2].setX(limit);
                 coords[3].setX(limit);
                 box = fact.createPolygon(coords);
                 selected = box.intersection(fir);
                 return jts2PolygonGeometry(selected);
            }
        } else {
            double limit = getLatLon(relationValue);
            double limit2 = getLatLon(relationValue2);
            Coordinate[] coords = firEnvelope.getCoordinates();
            switch (relationOperator) {
                case "N":
                // box with top edge at limit
                coords[0].setY(limit);
                coords[3].setY(limit);
                coords[4].setY(limit);
                if (relationOperator2.equals("E")){
                    //box with left edge at limit2
                    coords[0].setX(limit2);
                    coords[1].setX(limit2);
                    org.locationtech.jts.geom.Geometry box = fact.createPolygon(coords);
                    org.locationtech.jts.geom.Geometry selected = box.intersection(fir);
                    return jts2PolygonGeometry(selected);
                } else if (relationOperator2.equals("W")) {
                    //box with right edge at limit2
                    coords[2].setX(limit2);
                    coords[3].setX(limit2);
                    org.locationtech.jts.geom.Geometry box = fact.createPolygon(coords);
                    org.locationtech.jts.geom.Geometry selected = box.intersection(fir);
                    return jts2PolygonGeometry(selected);
                }
                break;
                case "S":
                // box with top edge at limit
                coords[1].setY(limit);
                coords[2].setY(limit);
                if (relationOperator2.equals("E")){
                    //box with left edge at limit2
                    coords[0].setX(limit2);
                    coords[1].setX(limit2);
                    org.locationtech.jts.geom.Geometry box = fact.createPolygon(coords);
                    org.locationtech.jts.geom.Geometry selected = box.intersection(fir);
                    return jts2PolygonGeometry(selected);
                } else if (relationOperator2.equals("W")) {
                    //box with right edge at limit2
                    coords[2].setX(limit2);
                    coords[3].setX(limit2);
                    org.locationtech.jts.geom.Geometry box = fact.createPolygon(coords);
                    org.locationtech.jts.geom.Geometry selected = box.intersection(fir);
                    return jts2PolygonGeometry(selected);
                }
            }
        }
        return null;
    }

    public static org.locationtech.jts.geom.Geometry PolygonGeometry2jtsGeometry(PolygonGeometry geometry) throws JtsToolsException {
        String json=toGeoJSON(geometry);
        Feature feature;

        try {
            feature = getObjectMapper().readValue(json, Feature.class);
            return JtsTools.jsonFeature2jtsGeometry(feature);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toGeoJSON(fi.fmi.avi.model.Geometry g) {
        PolygonGeometry pg = (PolygonGeometry)g;
        String polygonAsString="{\"type\": \"Feature\", \"properties\":{},"+
            "\"geometry\": { \"type\": \"Polygon\", \"coordinates\":[[";
            List<Double>exteriorRingPositions = pg.getExteriorRingPositions(Winding.CLOCKWISE);
            int l = exteriorRingPositions.size()/2;
            for (int i=0; i<l; i++){
                if (i>0) {
                    polygonAsString=polygonAsString+",";
                }
                polygonAsString=polygonAsString+String.format(Locale.US, "[%f, %f]",
                        exteriorRingPositions.get(i*2+1),
                        exteriorRingPositions.get(i*2));
            }
            polygonAsString=polygonAsString+"]]}}";
        return polygonAsString;
    }

    public static String toGeoJSON(GeometryCollection g) throws JtsToolsException {
        GeometryCollection geomCollection = (GeometryCollection)g;
        ObjectMapper om = getObjectMapper();
        String featureCollectionAsString="{\"type\": \"FeatureCollection\", \"features\": [";
        boolean first = true;
        for (int i=0; i< geomCollection.getNumGeometries(); i++){
            Feature f = JtsTools.jtsGeometry2jsonFeature(geomCollection.getGeometryN(i));
            String featureString="ERR";

            try {
                featureString = om.writeValueAsString(f);
                if (!first) {
                    featureCollectionAsString=featureCollectionAsString+",";
                }
                first=false;
                featureCollectionAsString=featureCollectionAsString+featureString;
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        featureCollectionAsString+="]}";
        return featureCollectionAsString;
    }

    public static PolygonGeometry getRelativeToLine(Lexeme lexeme, String firName, FirInfoStore firInfo) {
        Geometry fir = firInfo.getFirGeometry(firName, true);
        GeometryFactory geomFact = getGeometryFactory();
        List<Coordinate> coordinateList = new ArrayList<>();
        String coord1 = lexeme.getParsedValue(LINE_POINT1, String.class);
        if (coord1!=null) {
            coordinateList.add(getCoordinate(coord1));
        }
        String coord2 = lexeme.getParsedValue(LINE_POINT2, String.class);
        if (coord2!=null) {
            coordinateList.add(getCoordinate(coord2));
        }
        String coord3 = lexeme.getParsedValue(LINE_POINT3, String.class);
        if (coord3!=null) {
            coordinateList.add(getCoordinate(coord3));
        }
        String coord4 = lexeme.getParsedValue(LINE_POINT4, String.class);
        if (coord4!=null) {
            coordinateList.add(getCoordinate(coord4));
        }
        //Extend first and last segments
        if (coordinateList.size()==3) {
            double x1 = coordinateList.get(1).getX();
            double dLon=x1-coordinateList.get(0).getX();
            double y1 = coordinateList.get(1).getY();
            double dLat = y1-coordinateList.get(0).getY();
            coordinateList.get(0).setX(x1-1.1*dLon);
            coordinateList.get(0).setY(y1-1.1*dLat);
            dLon=coordinateList.get(2).getX()-x1;
            dLat = coordinateList.get(2).getY()-y1;
            coordinateList.get(2).setX(x1+1.1*dLon);
            coordinateList.get(2).setY(y1+1.1*dLat);
        } else if (coordinateList.size()==4) {
            double x1 = coordinateList.get(1).getX();
            double dLon=x1-coordinateList.get(0).getX();
            double y1 = coordinateList.get(1).getY();
            double dLat = y1-coordinateList.get(0).getY();
            coordinateList.get(0).setX(x1-1.1*dLon);
            coordinateList.get(0).setY(y1-1.1*dLat);
            double x2 = coordinateList.get(2).getX();
            dLon=coordinateList.get(3).getX()-x2;
            double y2 = coordinateList.get(2).getY();
            dLat = coordinateList.get(3).getY()-y2;
            coordinateList.get(3).setX(x2+1.1*dLon);
            coordinateList.get(3).setY(y2+1.1*dLat);
        }
        Geometry line = geomFact.createLineString(coordinateList.toArray(new Coordinate[0]));
        Geometry split = splitPolygon(fir, line);

        //Take end of first line segment, apply direction to that
        Coordinate testPoint = line.copy().getCoordinates()[1];

        final double latStep=0.01;
        final double lonStep=0.01;
        switch (lexeme.getParsedValue(RELATIONTYPE, String.class)) {
            case "N":
            testPoint.setY(testPoint.getY()+latStep);
            break;
            case "NE":
            testPoint.setY(testPoint.getY()+latStep);
            testPoint.setX(testPoint.getX()+lonStep);
            break;
            case "E":
            testPoint.setX(testPoint.getX()+lonStep);
            break;
            case "SE":
            testPoint.setY(testPoint.getY()-latStep);
            testPoint.setX(testPoint.getX()+lonStep);
            break;
            case "S":
            testPoint.setY(testPoint.getY()-latStep);
            break;
            case "SW":
            testPoint.setY(testPoint.getY()-latStep);
            testPoint.setX(testPoint.getX()-lonStep);
            break;
            case "W":
            testPoint.setX(testPoint.getX()-lonStep);
            break;
            case "NW":
            testPoint.setY(testPoint.getY()+latStep);
            testPoint.setX(testPoint.getX()-lonStep);
            break;
        }

        GeometryCollection splitcollection = (GeometryCollection)split;
        org.locationtech.jts.geom.Point test = GeometryFactory.createPointFromInternalCoord(testPoint, fir);
        Geometry retVal=null;
        if (test.within(splitcollection.getGeometryN(0))) {
            retVal = splitcollection.getGeometryN(0);
        } else if ((splitcollection.getNumGeometries()>1) && test.within(splitcollection.getGeometryN(1))) {
            retVal = splitcollection.getGeometryN(1);
        } else {
            retVal = fir;
        }
        return jts2PolygonGeometry(retVal);
    }

    public static Geometry polygonize(Geometry geometry) {
        @SuppressWarnings("unchecked")
        List<Geometry> lines = LineStringExtracter.getLines(geometry);
        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(lines);
        @SuppressWarnings("unchecked")
        Collection<org.locationtech.jts.geom.Polygon> polys = (Collection<org.locationtech.jts.geom.Polygon>)polygonizer.getPolygons();
        org.locationtech.jts.geom.Polygon[] polyArray = GeometryFactory.toPolygonArray(polys);
        return geometry.getFactory().createGeometryCollection(polyArray);
    }

    public static Geometry splitPolygon(Geometry poly, Geometry line) {
        Geometry nodedLinework = poly.getBoundary().union(line);
        Geometry polys = polygonize(nodedLinework);

        // Only keep polygons which are inside the input
        List<Geometry> output = new ArrayList<>();
        for (int i = 0; i < polys.getNumGeometries(); i++) {
            org.locationtech.jts.geom.Polygon candpoly = (org.locationtech.jts.geom.Polygon) polys.getGeometryN(i);
            if (poly.contains(candpoly.getInteriorPoint())) {
                output.add(candpoly);
            }
        }
        Geometry[] geoms = GeometryFactory.toGeometryArray(output);
        Geometry[] fixedGeoms = new Geometry[geoms.length];
        int i=0;
        for (Geometry geom: geoms) {
            fixedGeoms[i]=GeoUtilsTac.fixWinding(geom);
            i++;
        }

        return poly.getFactory().createGeometryCollection(fixedGeoms);
    }

    public static PolygonGeometry getRelativeTo2Lines(Lexeme lexeme, String firName, FirInfoStore firInfo) {
        //Construct a polygon by concatenating the 2 lines. If the polygon self intersects
        //reverse the coordinates of the second line.
        //intersect the resulting polygon with the fir to get the final Geometry
        Geometry fir = firInfo.getFirGeometry(firName, true);
        GeometryFactory geomFact = getGeometryFactory();
        List<Coordinate> coordinateList = new ArrayList<>();
        String coord1 = lexeme.getParsedValue(LINE_POINT1, String.class);
        if (coord1!=null) {
            coordinateList.add(getCoordinate(coord1));
        }
        String coord2 = lexeme.getParsedValue(LINE_POINT2, String.class);
        if (coord2!=null) {
            coordinateList.add(getCoordinate(coord2));
        }
        String coord3 = lexeme.getParsedValue(LINE_POINT3, String.class);
        if (coord3!=null) {
            coordinateList.add(getCoordinate(coord3));
        }
        String coord4 = lexeme.getParsedValue(LINE_POINT4, String.class);
        if (coord4!=null) {
            coordinateList.add(getCoordinate(coord4));
        }
        String coord2_1 = lexeme.getParsedValue(LINE2_POINT1, String.class);
        if (coord2_1!=null) {
            coordinateList.add(getCoordinate(coord2_1));
        }
        String coord2_2 = lexeme.getParsedValue(LINE2_POINT2, String.class);
        if (coord2_2!=null) {
            coordinateList.add(getCoordinate(coord2_2));
        }
        String coord2_3 = lexeme.getParsedValue(LINE2_POINT3, String.class);
        if (coord2_3!=null) {
            coordinateList.add(getCoordinate(coord2_3));
        }
        String coord2_4 = lexeme.getParsedValue(LINE2_POINT4, String.class);
        if (coord2_4!=null) {
            coordinateList.add(getCoordinate(coord2_4));
        }
        coordinateList.add(coordinateList.get(0));
        Geometry poly = geomFact.createPolygon(coordinateList.toArray(new Coordinate[0]));

        if (!poly.isSimple()){
            coordinateList.clear();
            if (coord1!=null) {
                coordinateList.add(getCoordinate(coord1));
            }
            if (coord2!=null) {
                coordinateList.add(getCoordinate(coord2));
            }
            if (coord3!=null) {
                coordinateList.add(getCoordinate(coord3));
            }
            if (coord4!=null) {
                coordinateList.add(getCoordinate(coord4));
            }
            if (coord2_4!=null) {
                coordinateList.add(getCoordinate(coord2_4));
            }
            if (coord2_3!=null) {
                coordinateList.add(getCoordinate(coord2_3));
            }
            if (coord2_2!=null) {
                coordinateList.add(getCoordinate(coord2_2));
            }
            if (coord2_1!=null) {
                coordinateList.add(getCoordinate(coord2_1));
            }
            coordinateList.add(coordinateList.get(0));
            poly = geomFact.createPolygon(coordinateList.toArray(new Coordinate[0]));
        }
        Geometry intersected = poly.intersection(fir);
        return jts2PolygonGeometry(intersected);
    }

    public static PolygonGeometry getPolygonAprxWidth(Lexeme lexeme, String firName, FirInfoStore firInfo) {

        Geometry fir = firInfo.getFirGeometry(firName, true);
        GeometryFactory geomFact = getGeometryFactory();
        List<Coordinate> coordinateList = new ArrayList<>();
        String widthString = lexeme.getParsedValue(ParsedValueName.APRX_LINE_WIDTH, String.class);
        if (widthString!=null) {
            double width = Double.parseDouble(widthString);
            String widthUnit =  lexeme.getParsedValue(ParsedValueName.APRX_LINE_WIDTH_UNIT, String.class);
            if (widthUnit.equals("NM")) {
                width = width * 1850;
            } else if (widthUnit.equals("KM")) {
                width = width * 1000;
            } else {}
            //Make a line out of max. 4 points
            String coord1 = lexeme.getParsedValue(ParsedValueName.APRX_POINT1, String.class);
            if (coord1!=null) {
                coordinateList.add(getCoordinate(coord1));
            }
            String coord2 = lexeme.getParsedValue(ParsedValueName.APRX_POINT2, String.class);
            if (coord2!=null) {
                coordinateList.add(getCoordinate(coord2));
            }
            String coord3 = lexeme.getParsedValue(ParsedValueName.APRX_POINT3, String.class);
            if (coord3!=null) {
                coordinateList.add(getCoordinate(coord3));
            }
            String coord4 = lexeme.getParsedValue(ParsedValueName.APRX_POINT4, String.class);
            if (coord4!=null) {
                coordinateList.add(getCoordinate(coord4));
            }
            Geometry line = geomFact.createLineString(coordinateList.toArray(new Coordinate[0]));
            //Base width of line in degrees on lat of first point.
            double lat = coordinateList.get(0).getY();
            double metersperdegree = Math.cos(Math.toRadians(lat))*400075./360.*1000.;
            double widthInDegrees = width/metersperdegree;
            //Generate a polygon as a buffer around that lineString
            Geometry poly = line.buffer(widthInDegrees);
            poly=poly.intersection(fir);
            return jts2PolygonGeometry(poly);
        }
        return null;
    }

    protected static Coordinate getCoordinate(String c) {
        String[] terms = c.trim().split(" ");
        Coordinate coord = new Coordinate(getLon(terms[1]), getLat(terms[0]));
        return coord;
    }

    public static Double getLatLon(String l) {
        if (l.startsWith("N")||l.startsWith("S")) {
            return getLat(l);
        }
        if (l.startsWith("E")||l.startsWith("W")) {
            return getLon(l);
        }
        return null;
    }

    protected static Double getLat(String latStr) {
        if (latStr.length()==3) {
            Double lat = Double.valueOf(latStr.substring(1));
            if (latStr.startsWith("S")) {
                return -1*lat.doubleValue();
            }
            return lat.doubleValue();
        }
        if (latStr.length()==5) {
            Double lat = Double.valueOf(latStr.substring(1,3));
            Double minutes = Double.valueOf(latStr.substring(3,5));
            lat = lat+minutes/60.;
            if (latStr.startsWith("S")) {
                return -1*lat.doubleValue();
            }
            return lat.doubleValue();
        }
        return null;
    }

    protected static Double getLon(String lonStr) {
        if (lonStr.length()==4) {
            Double lon = Double.valueOf(lonStr.substring(1));
            if (lonStr.startsWith("W")) {
                return -1*lon.doubleValue();
            }
            return lon.doubleValue();
        }
        if (lonStr.length()==6) {
            Double lon = Double.valueOf(lonStr.substring(1,4));
            Double minutes = Double.valueOf(lonStr.substring(4,6));
            lon = lon+minutes/60.;
            if (lonStr.startsWith("W")) {
                return -1*lon.doubleValue();
            }
            return lon.doubleValue();
        }
        return null;
    }

    public static Geometry fixWinding(Geometry g) {
        Coordinate[] coords=g.getCoordinates();
		if (!Orientation.isCCW(coords)) {
            Geometry reversed = g.reverse();
			return reversed;
		}
        return g;
    }

    public static CircleByCenterPoint getWithinRadius(Lexeme lexeme) {
            CircleByCenterPointImpl.Builder circleBuilder = CircleByCenterPointImpl.builder();
            NumericMeasureImpl.Builder radiusBuilder = NumericMeasureImpl.builder()
                    .setValue(lexeme.getParsedValue(RDOACT_RADIUS, Integer.class).doubleValue())
                    .setUom(lexeme.getParsedValue(RDOACT_RADIUS_UNIT, String.class));
            circleBuilder.setRadius(radiusBuilder.build());
            double []pts = {lexeme.getParsedValue(RDOACT_LAT, Double.class), lexeme.getParsedValue(RDOACT_LON, Double.class)};
            circleBuilder.addCenterPointCoordinates(pts);
            ObjectMapper om = new ObjectMapper();
            try {
                System.out.println("CIRCLE:"+om.writeValueAsString(circleBuilder.build()));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return circleBuilder.build();
    }
}
