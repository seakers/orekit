/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import orekit.access.TimeIntervalArray;
import orekit.access.TimeIntervalMerger;
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.Satellite;
import orekit.propagation.PropagatorFactory;
import orekit.propagation.PropagatorType;
import orekit.scenario.Scenario;
import orekit.scenario.ScenarioIO;
import orekit.scenario.ScenarioStepWise;
import orekit.util.OrekitConfig;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.events.FieldOfView;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

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
        AbsoluteDate startDate = new AbsoluteDate(2004, 01, 01, 00, 00, 00.000, utc);
        AbsoluteDate endDate =   new AbsoluteDate(2004, 01, 01, 12, 00, 00.000, utc);

        double mu = Constants.EGM96_EARTH_MU; // gravitation coefficient
        CelestialBody earth = CelestialBodyFactory.getEarth();
        Frame eme2000 = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                earth.getBodyOrientedFrame());

        //Enter satellites
        double a = 6971000.0;
        double e = 0.0000001;
        double i = FastMath.toRadians(90);
        double argofperigee = 0.;
        double raan = 0.0;
        double anomaly = FastMath.toRadians(90);
        Orbit initialOrbit1 = new KeplerianOrbit(a, e, i, argofperigee, raan, anomaly, PositionAngle.TRUE, earth.getInertiallyOrientedFrame(), startDate, mu);

        double anomaly2 = FastMath.toRadians(0);
        Orbit initialOrbit2 = new KeplerianOrbit(a * 1.1, e, i, argofperigee, raan, anomaly2, PositionAngle.TRUE, earth.getInertiallyOrientedFrame(), startDate, mu);

        NadirPointing nadPoint = new NadirPointing(earth.getInertiallyOrientedFrame(), earthShape);
        Satellite sat1 = new Satellite("sat1", initialOrbit1, nadPoint);
        FieldOfView fov1 = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                FastMath.toRadians(25), Vector3D.PLUS_J, FastMath.toRadians(25), .001);
        Instrument view1 = new Instrument("view1", fov1);
        sat1.addInstrument(view1);

        Satellite sat2 = new Satellite("sat2", initialOrbit2, nadPoint);
        FieldOfView fov2 = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                FastMath.toRadians(25), Vector3D.PLUS_J, FastMath.toRadians(25), .001);
        Instrument view2 = new Instrument("view1", fov2);
        sat2.addInstrument(view2);

        ArrayList<Satellite> satGroup1 = new ArrayList<>();
        satGroup1.add(sat1);
        satGroup1.add(sat2);

        Constellation constel1 = new Constellation("constel1", satGroup1);

//        ArrayList<GeodeticPoint> pts = new ArrayList<>();
//        pts.add(new GeodeticPoint(FastMath.PI / 2, 0, 0));
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 30, earthShape, startDate, endDate);
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", pts, earthShape, startDate, endDate);

        covDef1.assignConstellation(constel1);

        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN, initialOrbit2);

//        Scenario scen = new Scenario("test", startDate, endDate, utc, earth.getInertiallyOrientedFrame(), pf);
        ScenarioStepWise scen = new ScenarioStepWise("test", startDate, endDate, utc, earth.getInertiallyOrientedFrame(), pf);

        scen.addCoverageDefinition(covDef1);

        scen.call();

        System.out.println(String.format("Done Running Scenario %s", scen));

        HashMap<CoveragePoint, TimeIntervalArray> covDefAccess = scen.getMergedAccesses(covDef1);
        DescriptiveStatistics accessStats = new DescriptiveStatistics();
        DescriptiveStatistics gapStats = new DescriptiveStatistics();
        for (CoveragePoint pt : covDefAccess.keySet()) {
            for(Double duration : covDefAccess.get(pt).getDurations()){
                accessStats.addValue(duration);
            }
            for(Double duration : covDefAccess.get(pt).negate().getDurations()){
                gapStats.addValue(duration);
            }
        }

        System.out.println(String.format("Max access time %s", accessStats.getMax()));
        System.out.println(String.format("Mean access time %s", accessStats.getMean()));
        System.out.println(String.format("Min access time %s", accessStats.getMin()));

        System.out.println(String.format("Max gap time %s", gapStats.getMax()));
        System.out.println(String.format("Mean gap time %s", gapStats.getMean()));
        System.out.println(String.format("Min gap time %s", gapStats.getMin()));
        
        System.out.println("Saving scenario...");

//        ScenarioIO.save(Paths.get(path, ""), filename, scen);
//        ScenarioIO.saveReadMe(Paths.get(path, ""), filename, scen);

        long end = System.nanoTime();
        System.out.println("Took " + (end - start) / Math.pow(10, 9) + " sec");
    }

}
