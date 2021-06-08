package fi.fmi.avi.converter.tac.geoinfo;

import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

import fi.fmi.avi.converter.tac.geoinfo.impl.FirInfoImpl;

public class FirInfoTest {

    @Test
    public void TestFirInfo() {
        FirInfo fi = new FirInfoImpl();
        Geometry geom = fi.getFir("EHAA", false);
        System.err.println(geom);
        geom = fi.getFir("EHAA", true);
        System.err.println(geom);
        geom = fi.getFir("EFIN", true);
        System.err.println(geom);
    }
}
