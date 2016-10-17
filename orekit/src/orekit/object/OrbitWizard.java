/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object;

import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

/**
 *
 * @author nozomihitomi
 */
public class OrbitWizard {

    /**
     * computes the inclination of a sun synchronous orbit (Earth centric)
     * given, the semi-major axis and eccentricity. Uses WGS84 model. Computes
     * J2 = (2/3)f-R^3*w^2/(3mu), where f is the Earth's flattening, R is the
     * Earth's equitorial radius, w is the Earth's angular velocity, and mu is
     * the Universal gravitational constant multiplied by the Earth's mass.
     *
     * Equations used from http://topex.ucsd.edu/geodynamics/14gravity1_2.pdf;
     * Reference Earth Model - WGS84 (Copyright 2002, David T. Sandwell)
     *
     * @param semimajorAxis semi-major axis of orbit
     * @param ecc eccentricity of orbit
     * @return
     */
    public static double SSOinc(double semimajorAxis, double ecc) {
        double omegaDot = 2 * FastMath.PI / (365.25 * 24 * 3600); //rad/s
        double mu = Constants.EGM96_EARTH_MU; // gravitation coefficient
        double r = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double J2 = (2. / 3.) * Constants.WGS84_EARTH_FLATTENING
                - FastMath.pow(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 3)
                * FastMath.pow(Constants.WGS84_EARTH_ANGULAR_VELOCITY, 2)
                / (3 * Constants.WGS84_EARTH_MU);
        double lhs = -(3. / 2.) * FastMath.pow(1 - FastMath.pow(ecc, 2), -2)
                * FastMath.pow(mu / FastMath.pow(semimajorAxis, 3), 0.5)
                * J2 * FastMath.pow(r / semimajorAxis, 2);
        return FastMath.acos(omegaDot / lhs);
    }
}
