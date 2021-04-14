/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.analysis.ephemeris.OrbitalElementsAnalysis;
import seakers.orekit.event.*;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
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
import seakers.orekit.coverage.analysis.AnalysisMetric;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.object.Constellation;
import static seakers.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA;
import seakers.orekit.object.CommunicationBand;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import seakers.orekit.util.Orbits;

/**
 *
 * @author paugarciabuzzi
 */
public class Orekit_Pau {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
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

        OrekitConfig.init(4);
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2020, 1, 8, 00, 00, 00.000, utc);
        //AbsoluteDate endDate = new AbsoluteDate(2021, 1, 1, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        //Enter satellite orbital parameters
        double a500 = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+500000;
        double a600 = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+600000;
        double a700 = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+700000;
        double i30 = FastMath.toRadians(30);
        double i51 = FastMath.toRadians(51.6);
        double i90 = FastMath.toRadians(90);
        double iSSO = Orbits.incSSO(600);
        //define instruments
        //NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
        NadirRectangularFOV fov = new NadirRectangularFOV(FastMath.toRadians(57), FastMath.toRadians(20), 0, earthShape);
        //OffNadirRectangularFOV fov = new OffNadirRectangularFOV(FastMath.toRadians(70),
        //        FastMath.toRadians(2),FastMath.toRadians(2),0,earthShape);
        ArrayList<Instrument> payload = new ArrayList<>();
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        payload.add(view1);
        
//        //number of total satellites
//        int t=10;
//        //number of planes
//        int p=1;
//        
//        int f=0;
//        
//        Walker constel = new Walker("walker1", payload, a, i, t, p, f, inertialFrame, startDate, mu);
        ArrayList<Satellite> satellites=new ArrayList<>();
        HashSet<CommunicationBand> satBands = new HashSet<>();
        satBands.add(CommunicationBand.UHF);
        Orbit orb1 = new KeplerianOrbit(a600, 0.0001, iSSO, 0.0, Math.toRadians(0), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        //Satellite sat1 = new Satellite("sat1", orb1, null, payload);
        Satellite sat1 = new Satellite("sat1", orb1, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Orbit orb2 = new KeplerianOrbit(a600, 0.0001, iSSO, 0.0, Math.toRadians(0), Math.toRadians(180), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat2 = new Satellite("sat2", orb2, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Orbit orb3 = new KeplerianOrbit(a600, 0.0001, iSSO, 0.0, Math.toRadians(120), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat3 = new Satellite("sat3", orb3, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Orbit orb4 = new KeplerianOrbit(a600, 0.0001, iSSO, 0.0, Math.toRadians(120), Math.toRadians(180), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat4 = new Satellite("sat4", orb4, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Orbit orb5 = new KeplerianOrbit(a600, 0.0001, iSSO, 0.0, Math.toRadians(240), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat5 = new Satellite("sat5", orb5, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Orbit orb6 = new KeplerianOrbit(a600, 0.0001, iSSO, 0.0, Math.toRadians(240), Math.toRadians(180), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat6 = new Satellite("sat6", orb6, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
//        Orbit orb7 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat7 = new Satellite("sat7", orb7, null, payload);
//        Orbit orb8 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(60), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat8 = new Satellite("sat8", orb8, null, payload);
//        Orbit orb9 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(120), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat9 = new Satellite("sat9", orb9, null, payload);
//        Orbit orb10 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(180), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat10 = new Satellite("sat10", orb10, null, payload);
//        Orbit orb11 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(240), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat11 = new Satellite("sat11", orb11, null, payload);
//        Orbit orb12 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(300), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat12 = new Satellite("sat12", orb12, null, payload);
        satellites.add(sat1);
        satellites.add(sat2);
//        satellites.add(sat3);
//        satellites.add(sat4);
//        satellites.add(sat5);
//        satellites.add(sat6);
//        satellites.add(sat7);
//        satellites.add(sat8);
//        satellites.add(sat9);
//        satellites.add(sat10);
//        satellites.add(sat11);
//        satellites.add(sat12);


        Constellation constel = new Constellation ("tropics2",satellites);

        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 9, earthShape, EQUAL_AREA);

        covDef1.assignConstellation(constel);
        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef1);
        
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
        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);
        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2MODIFIED,propertiesPropagator);
        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.NUMERICAL,propertiesPropagator);
        
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");
        propertiesEventAnalysis.setProperty("fov.saveAccess", "true");

        
        //set the event analyses
        EventAnalysisFactory eaf = new EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
        ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
        FieldOfViewEventAnalysis fovEvent = (FieldOfViewEventAnalysis) eaf.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);

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
        for (Satellite sat : constel.getSatellites()){
            stationAssignment.put(sat, gndStations);
        }
        
        //eventanalyses.add(fovEvent);
        GndStationEventAnalysis gndEvent = (GndStationEventAnalysis) eaf.createGroundStationAnalysis(EventAnalysisEnum.ACCESS, stationAssignment, propertiesEventAnalysis);
        eventanalyses.add(gndEvent);



//        //set the analyses
//        double analysisTimeStep = 60;
        ArrayList<Analysis<?>> analyses = new ArrayList<>();
//        for (Satellite sat : walker.getSatellites()) {
//            analyses.add(new OrbitalElementsAnalysis(startDate, endDate, 0.1, sat, pf));
//        }
        
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses).analysis(analyses).
                covDefs(covDefs).name("trialOffNadir").properties(propertiesEventAnalysis).
                propagatorFactory(pf).build();
        try {
//            Logger.getGlobal().finer(String.format("Running Scenario %s", scen));
//            Logger.getGlobal().finer(String.format("Number of points:     %d", covDef1.getNumberOfPoints()));
//            Logger.getGlobal().finer(String.format("Number of satellites: %d", constel.getSatellites().size()));
            long start1 = System.nanoTime();
            scen.call();
            long end1 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end1 - start1) / Math.pow(10, 9)));
            
        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        Logger.getGlobal().finer(String.format("Done Running Scenario %s", scen));
        
        
        //set the event analyses
        ArrayList<EventAnalysis> eventanalyses2 = new ArrayList<>();
        FieldOfViewAndGndStationEventAnalysis Event2 = new FieldOfViewAndGndStationEventAnalysis(startDate, endDate,
            inertialFrame, covDefs, stationAssignment,pf, true, false, false);
        eventanalyses2.add(Event2);
        Scenario scen2 = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses2).analysis(analyses).
                covDefs(covDefs).name("Latency_trial2").properties(propertiesEventAnalysis).
                propagatorFactory(pf).build();
        try {
            long start2 = System.nanoTime();
            scen2.call();
            long end2 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end2 - start2) / Math.pow(10, 9)));
            
        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }
        
        Logger.getGlobal().finer(String.format("Done Running Scenario %s", scen2));
        
