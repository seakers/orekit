/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.dev;

import orekit.util.OrekitConfig;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

/**
 *
 * @author nozomihitomi
 */
public class testing {

    public static void main(String[] args) throws OrekitException {
        
        OrekitConfig.init();

        Frame inertialFrame = FramesFactory.getEME2000();

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);

        double mu = 3.986004415e+14;

        double a = 24396159;                 // semi major axis in meters
        double e = 0.72831215;               // eccentricity
        double i = Math.toRadians(7);        // inclination
        double omega = Math.toRadians(180);  // perigee argument
        double raan = Math.toRadians(261);   // right ascension of ascending node
        double lM = 0;                       // mean anomaly

        Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                inertialFrame, initialDate, mu);

        KeplerianPropagator kepler = new KeplerianPropagator(initialOrbit);
        kepler.setSlaveMode();

        double duration = 600.;
        AbsoluteDate finalDate = initialDate.shiftedBy(duration);
        double stepT = 60.;
        int cpt = 1;
        for (AbsoluteDate extrapDate = initialDate;
                extrapDate.compareTo(finalDate) <= 0;
                extrapDate = extrapDate.shiftedBy(stepT)) {
            SpacecraftState currentState = kepler.propagate(extrapDate);
            System.out.println("step " + cpt++);
            System.out.println(" time : " + currentState.getDate());
            System.out.println(" " + currentState.getOrbit());
        }

    }

}
