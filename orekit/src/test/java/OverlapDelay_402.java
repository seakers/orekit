/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;
import seakers.orekit.event.detector.FOVDetector;
import seakers.orekit.event.detector.TimeIntervalHandler;
import seakers.orekit.object.*;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.util.OrekitConfig;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static seakers.orekit.object.CoverageDefinition.GridStyle.UNIFORM;

/**
 *
 * @author ben_gorr
 */
public class OverlapDelay_402 {

    /**
     * @param args the command line arguments
     * @throws OrekitException
     */
    public static void main(String[] args) {
        double durationDays = 1;
        double[] inclinations = {97.5896};
        double[] altitudes = {556};

        try{
            FileWriter csvWriter = new FileWriter("./orekit/src/test/output/delay_results.csv");
            for (int i = 0; i<1; i++) {
                ArrayList<Double> result = coverageGivenAltInc(altitudes[i],inclinations[i],durationDays);

                String rowData = getListAsCsvString(result);
                csvWriter.append(rowData);
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (Exception e) {
            System.out.println(e.toString());
            System.out.println("Error writing to CSV");
        }
    }
    public static String getListAsCsvString(ArrayList<Double> list){

        StringBuilder sb = new StringBuilder();
        for(Double str:list){
            if(sb.length() != 0){
                sb.append(",");
            }
            sb.append(Double.toString(str));
        }
        return sb.toString();
    }
    public static ArrayList<Double> coverageGivenAltInc(double alt, double inc, double durationDays) {
        long start = System.nanoTime();
        OrekitConfig.init(4);
        File orekitData = new File("./orekit/src/main/resources");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);
        TimeScale utc = TimeScalesFactory.getUTC();
        //AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate startDate = new AbsoluteDate(2021, 12, 3, 07, 00, 00.000, utc);
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
        NadirSimpleConicalFOV subSatFOV = new NadirSimpleConicalFOV(FastMath.toRadians(20), earthShape); // 0.06 deg
        NadirSimpleConicalFOV mainSatFOV = new NadirSimpleConicalFOV(FastMath.toRadians(20),earthShape); // 0.38 deg

        Instrument subSatPayload = new Instrument("view1", subSatFOV, 100, 100);
        Instrument mainSatPayload = new Instrument("view2", mainSatFOV, 100, 100);

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

        double subSatHeight=alt*1000;
        double a_subSat = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+subSatHeight;
        double subSat_i = FastMath.toRadians(inc);
        //Enter satellite orbital parameters
        ArrayList<Satellite> subSats=new ArrayList<>();
        ArrayList<Satellite> mainSats=new ArrayList<>();
        double subSatMass = 100;
        double mainSatMass = 100;

        Collection<Instrument> subSatInstruments = new ArrayList<>();
        subSatInstruments.add(subSatPayload);

        Orbit swotOrbit = new KeplerianOrbit(6378000+891000, 0.0001, FastMath.toRadians(77.7), 0.0, 0.0, 0.0, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Collection<Instrument> swotPayload = new ArrayList<>();
        double swotCrossFOVRadians = Math.atan(60.0/891.0);
        double swotAlongFOVRadians = Math.atan(400.0/891.0);
        NadirRectangularFOV swotFOV = new NadirRectangularFOV(swotCrossFOVRadians,swotAlongFOVRadians,0.0,earthShape);
        Instrument swotAltimeter = new Instrument("SWOT Altimeter", swotFOV, 100.0, 100.0);
        swotPayload.add(swotAltimeter);
        Satellite SWOT = new Satellite("SWOT", swotOrbit, swotPayload);
        Orbit sample_orb = new KeplerianOrbit(a_subSat, 0.0001, subSat_i, 0.0, FastMath.toRadians(250), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Propagator prop2 = pf.createPropagator(swotOrbit, mainSatMass);

        int r = 1;
        int s = 11;
        for(int m = 0; m < r; m++) {
            for(int n = 0; n < s; n++) {
                int pu = 360 / (r*s);
                int delAnom = pu * r; //in plane spacing between satellites
                int delRAAN = pu * s; //node spacing
                int RAAN = delRAAN * m;
                int f = 1;
                int phasing = pu * f;
                int anom = (n * delAnom + phasing * m);
                Orbit orb1 = new KeplerianOrbit(a_subSat, 0.0001, subSat_i, 0.0, FastMath.toRadians(250+RAAN), Math.toRadians(anom), PositionAngle.MEAN, inertialFrame, startDate, mu);
                Satellite sat1 = new Satellite("sat"+r+s, orb1, subSatInstruments);
                subSats.add(sat1);
            }
        }

        Propagator prop1 = pf.createPropagator(sample_orb, subSatMass);
        mainSats.add(SWOT);

        Constellation subSatConstellation = new Constellation ("Sub Sat",subSats);
        Constellation mainSatConstellation = new Constellation ("Main Sat",mainSats);

        CoverageDefinition covDef1 = new CoverageDefinition("Whole Earth", 10,  earthShape, UNIFORM);
        System.out.println("Number of points: "+covDef1.getNumberOfPoints());
        covDef1.assignConstellation(subSatConstellation);
        covDef1.assignConstellation(mainSatConstellation);

        SpacecraftState initialState1 = prop1.getInitialState();
        SpacecraftState initialState2 = prop2.getInitialState();

        HashMap<TopocentricFrame, TimeIntervalArray> satAccesses;
        satAccesses = new HashMap<>(covDef1.getNumberOfPoints());
        for (CoveragePoint pt : covDef1.getPoints()) {
            TimeIntervalArray emptyTimeArray = new TimeIntervalArray(startDate, endDate);
            satAccesses.put(pt, emptyTimeArray);
        }
        ArrayList<Double> coverageByDelay = new ArrayList<>();
        for (int i = 0; i < 24*4; i++) {
            coverageByDelay.add(0.0);
        }
        ArrayList<Double> totalCoverageByDelay = new ArrayList<>();
        for (int i = 0; i < 24*4; i++) {
            totalCoverageByDelay.add(0.0);
        }
        int count = 0;
        for (CoveragePoint pt : covDef1.getPoints()) {
            count++;
            if(count%100==0) {
                System.out.println(count);
            }
            long pointStart = System.nanoTime();
            //need to reset initial state of the propagators or will propagate from the last stop time
            if (!lineOfSightPotential(pt, initialState1.getOrbit(), FastMath.toRadians(5.0))) {
                //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
                continue;
            }
            prop1.resetInitialState(initialState1);
            prop1.clearEventsDetectors();
            //Next search through intervals with line of sight to compute when point is in field of view
            double fovStepSize = sample_orb.getKeplerianPeriod() / 100.;
            double threshold = 1e-3;
            FOVDetector fovDetec = new FOVDetector(pt, subSatPayload).withMaxCheck(fovStepSize).withThreshold(threshold);
            TimeIntervalHandler<FOVDetector> fovHandler = new TimeIntervalHandler<>(startDate, endDate, fovDetec.g(initialState1), Action.CONTINUE);
            fovDetec = fovDetec.withHandler(fovHandler);
            prop1.addEventDetector(fovDetec);
            long propStart = System.nanoTime();
            prop1.propagate(startDate, endDate);
            long propEnd = System.nanoTime();
            //System.out.printf("Prop took %.2f sec\n",(propEnd-propStart)/ Math.pow(10, 9));
            FOVDetector fovDetec2 = new FOVDetector(pt, mainSatPayload).withMaxCheck(fovStepSize).withThreshold(threshold);
            TimeIntervalHandler<FOVDetector> fovHandler2 = new TimeIntervalHandler<>(startDate, endDate, fovDetec2.g(initialState2), Action.CONTINUE);
            fovDetec2 = fovDetec2.withHandler(fovHandler2);
            prop2.addEventDetector(fovDetec2);
            prop2.propagate(startDate, endDate);
            TimeIntervalArray fovTimeArray = fovHandler.getTimeArray().createImmutable();
            if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                //System.out.println("nada");
                continue;
            } else {
                //System.out.println("We got something");
            }
            TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
            double[] riseandsets = merger.orCombine().getRiseAndSetTimesList();
            TimeIntervalArray fovTimeArray2 = fovHandler2.getTimeArray().createImmutable();
            TimeIntervalMerger merger2 = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray2);
            double[] riseandsets2 = merger2.orCombine().getRiseAndSetTimesList();
            double decorr;
            long compStart = System.nanoTime();
            for (int i = 0; i < 24*4; i++) {
                double coverageTime = 0;
                double totalCoverageTime = 0;
                decorr = (double) i / 4*3600;
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
                        }
                    }
                    totalCoverageTime = totalCoverageTime + riseandsets[j+1]+decorr-riseandsets[j];
                }
                coverageByDelay.set(i,coverageByDelay.get(i)+coverageTime);
            }
            long compEnd = System.nanoTime();
            //System.out.printf("Comp took %.4f sec\n", (compEnd - compStart) / Math.pow(10, 9));
            prop1.clearEventsDetectors();
            prop2.clearEventsDetectors();
            long pointEnd = System.nanoTime();
            //System.out.printf("Point took %.4f sec\n", (pointEnd - pointStart) / Math.pow(10, 9));
        }
        long end = System.nanoTime();
        OrekitConfig.end();
        System.out.printf("Took %.4f sec\n", (end - start) / Math.pow(10, 9));
        return coverageByDelay;
    }
    private static boolean lineOfSightPotential(CoveragePoint pt, Orbit orbit, double latitudeMargin) throws OrekitException {
        //this computation assumes that the orbit frame is in ECE
        double distance2Pt = pt.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition().getNorm();
        double distance2Sat = orbit.getPVCoordinates().getPosition().getNorm();
        double subtendedAngle = FastMath.acos(distance2Pt / distance2Sat);

        return FastMath.abs(pt.getPoint().getLatitude()) - latitudeMargin < subtendedAngle + orbit.getI();
    }
}