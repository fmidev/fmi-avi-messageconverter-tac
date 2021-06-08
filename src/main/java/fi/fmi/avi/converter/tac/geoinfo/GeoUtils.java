package fi.fmi.avi.converter.tac.geoinfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.MultiPolygonGeometryImpl;
import fi.fmi.avi.model.immutable.PointGeometryImpl;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;

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
