package seakers.orekit;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.constellations.Walker;
import seakers.orekit.event.*;
import seakers.orekit.object.*;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static seakers.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA;

public class Orekit_402 {
    public static void main(String[] args) throws OrekitException {
        //initializes the look up tables for planteary position (required!)
        OrekitConfig.init(4);

        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));

        File orekitData = new File("orekit\\resources");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "tropics";
        }

        //setup logger
        Level level = Level.FINER;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2021, 2, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2021, 3, 1, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        // define orbits
        double a = 6771*1e3;
        double e = 0.0001;
        double i = FastMath.toRadians(98);
        double w = 0.0;

        double raan1 = 0;
        double raan2 = 0;
        double raan3 = FastMath.toRadians(90);
        double raan4 = FastMath.toRadians(90);

        double anom1 = 0;
        double anom2 = FastMath.toRadians(180);
        double anom3 = FastMath.toRadians(90);
        double anom4 = FastMath.toRadians(270);

        Orbit orb1 = new KeplerianOrbit(a, e, i, w, raan1, anom1, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb2 = new KeplerianOrbit(a, e, i, w, raan2, anom2, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb3 = new KeplerianOrbit(a, e, i, w, raan3, anom3, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb4 = new KeplerianOrbit(a, e, i, w, raan4, anom4, PositionAngle.MEAN, inertialFrame, startDate, mu);

        // define payloads
        // -Reflectometer Receivers
        NadirSimpleConicalFOV fov1 = new NadirSimpleConicalFOV(FastMath.toRadians(7.5),earthShape);
        NadirSimpleConicalFOV fov2 = new NadirSimpleConicalFOV(FastMath.toRadians(15),earthShape);
        NadirSimpleConicalFOV fov3 = new NadirSimpleConicalFOV(FastMath.toRadians(30),earthShape);

        ArrayList<Instrument> payload1 = new ArrayList<>();
        Instrument view11 = new Instrument("view1", fov1, 100, 100);
        payload1.add(view11);

        ArrayList<Instrument> payload2 = new ArrayList<>();
        Instrument view21 = new Instrument("view1", fov2, 100, 100);
        payload2.add(view21);

        ArrayList<Instrument> payload3 = new ArrayList<>();
        Instrument view31 = new Instrument("view1", fov3, 100, 100);
        payload3.add(view31);

        // -GNSS Transmitters
        NadirSimpleConicalFOV gpsFOV = new NadirSimpleConicalFOV(FastMath.toRadians(42.6),earthShape);

        ArrayList<Instrument> gpsPayload = new ArrayList<>();
        Instrument gpsAntenna = new Instrument("GPS", gpsFOV, 100,100);
        gpsPayload.add(gpsAntenna);

        // define constellations
        // -Reflectometerr Receivers
        HashSet<CommunicationBand> satBands = new HashSet<>();
        satBands.add(CommunicationBand.UHF);

        Satellite sat11 = new Satellite("sat1", orb1, null, payload1,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat12 = new Satellite("sat2", orb2, null, payload1,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat13 = new Satellite("sat3", orb3, null, payload1,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat14 = new Satellite("sat4", orb4, null, payload1,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

        ArrayList<Satellite> satellites1 =new ArrayList<>();
        satellites1.add(sat11);
        satellites1.add(sat12);
        satellites1.add(sat13);
        satellites1.add(sat14);
        Constellation constel1 = new Constellation ("Reflectometers0",satellites1);

        Satellite sat21 = new Satellite("sat1", orb1, null, payload2,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat22 = new Satellite("sat2", orb2, null, payload2,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat23 = new Satellite("sat3", orb3, null, payload2,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat24 = new Satellite("sat4", orb4, null, payload2,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

        ArrayList<Satellite> satellites2 =new ArrayList<>();
        satellites2.add(sat21);
        satellites2.add(sat22);
        satellites2.add(sat23);
        satellites2.add(sat24);
        Constellation constel2 = new Constellation ("Reflectometers15",satellites2);

        Satellite sat31 = new Satellite("sat1", orb1, null, payload3,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat32 = new Satellite("sat2", orb2, null, payload3,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat33 = new Satellite("sat3", orb3, null, payload3,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat34 = new Satellite("sat4", orb4, null, payload3,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

        ArrayList<Satellite> satellites3 =new ArrayList<>();
        satellites2.add(sat31);
        satellites2.add(sat32);
        satellites2.add(sat33);
        satellites2.add(sat34);
        Constellation constel3 = new Constellation ("Reflectometers30",satellites3);

        // -GNSS Transmitters
        double aGPS = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 26559.8e3;
        double iGPS = FastMath.toRadians(55);

        double aMUOS = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 35786e3;
        double iMUOS = 0;

        Walker gpsWalker = new Walker("GPS_Walker", gpsPayload, aGPS, iGPS, 24, 6, 0, inertialFrame, startDate, mu, 0, 0);

        ArrayList<Constellation> constellations1 = new ArrayList<>();
        constellations1.add(constel1);

        ArrayList<Constellation> constellations2 = new ArrayList<>();
        constellations2.add(constel2);

        ArrayList<Constellation> constellations3 = new ArrayList<>();
        constellations3.add(constel3);

        // Set Coverage Definition
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("./CoverageDatabase/CoverageDefinition1_Grid_Point_Information.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        }
        catch (Exception err) {
            System.out.println(err);
        }

        ArrayList<CoveragePoint> points = new ArrayList<>();
        for(int idx = 1; idx < records.size()-1; idx++) {
//            System.out.println(idx);

            int id = parseInt(records.get(idx).get(0));
            double lat = parseDouble(records.get(idx).get(1));
            double lon = parseDouble(records.get(idx).get(2));

//            if(lon < 0){
//                lon = 360 + lon;
//            }

            lat = Math.toRadians(lat);
            lon = Math.toRadians(lon);
            GeodeticPoint landPoint = new GeodeticPoint(lat,lon,0.0);
            CoveragePoint point = new CoveragePoint(earthShape, landPoint, String.valueOf(id));

            points.add(point);
        }


        //create a coverage definition
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", points);
//        CoverageDefinition covDef2 = new CoverageDefinition("covdef2", points);
//        CoverageDefinition covDef3 = new CoverageDefinition("covdef3", points);

        double deg = 1;
        double th_g = Math.toRadians(deg);

        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", deg, earthShape, EQUAL_AREA);
        CoverageDefinition covDef2 = new CoverageDefinition("covdef2", deg, earthShape, EQUAL_AREA);
        CoverageDefinition covDef3 = new CoverageDefinition("covdef3", deg, earthShape, EQUAL_AREA);


        covDef1.assignConstellation(constellations1);
        covDef2.assignConstellation(constellations2);
        covDef3.assignConstellation(constellations3);

        HashSet<CoverageDefinition> covDefs1 = new HashSet<>();
        covDefs1.add(covDef1);
        HashSet<CoverageDefinition> covDefs2 = new HashSet<>();
        covDefs2.add(covDef2);
        HashSet<CoverageDefinition> covDefs3 = new HashSet<>();
        covDefs3.add(covDef3);

        // set up propagator
        Properties propertiesPropagator = new Properties();
        PropagatorFactory pfJ2 = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);

        //can set the properties of the analyses
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");
        propertiesEventAnalysis.setProperty("fov.saveAccess", "true");

        //set the coverage event analyses
        EventAnalysisFactory eaf = new EventAnalysisFactory(startDate, endDate, inertialFrame, pfJ2);

        ArrayList<EventAnalysis> eventanalyses1 = new ArrayList<>();
        ReflectometerEventAnalysis refEvent1 = new ReflectometerEventAnalysis(startDate, endDate, inertialFrame, covDefs1, pfJ2, true, true, gpsWalker, th_g);
        eventanalyses1.add(refEvent1);

        ArrayList<EventAnalysis> eventanalyses2 = new ArrayList<>();
        ReflectometerEventAnalysis refEvent2 = new ReflectometerEventAnalysis(startDate, endDate, inertialFrame, covDefs2, pfJ2, true, true, gpsWalker, th_g);
        eventanalyses2.add(refEvent2);

        ArrayList<EventAnalysis> eventanalyses3 = new ArrayList<>();
        ReflectometerEventAnalysis refEvent3 = new ReflectometerEventAnalysis(startDate, endDate, inertialFrame, covDefs3, pfJ2, true, true, gpsWalker, th_g);
        eventanalyses3.add(refEvent3);


        //set the analyses
        ArrayList<Analysis<?>> analyses = new ArrayList<>();

        Scenario scen1 = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses1).analysis(analyses).
                covDefs(covDefs1).name("402_0deg").properties(propertiesEventAnalysis).
                propagatorFactory(pfJ2).build();

        Scenario scen2 = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses2).analysis(analyses).
                covDefs(covDefs1).name("402_15deg").properties(propertiesEventAnalysis).
                propagatorFactory(pfJ2).build();

        Scenario scen3 = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses3).analysis(analyses).
                covDefs(covDefs1).name("402_30deg").properties(propertiesEventAnalysis).
                propagatorFactory(pfJ2).build();

        try {
            long start1 = System.nanoTime();
            scen1.call();
            long end1 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end1 - start1) / Math.pow(10, 9)));

        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        try {
            long start2 = System.nanoTime();
            scen2.call();
            long end2 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end2 - start2) / Math.pow(10, 9)));

        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        try {
            long start3 = System.nanoTime();
            scen3.call();
            long end3 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end3 - start3) / Math.pow(10, 9)));

        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }


        Logger.getGlobal().finer(String.format("Done Running Scenario %s", scen2));

        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""),
                String.format("%s_%s","Reflectometer0deg","coverage"), scen1, covDef1, refEvent1);
        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""),
                String.format("%s_%s","Reflectometer15deg","coverage"), scen2, covDef2, refEvent2);
        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""),
                String.format("%s_%s","Reflectometer30deg","coverage"), scen3, covDef3, refEvent3);

        OrekitConfig.end();
    }

}
