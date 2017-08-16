/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import seak.orekit.analysis.Analysis;
import seak.orekit.analysis.ephemeris.OrbitalElementsAnalysis;
import seak.orekit.constellations.Walker;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.object.Instrument;
import seak.orekit.object.Satellite;
import seak.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seak.orekit.object.linkbudget.LinkBudget;
import seak.orekit.propagation.PropagatorFactory;
import seak.orekit.propagation.PropagatorType;
import seak.orekit.scenario.Scenario;
import seak.orekit.scenario.ScenarioIO;
import seak.orekit.util.OrekitConfig;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seak.orekit.analysis.AbstractSpacecraftAnalysis;
import seak.orekit.analysis.CompoundSpacecraftAnalysis;
import seak.orekit.coverage.analysis.AnalysisMetric;
import seak.orekit.coverage.analysis.FastCoverageAnalysis;
import seak.orekit.coverage.analysis.GroundEventAnalyzer;
import seak.orekit.event.EventAnalysis;
import seak.orekit.event.EventAnalysisEnum;
import seak.orekit.event.EventAnalysisFactory;
import seak.orekit.event.FieldOfViewEventAnalysis;
import seak.orekit.object.Constellation;
import seak.orekit.orbit.J2KeplerianOrbit;
import seak.orekit.sensitivity.CoverageVersusOrbitalElements;

/**
 *
 * @author nozomihitomi
 */
public class Orekit {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));
        long start = System.nanoTime();

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "tropics_test";
        }

        OrekitConfig.init();
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2016, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2016, 1, 15, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        //Enter satellite orbital parameters
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 600000;
        double i = FastMath.toRadians(45);

        //define instruments
        NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
//        NadirRectangularFOV fov = new NadirRectangularFOV(FastMath.toRadians(57), FastMath.toRadians(2.5), 0, earthShape);
        ArrayList<Instrument> payload = new ArrayList<>();
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        payload.add(view1);

        Walker walker = new Walker("walker1", payload, a, i, 2, 2, 0, inertialFrame, startDate, mu);

        ArrayList<GeodeticPoint> pts = new ArrayList<>();
//        pts.add(new GeodeticPoint(-0.1745329251994330, 6.0737457969402699, 0.0));
//        pts.add(new GeodeticPoint(-0.8726646259971650,  -2.72271363311116, 0.0));
//        pts.add(new GeodeticPoint(1.5707963267949001, 0.0000000000000000, 0.0));
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", pts, earthShape);
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 20, earthShape, CoverageDefinition.GridStyle.UNIFORM);
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", STKGRID.getPoints20(), earthShape);

        CoverageVersusOrbitalElements cvoe = 
                new CoverageVersusOrbitalElements.
                        Builder(1000, startDate, endDate, covDef1.getPoints()).
                        setSAParam(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400000, Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 800000).
                        setIParam(FastMath.toRadians(30), FastMath.toRadians(90)).
                        setSensorParam(FastMath.toRadians(30), FastMath.toRadians(90)).
                        setNThreads(6).build();
        try {
            cvoe.run();
        } catch (Exception ex) {
            Logger.getLogger(Orekit.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.exit(0);

        covDef1.assignConstellation(walker);

        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef1);

        FastCoverageAnalysis fca = new FastCoverageAnalysis(startDate, endDate, inertialFrame, covDefs, FastMath.toRadians(45), 6);
        start = System.nanoTime();
        fca.call();
//        System.exit(0);
        long end1 = System.nanoTime();
        Logger.getGlobal().finest(String.format("Took %.4f sec", (end1 - start) / Math.pow(10, 9)));

        Properties propertiesPropagator = new Properties();
        propertiesPropagator.setProperty("orekit.propagator.atmdrag", "true");
        propertiesPropagator.setProperty("orekit.propagator.dragarea", "10");
        propertiesPropagator.setProperty("orekit.propagator.dragcoeff", "2.2");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.sun", "true");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.moon", "true");
        propertiesPropagator.setProperty("orekit.propagator.solarpressure", "true");
        propertiesPropagator.setProperty("orekit.propagator.solararea", "10");

        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN, propertiesPropagator);
//        PropagatorFactory pf = new PropagatorFactory(PropagatorType.NUMERICAL, propertiesPropagator);

        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("numThreads", "6");

        //set the event analyses
        EventAnalysisFactory eaf = new EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
        ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
        FieldOfViewEventAnalysis fovEvent = (FieldOfViewEventAnalysis) eaf.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
        eventanalyses.add(fovEvent);
//        GroundBodyAngleEventAnalysis gndSunAngEvent = (GroundBodyAngleEventAnalysis) eaf.create(EventAnalysisEnum.GND_BODY_ANGLE, properties);
//        eventanalyses.add(gndSunAngEvent);

        //set the analyses
        double analysisTimeStep = 60;
        ArrayList<Analysis> analyses = new ArrayList<>();
