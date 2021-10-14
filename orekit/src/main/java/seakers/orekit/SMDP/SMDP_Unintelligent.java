/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.SMDP;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hipparchus.ode.events.Action;
import seakers.orekit.SMDP.*;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Satellite;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.propagation.*;
import seakers.orekit.object.*;
import seakers.orekit.coverage.access.*;
import seakers.orekit.event.detector.*;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
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
import seakers.orekit.util.Orbits;
import seakers.orekit.util.OrekitConfig;

import static java.lang.Double.parseDouble;

/**
 *
 * @author ben_gorr
 */
public class SMDP_Unintelligent {
    public static void main(String[] args) {
        Locale.setDefault(new Locale("en", "US"));
        long start = System.nanoTime();
        OrekitConfig.init(4);
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2020, 1, 2, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU;

        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        double a600 = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+600000;
        double iSSO = Orbits.incSSO(600);
        NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(30), earthShape);
        ArrayList<Instrument> payload = new ArrayList<>();
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        payload.add(view1);
        ArrayList<Satellite> satellites=new ArrayList<>();
        Orbit orb1 = new KeplerianOrbit(a600, 0.0001, iSSO, 0.0, Math.toRadians(0), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat1 = new Satellite("sat1", orb1,  payload);

        satellites.add(sat1);

        Constellation constel = new Constellation ("constel",satellites);

        List<List<String>> cloudRecords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("D:\\Documents\\VASSAR\\orekit\\CloudGrid_Aug.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                cloudRecords.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
        Set<GeodeticPoint> landPoints = new HashSet<>();
        HashMap<GeodeticPoint,Double> actualRewardGrid = new HashMap<>();
        for(int idx = 0; idx < cloudRecords.size(); idx++) {
            double lat = parseDouble(cloudRecords.get(idx).get(0));
            double lon = parseDouble(cloudRecords.get(idx).get(1));
            double cloudCover = parseDouble(cloudRecords.get(idx).get(2));
            GeodeticPoint landPoint = new GeodeticPoint(lat,lon,0.0);
            landPoints.add(landPoint);
            actualRewardGrid.put(landPoint,cloudCover);
        }
        //create a coverage definition
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", landPoints, earthShape);

        covDef1.assignConstellation(constel);
        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef1);

        Properties propertiesPropagator = new Properties();

        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);
        Propagator prop1 = pf.createPropagator(orb1, 100);
        SpacecraftState initialState1 = prop1.getInitialState();
        HashMap<TopocentricFrame, TimeIntervalArray> satAccesses;
        HashMap<double[],GeodeticPoint> accessLibrary = new HashMap<>();
        HashMap<GeodeticPoint,Double> rewardGrid = new HashMap<>();
        HashMap<GeodeticPoint,Double> staticRewardGrid = new HashMap<>();
        satAccesses = new HashMap<>(covDef1.getNumberOfPoints());
        for (CoveragePoint pt : covDef1.getPoints()) {
            TimeIntervalArray emptyTimeArray = new TimeIntervalArray(startDate, endDate);
            satAccesses.put(pt, emptyTimeArray);
        }
        for (CoveragePoint pt : covDef1.getPoints()) {
            if (!lineOfSightPotential(pt, initialState1.getOrbit(), FastMath.toRadians(5.0))) {
                //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
                continue;
            }
            prop1.resetInitialState(initialState1);
            prop1.clearEventsDetectors();
            //Next search through intervals with line of sight to compute when point is in field of view
            double fovStepSize = orb1.getKeplerianPeriod() / 100.;
            double threshold = 1e-3;
            FOVDetector fovDetec = new FOVDetector(pt, view1).withMaxCheck(fovStepSize).withThreshold(threshold);
            TimeIntervalHandler<FOVDetector> fovHandler = new TimeIntervalHandler<>(startDate, endDate, fovDetec.g(initialState1), Action.CONTINUE);
            fovDetec = fovDetec.withHandler(fovHandler);
            prop1.addEventDetector(fovDetec);
            prop1.propagate(startDate, endDate);
            TimeIntervalArray fovTimeArray = fovHandler.getTimeArray().createImmutable();
            if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                continue;
            }
            TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
            double[] riseandsets = merger.orCombine().getRiseAndSetTimesList();
            accessLibrary.put(riseandsets,pt.getPoint());
            rewardGrid.put(pt.getPoint(),0.5);
            staticRewardGrid.put(pt.getPoint(),0.5);
            prop1.clearEventsDetectors();
        }
        OrekitConfig.end();
        long end = System.nanoTime();
        System.out.printf("Took %.4f sec\n", (end - start) / Math.pow(10, 9));
        ArrayList<StateAction> results = new ArrayList<StateAction>();
        ArrayList<GeodeticPoint> initialImages = new ArrayList<GeodeticPoint>();
        results = forwardSearch(new SatelliteState(0,0,initialImages),4,0.995,accessLibrary,rewardGrid,staticRewardGrid,actualRewardGrid);
        System.out.println(results);
    }
    public static ActionResult SelectAction(SatelliteState s, int dSolve, double gamma, HashMap<double[],GeodeticPoint> accessLibrary,HashMap<GeodeticPoint,Double> rewardGrid){
        if (dSolve == 0) {
            return new ActionResult(null,0);
        }
        ActionResult res = new ActionResult(null,Double.NEGATIVE_INFINITY);
        SatelliteState sCopy = new SatelliteState(s.getT(),s.gettPrevious(),s.getImages());
        ArrayList<SatelliteAction> feasibleActions = getActionSpace(sCopy,accessLibrary);
        for (int a = 0; a < feasibleActions.size(); a++) {
            double value = 0;
            value = rewardFunction(sCopy,feasibleActions.get(a),gamma,rewardGrid);
            SatelliteState tempSatelliteState = transitionFunction(sCopy,feasibleActions.get(a));
            ActionResult tempRes = SelectAction(tempSatelliteState,dSolve-1,gamma, accessLibrary,rewardGrid);
            value = value + Math.pow(gamma,feasibleActions.get(a).gettStart()-sCopy.getT())*tempRes.getV();
            if (value > res.getV()) {
                res = new ActionResult(feasibleActions.get(a),value);
            }
        }
        return res;
    }
    public static ArrayList<StateAction> forwardSearch(SatelliteState initialState, int dSolve, double gamma, HashMap<double[],GeodeticPoint> accessLibrary,HashMap<GeodeticPoint,Double> rewardGrid,HashMap<GeodeticPoint,Double> staticRewardGrid,HashMap<GeodeticPoint,Double> actualRewardGrid) {
        ArrayList<StateAction> resultList = new ArrayList<>();
        HashMap<GeodeticPoint,Double> newRewardGrid;
        ActionResult initRes = SelectAction(initialState,dSolve,gamma,accessLibrary,staticRewardGrid);
        try{
            newRewardGrid = updateRewardGrid(rewardGrid,initRes.getA().getLocation(),actualRewardGrid);
        } catch (Exception e) {
            newRewardGrid = rewardGrid;
            System.out.println(e);
        }
        SatelliteState newSatelliteState = transitionFunction(initialState,initRes.getA());
        resultList.add(new StateAction(initialState,initRes.getA()));
        double value = initRes.getV();
        double totalScore = 0;
        while(value != Double.NEGATIVE_INFINITY) {
            ActionResult res = SelectAction(newSatelliteState,dSolve,gamma, accessLibrary,staticRewardGrid);
            try{
                newRewardGrid = updateRewardGrid(newRewardGrid,res.getA().getLocation(),actualRewardGrid);
            } catch (Exception e) {
                System.out.println(e);
            }
            totalScore = totalScore + finalRewardFunction(newSatelliteState,res.getA(),gamma,newRewardGrid);
            newSatelliteState = transitionFunction(newSatelliteState,res.getA());

            System.out.println(newSatelliteState.getT());
            resultList.add(new StateAction(newSatelliteState,res.getA()));
            value = res.getV();
            System.out.println(totalScore);
            System.out.println(resultList.size());
        }
        return resultList;
    }

    public static double rewardFunction (SatelliteState s, SatelliteAction a, double gamma,HashMap<GeodeticPoint,Double> rewardGrid){
        ArrayList<GeodeticPoint> newImageSet = s.getImages();
        double reward = 0;
        if(!newImageSet.contains(a.getLocation())) {
            reward = rewardGrid.get(a.getLocation());
            reward = (1-reward)*Math.pow(gamma,a.gettStart()-s.getT());
        }
        return reward;
    }
    public static double finalRewardFunction (SatelliteState s, SatelliteAction a, double gamma,HashMap<GeodeticPoint,Double> rewardGrid){
        double reward = 0;
        reward = rewardGrid.get(a.getLocation());
        reward = (1-reward)*Math.pow(gamma,a.gettStart()-s.getT());
        return reward;
    }
    public static HashMap<GeodeticPoint,Double> updateRewardGrid (HashMap<GeodeticPoint,Double> rewardGrid, GeodeticPoint pointOfInterest, HashMap<GeodeticPoint,Double> actualRewardGrid){
        double cloudCover = actualRewardGrid.get(pointOfInterest);
        double multiplier = 0;
        for(int i = -3; i <= 3; i++) {
            for(int j = -3; j <= 3; j++) {
                GeodeticPoint updatePoint = new GeodeticPoint(pointOfInterest.getLatitude()+Math.toRadians(i),pointOfInterest.getLongitude()+Math.toRadians(j),0);
                if(!rewardGrid.containsKey(updatePoint)) {
                    continue;
                }
                double oldValue = rewardGrid.get(updatePoint);
                double newValue = 0;
                if(i == 0 && j == 0) {
                    newValue = cloudCover;
                } else if (Math.abs(i)<=1 && Math.abs(j)<=1) {
                    multiplier = 3;
                    newValue = (cloudCover*multiplier+oldValue)/4;
                } else if (Math.abs(i)<=2 && Math.abs(j)<=2) {
                    multiplier = 2;
                    newValue = (cloudCover*multiplier+oldValue)/4;
                } else {
                    multiplier = 1;
                    newValue = (cloudCover*multiplier+oldValue)/4;
                }
                rewardGrid.put(updatePoint,newValue);
            }
        }
        return rewardGrid;
    }

    public static SatelliteState transitionFunction(SatelliteState s, SatelliteAction a) {
        double t = a.gettStart();
        double tPrevious = s.getT();
        GeodeticPoint location = a.getLocation();
        ArrayList<GeodeticPoint> newImageSet = (ArrayList<GeodeticPoint>) s.getImages().clone();
        if(!s.getImages().contains(location)) {
            newImageSet.add(location);
        }
        SatelliteState newS = new SatelliteState(t,tPrevious,newImageSet);
        return newS;
    }
    public static ArrayList<SatelliteAction> getActionSpace(SatelliteState s, HashMap<double[],GeodeticPoint> accessLibrary) {
        double currentTime = s.getT();
        double allowableTime = 15;
        boolean satisfied = false;
        ArrayList<SatelliteAction> possibleActions = new ArrayList<>();
        List<double[]> accesses = new ArrayList<>(accessLibrary.keySet());
        while(!satisfied) {
            for (int i = 0; i < accesses.size(); i++) {
                double[] riseandsets = accesses.get(i);
                for (int j = 0; j < riseandsets.length; j = j + 2) {
                    if (currentTime < riseandsets[j] && riseandsets[j] < currentTime+allowableTime) {
                        SatelliteAction action = new SatelliteAction(riseandsets[j], riseandsets[j + 1], accessLibrary.get(accesses.get(i)));
                        possibleActions.add(action);
                    }
                }
            }
            if(possibleActions.size() < 10 && currentTime+allowableTime < 86400) {
                allowableTime = allowableTime + 15;
            } else {
                satisfied = true;
            }
        }
        return possibleActions;
    }
    private static boolean lineOfSightPotential(CoveragePoint pt, Orbit orbit, double latitudeMargin) throws OrekitException {
        //this computation assumes that the orbit frame is in ECE
        double distance2Pt = pt.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition().getNorm();
        double distance2Sat = orbit.getPVCoordinates().getPosition().getNorm();
        double subtendedAngle = FastMath.acos(distance2Pt / distance2Sat);

        return FastMath.abs(pt.getPoint().getLatitude()) - latitudeMargin < subtendedAngle + orbit.getI();
    }

}
