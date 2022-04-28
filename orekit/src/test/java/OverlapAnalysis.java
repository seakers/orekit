/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.*;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.hipparchus.util.FastMath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.analysis.ephemeris.GroundTrackAnalysis;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.FieldOfViewEventAnalysis;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Satellite;
import seakers.orekit.object.Instrument;
import seakers.orekit.analysis.Record;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.parallel.ParallelRoutine;
import seakers.orekit.propagation.*;
import seakers.orekit.object.*;
import seakers.orekit.coverage.access.*;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.util.OrekitConfig;

import javax.imageio.ImageIO;

import static java.lang.Double.parseDouble;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.io.FileUtils.getFile;

public class OverlapAnalysis {
    
    public static void main(String[] args) {

        // Orekit initialization needs
        OrekitConfig.init(1);
        String overlapFilePath = "./orekit/src/test/output/overlap/";
        File orekitData = new File("./orekit/src/main/resources");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 10, 30, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU;

        // Initializing
        ArrayList<Satellite> altimeters = new ArrayList<>();
        ArrayList<Satellite> imagers = new ArrayList<>();

        // Landsat 7
        Orbit landsat7Orbit = new KeplerianOrbit(6378000+705000, 0.0, FastMath.toRadians(98.2), 0.0, 0.0, 0.0, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Collection<Instrument> landsat7Payload = new ArrayList<>();
        double landsatFOVRadians = Math.atan(185.0/705.0/2);
        NadirSimpleConicalFOV etmPlusFOV = new NadirSimpleConicalFOV(landsatFOVRadians,earthShape);
        Instrument etmPlus = new Instrument("ETM+", etmPlusFOV, 100.0, 100.0);
        landsat7Payload.add(etmPlus);
        Satellite landsat7 = new Satellite("Landsat 7", landsat7Orbit, landsat7Payload);
        //imagers.add(landsat7);

        // Jason-3
        Orbit jason3Orbit = new KeplerianOrbit(6378000+1336001, 0.0, FastMath.toRadians(66.038), 0.0, 0.0, 0.0, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Collection<Instrument> jason3Payload = new ArrayList<>();
        NadirSimpleConicalFOV poseidon3BFOV = new NadirSimpleConicalFOV(FastMath.toRadians(1.28),earthShape);
        Instrument poseidon3B = new Instrument("Poseidon-3B", poseidon3BFOV, 100.0, 100.0);
        jason3Payload.add(poseidon3B);
        Satellite jason3 = new Satellite("Jason-3", jason3Orbit, jason3Payload);
        //altimeters.add(jason3);

        // Sentinel-2
        Orbit sentinel2Orbit = new KeplerianOrbit(6378000+786000, 0.0, FastMath.toRadians(99.5), 0.0, 0.0, 0.0, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Collection<Instrument> sentinel2Payload = new ArrayList<>();
        double sentinel2FOVRadians = Math.atan(290.0/705.0/2);
        NadirSimpleConicalFOV msiFOV = new NadirSimpleConicalFOV(sentinel2FOVRadians,earthShape);
        Instrument msi = new Instrument("MSI", msiFOV, 100.0, 100.0);
        sentinel2Payload.add(msi);
        Satellite sentinel2 = new Satellite("Landsat 8", sentinel2Orbit, sentinel2Payload);
        //imagers.add(sentinel2);

        // Sentinel-6
        Orbit sentinel6Orbit = new KeplerianOrbit(6378000+1336000, 0.0, FastMath.toRadians(66.038), 0.0, 0.0, 0.0, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Collection<Instrument> sentinel6Payload = new ArrayList<>();
        double poseidon4FOVRadians = Math.atan(20/1336.0/2);
        NadirSimpleConicalFOV poseidon4FOV = new NadirSimpleConicalFOV(poseidon4FOVRadians, earthShape);
        Instrument poseidon4 = new Instrument("Poseidon-4", poseidon4FOV, 100.0, 100.0);
        sentinel6Payload.add(poseidon4);
        Satellite sentinel6 = new Satellite("Sentinel-6", sentinel6Orbit, sentinel6Payload);
        //altimeters.add(sentinel6);

        // Icesat-2
        Orbit icesat2Orbit = new KeplerianOrbit(6378000+481000, 0.0, FastMath.toRadians(66.038), 0.0, 0.0, 0.0, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Collection<Instrument> icesat2Payload = new ArrayList<>();
        double atlasFOVRadians = Math.atan(5/481.0/2);
        NadirSimpleConicalFOV atlasFOV = new NadirSimpleConicalFOV(atlasFOVRadians, earthShape);
        Instrument atlas = new Instrument("ATLAS", atlasFOV, 100.0, 100.0);
        icesat2Payload.add(atlas);
        Satellite icesat2 = new Satellite("IceSat-2", icesat2Orbit, icesat2Payload);
        //altimeters.add(icesat2);

        // Landsat-8
        Orbit landsat8Orbit = new KeplerianOrbit(6378000+705001, 0.0, FastMath.toRadians(98.2), 0.0, 3.1415, 0.0, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Collection<Instrument> landsat8Payload = new ArrayList<>();
        double landsat8FOVRadians = Math.atan(185.0/705.0/2);
        NadirSimpleConicalFOV oliFOV = new NadirSimpleConicalFOV(landsat8FOVRadians,earthShape);
        Instrument oli = new Instrument("OLI", oliFOV, 100.0, 100.0);
        landsat8Payload.add(oli);
        Satellite landsat8 = new Satellite("Landsat 8", landsat8Orbit, landsat8Payload);
        //imagers.add(landsat8);

        // SWOT
        Orbit swotOrbit = new KeplerianOrbit(6378000+891000, 0.0, FastMath.toRadians(78), 0.0, 0.0, 0.0, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Collection<Instrument> swotPayload = new ArrayList<>();
        double swotCrossFOVRadians = Math.atan(60.0/891.0);
        double swotAlongFOVRadians = Math.atan(400.0/891.0);
        NadirRectangularFOV swotFOV = new NadirRectangularFOV(swotCrossFOVRadians,swotAlongFOVRadians,0.0,earthShape);
        Instrument swotAltimeter = new Instrument("SWOT Altimeter", swotFOV, 100.0, 100.0);
        swotPayload.add(swotAltimeter);
        Satellite SWOT = new Satellite("SWOT", swotOrbit, swotPayload);
        altimeters.add(SWOT);

        // Smallsat imagers
        Collection<Instrument> ssPayload = new ArrayList<>();
        double ssCrossFOVRadians = Math.toRadians(20.0);
        double ssAlongFOVRadians = Math.atan(400.0/500.0);
        NadirRectangularFOV ssFOV = new NadirRectangularFOV(ssCrossFOVRadians,ssAlongFOVRadians,0.0,earthShape);
        Instrument ssImager = new Instrument("Smallsat imager", ssFOV, 100.0, 100.0);
        ssPayload.add(ssImager);
        int r = 1;
        int s = 11;
        for(int m = 0; m < r; m++) {
            for(int n = 0; n < s; n++) {
                int pu = 360 / (r*s);
                int delAnom = pu * r; //in plane spacing between satellites
                int delRAAN = pu * s; //node spacing
                int RAAN = delRAAN * m;
                int f = 1;
                int phasing = pu * f;
                int anom = (n * delAnom + phasing * m);
                Orbit ssOrbit = new KeplerianOrbit(6378000+556000, 0.0, FastMath.toRadians(97.5896), 0.0, FastMath.toRadians(250.0+RAAN), FastMath.toRadians(anom), PositionAngle.MEAN, inertialFrame, startDate, mu);
                Satellite smallsat = new Satellite("smallsat"+m+n, ssOrbit, ssPayload);
                imagers.add(smallsat);
            }
        }

        // Computing results
        double duration = 1.0; // in days
        GroundEventAnalyzer altimeterAnalyzer = coverageByConstellation(altimeters, duration, startDate);
        Map<TopocentricFrame, TimeIntervalArray> altimeterEvents = altimeterAnalyzer.getEvents();
        GroundEventAnalyzer imagerAnalyzer = coverageByConstellation(imagers, duration, startDate);
        Map<TopocentricFrame, TimeIntervalArray> imagerEvents = imagerAnalyzer.getEvents();
        processCovPoints(imagerEvents,overlapFilePath+"covpoints.shp");
        processGroundTracks(altimeters, overlapFilePath+"altimetersgroundtrackoutput.shp", startDate, duration);
        processGroundTracks(imagers, overlapFilePath+"imagersgroundtrackoutput.shp", startDate, duration);

        // Analyzing overlap
        ArrayList<Integer> times = new ArrayList<>();
        ArrayList<Integer> overlapEvents = new ArrayList<>();
        for (int i = 0; i < 24*4; i++) {
            double delay = i * 3600 / 4;
            Map<TopocentricFrame, ArrayList<Double>> results = analyzeOverlap(altimeterEvents, imagerEvents, delay);
            times.add(i);
            overlapEvents.add(results.size());
        }
        Map<TopocentricFrame, ArrayList<Double>> results1hr = analyzeOverlap(altimeterEvents, imagerEvents, 3600.0);
        try{
            FileWriter csvWriter = new FileWriter("C:/Users/bgorr/Dropbox/delay_results.csv");
            for (int i = 0; i<1; i++) {
                String rowData = getListAsCsvString(times);
                String overlap = getListAsCsvString(overlapEvents);
                csvWriter.append(rowData);
                csvWriter.append("\n");
                csvWriter.append(overlap);
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (Exception e) {
            System.out.println(e.toString());
            System.out.println("Error writing to CSV");
        }
//        Map<TopocentricFrame, ArrayList<Double>> results1day = analyzeOverlap(altimeterEvents, imagerEvents, 3600.0*24);
//        Map<TopocentricFrame, ArrayList<Double>> results3days = analyzeOverlap(altimeterEvents, imagerEvents, 3600.0*24*3);
//        System.out.println("Number of overlap events in 15 min: "+results15min.size());
//        System.out.println("Number of overlap events in 1 hr: "+results1hr.size());
//        System.out.println("Number of overlap events in 1 day: "+results1day.size());
//        System.out.println("Number of overlap events in 3 days: "+results3days.size());
        processResults(results1hr, overlapFilePath+"resultsoutput.shp");

        plotResults(overlapFilePath);

        OrekitConfig.end();

    }
    public static String getListAsCsvString(ArrayList<Integer> list){

        StringBuilder sb = new StringBuilder();
        for(Integer str:list){
            if(sb.length() != 0){
                sb.append(",");
            }
            sb.append(str);
        }
        return sb.toString();
    }
    public static Collection<Record<String>> getGroundTrack(Orbit orbit, double duration, AbsoluteDate startDate) {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate endDate = startDate.shiftedBy(duration*86400);

        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        ArrayList<Instrument> payload = new ArrayList<>();
        Satellite sat1 = new Satellite(orbit.toString(), orbit,  payload);
        Properties propertiesPropagator = new Properties();
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);


        Collection<Analysis<?>> analyses = new ArrayList<>();
        double analysisTimeStep = 5;
        GroundTrackAnalysis gta = new GroundTrackAnalysis(startDate, endDate, analysisTimeStep, sat1, earthShape, pf);
        analyses.add(gta);
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                analysis(analyses).name(orbit.toString()).propagatorFactory(pf).build();
        try {
            scen.call();
        } catch (Exception ex) {
            throw new IllegalStateException("Ground track scenario failed to complete.");
        }
        return gta.getHistory();
    }
    public static void processGroundTracks(ArrayList<Satellite> satellites, String filepath, AbsoluteDate startDate, double duration) {
        try {

            final SimpleFeatureType TYPE =
                    DataUtilities.createType(
                            "Location",
                            "the_geom:Point:srid=4326,"
                    );
            List<SimpleFeature> features = new ArrayList<>();
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
            for (Satellite satellite : satellites) {
                Collection<Record<String>> coll = getGroundTrack(satellite.getOrbit(), duration, startDate);
                for (Record<String> ind : coll) {
                    String rawString = ind.getValue();
                    String[] splitString = rawString.split(",");
                    double latitude = Double.parseDouble(splitString[0]);
                    double longitude = Double.parseDouble(splitString[1]);
                    Point point = geometryFactory.createPoint(new Coordinate(latitude, longitude));
                    featureBuilder.add(point);
                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    features.add(feature);
                }
            }
            File newFile = getFile(new File(filepath));
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", newFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Uh oh");
        }
    }

    public static void processResults(Map<TopocentricFrame, ArrayList<Double>> results, String filepath) {
        try {

            final SimpleFeatureType TYPE =
                    DataUtilities.createType(
                            "Location",
                            "the_geom:Point:srid=4326,"
                    );
            List<SimpleFeature> features = new ArrayList<>();
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
            for (TopocentricFrame tf : results.keySet()) {
                Point point = geometryFactory.createPoint(new Coordinate(FastMath.toDegrees(tf.getPoint().getLatitude()), FastMath.toDegrees(tf.getPoint().getLongitude())));
                featureBuilder.add(point);
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }
            File newFile = getFile(new File(filepath));
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", newFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Uh oh");
        }
    }

    public static void processCovPoints(Map<TopocentricFrame, TimeIntervalArray> imagers, String filepath) {
        try {

            final SimpleFeatureType TYPE =
                    DataUtilities.createType(
                            "Location",
                            "the_geom:Point:srid=4326,"
                    );
            List<SimpleFeature> features = new ArrayList<>();
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
            for (TopocentricFrame tf : imagers.keySet()) {
                Point point = geometryFactory.createPoint(new Coordinate(FastMath.toDegrees(tf.getPoint().getLatitude()), FastMath.toDegrees(tf.getPoint().getLongitude())));
                featureBuilder.add(point);
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }
            File newFile = getFile(new File(filepath));
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", newFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Uh oh");
        }
    }

    public static void plotResults(String overlapFilePath) {
        try {
            MapContent map = new MapContent();
            map.setTitle("Test");

            File countries_file = getFile("./orekit/src/test/resources/ne_50m_admin_0_countries_lakes.shp");
            FileDataStore countries_store = FileDataStoreFinder.getDataStore(countries_file);
            SimpleFeatureSource countriesSource = countries_store.getFeatureSource();
            Style country_style = SLD.createPolygonStyle(Color.BLACK,null,1.0f);
            Layer country_layer = new FeatureLayer(countriesSource, country_style);
            map.addLayer(country_layer);

            File alt_groundtrack_file = getFile(overlapFilePath+"altimetersgroundtrackoutput.shp");
            FileDataStore alt_groundtrack_store = FileDataStoreFinder.getDataStore(alt_groundtrack_file);
            SimpleFeatureSource alt_groundtrackSource = alt_groundtrack_store.getFeatureSource();
            Style alt_style = SLD.createPointStyle("Square",Color.RED,Color.RED,0.5f,6.0f);
            Layer altimeter_track_layer = new FeatureLayer(alt_groundtrackSource, alt_style);
            map.addLayer(altimeter_track_layer);

            File imag_groundtrack_file = getFile(overlapFilePath+"imagersgroundtrackoutput.shp");
            FileDataStore imag_groundtrack_store = FileDataStoreFinder.getDataStore(imag_groundtrack_file);
            SimpleFeatureSource imag_groundtrackSource = imag_groundtrack_store.getFeatureSource();
            Style imag_style = SLD.createPointStyle("Square",Color.GREEN,Color.GREEN,0.5f,6.0f);
            Layer imag_altimeter_track_layer = new FeatureLayer(imag_groundtrackSource, imag_style);
            map.addLayer(imag_altimeter_track_layer);

            File res_groundtrack_file = getFile(overlapFilePath+"resultsoutput.shp");
            FileDataStore res_groundtrack_store = FileDataStoreFinder.getDataStore(res_groundtrack_file);
            SimpleFeatureSource res_groundtrackSource = res_groundtrack_store.getFeatureSource();
            Style res_style = SLD.createPointStyle("Circle",Color.YELLOW,Color.YELLOW,0.5f,12.0f);
            Layer res_track_layer = new FeatureLayer(res_groundtrackSource, res_style);
            map.addLayer(res_track_layer);

            File rivers_file = getFile("./orekit/src/test/resources/GRWL_summaryStats.shp");
            FileDataStore rivers_store = FileDataStoreFinder.getDataStore(rivers_file);
            SimpleFeatureSource riversSource = rivers_store.getFeatureSource();
            Style river_style = SLD.createPolygonStyle(Color.BLACK,null,1.0f);
            Layer river_layer = new FeatureLayer(riversSource, river_style);
            map.addLayer(river_layer);

            File lakes_file = getFile(overlapFilePath+"covpoints.shp");
            FileDataStore lakes_store = FileDataStoreFinder.getDataStore(lakes_file);
            SimpleFeatureSource lakesSource = lakes_store.getFeatureSource();
            Style lake_style = SLD.createPointStyle("Circle",Color.BLUE,Color.BLUE,0.5f,2.0f);
            Layer lake_layer = new FeatureLayer(lakesSource, lake_style);
            map.addLayer(lake_layer);

            JMapFrame.showMap(map);
            saveImage(map, "./src/test/output/overlap_map.jpeg",3000);
        } catch (Exception e) {
            System.out.println("Exception occurred in plotResults: "+e);
        }

    }


    public static Map<TopocentricFrame, ArrayList<Double>> analyzeOverlap(Map<TopocentricFrame, TimeIntervalArray> base, Map<TopocentricFrame, TimeIntervalArray> addl, double delay) {
        Map<TopocentricFrame, ArrayList<Double>> results = new HashMap<>();
        Map<TopocentricFrame, TimeIntervalArray> overlapTimes = new HashMap<>();

        for(TopocentricFrame tf : base.keySet()) {
            ArrayList<Double> overlapDelayTimes = new ArrayList<>();
            TimeIntervalArray baseTimes = base.get(tf);
            TimeIntervalArray addlTimes = addl.get(tf);
            TimeIntervalArray delayTimes = overlapWithDelay(baseTimes, addlTimes, delay, false);
            if(!delayTimes.isEmpty()) {
                double overlapTimeNoDelay = arraySum(delayTimes.getDurations());
                overlapDelayTimes.add(overlapTimeNoDelay);
                overlapDelayTimes.add((double)delayTimes.getDurations().length);
                results.put(tf, overlapDelayTimes);
            }
            overlapTimes.put(tf,delayTimes);
        }
        return results;
    }

    public static TimeIntervalArray overlapWithDelay(TimeIntervalArray base, TimeIntervalArray addl, double delay, Boolean onesided) {
        double[] rasBase = base.getRiseAndSetTimesList();
        double[] rasAddl = addl.getRiseAndSetTimesList();
        TimeIntervalArray overlapArray = new TimeIntervalArray(base.getHead(), base.getTail());
        for (int j=0;j<rasBase.length;j=j+2) {
            for (int k=0;k<rasAddl.length;k=k+2) {
                if(onesided) {
                    if(rasBase[j] < rasAddl[k] && rasBase[j+1]+delay > rasAddl[k]) {
                        if(rasBase[j] < rasAddl[k+1] && rasBase[j+1]+delay > rasAddl[k+1]) {
                            overlapArray.addRiseTime(rasAddl[k]);
                            overlapArray.addSetTime(rasAddl[k+1]);
                        } else {
                            overlapArray.addRiseTime(rasAddl[k]);
                            overlapArray.addSetTime(rasBase[j+1]+delay);
                        }
                    } else if(rasBase[j] < rasAddl[k+1] && rasBase[j+1]+delay > rasAddl[k+1]) {
                        overlapArray.addRiseTime(rasBase[j]);
                        overlapArray.addSetTime(rasAddl[k+1]);
                    } else if(rasBase[j]>=rasAddl[k] && rasBase[j+1]+delay<=rasAddl[k+1]) {
                        overlapArray.addRiseTime(rasBase[j]);
                        overlapArray.addSetTime(rasBase[j+1]+delay);
                    }
                } else {
                    if(rasBase[j+1]+delay > rasAddl[k+1] && rasBase[j]-delay < rasAddl[k]) {
                        overlapArray.addRiseTime(rasAddl[k]);
                        overlapArray.addSetTime(rasAddl[k+1]);
                    }
                }

            }
        }
        return overlapArray;
    }

    public static GroundEventAnalyzer coverageByConstellation(ArrayList<Satellite> satelliteList, double durationDays, AbsoluteDate startDate) {
        long start = System.nanoTime();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate endDate = startDate.shiftedBy(durationDays*86400);
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2);

        List<List<String>> riverRecords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("./orekit/src/test/resources/grwl_river_output.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                riverRecords.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println("Exception occurred in coverageByConstellation: "+e);
        }
        Set<GeodeticPoint> covPoints = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            double lon = Math.toRadians(parseDouble(riverRecords.get(i).get(0)));
            double lat = Math.toRadians(parseDouble(riverRecords.get(i).get(1)));
            GeodeticPoint riverPoint = new GeodeticPoint(lat, lon, 0.0);
            covPoints.add(riverPoint);
        }
        List<List<String>> lakeRecords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("./orekit/src/test/resources/hydrolakes.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                lakeRecords.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println("Exception occurred in coverageByConstellation: "+e);
        }
        for (int i = 0; i < 1000; i++) {
            double lat = Math.toRadians(parseDouble(lakeRecords.get(i).get(0)));
            double lon = Math.toRadians(parseDouble(lakeRecords.get(i).get(1)));
            GeodeticPoint lakePoint = new GeodeticPoint(lat, lon, 0.0);
            covPoints.add(lakePoint);
        }

//        Set<GeodeticPoint> subSet = landPoints.stream()
//                // .skip(10) // Use this to get elements later in the stream
//                .limit(5000)
//                .collect(toCollection(LinkedHashSet::new));
        //create a coverage definition
        CoverageDefinition covDef = new CoverageDefinition("covdef1", covPoints, earthShape);
        //CoverageDefinition covDef = new CoverageDefinition("Whole Earth", granularity, earthShape, UNIFORM);
        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        Constellation constellation = new Constellation("Constellation", satelliteList);
        covDef.assignConstellation(constellation);
        covDefs.add(covDef);

        ArrayList<EventAnalysis> eventAnalyses = new ArrayList<>();
        FieldOfViewEventAnalysis fovea = new FieldOfViewEventAnalysis(startDate, endDate, inertialFrame,covDefs,pf,false, false);
        eventAnalyses.add(fovea);

        Scenario scene = new Scenario.Builder(startDate, endDate, utc).eventAnalysis(eventAnalyses).covDefs(covDefs).name("CoverageByConstellation").propagatorFactory(pf).build();

        try {
            scene.call();
        } catch (Exception e) {
            e.printStackTrace();
        }

        GroundEventAnalyzer gea = new GroundEventAnalyzer(fovea.getEvents(covDef));
        long end = System.nanoTime();
        System.out.printf("coverageByConstellation took %.4f sec\n", (end - start) / Math.pow(10, 9));
        return gea;
    }

    public static double arraySum(double[] array) {
        double sum = 0;
        for (double value : array) {
            sum += value;
        }
        return sum;
    }
    public static void saveImage(final MapContent map, final String file, final int imageWidth) {

        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(map);

        Rectangle imageBounds = null;
        ReferencedEnvelope mapBounds = null;
        try {
            mapBounds = map.getMaxBounds();
            double heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0);
            imageBounds = new Rectangle(
                    0, 0, imageWidth, (int) Math.round(imageWidth * heightToWidth));

        } catch (Exception e) {
            // failed to access map layers
            throw new RuntimeException(e);
        }

        BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);

        Graphics2D gr = image.createGraphics();
        gr.setPaint(Color.WHITE);
        gr.fill(imageBounds);

        try {
            renderer.paint(gr, imageBounds, mapBounds);
            File fileToSave = new File(file);
            ImageIO.write(image, "jpeg", fileToSave);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}