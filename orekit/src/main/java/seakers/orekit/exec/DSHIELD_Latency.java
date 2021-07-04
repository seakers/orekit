package seakers.orekit.exec;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
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
import seakers.orekit.Orekit_Pau;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.coverage.analysis.GSAccessAnalyser;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.GndStationEventAnalysis;
import seakers.orekit.object.*;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.object.fieldofview.OffNadirRectangularFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.util.OrekitConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

public class DSHIELD_Latency {

    public static void main(String[] args) throws Exception {
        //initializes the look up tables for planteary position (required!)
        OrekitConfig.init(4);

        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));

        File orekitData = new File("orekit/resources");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "tropics";
        }

        //setup logger
        Level level = Level.SEVERE;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2020, 1, 2, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        // orbits
        double h = 502.5 * 1e3;
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + h;
        double e = 0.0001;
        double i = FastMath.toRadians(89);
        double w = 0.0;
        double raan = FastMath.toRadians(116);;


        Orbit orb11 = new KeplerianOrbit(a, e, i, w, raan, FastMath.toRadians(0.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb12 = new KeplerianOrbit(a, e, i, w, raan, FastMath.toRadians(120.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb13 = new KeplerianOrbit(a, e, i, w, raan, FastMath.toRadians(240.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);

        Orbit orb21 = new KeplerianOrbit(a, e, i, w, raan, FastMath.toRadians(0.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb22 = new KeplerianOrbit(a, e, i, w, raan, FastMath.toRadians(60.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb23 = new KeplerianOrbit(a, e, i, w, raan, FastMath.toRadians(120.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb24 = new KeplerianOrbit(a, e, i, w, raan, FastMath.toRadians(180.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb25 = new KeplerianOrbit(a, e, i, w, raan, FastMath.toRadians(240.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb26 = new KeplerianOrbit(a, e, i, w, raan, FastMath.toRadians(300.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);


        // instruments
        NadirSimpleConicalFOV fovRef = new NadirSimpleConicalFOV(FastMath.toRadians(45),earthShape);
        OffNadirRectangularFOV fovSARL = new OffNadirRectangularFOV(FastMath.toRadians(-35),
                            FastMath.toRadians(2.8/2), FastMath.toRadians(2.8/2), 0.0, earthShape);
        OffNadirRectangularFOV fovSARR = new OffNadirRectangularFOV(FastMath.toRadians(35),
                FastMath.toRadians(2.8/2), FastMath.toRadians(2.8/2), 0.0, earthShape);

        Instrument refl = new Instrument("refl", fovRef, 100, 100);
        Instrument sarL = new Instrument("SARL", fovSARL, 100, 100);
        Instrument sarR = new Instrument("SARL", fovSARR, 100, 100);

        ArrayList<Instrument> payloadRef = new ArrayList<>();
        payloadRef.add(refl);

        ArrayList<Instrument> payloadSAR = new ArrayList<>();
        payloadSAR.add(sarL);
        payloadSAR.add(sarR);

        ArrayList<Instrument> payloadAll = new ArrayList<>();
        payloadRef.add(refl);
        payloadSAR.add(sarL);
        payloadSAR.add(sarR);

        // constellations
        HashSet<CommunicationBand> satBands = new HashSet<>();
        satBands.add(CommunicationBand.UHF);
            // -radar arch
            Satellite sat11 = new Satellite("sat11", orb11, null, payloadSAR,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat12 = new Satellite("sat12", orb12, null, payloadSAR,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat13 = new Satellite("sat13", orb13, null, payloadSAR,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

            ArrayList<Satellite> satellites1 =new ArrayList<>();
            satellites1.add(sat11);
            satellites1.add(sat12);
            satellites1.add(sat13);
            Constellation constel1 = new Constellation ("SARs",satellites1);

            // -full arch
            Satellite sat21 = new Satellite("sat21", orb11, null, payloadAll,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat22 = new Satellite("sat22", orb12, null, payloadAll,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat23 = new Satellite("sat23", orb13, null, payloadAll,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

            ArrayList<Satellite> satellites2 =new ArrayList<>();
            satellites2.add(sat21);
            satellites2.add(sat22);
            satellites2.add(sat23);
            Constellation constel2 = new Constellation ("FullArch",satellites2);

            // -radar + complementary
            Satellite sat31 = new Satellite("sat31", orb21, null, payloadSAR,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat32 = new Satellite("sat32", orb22, null, payloadRef,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat33 = new Satellite("sat33", orb23, null, payloadSAR,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat34 = new Satellite("sat34", orb24, null, payloadRef,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat35 = new Satellite("sat35", orb25, null, payloadSAR,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat36 = new Satellite("sat36", orb26, null, payloadRef,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

            ArrayList<Satellite> satellites3 =new ArrayList<>();
            satellites3.add(sat31);
            satellites3.add(sat32);
            satellites3.add(sat33);
            satellites3.add(sat34);
            satellites3.add(sat35);
            satellites3.add(sat36);
            Constellation constel3 = new Constellation ("radar-and-comp",satellites3);

            // -full + complementary
            Satellite sat41 = new Satellite("sat41", orb21, null, payloadAll,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat42 = new Satellite("sat42", orb22, null, payloadRef,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat43 = new Satellite("sat43", orb23, null, payloadAll,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat44 = new Satellite("sat44", orb24, null, payloadRef,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat45 = new Satellite("sat45", orb25, null, payloadAll,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
            Satellite sat46 = new Satellite("sat46", orb26, null, payloadRef,
                    new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

            ArrayList<Satellite> satellites4 =new ArrayList<>();
            satellites4.add(sat41);
            satellites4.add(sat42);
            satellites4.add(sat43);
            satellites4.add(sat44);
            satellites4.add(sat45);
            satellites4.add(sat46);
            Constellation constel4 = new Constellation ("full-and-comp",satellites4);

        ArrayList<Constellation> constellations = new ArrayList<>();
//        constellations.add(constel1);
//        constellations.add(constel2);
//        constellations.add(constel3);
        constellations.add(constel4);

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
            int id = parseInt(records.get(idx).get(0));
            double lat = parseDouble(records.get(idx).get(1));
            double lon = parseDouble(records.get(idx).get(2));

            lat = Math.toRadians(lat);
            lon = Math.toRadians(lon);
            GeodeticPoint landPoint = new GeodeticPoint(lat,lon,0.0);
            CoveragePoint point = new CoveragePoint(earthShape, landPoint, String.valueOf(id));

            points.add(point);
        }

        //create a coverage definition
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", points);
        CoverageDefinition covDef2 = new CoverageDefinition("covdef2", points);
        CoverageDefinition covDef3 = new CoverageDefinition("covdef3", points);
        CoverageDefinition covDef4 = new CoverageDefinition("covdef4", points);

        covDef1.assignConstellation(constel1);
        covDef2.assignConstellation(constel2);
        covDef3.assignConstellation(constel3);
        covDef4.assignConstellation(constel4);

        HashSet<CoverageDefinition> covDefs1 = new HashSet<>();
        covDefs1.add(covDef1);
        HashSet<CoverageDefinition> covDefs2 = new HashSet<>();
        covDefs2.add(covDef2);
        HashSet<CoverageDefinition> covDefs3 = new HashSet<>();
        covDefs3.add(covDef3);
        HashSet<CoverageDefinition> covDefs4 = new HashSet<>();
        covDefs4.add(covDef4);

        HashMap<Constellation,HashSet<CoverageDefinition>> covDefs = new HashMap<>();
        covDefs.put(constel1,covDefs1);
        covDefs.put(constel2,covDefs2);
        covDefs.put(constel3,covDefs3);
        covDefs.put(constel4,covDefs4);

        // set up propagator
        Properties propertiesPropagator = new Properties();
        propertiesPropagator.setProperty("orekit.propagator.mass", "6");
        propertiesPropagator.setProperty("orekit.propagator.atmdrag", "true");
        propertiesPropagator.setProperty("orekit.propagator.dragarea", "0.075");
        propertiesPropagator.setProperty("orekit.propagator.dragcoeff", "2.2");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.sun", "true");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.moon", "true");
        propertiesPropagator.setProperty("orekit.propagator.solarpressure", "true");
        propertiesPropagator.setProperty("orekit.propagator.solararea", "0.058");

        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN,propertiesPropagator);
        PropagatorFactory pfJ2 = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);

        //can set the properties of the analyses
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");
        propertiesEventAnalysis.setProperty("fov.saveAccess", "true");

        // set up ground stations
        HashSet gndStationsNEN = (new GndStationNetwork()).NEN;
        HashSet gndStationsAWS = (new GndStationNetwork()).AWS;

        ArrayList<HashSet> gndNetworks = new ArrayList<>();
        gndNetworks.add(gndStationsNEN);
        gndNetworks.add(gndStationsAWS);

        HashMap<HashSet, String> gndNetworkNames = new HashMap<>();
//        gndNetworkNames.put(gndStationsNEN,"NEN");
        gndNetworkNames.put(gndStationsAWS,"AWS");

        for(HashSet gndNetwork : gndNetworks){
            HashMap<Constellation, HashMap<Satellite, Set<GndStation>>> stationAssignment = new HashMap<>();

            for(Constellation cons : constellations) {
                stationAssignment.put(cons,new HashMap<>());
                for (Satellite sat : cons.getSatellites()) {
                    stationAssignment.get(cons).put(sat, gndNetwork);
                }
            }


            System.out.println("*********************************************************" );
            System.out.println("Ground Station Network: " + gndNetworkNames.get(gndNetwork));
            System.out.println("*********************************************************" );

            for(Constellation cons : constellations){
                // set the analyses
                ArrayList<Analysis<?>> analyses = new ArrayList<>();

                // set event analyses
                ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
                GndStationEventAnalysis events = new GndStationEventAnalysis(startDate, endDate,
                        inertialFrame, stationAssignment.get(cons), pfJ2);
                eventanalyses.add(events);

                // build scenario
                Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                        eventAnalysis(eventanalyses).analysis(analyses).
                        covDefs(covDefs.get(cons)).name("Latency_trial").properties(propertiesEventAnalysis).
                        propagatorFactory(pf).build();
                long start_i = System.nanoTime();
                try {
                    scen.call();
                } catch (Exception ex) {
                    Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IllegalStateException("scenario failed to complete.");
                }
                long end_i = System.nanoTime();
                Logger.getGlobal().finest(String.format("Took %.4f sec", (end_i - start_i) / Math.pow(10, 9)));
                System.out.println("_________________________________________________________" );
                System.out.println("CONSTELLATION: " + cons.getName() );
                System.out.println(String.format("Took %.4f sec", (end_i - start_i) / Math.pow(10, 9)));

                // gs access time - no Cross-Links
                GSAccessAnalyser accessTimes = new GSAccessAnalyser(events.getAllAccesses(),startDate,endDate,false, GSAccessAnalyser.Strategy.CONSERVATIVE);
                HashMap<Satellite, DescriptiveStatistics> accessTimesStats = accessTimes.getAccessTimesStatistics();

                System.out.println("\nGround Station Access Times - Without Cross-Links:");
                for(Satellite sat : cons.getSatellites()) {
                    System.out.println("-" + sat.getName() + ":");
                    System.out.println(String.format("\tMax access time %s mins", accessTimesStats.get(sat).getMax() / 60));
                    System.out.println(String.format("\tMean access time %s mins", accessTimesStats.get(sat).getMean() / 60));
                    System.out.println(String.format("\tMin access time %s mins", accessTimesStats.get(sat).getMin() / 60));
                    System.out.println(String.format("\t50th access time %s mins", accessTimesStats.get(sat).getPercentile(50) / 60));
                    System.out.println(String.format("\t80th access time %s mins", accessTimesStats.get(sat).getPercentile(80) / 60));
                    System.out.println(String.format("\t90th access time %s mins", accessTimesStats.get(sat).getPercentile(90) / 60));
                }

                // gs access time - with Cross-Links
                GSAccessAnalyser accessTimesCL = new GSAccessAnalyser(events.getAllAccesses(),startDate,endDate,true, GSAccessAnalyser.Strategy.CONSERVATIVE);
                HashMap<Satellite, DescriptiveStatistics> accessTimesStatsCL = accessTimesCL.getAccessTimesStatistics();

                System.out.println("\nGround Station Access Times - With Cross-Links:");
                Satellite sat1 = accessTimesStatsCL.keySet().iterator().next();
                System.out.println("-All Sats:");
                System.out.println(String.format("\tMax access time %s mins", accessTimesStatsCL.get(sat1).getMax()/60));
                System.out.println(String.format("\tMean access time %s mins", accessTimesStatsCL.get(sat1).getMean()/60));
                System.out.println(String.format("\tMin access time %s mins", accessTimesStatsCL.get(sat1).getMin()/60));
                System.out.println(String.format("\t50th access time %s mins", accessTimesStatsCL.get(sat1).getPercentile(50)/60));
                System.out.println(String.format("\t80th access time %s mins", accessTimesStatsCL.get(sat1).getPercentile(80)/60));
                System.out.println(String.format("\t90th access time %s mins", accessTimesStatsCL.get(sat1).getPercentile(90)/60));

                // gs access duration - without Cross-Links
                HashMap<Satellite, Double> accessDurationStats = accessTimes.getAccessDurationStatistics();

                System.out.println("\nGround Station Total Access Duration - Without Cross-Links:");
                for(Satellite sat : cons.getSatellites()) {
                    System.out.println("-" + sat.getName() + ": " + accessDurationStats.get(sat)/60 + " min");
                }

                // gs access duration - with Cross-Links
                HashMap<Satellite, Double> accessDurationStatsCL = accessTimesCL.getAccessDurationStatistics();

                System.out.println("\nGround Station Total Access Duration - With Cross-Links:");
                System.out.println("-All Sats: " + accessDurationStatsCL.get(sat1)/60 + " min");

                // planner latency - no Cross-Links
                HashMap<Satellite, DescriptiveStatistics> latencyPlanStats = accessTimes.getGapTime();

                System.out.println("\nPlanner Latency - Without Cross-Links:");
                for(Satellite sat : cons.getSatellites()) {
                    System.out.println("-" + sat.getName() + ":");
                    System.out.println(String.format("\tMax latency time %s mins", latencyPlanStats.get(sat).getMax() / 60));
                    System.out.println(String.format("\tMean latency time %s mins", latencyPlanStats.get(sat).getMean() / 60));
                    System.out.println(String.format("\tMin latency time %s mins", latencyPlanStats.get(sat).getMin() / 60));
                    System.out.println(String.format("\t50th latency time %s mins", latencyPlanStats.get(sat).getPercentile(50) / 60));
                    System.out.println(String.format("\t80th latency time %s mins", latencyPlanStats.get(sat).getPercentile(80) / 60));
                    System.out.println(String.format("\t90th latency time %s mins", latencyPlanStats.get(sat).getPercentile(90) / 60));
                }

                // planner latency - with Cross-Links
                HashMap<Satellite, DescriptiveStatistics> latencyPlanCLStats = accessTimesCL.getGapTime();

                System.out.println("-All Sats:");
                System.out.println(String.format("\tMax latency time %s mins", latencyPlanCLStats.get(sat1).getMax()/60));
                System.out.println(String.format("\tMean latency time %s mins", latencyPlanCLStats.get(sat1).getMean()/60));
                System.out.println(String.format("\tMin latency time %s mins", latencyPlanCLStats.get(sat1).getMin()/60));
                System.out.println(String.format("\t50th latency time %s mins", latencyPlanCLStats.get(sat1).getPercentile(50)/60));
                System.out.println(String.format("\t80th latency time %s mins", latencyPlanCLStats.get(sat1).getPercentile(80)/60));
                System.out.println(String.format("\t90th latency time %s mins", latencyPlanCLStats.get(sat1).getPercentile(90)/60));

                // mean response time - without Cross-Links
                HashMap<Satellite, Double> meanResponseTime = accessTimes.getMeanResponseTime();

                System.out.println("\nMean Response Time - Without Cross-Links:");
                for(Satellite sat : cons.getSatellites()) {
                    System.out.println("-" + sat.getName() + ": " + meanResponseTime.get(sat)/60 + " min");
                }

                // gs access duration - with Cross-Links
                HashMap<Satellite, Double> meanResponseTimeCL = accessTimesCL.getMeanResponseTime();

                System.out.println("\nMean Response Time - With Cross-Links:");
                System.out.println("-All Sats: " + meanResponseTimeCL.get(sat1)/60 + " min");

                // time average gap - without Cross-Links
                HashMap<Satellite, Double> timeAverageGap = accessTimes.getTimeAverageGap();

                System.out.println("\nTime Average Gap - Without Cross-Links:");
                for(Satellite sat : cons.getSatellites()) {
                    System.out.println("-" + sat.getName() + ": " + timeAverageGap.get(sat)/60 + " min");
                }

                // time average gap - with Cross-Links
                HashMap<Satellite, Double> timeAverageGapCL = accessTimesCL.getTimeAverageGap();

                System.out.println("\nTime Average Gap - With Cross-Links:");
                System.out.println("-All Sats: " + timeAverageGapCL.get(sat1)/60 + " min");

                // number of passes per day - without Cross-Links
                HashMap<Satellite, Double> passes = accessTimes.getNumberOfPasses();

                System.out.println("\nTime Average Gap - Without Cross-Links:");
                for(Satellite sat : cons.getSatellites()) {
                    System.out.println("-" + sat.getName() + ": " + passes.get(sat));
                }

                // number of passes per day - with Cross-Links
                HashMap<Satellite, Double> passesCL = accessTimesCL.getNumberOfPasses();

                System.out.println("\nTime Average Gap - With Cross-Links:");
                System.out.println("-All Sats: " + passesCL.get(sat1));
            }
        }

        System.out.print("DONE");
        OrekitConfig.end();
    }
}
