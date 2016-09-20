/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.doe;

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

        double mu = Constants.EGM96_EARTH_MU; // gravitation coefficient
        CelestialBody earth = CelestialBodyFactory.getEarth();
        Frame eme2000 = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                earth.getBodyOrientedFrame());

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
                    anomaly, PositionAngle.TRUE, earth.getInertiallyOrientedFrame(), 
                    startDate, mu));
               ind++;
           }
       }

        NadirPointing nadPoint = new NadirPointing(earth.getInertiallyOrientedFrame(), earthShape);
        ArrayList<Satellite> satellites = new ArrayList<>(numPlanes*numSatsPerPlane);
        for (int satnum=0; satnum<numPlanes*numSatsPerPlane; satnum++){
            String name= "sat" + Integer.toString(satnum);
            Satellite sat = new Satellite(name, orbits.get(satnum), nadPoint);
            FieldOfView fov = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I,
                                                FastMath.toRadians(25), Vector3D.PLUS_J, 
                                                FastMath.toRadians(25), .001);
            Instrument view = new Instrument("view1", fov);
            sat.addInstrument(view);
            satellites.add(satnum, sat);
        }

        ArrayList<Satellite> satGroup = new ArrayList<>();
        for (int satnum=0; satnum<numPlanes*numSatsPerPlane; satnum++){
            satGroup.add(satellites.get(satnum));
        }


        Constellation constel = new Constellation("constel", satGroup);

        CoverageDefinition covDef = new CoverageDefinition("covdef", 30, earthShape, startDate, endDate);

        covDef.assignConstellation(constel);

        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN, orbits.get(0));

        ScenarioStepWise scen = new ScenarioStepWise("test", startDate, endDate, utc, earth.getInertiallyOrientedFrame(), pf);

        scen.addCoverageDefinition(covDef);

        scen.call();

        System.out.println(String.format("Done Running Scenario %s", scen));
        
        HashMap<CoveragePoint, TimeIntervalArray> covDefAccess = scen.getMergedAccesses(covDef);
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
