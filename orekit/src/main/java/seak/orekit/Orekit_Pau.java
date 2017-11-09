/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import seak.orekit.analysis.Analysis;
import seak.orekit.analysis.ephemeris.OrbitalElementsAnalysis;
import seak.orekit.constellations.Walker;
import seak.orekit.coverage.access.TimeIntervalArray;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.object.CoveragePoint;
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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seak.orekit.analysis.ephemeris.HohmannTransferAnalysis;
import seak.orekit.analysis.vectors.VectorAnalisysEclipseSunlightDiffDrag;
import seak.orekit.coverage.analysis.AnalysisMetric;
import seak.orekit.coverage.analysis.GroundEventAnalyzer;
import seak.orekit.event.EventAnalysis;
import seak.orekit.event.EventAnalysisEnum;
import seak.orekit.event.EventAnalysisFactory;
import seak.orekit.event.FieldOfViewEventAnalysis;
import seak.orekit.event.GroundBodyAngleEventAnalysis;
import seak.orekit.object.Constellation;
import static seak.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA;
import static seak.orekit.object.CoverageDefinition.GridStyle.UNIFORM;
import seak.orekit.object.fieldofview.NadirRectangularFOV;

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
        long start = System.nanoTime();

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "tropics";
        }

        OrekitConfig.init();
        //setup logger
        Level level = Level.ALL;
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

        //Enter satellite orbital parameters
        int h=600000;
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+h;
        double ideg=30;
        double i = FastMath.toRadians(ideg);
        //define instruments
        //NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
        NadirRectangularFOV fov = new NadirRectangularFOV(FastMath.toRadians(57), FastMath.toRadians(20), 0, earthShape);
        ArrayList<Instrument> payload = new ArrayList<>();
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        payload.add(view1);
        
        //number of total satellites
        int t=4;
        //number of planes
        int p=2;
        //
        int f=0;
        
        Walker walker = new Walker("walker1", payload, a, i, t, p, f, inertialFrame, startDate, mu);
        
//        ArrayList<GeodeticPoint> pts = new ArrayList<>();
//        pts.add(new GeodeticPoint(-0.1745329251994330, 6.0737457969402699, 0.0));
//        pts.add(new GeodeticPoint(-0.8726646259971650,  0.209439510239320, 0.0));
//        pts.add(new GeodeticPoint(1.5707963267949001, 0.0000000000000000, 0.0));
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", pts, earthShape);
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 6, earthShape, UNIFORM);
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 9, earthShape, EQUAL_AREA);
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", STKGRID.getPoints20(), earthShape);

        covDef1.assignConstellation(walker);
        
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

        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN,propertiesPropagator);
        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.NUMERICAL,propertiesPropagator);
        
        //LINK BUDGET
//        double txPower = 0.05;
//        double txGain = 1;
//        double rxGain = 31622.8;
//        double lambda = 0.15;
//        double noiseTemperature = 165;
//        double dataRate = 50e6;
//        LinkBudget lb = new LinkBudget(txPower, txGain, rxGain, lambda, noiseTemperature, dataRate);

        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");

        
        //set the event analyses
        EventAnalysisFactory eaf = new EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
        ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
        FieldOfViewEventAnalysis fovEvent = (FieldOfViewEventAnalysis) eaf.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
        eventanalyses.add(fovEvent);

        //set the analyses
        double analysisTimeStep = 60;
        ArrayList<Analysis> analyses = new ArrayList<>();
        for (Satellite sat : walker.getSatellites()) {
            //analyses.add(new OrbitalElementsAnalysis(startDate, endDate, analysisTimeStep, sat, pf));
            //analyses.add(new VectorAnalisysEclipseSunlightDiffDrag(startDate, endDate, analysisTimeStep, sat, pf, inertialFrame, 0.015, 0.075, 0.058, 6));
            //analyses.add(new HohmannTransferAnalysis(startDate, endDate, analysisTimeStep, sat, pf, startDate.shiftedBy(86400),200000,400));
        }
        
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses).analysis(analyses).
                covDefs(covDefs).name(String.format("%s_%s_%s_%s_%s", h,ideg,t,p,f)).properties(propertiesEventAnalysis).
                propagatorFactory(pf).build();
        try {
            Logger.getGlobal().finer(String.format("Running Scenario %s", scen));
            Logger.getGlobal().finer(String.format("Number of points:     %d", covDef1.getNumberOfPoints()));
            Logger.getGlobal().finer(String.format("Number of satellites: %d", walker.getSatellites().size()));
            scen.call();
        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        Logger.getGlobal().finer(String.format("Done Running Scenario %s", scen));
        
//        GroundEventAnalyzer ea = new GroundEventAnalyzer(fovEvent.getEvents(covDef1));
//        DescriptiveStatistics accessStats = ea.getStatistics(AnalysisMetric.DURATION, true);
//        DescriptiveStatistics gapStats = ea.getStatistics(AnalysisMetric.DURATION, false);
//
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
//
        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename, scen, covDef1, fovEvent);
        //ScenarioIO.saveGroundEventAnalysisMetrics(Paths.get(System.getProperty("results"), ""), filename, scen, covDef1, fovEvent);
        ScenarioIO.saveGroundEventAnalyzerObject(Paths.get(System.getProperty("results"), ""), filename, scen,covDef1, fovEvent);
//        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename + "_gsa", scen, covDef1, gndSunAngEvent);
//        ScenarioIO.saveLinkBudget(Paths.get(System.getProperty("results"), ""), filename, scenComp, cdefToSave);
//        ScenarioIO.saveReadMe(Paths.get(path, ""), filename, scenComp);
        
        for (Analysis analysis : analyses) {
            ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                    String.format("%s_%s",scen.toString(),analysis.getName()), analysis);
        }
        long end = System.nanoTime();
        Logger.getGlobal().finest(String.format("Took %.4f sec", (end - start) / Math.pow(10, 9)));
        
        OrekitConfig.end();
    }
    
}
