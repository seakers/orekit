/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import orekit.analysis.Analysis;
import orekit.analysis.CompoundAnalysis;
import orekit.analysis.ephemeris.OrbitalElementsAnalysis;
import orekit.analysis.vectors.VectorAnalysis;
import orekit.constellations.Walker;
import orekit.coverage.access.TimeIntervalArray;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.Satellite;
import orekit.object.fieldofview.NadirSimpleConicalFOV;
import orekit.object.linkbudget.LinkBudget;
import orekit.propagation.PropagatorFactory;
import orekit.propagation.PropagatorType;
import orekit.scenario.Scenario3;
import orekit.scenario.Scenario4;
import orekit.scenario.ScenarioIO;
import orekit.util.OrekitConfig;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 *
 * @author paugarciabuzzi
 */
public class Orekit_lifetime {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));
        long start = System.nanoTime();

        String filename;
        String path;
        if (args.length > 0) {
            path = args[0];
            filename = args[1];
        } else {
            path="/Users/paugarciabuzzi/Desktop/Outputs_Orekit/Lifetime";
//            path = "/Users/nozomihitomi/Desktop";
//            path = "C:\\Users\\SEAK1\\Nozomi\\OREKIT\\";
            filename = "tropics_lifetime_test";
        }

        OrekitConfig.init();

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2010, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2011, 1, 1, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        
        Frame inertialFrame = FramesFactory.getEME2000();

        //Enter satellite orbital parameters
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+500000;
        double i = FastMath.toRadians(30);

        //Walker walker = new Walker("walker1", i, 1, 1, 0, a, inertialFrame, startDate, mu);
        //Satellite sat = walker.getSatellites().iterator().next();
        Orbit orb = new KeplerianOrbit(a, 0.0001, i, 0.0, 0.0, Math.PI, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat = new Satellite("sat_1", orb);

//        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN, OrbitType.KEPLERIAN);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.NUMERICAL, OrbitType.KEPLERIAN);
        
        double analysisTimeStep = 60;
        ArrayList<Analysis> analysesList = new ArrayList<>();
        analysesList.add(new OrbitalElementsAnalysis(analysisTimeStep));

        analysesList.add(new VectorAnalysis(inertialFrame, 60) {
            private static final long serialVersionUID = 4680062066885650976L;
            @Override
            public Vector3D getVector(SpacecraftState currentState, Frame frame) throws OrekitException {
                return currentState.getPVCoordinates(frame).getPosition();
            }
        });

        CompoundAnalysis analyses = new CompoundAnalysis(analysesList);


        Scenario4 scen = new Scenario4.Builder(startDate, endDate, utc,sat).
                analysis(analyses).name("test1").numThreads(1).
                propagatorFactory(pf).build();
        scen.call();
        Scenario4 scenComp = scen;
        ScenarioIO.saveAnalyses(Paths.get(path, ""), scenComp);

        long end = System.nanoTime();
        System.out.println("Took " + (end - start) / Math.pow(10, 9) + " sec");
    }

}