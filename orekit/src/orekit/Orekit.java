/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import orekit.analysis.Analysis;
import orekit.analysis.CompoundAnalysis;
import orekit.analysis.ephemeris.OrbitalElementsAnalysis;
import orekit.analysis.vectors.VectorAngleAnalysis;
import orekit.attitude.OscillatingYawSteering;
import orekit.coverage.access.TimeIntervalArray;
import orekit.coverage.parallel.ParallelCoverage;
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.OrbitWizard;
import orekit.object.Satellite;
import orekit.object.fieldofview.RectangularFieldOfView;
import orekit.propagation.PropagatorFactory;
import orekit.propagation.PropagatorType;
import orekit.scenario.Scenario;
import orekit.scenario.ScenarioIO;
import orekit.util.OrekitConfig;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
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
            path = "/Users/nozomihitomi/Desktop";
//            path = "C:\\Users\\SEAK1\\Nozomi\\OREKIT\\";
            filename = "rotating";
        }

        OrekitConfig.init();

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2016, 1, 1, 0, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2016, 1, 3, 0, 00, 00.000, utc);
        double mu = Constants.GRIM5C1_EARTH_MU; // gravitation coefficient

        //must use these frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
                Constants.GRIM5C1_EARTH_FLATTENING, earthFrame);

        //Enter satellites
        double a = 6978137.0;
        double e = 0.0;
        double i = OrbitWizard.SSOinc(a, e);
        double argofperigee = 0.;
        double raan = 0;
        double anomaly = FastMath.toRadians(0);
        Orbit initialOrbit1 = new KeplerianOrbit(a, e, i, argofperigee, raan, anomaly, PositionAngle.TRUE, inertialFrame, startDate, mu);

        double anomaly2 = FastMath.toRadians(90);
        Orbit initialOrbit2 = new KeplerianOrbit(a, e, i, argofperigee, raan, anomaly2, PositionAngle.TRUE, inertialFrame, startDate, mu);

        NadirPointing nadPoint = new NadirPointing(inertialFrame, earthShape);
        OscillatingYawSteering yawSteer = new OscillatingYawSteering(nadPoint, startDate, Vector3D.PLUS_K, FastMath.toRadians(0.1), 0);
        Satellite sat1 = new Satellite("sat1", initialOrbit1);
        RectangularFieldOfView fov_rect = new RectangularFieldOfView(Vector3D.PLUS_K,
                FastMath.toRadians(80), FastMath.toRadians(45), 0);
        Instrument view1 = new Instrument("view1", fov_rect);
        sat1.addInstrument(view1);

        ArrayList<Satellite> satGroup1 = new ArrayList<>();
        satGroup1.add(sat1);

        Constellation constel1 = new Constellation("constel1", satGroup1);

        ArrayList<GeodeticPoint> pts = new ArrayList<>();
//        pts.add(new GeodeticPoint(FastMath.PI / 2, 0, 0));
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 20, earthShape, startDate, endDate);
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", pts, earthShape, startDate, endDate);
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", STKGRID.getPoints(), earthShape, startDate, endDate);

        covDef1.assignConstellation(constel1);

        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef1);
        
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2, initialOrbit2.getType());
        
        double analysisTimeStep = 600;
        ArrayList<Analysis> analysesList = new ArrayList<>();
        analysesList.add(new OrbitalElementsAnalysis(analysisTimeStep));
        analysesList.add(new VectorAngleAnalysis(earthFrame, analysisTimeStep) {
            private static final long serialVersionUID = 4556305811451847873L;
            
            @Override
            public Vector3D getVector1(SpacecraftState currentState, Frame frame) {
                try {
                    Vector3D earthsunPos = CelestialBodyFactory.getSun().getPVCoordinates(currentState.getDate(), frame).getPosition();
//                    Vector3D earthPos = CelestialBodyFactory.getEarth().getPVCoordinates(currentState.getDate(), frame).getPosition();
//                    Vector3D earthSun = sunPos.subtract(earthPos);
                    return new Vector3D(earthsunPos.getX(),earthsunPos.getY(),0.0);
                } catch (OrekitException ex) {
                    Logger.getLogger(Orekit.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
            
            @Override
            public Vector3D getVector2(SpacecraftState currentState, Frame frame) {
                try {
                    Vector3D vec = currentState.getPVCoordinates(frame).getPosition().crossProduct(currentState.getPVCoordinates(frame).getVelocity());
                    return new Vector3D(vec.getX(),vec.getY(),0.0);
                } catch (OrekitException ex) {
                    Logger.getLogger(Orekit.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        });
        CompoundAnalysis analyses = new CompoundAnalysis(analysesList);

        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                analysis(analyses).covDefs(covDefs).name("test1").numThreads(1).
                propagatorFactory(pf).saveAllAccesses(true).build();
//        scen.call();
        ParallelCoverage pc = new ParallelCoverage();
//        try {
//            pc.createSubScenarios(scen, 4, new File("/Users/nozomihitomi/Desktop"));
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Orekit.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        Scenario scenComp = new Scenario(pc.loadRunAndSave(new File("/Users/nozomihitomi/Desktop").toPath(), 2));

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

        ScenarioIO.save(Paths.get(path, ""), filename, scenComp);
        ScenarioIO.saveReadMe(Paths.get(path, ""), filename, scenComp);
        ScenarioIO.saveAnalyses(Paths.get(path, ""), scenComp);

        long end = System.nanoTime();
        System.out.println("Took " + (end - start) / Math.pow(10, 9) + " sec");
    }

}
