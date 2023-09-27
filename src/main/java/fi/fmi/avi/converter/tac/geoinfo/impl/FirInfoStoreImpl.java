package fi.fmi.avi.converter.tac.geoinfo.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.core.io.ClassPathResource;
import fi.fmi.avi.converter.tac.geoinfo.FirInfoStore;
import fi.fmi.avi.util.JtsTools;
import fi.fmi.avi.util.JtsToolsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FirInfoStoreImpl implements FirInfoStore {
    private static final Logger log = LoggerFactory.getLogger(FirInfoStoreImpl.class);

    private static GeometryFactory gf;
    private static ObjectMapper om;
    private static GeoJsonReader reader;
    private static GeoJsonWriter writer;
    private String worldFIRFile;
    private String delegatedFile;
    private String simplifiedFIRFile;
    private Map<String, Feature> worldFIRInfos;
    private Map<String, Feature> simplifiedFIRInfos;
    private Map<String, List<Feature>> delegatedAirspaces;

    public FirInfoStoreImpl() {
        this.worldFIRFile = "world_firs.json";
        this.delegatedFile = "delegated.json";
        this.simplifiedFIRFile = "simplified_firs.json";
        try {
            initStore();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static String readResource(String name) throws IOException {
        StringBuilder result = new StringBuilder("");
        ClassPathResource resource = new ClassPathResource(name);

        InputStream inputStream = resource.getInputStream();

        BufferedReader bufferedReader = new BufferedReader(
          new InputStreamReader(inputStream)
        );
        String line;

        while ((line = bufferedReader.readLine()) != null) {
          if (result.length() > 0) {
            result.append("\n");
          }
          result.append(line);
        }
        inputStream.close();
        return result.toString();
    }

    private static GeometryFactory getGeometryFactory() {
        if (gf == null) {
            gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
        }
        return gf;
    }

    private static GeoJsonWriter getWriter() {
        if (writer == null) {
            writer = new GeoJsonWriter();
        }
        return writer;
    }

    private static GeoJsonReader getReader() {
        if (reader == null) {
            reader = new GeoJsonReader(getGeometryFactory());
        }
        return reader;
    }

    private static ObjectMapper getObjectMapper() {
        if (om == null) {
            om = new ObjectMapper();
        }
        return om;
    }

    private Feature lookup(final String name, final boolean includeDelegatedAirspaces) {
        if (worldFIRInfos == null) {
          try {
            initStore();
          } catch (final IOException e) {
            return null;
          }
        }

        Feature feature = null;
        if (simplifiedFIRInfos.containsKey(name)) {
          feature = cloneThroughSerialize(simplifiedFIRInfos.get(name));
        } else if (worldFIRInfos.containsKey(name)) {
          feature = cloneThroughSerialize(worldFIRInfos.get(name));
        }

        if (includeDelegatedAirspaces) {
          if (delegatedAirspaces.containsKey(name)) {
            for (final Feature f : delegatedAirspaces.get(name)) {
              // Merge f with feature
              try {
                feature = JtsTools.merge(feature, f);
              } catch (JtsToolsException e) {
                // Do not add delegated airspace
              }
            }
          }
        }

        return feature;
      }

      private static Feature cloneThroughSerialize(final Feature t) {
        try {
          final ByteArrayOutputStream bos = new ByteArrayOutputStream();
          serializeToOutputStream(t, bos);
          final byte[] bytes = bos.toByteArray();
          final ObjectInputStream ois = new ObjectInputStream(
            new ByteArrayInputStream(bytes)
          );
          return (Feature) ois.readObject();
        } catch (final Exception e) {
          return null;
        }
      }

      private static void serializeToOutputStream(
        final Serializable ser,
        final OutputStream os
      )
        throws IOException {
        ObjectOutputStream oos = null;
        try {
          oos = new ObjectOutputStream(os);
          oos.writeObject(ser);
          oos.flush();
        } finally {
          oos.close();
        }
      }

    @Override
    public Geometry getFirGeometry(String firName, boolean addDelegate) {
        Feature f = lookup(firName, addDelegate);
        if (f!=null) {
            try {
                return JtsTools.jsonFeature2jtsGeometry(f);
            } catch (JtsToolsException e) {
                return null;
            }
        }
        return null;
    }

    @PostConstruct
    public void initStore() throws IOException {
        this.worldFIRInfos = new HashMap<String, Feature>();
        this.delegatedAirspaces = new HashMap<String, List<Feature>>();
        this.simplifiedFIRInfos = new HashMap<String, Feature>();

        final ObjectMapper om = new ObjectMapper();

        try {
          final GeoJsonObject FIRInfo = om.readValue(
            readResource(this.worldFIRFile),
            GeoJsonObject.class
          );
          final FeatureCollection fc = (FeatureCollection) FIRInfo;
          for (final Feature f : fc.getFeatures()) {
            final String FIRname = f.getProperty("FIRname");
            final String ICAOCode = f.getProperty("ICAOCODE");
            worldFIRInfos.put(FIRname, f);
            worldFIRInfos.put(ICAOCode, f);
          }
        } catch (final IOException e) {
          log.error(e.getMessage());
        }
        log.debug("Found " + worldFIRInfos.size() + " records of FIRinfo");

        try {
          final GeoJsonObject simplifiedFIRInfo = om.readValue(
            readResource(this.simplifiedFIRFile),
            GeoJsonObject.class
          );
          final FeatureCollection simplifiedFc = (FeatureCollection) simplifiedFIRInfo;
          for (final Feature f : simplifiedFc.getFeatures()) {
            final String FIRname = f.getProperty("FIRname");
            final String ICAOCode = f.getProperty("ICAOCODE");
            simplifiedFIRInfos.put(FIRname, f);
            simplifiedFIRInfos.put(ICAOCode, f);
          }
        } catch (final IOException e) {
          log.error(e.getMessage());
        }
        log.debug(
          "Found " + simplifiedFIRInfos.size() + " records of simplified FIRinfo"
        );

        try {
          final GeoJsonObject DelegatedInfo = om.readValue(
            readResource(this.delegatedFile),
            GeoJsonObject.class
          );
          final FeatureCollection fc = (FeatureCollection) DelegatedInfo;
          for (final Feature f : fc.getFeatures()) {
            final String FIRname = f.getProperty("FIRname");
            final String ICAOCode = f.getProperty("ICAONAME");
            if (!delegatedAirspaces.containsKey(FIRname)) {
              final List<Feature> delegated = new ArrayList<Feature>();
              delegatedAirspaces.put(FIRname, delegated);
              delegatedAirspaces.put(ICAOCode, delegated);
            }
            delegatedAirspaces.get(FIRname).add(f);
          }
        } catch (final IOException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getFirName(String icaoCode) {
        Feature f = lookup(icaoCode, false);
        if (f!=null) {
          String firName = f.getProperty("FIRname");
          if (firName!=null) {
            if (firName.startsWith("FIR ")) {
              firName = firName.replaceFirst("FIR ", "");
              return firName + " FIR";
            } else if (firName.endsWith(" FIR")) {
              return firName;
            } else if (firName.endsWith(" UIR")) {
              return firName;
            } else if (firName.startsWith("UIR ")) {
              firName = firName.replaceFirst("UIR ", "");
              return firName + " UIR";
            }
          }
        }
        return null;
    }
}
