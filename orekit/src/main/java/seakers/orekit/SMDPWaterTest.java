/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import org.hipparchus.ode.events.Action;
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
import seakers.orekit.SMDP.ActionResult;
import seakers.orekit.SMDP.SatelliteAction;
import seakers.orekit.SMDP.SatelliteState;
import seakers.orekit.SMDP.StateAction;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.analysis.CompoundSpacecraftAnalysis;
import seakers.orekit.analysis.Record;
import seakers.orekit.analysis.ephemeris.GroundTrackAnalysis;
import seakers.orekit.analysis.ephemeris.OrbitalElementsAnalysis;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;
import seakers.orekit.event.detector.FOVDetector;
import seakers.orekit.event.detector.TimeIntervalHandler;
import seakers.orekit.object.*;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.Orbits;
import seakers.orekit.util.OrekitConfig;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Double.parseDouble;
/**
 *
 * @author ben_gorr
 */
public class SMDPWaterTest {
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

        double a600 = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+800000;
        double iSSO = Orbits.incSSO(800);
        NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
        ArrayList<Instrument> payload = new ArrayList<>();
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        payload.add(view1);
        ArrayList<Satellite> satellites=new ArrayList<>();
        Orbit orb1 = new KeplerianOrbit(a600, 0.0001, iSSO, 0.0, Math.toRadians(0), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat1 = new Satellite("sat1", orb1,  payload);

        satellites.add(sat1);

        Constellation constel = new Constellation ("constel",satellites);

        List<List<String>> waterRecords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("D:\\Documents\\VASSAR\\orekit\\WaterPoints.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                waterRecords.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
        Set<GeodeticPoint> landPoints = new HashSet<>();
        HashMap<GeodeticPoint,Double> actualRewardGrid = new HashMap<>();
        for(int idx = 0; idx < waterRecords.size(); idx++) {
            double lat = Math.toRadians(parseDouble(waterRecords.get(idx).get(0)));
            double lon = Math.toRadians(parseDouble(waterRecords.get(idx).get(1)));
            double reward = parseDouble(waterRecords.get(idx).get(2));
            GeodeticPoint landPoint = new GeodeticPoint(lat,lon,0.0);
            landPoints.add(landPoint);
            actualRewardGrid.put(landPoint,reward);
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
            prop1.clearEventsDetectors();
        }
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");
        propertiesEventAnalysis.setProperty("fov.saveAccess", "true");

        //set the event analyses

        Collection<Analysis<?>> analyses = new ArrayList<>();
        double analysisTimeStep = 16;
        GroundTrackAnalysis gta = new GroundTrackAnalysis(startDate, endDate, analysisTimeStep, sat1, earthShape, pf);
        OrbitalElementsAnalysis oea = new OrbitalElementsAnalysis(startDate, endDate, analysisTimeStep, sat1,PositionAngle.MEAN, pf);
        analyses.add(gta);
        analyses.add(oea);
        String filename = "xd";
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                analysis(analyses).name("test1").propagatorFactory(pf).build();
        try {
            long start1 = System.nanoTime();
            scen.call();
            long end1 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end1 - start1) / Math.pow(10, 9)));

        } catch (Exception ex) {
            throw new IllegalStateException("scenario failed to complete.");
        }
        for (Analysis<?> analysis : analyses) {
            if (analysis instanceof CompoundSpacecraftAnalysis){
                for (Analysis<?> anal:((CompoundSpacecraftAnalysis) analysis).getAnalyses()){
                    ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                            String.format("%s_%s", filename, anal.getName()), anal);
                }
            } else{
                ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                        String.format("%s_%s", filename, analysis.getName()), analysis);
            }
        }
        Collection<Record<String>> coll = gta.getHistory();
        //ScenarioIO.printAnalysis(gta);
        Iterator<Record<String>> iter = coll.iterator();
        List<List<String>> dataLines = new ArrayList<>();
        for (Iterator<Record<String>> it = iter; it.hasNext(); ) {
            Record<String> ind = it.next();
            String rawString = ind.getValue();
            String[] splitString = rawString.split(",");
            dataLines.add(Arrays.asList(splitString));
        }
        try {
            FileWriter csvWriter = new FileWriter("groundtrack.csv");
            for (List<String> rowData : dataLines) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        OrekitConfig.end();
        long end = System.nanoTime();
        System.out.printf("Took %.4f sec\n", (end - start) / Math.pow(10, 9));
        ArrayList<StateAction> results = new ArrayList<StateAction>();
        ArrayList<GeodeticPoint> initialImages = new ArrayList<GeodeticPoint>();
        results = forwardSearch(new SatelliteState(0,0,initialImages),1,0.99999999,accessLibrary,actualRewardGrid);
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
            if(feasibleActions.get(a).gettStart()-sCopy.getT() < 0) {
                System.out.println("time travel");
            }
            value = value + Math.pow(gamma,feasibleActions.get(a).gettStart()-sCopy.getT())*tempRes.getV();
            if (value > res.getV()) {
                res = new ActionResult(feasibleActions.get(a),value);
            }
        }
        return res;
    }
    public static ArrayList<StateAction> forwardSearch(SatelliteState initialState, int dSolve, double gamma, HashMap<double[],GeodeticPoint> accessLibrary,HashMap<GeodeticPoint,Double> actualRewardGrid) {
        ArrayList<StateAction> resultList = new ArrayList<>();
        ActionResult initRes = SelectAction(initialState,dSolve,gamma,accessLibrary,actualRewardGrid);
        SatelliteState newSatelliteState = transitionFunction(initialState,initRes.getA());
        resultList.add(new StateAction(initialState,initRes.getA()));
        double value = initRes.getV();
        double totalScore = rewardFunction(newSatelliteState,initRes.getA(),gamma,actualRewardGrid);
        System.out.println(newSatelliteState.getT());
        System.out.println(totalScore);
        System.out.println(resultList.size());
        System.out.println(newSatelliteState.getImages());
        while(value != Double.NEGATIVE_INFINITY) {
            ActionResult res = SelectAction(newSatelliteState,dSolve,gamma, accessLibrary,actualRewardGrid);
            totalScore = totalScore + rewardFunction(newSatelliteState,res.getA(),gamma,actualRewardGrid);
            newSatelliteState = transitionFunction(newSatelliteState,res.getA());

            System.out.println(newSatelliteState.getT());
            resultList.add(new StateAction(newSatelliteState,res.getA()));
            value = res.getV();
            System.out.println(totalScore);
            System.out.println(resultList.size());
            System.out.println(newSatelliteState.getImages());
        }
        return resultList;
    }


    public static double rewardFunction (SatelliteState s, SatelliteAction a, double gamma,HashMap<GeodeticPoint,Double> rewardGrid){
        ArrayList<GeodeticPoint> newImageSet = s.getImages();
        double reward = 0;
        if(!newImageSet.contains(a.getLocation())) {
            reward = rewardGrid.get(a.getLocation());
            reward = reward*Math.pow(gamma,a.gettStart()-s.getT());
        }
        return reward;
    }
//    public static double rewardFunction (SatelliteState s, SatelliteAction a, double gamma,HashMap<GeodeticPoint,Double> rewardGrid){
//        double reward = 0;
//        reward = rewardGrid.get(a.getLocation());
//        reward = reward*Math.pow(gamma,a.gettStart()-s.getT());
//        return reward;
//    }

    public static SatelliteState transitionFunction(SatelliteState s, SatelliteAction a) {
        double t = a.gettStart()+1;
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
            if(possibleActions.size() < 5 && currentTime+allowableTime < 86400) {
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