/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Satellite;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.propagation.*;
import seakers.orekit.object.*;
import seakers.orekit.coverage.access.*;
import seakers.orekit.event.detector.*;
import seakers.orekit.object.fieldofview.FieldOfViewDefinition;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;
import seakers.orekit.util.OrekitConfig;

import static java.lang.Double.parseDouble;
import static seakers.orekit.object.CoverageDefinition.GridStyle.UNIFORM;

/**
 *
 * @author ben_gorr
 */
public class Overlap_sequel {

    /**
     * @param args the command line arguments
     * @throws OrekitException
     */
    public static void main(String[] args) {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("D:/Documents/VASSAR/orekit/orekit/src/main/java/seakers/orekit/repeat_orbits_nonSSO_varinc.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
        double[] results = new double[records.size()];

//        for(int i = 0; i<records.size(); i++) {
//            double alt = parseDouble(records.get(i).get(1));
//            double inc = parseDouble(records.get(i).get(4));
//            double durationDays = 21;
//            double result = coverageGivenAltInc(alt,inc,durationDays)*100;
//            System.out.println("Coverage in % for "+alt+" km altitude, "+inc+" deg inclination: "+result+"%");
//            results[i] = result;
//        }
        double alt = 890.6;
        double inc = 77.6;
        double durationDays = 21;
        double result = coverageGivenAltInc(alt,inc,durationDays);
        System.out.println(result);
//        try{
//            FileWriter csvWriter = new FileWriter("varinc_results_10_1.csv");
//            for (int i = 0; i<results.length; i++) {
//                String rowData = Double.toString(results[i]);
//                csvWriter.append(String.join(",", rowData));
//                csvWriter.append("\n");
//            }
//
//            csvWriter.flush();
//            csvWriter.close();
//        } catch (Exception e) {
//            System.out.println("Error writing to CSV");
//        }



    }

    public static double coverageGivenAltInc(double alt, double inc, double durationDays) {
        long start = System.nanoTime();
        OrekitConfig.init(4);
        File orekitData = new File("D:/Documents/VASSAR/orekit/resources");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);
        TimeScale utc = TimeScalesFactory.getUTC();
        //AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 10, 30, 00.000, utc);
        //AbsoluteDate endDate = new AbsoluteDate(2020, 1, 8, 00, 00, 00.000, utc);
        //AbsoluteDate endDate = new AbsoluteDate(2020, 1, 8, 10, 30, 00.000, utc);
        AbsoluteDate endDate = startDate.shiftedBy(durationDays*86400);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient
        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        //define instruments
        NadirSimpleConicalFOV SWOT_fov = new NadirSimpleConicalFOV(FastMath.toRadians(3.86), earthShape);
        NadirSimpleConicalFOV SWOTlet_fov = new NadirSimpleConicalFOV(FastMath.toRadians(20),earthShape);
        //NadirRectangularFOV SWOT_fov = new NadirRectangularFOV(FastMath.toRadians(10), FastMath.toRadians(5), 0, earthShape);
        Instrument SWOT_payload = new Instrument("view1", SWOT_fov, 100, 100);
        //NadirRectangularFOV SWOTlet_fov = new NadirRectangularFOV(FastMath.toRadians(5), FastMath.toRadians(5), 0, earthShape);
        Instrument SWOTlet_VNIR = new Instrument("view1", SWOTlet_fov, 100, 100);

        Properties propertiesPropagator = new Properties();
//        propertiesPropagator.setProperty("orekit.propagator.mass", "6");
//        propertiesPropagator.setProperty("orekit.propagator.atmdrag", "true");
//        propertiesPropagator.setProperty("orekit.propagator.dragarea", "0.075");
//        propertiesPropagator.setProperty("orekit.propagator.dragcoeff", "2.2");
//        propertiesPropagator.setProperty("orekit.propagator.thirdbody.sun", "true");
//        propertiesPropagator.setProperty("orekit.propagator.thirdbody.moon", "true");
//        propertiesPropagator.setProperty("orekit.propagator.solarpressure", "true");
//        propertiesPropagator.setProperty("orekit.propagator.solararea", "0.058");

        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN,propertiesPropagator);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);
        //PropagatorFactory pf =  new PropagatorFactory(PropagatorType.NUMERICAL,propertiesPropagator);

        int SWOT_height=890600;
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+SWOT_height;
        double a_SWOTlet = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+alt*1000;
        double ideg=77.6;
        double SWOT_i = FastMath.toRadians(ideg);
        double SWOTlet_i = FastMath.toRadians(inc);
        //Enter satellite orbital parameters
        ArrayList<Satellite> SWOTlets=new ArrayList<>();
        ArrayList<Satellite> SWOT=new ArrayList<>();
        double SWOT_mass = 600;
        double SWOTlet_mass = 12;

        Collection<Instrument> SWOT_instruments = new ArrayList<>();
        SWOT_instruments.add(SWOT_payload);
        Collection<Instrument> SWOTlet_instruments = new ArrayList<>();
        SWOTlet_instruments.add(SWOTlet_VNIR);
        Orbit orb1 = new KeplerianOrbit(a, 0.0001, SWOT_i, 0.0, FastMath.toRadians(257.8), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat1 = new Satellite("SWOT", orb1, SWOT_instruments);
        Orbit orb2 = new KeplerianOrbit(a_SWOTlet, 0.0001, SWOTlet_i, 0.0, FastMath.toRadians(257.8), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat2 = new Satellite("SWOTlet", orb2, SWOTlet_instruments);
        Propagator prop1 = pf.createPropagator(orb1, SWOT_mass);
        Propagator prop2 = pf.createPropagator(orb2, SWOTlet_mass);


        SWOT.add(sat1);
        SWOTlets.add(sat2);
        //CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 9, earthShape, EQUAL_AREA);
        Constellation SWOT_constel = new Constellation ("Just SWOT",SWOT);
        Constellation SWOTlet_constel = new Constellation ("SWOTlet",SWOTlets);
        CoverageDefinition covDef1 = new CoverageDefinition("Whole Earth", 20, earthShape, UNIFORM);
        covDef1.assignConstellation(SWOT_constel);
        covDef1.assignConstellation(SWOTlet_constel);
        SpacecraftState initialState1 = prop1.getInitialState();
        SpacecraftState initialState2 = prop2.getInitialState();
        HashMap<TopocentricFrame, TimeIntervalArray> satAccesses;
        satAccesses = new HashMap<>(covDef1.getNumberOfPoints());
        double coverage = 0;
        double totalCoverage = 0;
        double totalCoverage2 = 0;
        for (CoveragePoint pt : covDef1.getPoints()) {
            TimeIntervalArray emptyTimeArray = new TimeIntervalArray(startDate, endDate);
            satAccesses.put(pt, emptyTimeArray);
        }
        for (CoveragePoint pt : covDef1.getPoints()) {
            //need to reset initial state of the propagators or will propagate from the last stop time
            if (!lineOfSightPotential(pt, initialState1.getOrbit(), FastMath.toRadians(5.0))) {
                //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
                continue;
            }
            prop1.resetInitialState(initialState1);
            prop1.clearEventsDetectors();
            //Next search through intervals with line of sight to compute when point is in field of view
            double fovStepSize = orb1.getKeplerianPeriod() / 100.;
            double threshold = 1e-3;
            FOVDetector fovDetec = new FOVDetector(pt, SWOT_payload).withMaxCheck(fovStepSize).withThreshold(threshold);
            TimeIntervalHandler<FOVDetector> fovHandler = new TimeIntervalHandler<>(startDate, endDate, fovDetec.g(initialState1), Action.CONTINUE);
            fovDetec = fovDetec.withHandler(fovHandler);
            prop1.addEventDetector(fovDetec);
            prop1.propagate(startDate, endDate);
            FOVDetector fovDetec2 = new FOVDetector(pt, SWOTlet_VNIR).withMaxCheck(fovStepSize).withThreshold(threshold);
            TimeIntervalHandler<FOVDetector> fovHandler2 = new TimeIntervalHandler<>(startDate, endDate, fovDetec2.g(initialState2), Action.CONTINUE);
            fovDetec2 = fovDetec2.withHandler(fovHandler2);
            prop2.addEventDetector(fovDetec2);
            prop2.propagate(startDate, endDate);
            TimeIntervalArray fovTimeArray = fovHandler.getTimeArray().createImmutable();
            if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                continue;
            }
            TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
            double[] riseandsets = merger.orCombine().getRiseAndSetTimesList();
            TimeIntervalArray fovTimeArray2 = fovHandler2.getTimeArray().createImmutable();
            TimeIntervalMerger merger2 = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray2);
            double[] riseandsets2 = merger2.orCombine().getRiseAndSetTimesList();
            double coverageTime = 0;
            double decorr = 0;
            double totalCoverageTime = 0;
            double totalCoverageTime2 = 0;
            for (int j=0;j<riseandsets.length;j=j+2) {
                for (int k=0;k<riseandsets2.length;k=k+2) {
                    if(riseandsets[j] < riseandsets2[k] && riseandsets[j+1]+decorr > riseandsets2[k]) {
                        if(riseandsets[j] < riseandsets2[k+1] && riseandsets[j+1]+decorr > riseandsets2[k+1]) {
                            coverageTime = coverageTime + riseandsets2[k+1] - riseandsets2[k];
                        } else {
                            coverageTime = coverageTime + riseandsets[j+1]+decorr - riseandsets2[k];
                        }
                    } else if(riseandsets[j] < riseandsets2[k+1] && riseandsets[j+1]+decorr > riseandsets2[k+1]) {
                        coverageTime = coverageTime + riseandsets2[k+1] - riseandsets[j];
                    } else if(riseandsets[j]>=riseandsets2[k] && riseandsets[j+1]+decorr<=riseandsets2[k+1]) {
                        coverageTime = coverageTime + riseandsets[j+1]+decorr - riseandsets[j];
                    } else {
                        coverageTime = coverageTime;
                    }
                    totalCoverageTime2 = totalCoverageTime2 + riseandsets2[k+1]+decorr-riseandsets2[k];
                }
                totalCoverageTime = totalCoverageTime + riseandsets[j+1]+decorr-riseandsets[j];
            }
            prop1.clearEventsDetectors();
            prop2.clearEventsDetectors();
            double percent = coverageTime/totalCoverageTime*100;
            if (percent>0) {
                //System.out.println("Overlapping coverage time for Lat "+pt.getPoint().getLatitude()*180/3.14+", Long "+pt.getPoint().getLongitude()*180/3.14+" : "+coverageTime);
            }
            coverage = coverage + coverageTime;
            totalCoverage = totalCoverage + totalCoverageTime;
            totalCoverage2 = totalCoverage2 + totalCoverageTime2;
        }
        long end = System.nanoTime();
        OrekitConfig.end();
        System.out.printf("Took %.4f sec\n", (end - start) / Math.pow(10, 9));
        //System.out.println(coverage/3600);
        System.out.println(coverage/totalCoverage);
        System.out.println(coverage/totalCoverage2);
        return coverage/totalCoverage;
    }
    private static boolean lineOfSightPotential(CoveragePoint pt, Orbit orbit, double latitudeMargin) throws OrekitException {
        //this computation assumes that the orbit frame is in ECE
        double distance2Pt = pt.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition().getNorm();
        double distance2Sat = orbit.getPVCoordinates().getPosition().getNorm();
        double subtendedAngle = FastMath.acos(distance2Pt / distance2Sat);

        return FastMath.abs(pt.getPoint().getLatitude()) - latitudeMargin < subtendedAngle + orbit.getI();
    }
}