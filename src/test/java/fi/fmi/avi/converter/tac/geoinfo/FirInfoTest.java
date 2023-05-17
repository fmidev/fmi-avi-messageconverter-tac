package fi.fmi.avi.converter.tac.geoinfo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

import fi.fmi.avi.converter.tac.geoinfo.impl.FirInfoStoreImpl;

public class FirInfoTest {

    @Test
    public void TestFirInfo() {
        FirInfoStore fi = new FirInfoStoreImpl();
        //18 points without delegated area
        Geometry geom = fi.getFir("EHAA", false);
        assertEquals(18, geom.getNumPoints());

        //28 points with delegated area
        geom = fi.getFir("EHAA", true);
        assertEquals(28, geom.getNumPoints());
        geom = fi.getFir("EFIN", true);
        assertEquals(68, geom.getNumPoints());

        String scottish = fi.getFirName("EGPX");
        assertEquals("SCOTTISH FIR", scottish);
    }
}
