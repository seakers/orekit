/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.object;

import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

/**
 *
 * @author nozomihitomi
 */
public class OrbitWizard {

    /**
     * computes the inclination of a sun synchronous orbit (Earth centric)
     * given, the semi-major axis and eccentricity. Uses GRIM5C1 mdoel.
     *
     * @param semimajorAxis semi-major axis of orbit
     * @param ecc eccentricity of orbit
     * @return
     */
    public static double SSOinc(double semimajorAxis, double ecc) {
        double omegaDot = 2 * FastMath.PI / (Constants.JULIAN_YEAR); //rad/s
        double mu = Constants.GRIM5C1_EARTH_MU; // gravitation coefficient
        double r = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
//        double J2 = (2. / 3.) * Constants.WGS84_EARTH_FLATTENING
//                - FastMath.pow(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 3)
//                * FastMath.pow(Constants.WGS84_EARTH_ANGULAR_VELOCITY, 2)
//                / (3 * Constants.WGS84_EARTH_MU);w
        double lhs = -(3. / 2.) * FastMath.pow(1 - FastMath.pow(ecc, 2), -2)
                * FastMath.pow(mu / FastMath.pow(semimajorAxis, 3), 0.5)
                * (-Constants.GRIM5C1_EARTH_C20) * FastMath.pow(r / semimajorAxis, 2);
        return FastMath.acos(omegaDot / lhs);
    }
}