//        //without crosslinks
//        LatencyGroundEventAnalyzer latencyAnalyzer=new LatencyGroundEventAnalyzer(Event2.getAllAccesses().get(covDef1),Event2.getAllAccessesGS(),false);
//        DescriptiveStatistics latencyStats = latencyAnalyzer.getStatistics(new double[]{FastMath.toRadians(-30), FastMath.toRadians(30)}, new double[]{-Math.PI, Math.PI});
//        System.out.println("Without crosslinks:");
//        System.out.println(String.format("Max latency time %s mins", latencyStats.getMax()/60));
//        System.out.println(String.format("Mean latency time %s mins", latencyStats.getMean()/60));
//        System.out.println(String.format("Min latency time %s mins", latencyStats.getMin()/60));
//        System.out.println(String.format("50th latency time %s mins", latencyStats.getPercentile(50)/60));
//        System.out.println(String.format("80th latency time %s mins", latencyStats.getPercentile(80)/60));
//        System.out.println(String.format("90th latency time %s mins", latencyStats.getPercentile(90)/60));
//        //with crosslinks
//        LatencyGroundEventAnalyzer latencyAnalyzer2=new LatencyGroundEventAnalyzer(Event2.getAllAccesses().get(covDef1),Event2.getAllAccessesGS(),true);
//        DescriptiveStatistics latencyStats2 = latencyAnalyzer2.getStatistics(new double[]{FastMath.toRadians(-30), FastMath.toRadians(30)}, new double[]{-Math.PI, Math.PI});
//       System.out.println("With crosslinks:");
//        System.out.println(String.format("Max latency time %s mins", latencyStats2.getMax()/60));
//        System.out.println(String.format("Mean latency time %s mins", latencyStats2.getMean()/60));
//        System.out.println(String.format("Min latency time %s mins", latencyStats2.getMin()/60));
//        System.out.println(String.format("50th latency time %s mins", latencyStats2.getPercentile(50)/60));
//        System.out.println(String.format("80th latency time %s mins", latencyStats2.getPercentile(80)/60));
//        System.out.println(String.format("90th latency time %s mins", latencyStats2.getPercentile(90)/60));
        
        
//        Properties props=new Properties();
//        GroundEventAnalyzer ea = new GroundEventAnalyzer(fovEvent.getEvents(covDef1));
//        DescriptiveStatistics accessStats = ea.getStatistics(AnalysisMetric.DURATION, true,props);
//        DescriptiveStatistics gapStats = ea.getStatistics(AnalysisMetric.DURATION, false,props);

