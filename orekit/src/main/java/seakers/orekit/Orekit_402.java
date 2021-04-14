package seakers.orekit;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
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
import org.orekit.frames.TopocentricFrame;
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
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.analysis.AnalysisMetric;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.event.*;
import seakers.orekit.object.*;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.object.fieldofview.OffNadirRectangularFOV;
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

        File orekitData = new File("../dmas3/data/orekit-data");
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
        AbsoluteDate endDate = new AbsoluteDate(2021, 2, 8, 00, 00, 00.000, utc);
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
//        NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(42.6),earthShape);
        OffNadirRectangularFOV fov = new OffNadirRectangularFOV(FastMath.toRadians(35), FastMath.toRadians(15),FastMath.toRadians(15),0,earthShape);
        OffNadirRectangularFOV fov_opposite = new OffNadirRectangularFOV(FastMath.toRadians(-35), FastMath.toRadians(15),FastMath.toRadians(15),0,earthShape);

        ArrayList<Instrument> payload = new ArrayList<>();
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        Instrument view2 = new Instrument("view2", fov_opposite, 100, 100);
        payload.add(view1);
        payload.add(view2);

        // -GNSS Transmitters
        NadirSimpleConicalFOV gpsFOV = new NadirSimpleConicalFOV(FastMath.toRadians(42.6),earthShape);

        ArrayList<Instrument> gpsPayload = new ArrayList<>();
        Instrument gpsAntenna = new Instrument("GPS", gpsFOV, 100,100);
        gpsPayload.add(gpsAntenna);

        // define constellations
        // -Reflectometerr Receivers
        HashSet<CommunicationBand> satBands = new HashSet<>();
        satBands.add(CommunicationBand.UHF);

        Satellite sat1 = new Satellite("sat1", orb1, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat2 = new Satellite("sat2", orb2, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat3 = new Satellite("sat3", orb3, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite sat4 = new Satellite("sat4", orb4, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

        ArrayList<Satellite> satellites=new ArrayList<>();
        satellites.add(sat1);
        satellites.add(sat2);
        satellites.add(sat3);
        satellites.add(sat4);
        Constellation constel = new Constellation ("Reflectometers",satellites);

        // -GNSS Transmitters
        double aGPS = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 20180e3;
        double iGPS = FastMath.toRadians(55);

        double aMUOS = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 35786e3;
        double iMUOS = 0;

        Walker gpsWalker = new Walker("GPS_Walker", gpsPayload, aGPS, iGPS, 24, 4, 0, inertialFrame, startDate, mu, 0, 0);

        ArrayList<Constellation> constellations = new ArrayList<>();
        constellations.add(constel);
        constellations.add(gpsWalker);

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

//        System.out.println(records.size());

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
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", points);

        covDef1.assignConstellation(constellations);

        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef1);

        // set up propagator
        Properties propertiesPropagator = new Properties();
        PropagatorFactory pfJ2 = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);

        //can set the properties of the analyses
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");
        propertiesEventAnalysis.setProperty("fov.saveAccess", "true");
//        propertiesEventAnalysis.setProperty("fov.saveAccess", "false");

        //set up ground stations
        Set<GndStation> gndStations = new HashSet<>();
        TopocentricFrame wallopsTopo = new TopocentricFrame(earthShape, new GeodeticPoint(FastMath.toRadians(37.94019444), FastMath.toRadians(-75.46638889), 0.), "Wallops");
        HashSet<CommunicationBand> wallopsBands = new HashSet<>();
        wallopsBands.add(CommunicationBand.UHF);
        gndStations.add(new GndStation(wallopsTopo, new ReceiverAntenna(6., wallopsBands), new TransmitterAntenna(6., wallopsBands), FastMath.toRadians(10.)));
        TopocentricFrame moreheadTopo = new TopocentricFrame(earthShape, new GeodeticPoint(FastMath.toRadians(38.19188139), FastMath.toRadians(-83.43861111), 0.), "Mroehead");
        HashSet<CommunicationBand> moreheadBands = new HashSet<>();
        moreheadBands.add(CommunicationBand.UHF);
        gndStations.add(new GndStation(moreheadTopo, new ReceiverAntenna(47., moreheadBands), new TransmitterAntenna(47., moreheadBands), FastMath.toRadians(10.)));
        TopocentricFrame GS1Topo = new TopocentricFrame(earthShape, new GeodeticPoint(FastMath.toRadians(0), FastMath.toRadians(0), 0.), "GS1");
        HashSet<CommunicationBand> GS1Bands = new HashSet<>();
        GS1Bands.add(CommunicationBand.UHF);
        gndStations.add(new GndStation(GS1Topo, new ReceiverAntenna(10., GS1Bands), new TransmitterAntenna(10., GS1Bands), FastMath.toRadians(10.)));
        TopocentricFrame GS2Topo = new TopocentricFrame(earthShape, new GeodeticPoint(FastMath.toRadians(20), FastMath.toRadians(80), 0.), "GS2");
        HashSet<CommunicationBand> GS2Bands = new HashSet<>();
        GS2Bands.add(CommunicationBand.UHF);
        gndStations.add(new GndStation(GS2Topo, new ReceiverAntenna(10., GS2Bands), new TransmitterAntenna(10., GS2Bands), FastMath.toRadians(10.)));


        HashMap<Satellite, Set<GndStation>> stationAssignment = new HashMap<>();

        for(Constellation cons : constellations) {
            for (Satellite sat : cons.getSatellites()) {
                stationAssignment.put(sat, gndStations);
            }
        }

        //set the coverage event analyses
        EventAnalysisFactory eaf = new EventAnalysisFactory(startDate, endDate, inertialFrame, pfJ2);
        ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();

        ReflectionEventAnalysis reflEvent = (ReflectionEventAnalysis) eaf.createReflectionAnalysis(EventAnalysisEnum.REFLECTOR, constel, gpsWalker, covDefs, propertiesEventAnalysis);
        eventanalyses.add(reflEvent);
//set the analyses
        ArrayList<Analysis<?>> analyses = new ArrayList<>();


        //set the event analyses
        ArrayList<EventAnalysis> eventanalyses2 = new ArrayList<>();
        FieldOfViewAndGndStationEventAnalysis Event2 = new FieldOfViewAndGndStationEventAnalysis(startDate, endDate, inertialFrame, covDefs, stationAssignment,pfJ2, true, false, true);
//        FieldOfViewEventAnalysis Event2 = (FieldOfViewEventAnalysis) eaf.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
        eventanalyses2.add(Event2);


        Scenario scen2 = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses).analysis(analyses).
                covDefs(covDefs).name("402Test").properties(propertiesEventAnalysis).
                propagatorFactory(pfJ2).build();
        try {
            long start2 = System.nanoTime();
            scen2.call();
            long end2 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end2 - start2) / Math.pow(10, 9)));

        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        HashMap<CoverageDefinition, HashMap<Satellite, HashMap<TopocentricFrame, TimeIntervalArray>>> access =  ((FieldOfViewAndGndStationEventAnalysis) ( (ArrayList) scen2.getEventAnalyses()).get(0)).getAllAccesses();

        for(CoverageDefinition cdef : covDefs){
            HashMap<TopocentricFrame, TimeIntervalArray> cov = access.get(cdef).get(sat1);
            for(TopocentricFrame frame : cov.keySet()){
                TimeIntervalArray interval = cov.get(frame);
                if(!interval.isEmpty()){
                    int x = 1;
                }
            }
        }

        Logger.getGlobal().finer(String.format("Done Running Scenario %s", scen2));

        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""),
                String.format("%s_%s",scen2.toString(),"coverage"), scen2, covDef1, Event2);

        OrekitConfig.end();
    }

}
