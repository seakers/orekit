/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.dev;

import orekit.kevins_code.KevCoverageGrid;
import orekit.util.OrekitConfig;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.CircularFieldOfViewDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldOfView;
import org.orekit.propagation.events.FieldOfViewDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

/**
 *
 * @author nozomihitomi
 */
public class testing {

    public static void main(String[] args) throws OrekitException {
        long startTime = System.nanoTime();

        OrekitConfig.init();

        Frame inertialFrame = FramesFactory.getEME2000();

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2005, 01, 01, 23, 30, 00.000, utc);

        // Spheric earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., inertialFrame);

        // Create nadir pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(inertialFrame, earthShape);

        double mu = 3.986004415e+14;

        double a = 24396159;                 // semi major axis in meters
        double e = 0.01;                     // eccentricity
        double i = Math.toRadians(7);        // inclination
        double omega = Math.toRadians(180);  // perigee argument
        double raan = Math.toRadians(261);   // right ascension of ascending node
        double lM = 0;                       // mean anomaly

        Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                inertialFrame, startDate, mu);

//        Propagator propagator = new EcksteinHechlerPropagator(initialOrbit, Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
//                Constants.EIGEN5C_EARTH_MU, Constants.EIGEN5C_EARTH_C20, Constants.EIGEN5C_EARTH_C30,
//                Constants.EIGEN5C_EARTH_C40, Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);
        
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, mu);
        propagator.setSlaveMode();

        SpacecraftState initState = propagator.getInitialState();
        PVCoordinates initcoor = initState.getPVCoordinates();
        Vector3D nadir = initcoor.getPosition().negate();

        double granularity = FastMath.toRadians(10);

        double maxcheck = 1;
        double threshold = 0.001;
        double elevation = 1;

        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                inertialFrame);

        System.out.println("Setting up stations");
        int latcount = (int) (90 / granularity + .001);
        for (double lat = -latcount * granularity; lat <= 90.001; lat = lat + granularity) {		//Creates grid of stations
            int satsAtLat = (int) (360 / granularity * FastMath.cos(FastMath.toRadians(lat)) + .001);
            for (double lon = 0; lon <= 359.999; lon = lon + 360.0 / satsAtLat) {
                double longitude = FastMath.toRadians(lon);
                double latitude = FastMath.toRadians(lat);
                double altitude = 0.;
                GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
                TopocentricFrame staFrame = new TopocentricFrame(earth, station, lat + "_" + lon);
//                EventDetector staVisi
//                        = new ElevationDetector(maxcheck, threshold, staFrame).
//                        withConstantElevation(elevation).
//                        withHandler(new VisibilityHandler());
//                propagator.addEventDetector(staVisi);
                EventDetector fovev
                        = new CircularFieldOfViewDetector(maxcheck, staFrame, initState.getPVCoordinates().getPosition().negate(),FastMath.toRadians(10)).
                        withHandler(new FOVHandler());
                propagator.addEventDetector(fovev);
            }
        }
        System.out.println("Propogating...");
        double duration = 7 * 24 * 3600;
        SpacecraftState currentState = propagator.propagate(startDate.shiftedBy(duration));
        long endTime = System.nanoTime();
        System.out.println("Took " + (endTime - startTime)/Math.pow(10, 9) + " sec");

    }

    private static class VisibilityHandler implements EventHandler<ElevationDetector> {

        public EventHandler.Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
                final boolean increasing) {
            int asterixIndex = detector.getTopocentricFrame().getName().indexOf('*');
			//			System.out.println(detector.getTopocentricFrame().getName());
            //			System.out.println(asterixIndex);
            int n = Integer.parseInt(detector.getTopocentricFrame().getName().substring(asterixIndex + 1));
            if (increasing) {									//Access between station and satellite begins
                System.out.println("In" + n + "DATE" + s.getDate());
            } else {											//Access ends
                System.out.println("Out" + n + "DATE" + s.getDate());
            }

            return EventHandler.Action.CONTINUE;
        }

    }
    
    private static class FOVHandler implements EventHandler<CircularFieldOfViewDetector> {

        public EventHandler.Action eventOccurred(final SpacecraftState s, final CircularFieldOfViewDetector detector,
                final boolean increasing) {
            int asterixIndex = ((TopocentricFrame)detector.getPVTarget()).getName().indexOf('*');
			//			System.out.println(detector.getTopocentricFrame().getName());
            //			System.out.println(asterixIndex);
            int n = Integer.parseInt(((TopocentricFrame)detector.getPVTarget()).getName().substring(asterixIndex + 1));
            if (increasing) {									//Access between station and satellite begins
                System.out.println("In" + n + "DATE" + s.getDate());
            } else {											//Access ends
                System.out.println("Out" + n + "DATE" + s.getDate());
            }

            return EventHandler.Action.CONTINUE;
        }

    }

}
