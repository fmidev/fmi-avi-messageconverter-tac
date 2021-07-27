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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.operation.polygonize.Polygonizer;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.MultiPolygonGeometryImpl;
import fi.fmi.avi.model.immutable.PointGeometryImpl;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;

public class GeoUtils {

	private static GeometryFactory gf;
	private static ObjectMapper om;
	private static GeoJsonReader reader;
	private static GeoJsonWriter writer;

	private static GeometryFactory getGeometryFactory() {
		if (gf==null) {
			gf=new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
		}
		return gf;
	}

	private static GeoJsonWriter getWriter() {
		if (writer==null) {
			writer=new GeoJsonWriter();
		}
		return writer;
	}

	private static GeoJsonReader getReader() {
		if (reader==null) {
			reader=new GeoJsonReader(GeoUtils.getGeometryFactory());
		}
		return reader;
	}

	private static ObjectMapper getObjectMapper() {
		if (om==null) {
			om=new ObjectMapper();
		}
		return om;
	}

    public static org.locationtech.jts.geom.Geometry PolygonGeometry2jtsGeometry(PolygonGeometry geometry) {
        String json=toGeoJSON(geometry);
        Feature feature;

        try {
            feature = getObjectMapper().readValue(json, Feature.class);
            return jsonFeature2jtsGeometry(feature);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

	public static Geometry jsonFeature2jtsGeometry(Feature F)  {
		try {
			ObjectMapper om=getObjectMapper();
			if (F.getGeometry()==null) {
				return null;
			}
			String json=om.writeValueAsString(F.getGeometry());
			return getReader().read(json);
		} catch(ParseException | JsonProcessingException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}

	/* TODO: Implement circle */
	public static fi.fmi.avi.model.Geometry jsonFeatureCollection2FmiAviGeometry(FeatureCollection f) {
		if (f.getFeatures().size() == 1) {
			if (f.getFeatures().get(0).getGeometry() instanceof Polygon) {
				PolygonGeometryImpl.Builder builder = PolygonGeometryImpl.builder();
				Polygon polygon = (Polygon)f.getFeatures().get(0).getGeometry();
				if (polygon.getCoordinates().size()>0) {
					List<LngLatAlt> lls = ((Polygon)f.getFeatures().get(0).getGeometry()).getExteriorRing();
					List<Double> extPoints=new ArrayList<>();
					for (LngLatAlt ll: lls) {
						extPoints.add(ll.getLatitude());
						extPoints.add(ll.getLongitude());
					}
					builder.addAllExteriorRingPositions(extPoints);
					return builder.build();
				} else {
					List<Double> extPoints=new ArrayList<>();
					builder.addAllExteriorRingPositions(extPoints);
					return builder.build();
				}
			} else if (f.getFeatures().get(0).getGeometry() instanceof Point) {
				PointGeometryImpl.Builder builder= PointGeometryImpl.builder();
				LngLatAlt ll = ((Point)f.getFeatures().get(0).getGeometry()).getCoordinates();
				List<Double> pts = Arrays.asList(ll.getLatitude(), ll.getLongitude());
				builder.addAllCoordinates(pts);
				return builder.build();
			}
			return null;
		} else {
			MultiPolygonGeometryImpl.Builder builder = MultiPolygonGeometryImpl.builder();
			for (Feature feat: f.getFeatures()) {
				if (feat.getGeometry() instanceof Polygon) {
					List<LngLatAlt> lls = ((Polygon)feat.getGeometry()).getExteriorRing();
					List<Double> extPoints=new ArrayList<>();
					for (LngLatAlt ll: lls) {
							extPoints.add(ll.getLatitude());
							extPoints.add(ll.getLongitude());
					}
					builder.addExteriorRingPositions(extPoints);
				}
			}
			return builder.build();
		}
	}

	public static Feature jtsGeometry2jsonFeature(Geometry g)  {
		Feature f=null;
		try {
			ObjectMapper om=getObjectMapper();
			String json=getWriter().write(g);
			org.geojson.Geometry<Double> geo= om.readValue(json, org.geojson.Geometry.class);
			f=new Feature();
			f.setGeometry(geo);
		} catch(IOException e) {
			System.err.println(e.getMessage());
		}
		return f;
	}

	public static Feature merge(Feature f1, Feature f2) {
		Geometry g1=jsonFeature2jtsGeometry(f1);
		Geometry g2=jsonFeature2jtsGeometry(f2);

        Geometry gNew=g1.union(g2);
		Coordinate[] coords=gNew.getCoordinates();
		if (!Orientation.isCCW(coords)) {
			gNew=gNew.reverse();
		}
		Feature f=jtsGeometry2jsonFeature(gNew);
		return f;
	}

    public static Feature fixWinding(Feature f) {
        Geometry g=jsonFeature2jtsGeometry(f);

		Coordinate[] coords=g.getCoordinates();
		if (!Orientation.isCCW(coords)) {
			g=g.reverse();
		}
		Feature newFeature=jtsGeometry2jsonFeature(g);
		return newFeature;
    }

    public static Geometry fixWinding(Geometry g) {
        Coordinate[] coords=g.getCoordinates();
		if (!Orientation.isCCW(coords)) {
            System.err.print("Reversing "+g);
            Geometry reversed = g.reverse();
			return reversed;
		}
        return g;
    }

    public static String getWinding(Geometry g) {
        Coordinate[] coords=g.getCoordinates();
		if (!Orientation.isCCW(coords)) {
			return "CW";
		}
        return "CCW";
    }

    public static  PolygonGeometry jts2PolygonGeometry(Geometry geom) {
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

    public static PolygonGeometry getPolygonOutside(Lexeme lexeme, String firName, FirInfo firInfo) {
        org.locationtech.jts.geom.Geometry fir = firInfo.getFir(firName, true);
        org.locationtech.jts.geom.Geometry firEnvelope = fir.getEnvelope();

        GeometryFactory fact = new GeometryFactory();

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

    public static String toGeoJSON(fi.fmi.avi.model.Geometry g) {
        PolygonGeometry pg = (PolygonGeometry)g;
        String polygonAsString="{\"type\": \"Feature\", \"properties\":{},"+
            "\"geometry\": { \"type\": \"Polygon\", \"coordinates\":[[";
            int l = pg.getExteriorRingPositions().size()/2;
            for (int i=0; i<l; i++){
                if (i>0) {
                    polygonAsString=polygonAsString+",";
                }
                polygonAsString=polygonAsString+String.format("[%f, %f]",
                        pg.getExteriorRingPositions().get(i*2+1),
                        pg.getExteriorRingPositions().get(i*2));
            }
            polygonAsString=polygonAsString+"]]}}";
        return polygonAsString;
    }

    public static String toGeoJSON(GeometryCollection g) {
        GeometryCollection geomCollection = (GeometryCollection)g;
        ObjectMapper om = getObjectMapper();
        String featureCollectionAsString="{\"type\": \"FeatureCollection\", \"features\": [";
        boolean first = true;
        for (int i=0; i< geomCollection.getNumGeometries(); i++){
            Feature f = jtsGeometry2jsonFeature(geomCollection.getGeometryN(i));
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

    public static PolygonGeometry getRelativeToLine(Lexeme lexeme, String firName, FirInfo firInfo) {
        System.err.println("found relative to 1 line "+lexeme.getTACToken());
        Geometry fir = firInfo.getFir(firName, true);
        GeometryFactory geomFact = new GeometryFactory();
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
        System.err.println("PRE:"+coordinateList);
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
        System.err.println("POST:"+coordinateList);
        Geometry line = geomFact.createLineString(coordinateList.toArray(new Coordinate[0]));
        Geometry split = splitPolygon(fir, line);

        if (split.getGeometryType().equals("GeometryCollection")) {
            System.err.println("Coll 1 line"+toGeoJSON((GeometryCollection)split));
        }

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
        System.err.println("TEST:"+test);
        Geometry retVal=null;
        if (test.within(splitcollection.getGeometryN(0))) {
            System.err.println("NR 0");
            retVal = splitcollection.getGeometryN(0);
        } else if ((splitcollection.getNumGeometries()>1) && test.within(splitcollection.getGeometryN(1))) {
            System.err.println("NR 1");
            retVal = splitcollection.getGeometryN(1);
        } else {
            System.err.println("FALLBACK to FIR");
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
        System.err.println("Splitting "+poly+" by "+line);
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
            fixedGeoms[i]=GeoUtils.fixWinding(geom);
            i++;
        }

        return poly.getFactory().createGeometryCollection(fixedGeoms);
    }

    public static PolygonGeometry getRelativeTo2Lines(Lexeme lexeme, String firName, FirInfo firInfo) {
        //Construct a polygon by concatenating the 2 lines. If the polygon self intersects
        //reverse the coordinates of the second line.
        //intersect the resulting polygon with the fir to get the final Geometry
        System.err.println("found relative to 2 lines "+lexeme.getTACToken());
        Geometry fir = firInfo.getFir(firName, true);
        GeometryFactory geomFact = new GeometryFactory();
        List<Coordinate> coordinateList = new ArrayList<>();
        // for (Lexeme.ParsedValueName n: lexeme.getParsedValues().keySet()) {
        //     System.err.println(">:"+n.name()+":"+lexeme.getParsedValue(n, String.class));
        // }
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

    public static PolygonGeometry getPolygonAprxWidth(Lexeme lexeme, String firName, FirInfo firInfo) {
        // System.err.println("APRX");
        // for (Lexeme.ParsedValueName n: lexeme.getParsedValues().keySet()) {
        //     System.err.println(">:"+n.name()+":"+lexeme.getParsedValue(n, String.class));

        // }

        Geometry fir = firInfo.getFir(firName, true);
        GeometryFactory geomFact = new GeometryFactory();
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
            System.err.println("Before buffer of "+width+" : "+line);
            //Base width of line in degrees on lat of first point.
            double lat = coordinateList.get(0).getY();
            double metersperdegree = Math.cos(Math.toRadians(lat))*400075./360.*1000.;
            double widthInDegrees = width/metersperdegree;
            //Generate a polygon as a buffer around that lineString
            Geometry poly = line.buffer(widthInDegrees);
            System.err.println("After buffer:"+poly);
            poly=poly.intersection(fir);
            System.err.println("After buffer:"+poly);
            return jts2PolygonGeometry(poly);
        }
        return null;
    }

    protected static Coordinate getCoordinate(String c) {
        String[] terms = c.trim().split(" ");
        Coordinate coord = new Coordinate(getLon(terms[1]), getLat(terms[0]));
        return coord;
    }

    protected static Double getLatLon(String l) {
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


}