//        for (Satellite sat : walker.getSatellites()) {
            //analyses.add(new OrbitalElementsAnalysis(startDate, endDate, analysisTimeStep, sat, pf));
//            analyses.add(new VectorAnalisysEclipseSunlightDiffDrag(startDate, endDate, analysisTimeStep, sat, pf, inertialFrame, 0.015, 0.075, 0.058, 6));
//        }

        //LINK BUDGET
        double txPower = 0.05;
        double txGain = 1;
        double rxGain = 31622.8;
        double lambda = 0.15;
        double noiseTemperature = 165;
        double dataRate = 50e6;
        LinkBudget lb = new LinkBudget(txPower, txGain, rxGain, lambda, noiseTemperature, dataRate);

        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses).analysis(analyses).
                covDefs(covDefs).name("test1").properties(propertiesEventAnalysis).
                propagatorFactory(pf).build();
        try {
            Logger.getGlobal().finer(String.format("Running Scenario %s", scen));
            Logger.getGlobal().finer(String.format("Number of points:     %d", covDef1.getNumberOfPoints()));
            Logger.getGlobal().finer(String.format("Number of satellites: %d", walker.getSatellites().size()));
            scen.call();
        } catch (Exception ex) {
            Logger.getLogger(Orekit.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }
        
        GroundEventAnalyzer ea2 = new GroundEventAnalyzer(fca.getEvents(covDef1));
        DescriptiveStatistics accessStats2 = ea2.getStatistics(AnalysisMetric.DURATION, true);
        DescriptiveStatistics gapStats2 = ea2.getStatistics(AnalysisMetric.DURATION, false);

        GroundEventAnalyzer ea = new GroundEventAnalyzer(fovEvent.getEvents(covDef1));
        DescriptiveStatistics accessStats = ea.getStatistics(AnalysisMetric.DURATION, true);
        DescriptiveStatistics gapStats = ea.getStatistics(AnalysisMetric.DURATION, false);

        System.out.println(String.format("Max access time %s\t%s", accessStats.getMax(), accessStats2.getMax()));
        System.out.println(String.format("Mean access time %s\t%s", accessStats.getMean(), accessStats2.getMean()));
        System.out.println(String.format("Min access time %s\t%s", accessStats.getMin(), accessStats2.getMin()));
        System.out.println(String.format("50th access time %s\t%s", accessStats.getPercentile(50), accessStats2.getPercentile(50)));
        System.out.println(String.format("80th acceses time %s\t%s", accessStats.getPercentile(80), accessStats2.getPercentile(80)));
        System.out.println(String.format("90th access time %s\t%s", accessStats.getPercentile(90), accessStats2.getPercentile(90)));

        System.out.println(String.format("Max gap time %s\t%s", gapStats.getMax(), gapStats2.getMax()));
        System.out.println(String.format("Mean gap time %s\t%s", gapStats.getMean(), gapStats2.getMean()));
        System.out.println(String.format("Min gap time %s\t%s", gapStats.getMin(), gapStats2.getMin()));
        System.out.println(String.format("50th gap time %s\t%s", gapStats.getPercentile(50), gapStats2.getPercentile(50)));
        System.out.println(String.format("80th gap time %s\t%s", gapStats.getPercentile(80), gapStats2.getPercentile(80)));
        System.out.println(String.format("90th gap time %s\t%s", gapStats.getPercentile(90), gapStats2.getPercentile(90)));
        
        for(int j=1; j<=100; j++){
            System.out.println(String.format("%s,%s,%s",j,gapStats.getPercentile(j), gapStats2.getPercentile(j)));
        }

        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename + "_cva", scen, covDef1, fca);
        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename + "_fov", scen, covDef1, fovEvent);
        ScenarioIO.saveGroundEventAnalysisMetrics(Paths.get(System.getProperty("results"), ""), filename + "_fov_metrics", scen, ea, AnalysisMetric.DURATION, false);
        ScenarioIO.saveGroundEventAnalysisMetrics(Paths.get(System.getProperty("results"), ""), filename + "_cva_metrics", scen, ea2, AnalysisMetric.DURATION, false);
//        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename + "_gsa", scen, covDef1, gndSunAngEvent);
//        ScenarioIO.saveLinkBudget(Paths.get(System.getProperty("results"), ""), filename, scenComp, cdefToSave);
//        ScenarioIO.saveReadMe(Paths.get(path, ""), filename, scenComp);

        for (Analysis analysis : analyses) {
            ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                    String.format("%s_%s", scen.toString(), "analysis"), analysis);
        }
        long end = System.nanoTime();
        Logger.getGlobal().finest(String.format("Took %.4f sec", (end - end1) / Math.pow(10, 9)));
    }

}
