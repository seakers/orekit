package seakers.orekit.exec;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
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
import seakers.orekit.Orekit_Pau;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.analysis.GSAccessAnalyser;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.FieldOfViewAndGndStationEventAnalysis;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static seakers.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA;

public class LatencyStudy {
    public static void main(String[] args) throws Exception {
        //initializes the look up tables for planetary position (required!)
        OrekitConfig.init(4);

        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));

        File orekitData = new File("orekit/resources");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        //setup logger
        Level level = Level.SEVERE;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2020, 1, 8, 00, 00, 00.000, utc);
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
        double inc = FastMath.toRadians(89);
        double w = 0.0;
        double raan = FastMath.toRadians(116);;


        Orbit orb11 = new KeplerianOrbit(a, e, inc, w, raan, FastMath.toRadians(0.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb12 = new KeplerianOrbit(a, e, inc, w, raan, FastMath.toRadians(120.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb13 = new KeplerianOrbit(a, e, inc, w, raan, FastMath.toRadians(240.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);

        Orbit orb21 = new KeplerianOrbit(a, e, inc, w, raan, FastMath.toRadians(0.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb22 = new KeplerianOrbit(a, e, inc, w, raan, FastMath.toRadians(60.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb23 = new KeplerianOrbit(a, e, inc, w, raan, FastMath.toRadians(120.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb24 = new KeplerianOrbit(a, e, inc, w, raan, FastMath.toRadians(180.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb25 = new KeplerianOrbit(a, e, inc, w, raan, FastMath.toRadians(240.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb26 = new KeplerianOrbit(a, e, inc, w, raan, FastMath.toRadians(300.0),
                PositionAngle.MEAN, inertialFrame, startDate, mu);

        // instruments
        NadirSimpleConicalFOV fovRef = new NadirSimpleConicalFOV(FastMath.toRadians(45),earthShape);
        OffNadirRectangularFOV fovSARL = new OffNadirRectangularFOV(FastMath.toRadians(-35),
                FastMath.toRadians(2.8/2), FastMath.toRadians(2.8/2), 0.0, earthShape);
        OffNadirRectangularFOV fovSARR = new OffNadirRectangularFOV(FastMath.toRadians(35),
                FastMath.toRadians(2.8/2), FastMath.toRadians(2.8/2), 0.0, earthShape);

        Instrument refl = new Instrument("refl", fovRef, 100, 100, 35e6);
        Instrument sarL = new Instrument("SARL", fovSARL, 100, 100, 35e6);
        Instrument sarR = new Instrument("SARL", fovSARR, 100, 100, 2*35e3);

        ArrayList<Instrument> payloadRef = new ArrayList<>();
        payloadRef.add(refl);

        ArrayList<Instrument> payloadSAR = new ArrayList<>();
        payloadSAR.add(sarL);
        payloadSAR.add(sarR);

        ArrayList<Instrument> payloadAll = new ArrayList<>();
        payloadAll.add(refl);
        payloadAll.add(sarL);
        payloadAll.add(sarR);

        // constellation
        HashSet<CommunicationBand> satBands = new HashSet<>();
        satBands.add(CommunicationBand.UHF);

        // -full + complementary
        double memory = 1e9;

        Satellite sat1 = new Satellite("sat41", orb21, null, payloadAll,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS, memory, 130e6);
        Satellite sat2 = new Satellite("sat42", orb22, null, payloadRef,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS, memory, 4e6);
        Satellite sat3 = new Satellite("sat43", orb23, null, payloadAll,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS, memory, 130e6);
        Satellite sat4 = new Satellite("sat44", orb24, null, payloadRef,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS, memory, 4e6);
        Satellite sat5 = new Satellite("sat45", orb25, null, payloadAll,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS, memory, 130e6);
        Satellite sat6 = new Satellite("sat46", orb26, null, payloadRef,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS, memory, 4e6);

        ArrayList<Satellite> satellites =new ArrayList<>();
        satellites.add(sat1);
        satellites.add(sat2);
        satellites.add(sat3);
        satellites.add(sat4);
        satellites.add(sat5);
        satellites.add(sat6);
        Constellation constellation = new Constellation ("full-and-comp",satellites);

        // set up ground stations
        HashSet gndStationsNEN = (new GndStationNetwork()).NEN;
        HashSet gndStationsAWS = (new GndStationNetwork()).AWS;

        ArrayList<ArrayList<GndStation>> gndNetworks = new ArrayList<>();
        gndNetworks.add(new ArrayList<>(gndStationsNEN));
        gndNetworks.add(new ArrayList<>(gndStationsAWS));

        HashMap<ArrayList<GndStation>, HashSet> networkMap= new HashMap<>();
        networkMap.put(gndNetworks.get(0), gndStationsNEN);
        networkMap.put(gndNetworks.get(1), gndStationsAWS);

        HashMap<ArrayList<GndStation>, String> networkNameMap= new HashMap<>();
        networkNameMap.put(gndNetworks.get(0), "NEN");
        networkNameMap.put(gndNetworks.get(1), "AWS");

        ArrayList<Boolean> crossLinks = new ArrayList<>();
        crossLinks.add(true); crossLinks.add(false);

        ArrayList<Double> costs = new ArrayList<>();
        costs.add(490.00); costs.add(22.0);

        // set up down-link strategies
        ArrayList<GSAccessAnalyser.Strategy> strategies = new ArrayList<>();
        strategies.add(GSAccessAnalyser.Strategy.CONSERVATIVE);
        strategies.add(GSAccessAnalyser.Strategy.EVERY_ACCESS);

        // set up directory
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = "latencyResults.csv";

        try{
            fileWriter = new FileWriter(outAddress, false);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        // run smulations
        int i_sim = 1;
        int i_tot = 0;
        double timeTol = 60*60/2;
        double day  = 3600*22;

        for(ArrayList<GndStation> gndNetwork : gndNetworks){
            i_tot += Math.pow(2,gndNetwork.size()) * 2;
        }

        System.out.println("Generating Ground Station Network Combinations...");
        HashMap<ArrayList<GndStation>, HashMap<Integer, ArrayList<String>>> networks = new HashMap<>();
        for(ArrayList<GndStation> gndNetwork : gndNetworks) {
            networks.put(gndNetwork, new HashMap<>());
            for (int j = 1; j < Math.pow(2, gndNetwork.size()); j++) {
                String networkBin = Integer.toBinaryString(j);

                int n_gnd = 0;
                for (int k = 0; k < networkBin.length(); k++) {
                    if(networkBin.charAt(k) == '1') n_gnd++;
                }

                if(!networks.get(gndNetwork).containsKey(n_gnd)){
                    networks.get(gndNetwork).put(n_gnd, new ArrayList<>());
                }
                networks.get(gndNetwork).get(n_gnd).add(networkBin);
            }
        }
        System.out.println("GS Combination Generation DONE");

        for(ArrayList<GndStation> gndNetwork : gndNetworks){
            // Set Coverage Definition
            HashSet<CoverageDefinition> covDefs = new HashSet<>();
            covDefs.add(new CoverageDefinition("covdef1", 9, earthShape, EQUAL_AREA));
            for(CoverageDefinition covDef : covDefs){
                covDef.assignConstellation(constellation);
            }

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

            PropagatorFactory pfJ2 = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);

            //can set the properties of the analyses
            Properties propertiesEventAnalysis = new Properties();
            propertiesEventAnalysis.setProperty("fov.numThreads", "4");
            propertiesEventAnalysis.setProperty("fov.saveAccess", "true");

            // assign ground stations
            HashMap<Satellite, Set<GndStation>> stationAssignment = new HashMap<>();
            for (Satellite sat : constellation.getSatellites()) {
                stationAssignment.put(sat, networkMap.get(gndNetwork));
            }

            // set the analyses
            ArrayList<Analysis<?>> analyses = new ArrayList<>();

            // set event analyses
            ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
            FieldOfViewAndGndStationEventAnalysis events = new FieldOfViewAndGndStationEventAnalysis(startDate,endDate,inertialFrame,covDefs,
                    stationAssignment,pfJ2,true,false,false);
            eventanalyses.add(events);

            // build scenario
            Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                    eventAnalysis(eventanalyses).analysis(analyses).
                    covDefs(covDefs).name("latency_sim").properties(propertiesEventAnalysis).
                    propagatorFactory(pfJ2).build();

            System.out.println("Propagating access times for " + networkNameMap.get(gndNetwork) + " GS network... ");
            try {
                scen.call();
            } catch (Exception ex) {
                Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException("scenario failed to complete.");
            }
            System.out.println("Propagation DONE! ");

            for(Boolean crossLink : crossLinks) {
                if(crossLink){
                    System.out.println("CrossLinks ON");
                }
                else{
                    System.out.println("CrossLinks OFF");
                }
                for(GSAccessAnalyser.Strategy strategy : strategies) {
                    for (Integer n_gnd : networks.get(gndNetwork).keySet()) {
                        for (String networkBin : networks.get(gndNetwork).get(n_gnd)) {
                            ArrayList<GndStation> gndNetworkActive = new ArrayList<>();

                            int delta = gndNetwork.size() - networkBin.length();

                            for (int k = 0; k < networkBin.length(); k++) {
                                if (networkBin.charAt(k) == '1') {
                                    gndNetworkActive.add(gndNetwork.get(k + delta));
                                }
                            }

                            long start_i = System.nanoTime();
                            boolean costPerAccess = gndNetworks.indexOf(gndNetwork) == 0;
                            double cost = costs.get(gndNetworks.indexOf(gndNetwork));

                            LatencyResults results = new LatencySim(events, constellation, covDefs, gndNetworkActive, crossLink, cost, costPerAccess, strategy, startDate, endDate).calcResults();

                            long end_i = System.nanoTime();
                            System.out.print(String.format("Sim no. " + i_sim + "/" + i_tot + " with " + n_gnd + " GS took %.4f sec (mean gap time %.4f min)\n", (end_i - start_i) / Math.pow(10, 9), results.getGapTime() / 60));

                            i_sim++;

                            if (results.getGapTime() >= day) {
                                continue;
                            }

                            printWriter.print(results.toString(networkBin, costPerAccess, crossLink));

//                        if(results.getGapTime() < timeTol){
//                            System.out.print("BREAK: " + n_gnd + " number of ground stations exceeds performance.\n");
//                            break;
//                        }
                            break;
                        }
                    }
                }
            }
        }
        System.out.print("DONE");
        printWriter.close();
    }
}
