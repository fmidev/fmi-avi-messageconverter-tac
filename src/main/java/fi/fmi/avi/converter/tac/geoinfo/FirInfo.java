package fi.fmi.avi.converter.tac.geoinfo;


import java.io.IOException;

import org.locationtech.jts.geom.Geometry;

public interface FirInfo {
    Geometry getFir(String firName, boolean addDelegate);
    Geometry getAirport(String firName);
    void initStore() throws IOException;

}
