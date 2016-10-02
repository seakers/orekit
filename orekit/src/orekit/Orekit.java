/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import orekit.coverage.access.TimeIntervalArray;
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.Satellite;
import orekit.object.fieldofview.RectangularFieldOfView;
import orekit.object.fieldofview.SimpleConicalFieldOfView;
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
            filename = "rotating";
        }

        OrekitConfig.init();

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2016, 1, 1, 16, 00, 00.000, utc);
        AbsoluteDate endDate   = new AbsoluteDate(2016, 3, 1, 16, 00, 00.000, utc);

        double mu = Constants.EGM96_EARTH_MU; // gravitation coefficient
        
        //must use these frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        //Enter satellites
        double a = 6978137.0;
        double e = 0.00000000001;
        double i = FastMath.toRadians(90);
        double argofperigee = 0.;
        double raan = 0;
        double anomaly = FastMath.toRadians(0);
        Orbit initialOrbit1 = new KeplerianOrbit(a, e, i, argofperigee, raan, anomaly, PositionAngle.TRUE, inertialFrame, startDate, mu);

        double anomaly2 = FastMath.toRadians(90);
        Orbit initialOrbit2 = new KeplerianOrbit(a, e, i, argofperigee, raan, anomaly2, PositionAngle.TRUE, inertialFrame, startDate, mu);

        NadirPointing nadPoint = new NadirPointing(inertialFrame, earthShape);
        Satellite sat1 = new Satellite("sat1", initialOrbit1, nadPoint);
        RectangularFieldOfView fov_rect = new RectangularFieldOfView(Vector3D.PLUS_K, 
                FastMath.toRadians(80), FastMath.toRadians(45), 0);
        SimpleConicalFieldOfView fov_cone = new SimpleConicalFieldOfView(Vector3D.PLUS_K,
                FastMath.toRadians(45));
        Instrument view1 = new Instrument("view1", fov_rect);
        sat1.addInstrument(view1);

        ArrayList<Satellite> satGroup1 = new ArrayList<>();
        satGroup1.add(sat1);

        Constellation constel1 = new Constellation("constel1", satGroup1);

//        ArrayList<GeodeticPoint> pts = new ArrayList<>();
//        pts.add(new GeodeticPoint(FastMath.PI / 2, 0, 0));
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 20, earthShape, startDate, endDate);
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", pts, earthShape, startDate, endDate);
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", STKGRID.getPoints(), earthShape, startDate, endDate);

        covDef1.assignConstellation(constel1);

        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN, initialOrbit2);

        Scenario scen = new Scenario("test", startDate, endDate, utc, inertialFrame, pf, false, 3);
//        ScenarioStepWise scen = new ScenarioStepWise("test", startDate, endDate, utc, inertialFrame, pf);

        scen.addCoverageDefinition(covDef1);

        scen.call();

        System.out.println(String.format("Done Running Scenario %s", scen));

        HashMap<CoveragePoint, TimeIntervalArray> covDefAccess = scen.getMergedAccesses(covDef1);

        DescriptiveStatistics accessStats = new DescriptiveStatistics();
        DescriptiveStatistics gapStats = new DescriptiveStatistics();
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

        System.out.println("Saving scenario...");

        ScenarioIO.save(Paths.get(path, ""), filename, scen);
        ScenarioIO.saveReadMe(Paths.get(path, ""), filename, scen);
        ScenarioIO.saveAccess(Paths.get(path, ""), filename, scen, covDef1);

        long end = System.nanoTime();
        System.out.println("Took " + (end - start) / Math.pow(10, 9) + " sec");
    }

}
