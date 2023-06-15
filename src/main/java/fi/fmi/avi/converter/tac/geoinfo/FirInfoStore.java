package fi.fmi.avi.converter.tac.geoinfo;


import org.locationtech.jts.geom.Geometry;

public interface FirInfoStore {
    Geometry getFirGeometry(String firName, boolean includeDelegatedAirspaces);
    String getFirName(String icaoCode);
}
