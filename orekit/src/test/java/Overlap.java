import java.io.File;
import java.util.Locale;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.J2DifferentialEffect;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** Orekit tutorial for slave mode propagation.
 * <p>This tutorial shows a basic usage of the slave mode in which the user drives all propagation steps.<p>
 * @author Pascal Parraud
 */
public class Overlap {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            File home       = new File(System.getProperty("user.home"));
            File orekitData = new File(home, "orekit-data");
            if (!orekitData.exists()) {
                System.err.format(Locale.US, "Failed to find %s folder%n",
                                  orekitData.getAbsolutePath());
                System.err.format(Locale.US, "You need to download %s from %s, unzip it in %s and rename it 'orekit-data' for this tutorial to work%n",
                                  "orekit-data-master.zip", "https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip",
                                  home.getAbsolutePath());
                System.exit(1);
            }
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.addProvider(new DirectoryCrawler(orekitData));

            // Initial orbit parameters
            double a = 7178000; // semi major axis in meters
            double e = 0.001; // eccentricity
            double i = FastMath.toRadians(78); // inclination
            double omega = FastMath.toRadians(180); // perigee argument
            double raan = FastMath.toRadians(261); // right ascension of ascending node
            double lM = 0; // mean anomaly
            
            double a2 = 6778000; // semi major axis in meters
            double e2 = 0; // eccentricity
            double i2 = FastMath.toRadians(35); // inclination
            double omega2 = FastMath.toRadians(180); // perigee argument
            double raan2 = FastMath.toRadians(261); // right ascension of ascending node
            double lM2 = 0; // mean anomaly

            // Inertial frame
            Frame inertialFrame = FramesFactory.getEME2000();

            // Initial date in UTC time scale
            TimeScale utc = TimeScalesFactory.getUTC();
            AbsoluteDate initialDate = new AbsoluteDate(2020, 01, 01, 23, 30, 00.000, utc);

            // gravitation coefficient
            double mu =  3.986004415e+14;

            // Orbit construction as Keplerian
            Orbit initial_SWOT_orbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                                                    inertialFrame, initialDate, mu);
            Orbit initial_SWOTlet_orbit = new KeplerianOrbit(a2, e2, i2, omega2, raan2, lM2, PositionAngle.MEAN,
                    inertialFrame, initialDate, mu);
            Orbit perturbed_SWOT_orbit = new KeplerianOrbit(initial_SWOT_orbit);
            UnnormalizedSphericalHarmonicsProvider prov = GravityFieldFactory.getConstantUnnormalizedProvider(10,10);
            // J2DifferentialEffect perturb = new J2DifferentialEffect(initial_SWOT_orbit,perturbed_SWOT_orbit, true, 6378000, mu, jtwo);
            // perturbed_SWOT_orbit = perturb.apply(initial_SWOT_orbit);
            // Simple extrapolation with Keplerian motion
            // KeplerianPropagator SWOT_kepler = new KeplerianPropagator(initial_SWOT_orbit);
            // KeplerianPropagator SWOTlet_kepler = new KeplerianPropagator(perturbed_SWOT_orbit);
            EcksteinHechlerPropagator SWOT_perturb = new EcksteinHechlerPropagator(initial_SWOT_orbit,prov);
            EcksteinHechlerPropagator SWOTlet_perturb = new EcksteinHechlerPropagator(initial_SWOTlet_orbit,prov);


            // Overall duration in seconds for extrapolation
            double duration = 600.;

            // Stop date
            final AbsoluteDate finalDate = initialDate.shiftedBy(duration);

            // Step duration in seconds
            double stepT = 60.;

            // Extrapolation loop
            int cpt = 1;
            for (AbsoluteDate extrapDate = initialDate;
                 extrapDate.compareTo(finalDate) <= 0;
                 extrapDate = extrapDate.shiftedBy(stepT))  {

                SpacecraftState current_SWOT_state = SWOT_perturb.propagate(extrapDate);
                SpacecraftState current_SWOTlet_state = SWOTlet_perturb.propagate(extrapDate);
                System.out.println("step " + cpt++);
                System.out.println(" time : " + current_SWOT_state.getDate());
                System.out.println(" " + current_SWOT_state.getOrbit() + current_SWOTlet_state.getOrbit());

            }

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
