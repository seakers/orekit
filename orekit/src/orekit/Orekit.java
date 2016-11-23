/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import orekit.analysis.Analysis;
import orekit.analysis.CompoundAnalysis;
import orekit.analysis.ephemeris.OrbitalElementsAnalysis;
import orekit.analysis.events.EventAnalysis;
import orekit.analysis.events.EventAnalysis2;
import orekit.constellations.Walker;
import orekit.coverage.access.TimeIntervalArray;
import orekit.coverage.parallel.ParallelCoverage;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.Satellite;
import orekit.object.fieldofview.FOVDetector;
import orekit.object.fieldofview.NadirRectangularFOV;
import orekit.object.fieldofview.NadirSimpleConicalFOV;
import orekit.propagation.PropagatorFactory;
import orekit.propagation.PropagatorType;
import orekit.scenario.Scenario;
import orekit.scenario.ScenarioIO;
import orekit.util.OrekitConfig;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.events.FieldOfView;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

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
        long start = System.nanoTime();

        String filename;
        String path;
        if (args.length > 0) {
            path = args[0];
            filename = args[1];
        } else {
//            path = "/Users/nozomihitomi/Desktop";
            path = "C:\\Users\\SEAK1\\Nozomi\\OREKIT\\";
            filename = "rotating";
        }

        OrekitConfig.init();

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2016, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2016, 1, 2, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use these frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        //Enter satellite orbital parameters
        double a = 6978137.0;
        double i = FastMath.toRadians(90);

        Walker walker = new Walker("walker1", i, 1, 1, 0, a, inertialFrame, startDate, mu);

        //define instruments
//        NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(Vector3D.PLUS_K, FastMath.toRadians(45), earthShape);
        NadirRectangularFOV fov = new NadirRectangularFOV(Vector3D.PLUS_K,  FastMath.toRadians(45), FastMath.toRadians(45), 0, earthShape);
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        //assign instruments
        for (Satellite sat : walker.getSatellites()) {
            sat.addInstrument(view1);
        }

        ArrayList<GeodeticPoint> pts = new ArrayList<>();
        pts.add(new GeodeticPoint(FastMath.PI / 2, 0, 0));
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 20, earthShape);
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", pts, earthShape);
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", STKGRID.getPoints20(), earthShape);

        covDef1.assignConstellation(walker);

        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef1);

        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN, OrbitType.KEPLERIAN);

        double analysisTimeStep = 1;
        ArrayList<Analysis> analysesList = new ArrayList<>();
        analysesList.add(new OrbitalElementsAnalysis(analysisTimeStep));
        ArrayList<FOVDetector> fovs = new ArrayList<FOVDetector>();
        for(CoveragePoint pt : covDef1.getPoints()){
            fovs.add( new FOVDetector(pt, view1).withMaxCheck(1));
        }
//        analysesList.add(new EventAnalysis(analysisTimeStep, fovs));
        analysesList.add(new EventAnalysis2(analysisTimeStep, new ArrayList(covDef1.getPoints()), earthShape,  new FieldOfView(Vector3D.PLUS_K,
                       Vector3D.PLUS_I, FastMath.toRadians(45),
                       Vector3D.PLUS_J, FastMath.toRadians(45),0),
                        inertialFrame, startDate));
        
        CompoundAnalysis analyses = new CompoundAnalysis(analysesList);

        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                analysis(analyses).covDefs(covDefs).name("test1").numThreads(1).
                propagatorFactory(pf).saveAllAccesses(true).saveToDB(true).build();
        scen.call();
        ParallelCoverage pc = new ParallelCoverage();
//        try {
//            pc.createSubScenarios(scen, 4, new File(path));
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Orekit.class.getName()).log(Level.SEVERE, null, ex);
//        }

//        Scenario scenComp = new Scenario(pc.loadRunAndSave(new File(path).toPath(), 4));
        Scenario scenComp = scen;

        System.out.println(String.format("Done Running Scenario %s", scenComp));

        CoverageDefinition cdefToSave = scenComp.getCoverageDefinitions().iterator().next();
        HashMap<CoveragePoint, TimeIntervalArray> covDefAccess = scenComp.getMergedAccesses(cdefToSave);

        DescriptiveStatistics accessStats = new DescriptiveStatistics();
        DescriptiveStatistics gapStats = new DescriptiveStatistics();
        if (covDefAccess != null) {
            for (CoveragePoint pt : covDefAccess.keySet()) {
                for (Double duration : covDefAccess.get(pt).getDurations()) {
                    accessStats.addValue(duration);
                }
                for (Double duration : covDefAccess.get(pt).negate().getDurations()) {
                    gapStats.addValue(duration);
                }

            }

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

            ScenarioIO.saveAccess(Paths.get(path, ""), filename, scenComp, cdefToSave);
        }

        System.out.println("Saving scenario...");

//        ScenarioIO.save(Paths.get(path, ""), filename, scenComp);
        ScenarioIO.saveReadMe(Paths.get(path, ""), filename, scenComp);
        ScenarioIO.saveAnalyses(Paths.get(path, ""), scenComp);

        long end = System.nanoTime();
        System.out.println("Took " + (end - start) / Math.pow(10, 9) + " sec");
    }

}