//        System.out.println(String.format("Max access time %s", accessStats.getMax()));
//        System.out.println(String.format("Mean access time %s", accessStats.getMean()));
//        System.out.println(String.format("Min access time %s", accessStats.getMin()));
//        System.out.println(String.format("50th access time %s", accessStats.getPercentile(50)));
//        System.out.println(String.format("80th access time %s", accessStats.getPercentile(80)));
//        System.out.println(String.format("90th access time %s", accessStats.getPercentile(90)));
//
//        System.out.println(String.format("Max gap time %s", gapStats.getMax()));
//        System.out.println(String.format("Mean gap time %s", gapStats.getMean()));
//        System.out.println(String.format("Min gap time %s", gapStats.getMin()));
//        System.out.println(String.format("50th gap time %s", gapStats.getPercentile(50)));
//        System.out.println(String.format("80th gap time %s", gapStats.getPercentile(80)));
//        System.out.println(String.format("90th gap time %s", gapStats.getPercentile(90)));

        Properties props=new Properties();
        GroundEventAnalyzer ea = new GroundEventAnalyzer(gndEvent.getEvents());
        DescriptiveStatistics accessStats = ea.getStatistics(AnalysisMetric.DURATION, true,props);
        DescriptiveStatistics gapStats = ea.getStatistics(AnalysisMetric.DURATION, false,props);

        System.out.println(String.format("Max access time %s", accessStats.getMax()));
        System.out.println(String.format("Mean access time %s", accessStats.getMean()));
        System.out.println(String.format("Min access time %s", accessStats.getMin()));
        System.out.println(String.format("50th access time %s", accessStats.getPercentile(50)));
        System.out.println(String.format("80th access time %s", accessStats.getPercentile(80)));
        System.out.println(String.format("90th access time %s", accessStats.getPercentile(90)));

        System.out.println(String.format("Max gap time %s", gapStats.getMax()));
        System.out.println(String.format("Mean gap time %s", gapStats.getMean()));
        System.out.println(String.format("Min gap time %s", gapStats.getMin()));
        System.out.println(String.format("50th gap time %s", gapStats.getPercentile(50)));
        System.out.println(String.format("80th gap time %s", gapStats.getPercentile(80)));
        System.out.println(String.format("90th gap time %s", gapStats.getPercentile(90)));

        GroundEventAnalyzer ea2 = new GroundEventAnalyzer(Event2.getEventsGS());
        DescriptiveStatistics accessStats2 = ea2.getStatistics(AnalysisMetric.DURATION, true,props);
        DescriptiveStatistics gapStats2 = ea2.getStatistics(AnalysisMetric.DURATION, false,props);
        System.out.println("---------------------------------");
        System.out.println(String.format("Max access time %s", accessStats2.getMax()));
        System.out.println(String.format("Mean access time %s", accessStats2.getMean()));
        System.out.println(String.format("Min access time %s", accessStats2.getMin()));
        System.out.println(String.format("50th access time %s", accessStats2.getPercentile(50)));
        System.out.println(String.format("80th access time %s", accessStats2.getPercentile(80)));
        System.out.println(String.format("90th access time %s", accessStats2.getPercentile(90)));

        System.out.println(String.format("Max gap time %s", gapStats2.getMax()));
        System.out.println(String.format("Mean gap time %s", gapStats2.getMean()));
        System.out.println(String.format("Min gap time %s", gapStats2.getMin()));
        System.out.println(String.format("50th gap time %s", gapStats2.getPercentile(50)));
        System.out.println(String.format("80th gap time %s", gapStats2.getPercentile(80)));
        System.out.println(String.format("90th gap time %s", gapStats2.getPercentile(90)));

       // ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename, scen, covDef1, fovEvent);
//        ScenarioIO.saveGroundEventAnalysisMetrics(Paths.get(System.getProperty("results"), ""), filename, scen, covDef1, fovEvent);
//        ScenarioIO.saveGroundEventAnalyzerObject(Paths.get(System.getProperty("results"), ""), filename, scen,covDef1, fovEvent);
//        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename + "_gsa", scen, covDef1, gndSunAngEvent);
//        ScenarioIO.saveReadMe(Paths.get(path, ""), filename, scenComp);
        

        for (Analysis<?> analysis : analyses) {
            ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                    String.format("%s_%s",scen.toString(),analysis.getName()), analysis);
        }
        long end = System.nanoTime();
//        Logger.getGlobal().finest(String.format("Took %.4f sec", (end - start) / Math.pow(10, 9)));
//        
        OrekitConfig.end();
    }
    
}
