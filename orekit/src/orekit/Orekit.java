/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit;

import java.util.ArrayList;
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.Instrument;
import orekit.object.Satellite;
import orekit.scenario.Scenario;
import orekit.util.OrekitConfig;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
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
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.conversion.KeplerianPropagatorBuilder;
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
     */
    public static void main(String[] args) throws OrekitException {
        long start = System.nanoTime();

        OrekitConfig.init();

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2005, 01, 01, 23, 30, 00.000, utc);

        double mu = Constants.EGM96_EARTH_MU; // gravitation coefficient
        CelestialBody earth = CelestialBodyFactory.getEarth();
        Frame eme2000 = FramesFactory.getEME2000();
        
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                eme2000);

        //Enter satellites
        double a = 6971000.0;
        double e = 0.0000001;
        double i = FastMath.toRadians(90);
        double argofperigee = 0.;
        double raan = 0.0;
        double anomaly = FastMath.toRadians(90);
        Orbit initialOrbit = new KeplerianOrbit(a, e, i, argofperigee, raan, anomaly, PositionAngle.TRUE, eme2000, startDate, mu);
        System.out.println(initialOrbit);
        
        NadirPointing nadPoint = new NadirPointing(eme2000, earthShape);
        Satellite sat1 = new Satellite("sat1", initialOrbit, nadPoint);
        FieldOfView fov1 = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                FastMath.toRadians(25), Vector3D.PLUS_J, FastMath.toRadians(25), .001);
        Instrument view1 = new Instrument("view1", fov1);
        sat1.addInstrument(view1);
        
        ArrayList<Satellite> satGroup1 = new ArrayList<>();
        satGroup1.add(sat1);

        Constellation constel1 = new Constellation("constel1", satGroup1);
        
        ArrayList<GeodeticPoint> pts = new ArrayList<>();
        pts.add(new GeodeticPoint(FastMath.PI/2, 0, 0));
//        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 30, earthShape);
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", pts, earthShape, startDate, endDate);
        
        covDef1.assignToConstellation(constel1);

        KeplerianPropagatorBuilder kpb = new KeplerianPropagatorBuilder(initialOrbit, PositionAngle.TRUE, 0);

        Scenario scen = new Scenario("test", startDate, endDate, utc, eme2000, kpb);

        scen.addCoverageDefinition(covDef1);

        scen.call();

        long end = System.nanoTime();
        System.out.println("Took " + (end - start) / Math.pow(10, 9) + " sec");
    }

}
