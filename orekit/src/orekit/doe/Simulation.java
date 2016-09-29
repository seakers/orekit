/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.doe;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import orekit.STKGRID;
import orekit.access.RiseSetTime;
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
import org.orekit.utils.IERSConventions;
import orekit.object.fieldofview.*;
/**
 *
 * @author paugarciabuzzi
 */
public class Simulation {
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;
    private final TimeScale utc;
    private final int numPlanes;
    private final int numSatsPerPlane;
    private final double inclination;
    private final double majorAxis;
    private final String path;
    private final String filename;
    
    public Simulation(AbsoluteDate startDate, AbsoluteDate endDate,TimeScale utc,
            int numPlanes, int numSatsPerPlane, double inclination, 
            double majorAxis){
        this.startDate=startDate;
        this.endDate=endDate;
        this.utc=utc;
        this.numPlanes=numPlanes;
        this.numSatsPerPlane=numSatsPerPlane;
        this.inclination=inclination;
        this.majorAxis=majorAxis;
        this.path="/Users/paugarciabuzzi/Desktop";
        this.filename="simulation1";
    }
    
    public void simulate() throws OrekitException{
        
        long start = System.nanoTime();
        
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        
        double mu = Constants.EGM96_EARTH_MU; // gravitation coefficient

        //Enter satellites
        double a = majorAxis;
        double e = 0.0000001;
        double i = FastMath.toRadians(inclination);
        double argofperigee = 0.;
        double raan0 = 0.0;
        double anomaly0 = 0.0;
        double raan;
        double anomaly;
        int ind=0;
        
        
        ArrayList<Orbit> orbits = new ArrayList<>(numPlanes*numSatsPerPlane);
        
       for (int ind1=0; ind1<numPlanes; ind1++){
           raan = raan0 + ind1*FastMath.toRadians(360/numPlanes);
           for (int ind2=0; ind2<numSatsPerPlane; ind2++){
               anomaly = anomaly0 + ind2*FastMath.toRadians(360/numSatsPerPlane);
               orbits.add(ind, new KeplerianOrbit(a, e, i, argofperigee, raan, 
                    anomaly, PositionAngle.TRUE, inertialFrame, 
                    startDate, mu));
               ind++;
           }
       }

        NadirPointing nadPoint = new NadirPointing(inertialFrame, earthShape);
        ArrayList<Satellite> satellites = new ArrayList<>(numPlanes*numSatsPerPlane);
        for (int satnum=0; satnum<numPlanes*numSatsPerPlane; satnum++){
            String name= "sat" + Integer.toString(satnum);
            Satellite sat = new Satellite(name, orbits.get(satnum), nadPoint);
            RectangularFieldOfView fov_rect = new RectangularFieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                FastMath.toRadians(45), Vector3D.PLUS_J, FastMath.toRadians(80), .001);
            SimpleConicalFieldOfView fov_cone = new SimpleConicalFieldOfView(Vector3D.PLUS_K,
                FastMath.toRadians(45));
            String nameview= "view" + Integer.toString(satnum);
            Instrument view = new Instrument(nameview, fov_cone);
            sat.addInstrument(view);
            satellites.add(satnum, sat);
        }
        
//        ArrayList<Satellite> satGroup = new ArrayList<>();
//        for (int satnum=0; satnum<numPlanes*numSatsPerPlane; satnum++){
//            satGroup.add(satellites.get(satnum));
//        }


        Constellation constel = new Constellation("constel", satellites);
        
//        CoverageDefinition covDef = new CoverageDefinition("covdef", STKGRID.getPoints(), earthShape, startDate, endDate);
        CoverageDefinition covDef = new CoverageDefinition("covdef", 30, earthShape, startDate, endDate);

        covDef.assignConstellation(constel);

        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN, orbits.get(0));

        Scenario scen = new Scenario("test", startDate, endDate, utc, inertialFrame, pf, false);
        
        scen.addCoverageDefinition(covDef);

        scen.call();

        System.out.println(String.format("Done Running Scenario %s", scen));
        
        HashMap<CoveragePoint, TimeIntervalArray> covDefAccess = scen.getMergedAccesses(covDef);

        for (CoveragePoint pt : covDefAccess.keySet()) {
            TimeIntervalArray array = covDefAccess.get(pt);
            for (RiseSetTime time : array.getRiseSetTimes()) {
                if (time.isRise()) {
                    System.out.print("" + time.getTime());
                } else {
                    System.out.println("," + time.getTime());
                }
            }
        }

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
        
//        System.out.println("Saving scenario...");
//
//        ScenarioIO.save(Paths.get(path, ""), filename, scen);
//        ScenarioIO.saveReadMe(Paths.get(path, ""), filename, scen);

        long end = System.nanoTime();
        System.out.println("Took " + (end - start) / Math.pow(10, 9) + " sec");
    }
    
    
}
