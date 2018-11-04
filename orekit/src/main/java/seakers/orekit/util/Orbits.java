/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.util;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
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
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.OrbitWizard;

/**
 * This class computes the nodal and apsidal precession rates associated with j2
 * perturbation
 *
 * @author nozomihitomi
 */
public class Orbits {

    private static final double J2 = Constants.WGS84_EARTH_C20;

    private static final double RE = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

    private static final double MU = Constants.WGS84_EARTH_MU;

    /**
     * Computes the nodal precession [rad/s] due to J2 effect
     *
     * @param orbit the orbit
     * @return the nodal precession [rad/s]
     */
    public static double nodalPrecession(Orbit orbit) {
        double sma = orbit.getA();
        double ecc = orbit.getE();
        double inc = orbit.getI();
        return nodalPrecession(sma, ecc, inc);
    }
    
    /**
     * Computes the nodal precession [rad/s] due to J2 effect
     *
     * @param sma semi-major axis [m]
     * @param ecc eccentricity
     * @param inc inclination [rad]
     * @return the nodal precession [rad/s]
     */
    public static double nodalPrecession(double sma, double ecc, double inc) {
        return -1.5 * meanMotion(sma) * J2 * (RE * RE) / (sma * sma) 
                * FastMath.cos(inc)
                /((1 - ecc * ecc)*(1 - ecc * ecc));
    }
    
    /**
     * Computes the nodal precession [rad/s] due to J2 effect
     *
     * @param orbit the orbit
     * @return the nodal precession [rad/s]
     */
    public static double apsidalPrecession(Orbit orbit) {
        double sma = orbit.getA();
        double ecc = orbit.getE();
        double inc = orbit.getI();
        return apsidalPrecession(sma, ecc, inc);
    }

    /**
     * Computes the apsidal precession [rad/s] due to J2 effect
     *
     * @param sma semi-major axis [m]
     * @param ecc eccentricity
     * @param inc inclination [rad]
     * @return the apsidal precession [rad/s]
     */
    public static double apsidalPrecession(double sma, double ecc, double inc) {
        return 0.75 * meanMotion(sma) * J2 * (RE * RE) / (sma * sma)
                * (4 - 5 * FastMath.pow(FastMath.sin(inc), 2))
                * FastMath.pow(1 - ecc * ecc, -2);
    }

    /**
     * Computes the mean motion or average angular velocity [rad/s]
     *
     * @param orbit the orbit
     * @return the mean motion or average angular velocity [rad/s]
     */
    public static double meanMotion(Orbit orbit) {
        double sma = orbit.getA();
        return FastMath.sqrt(MU / FastMath.pow(sma, 3));
    }
    
    /**
     * Computes the mean motion or average angular velocity [rad/s]
     *
     * @param sma semi-major axis [m]
     * @return the mean motion or average angular velocity [rad/s]
     */
    public static double meanMotion(double sma) {
        return FastMath.sqrt(MU / (sma * sma *sma));
    }
    
    /**
     * Computes the velocity for a circular orbit with a given semi-major axis
     * @param sma semi-major axis [m]
     * @return the velocity [m/s]
     */
    public static double circularOrbitVelocity(double sma){
        return FastMath.sqrt(MU/sma);
    }
    
    /**
     * Computes the inclination for a circular SSO with a given altitude
     * @param h altitude [m]
     * @return the inclination [rad]
     */
    public static double incSSO(double h){
        double kh=10.10949;
        double cosi=Math.pow(((RE+h)/RE), 3.5)/-kh;
        return FastMath.acos(cosi);
    }

    /**
     * Computes the RAAN for a circular SSO with a given altitude, LTAN (hour and minute) and launch date (day, month and
     * year). We make the assumption that the simulation start date will be the day of the launch at the LTAN time.
     * @param h altitude [m]
     * @param LTAN LTAN in decimal hours
     * @param dayLaunch day of the launch
     * @param monthLaunch month of the launch
     * @param yearLaunch year of the launch
     * @return the inclination [rad]
     */
    public static double LTAN2RAAN(double h, double LTAN, int dayLaunch, int monthLaunch, int yearLaunch) throws OrekitException {
        int hourLTAN = (int) LTAN;
        int minLTAN = (int) (LTAN * 60) % 60;
        int secLTAN = (int) (LTAN * (60*60)) % 60;
        TimeScale utc = TimeScalesFactory.getUTC();
        double mu = Constants.WGS84_EARTH_MU;
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
        double inc = OrbitWizard.SSOinc(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + h, 0);
        AbsoluteDate date = new AbsoluteDate(yearLaunch, monthLaunch, dayLaunch, hourLTAN, minLTAN, secLTAN, utc);
        double angle = 100000;
        double raanopt = 100000;
        GeodeticPoint p = new GeodeticPoint(0, 0, 0);
        CoveragePoint point = new CoveragePoint(earthShape, p, "");
        for (double raan = 0.0; raan < 360.0; raan = raan + 0.01) {
            Orbit SSO = new KeplerianOrbit(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + h, 0.0001, inc, 0.0,
                    FastMath.toRadians(raan), 0.0, PositionAngle.MEAN, inertialFrame, date, mu);
            Vector3D pt1 = SSO.getPVCoordinates().getPosition();
            Vector3D pt2 = point.getPVCoordinates(date, inertialFrame).getPosition();
            double ang = Vector3D.angle(pt1, pt2);
            if (Math.abs(ang) < Math.abs(angle)) {
                angle = ang;
                raanopt = raan;
            }
        }
        return FastMath.toRadians(raanopt);
    }
}
