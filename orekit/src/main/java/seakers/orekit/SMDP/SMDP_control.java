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
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hipparchus.ode.events.Action;
import seakers.orekit.SMDP.*;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.analysis.CompoundSpacecraftAnalysis;
import seakers.orekit.analysis.Record;
import seakers.orekit.analysis.ephemeris.GroundTrackAnalysis;
import seakers.orekit.analysis.ephemeris.OrbitalElementsAnalysis;
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
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.Orbits;
import seakers.orekit.util.OrekitConfig;

import static java.lang.Double.parseDouble;

/**
 *
 * @author ben_gorr
 */
public class SMDP_control {
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
        NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(20), earthShape);
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
            if (pt.getPoint().getLatitude() == 6 && pt.getPoint().getLongitude() == -99) {
                System.out.println("wtf");
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
        ScenarioIO.printAnalysis(gta);
        Iterator<Record<String>> iter = coll.iterator();
        for (Iterator<Record<String>> it = iter; it.hasNext(); ) {
            Record<String> ind = it.next();


        }
//        Iterator<Record<>> histIter = gta.getHistory().iterator();
//        System.out.println("#Epoch time," + gta.getHeader() + "\n");
//        while (histIter.hasNext()) {
//            Record<String> r = histIter.next();
//            System.out.println(String.format("%f,%s\n",
//                    r.getDate().durationFrom(gta.getStartDate()),
//                    r.getValue()));
//        }

        try {
            long start1 = System.nanoTime();
            scen.call();
            long end1 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end1 - start1) / Math.pow(10, 9)));

        } catch (Exception ex) {
            throw new IllegalStateException("scenario failed to complete.");
        }
        OrekitConfig.end();
        long end = System.nanoTime();
        System.out.printf("Took %.4f sec\n", (end - start) / Math.pow(10, 9));
        ArrayList<StateAction> results = new ArrayList<StateAction>();
        ArrayList<GeodeticPoint> initialImages = new ArrayList<GeodeticPoint>();
        results = sequentialSimulation(new SatelliteState(0,0,initialImages),3,0.995,accessLibrary,rewardGrid,actualRewardGrid);
        System.out.println(results);
    }
    public static ActionResult SelectAction(SatelliteState s, int dSolve, double gamma, HashMap<double[],GeodeticPoint> accessLibrary,HashMap<GeodeticPoint,Double> rewardGrid){
        ActionResult res = new ActionResult(null,Double.NEGATIVE_INFINITY);
        SatelliteState sCopy = new SatelliteState(s.getT(),s.gettPrevious(),s.getImages());
        double value = 0;
        SatelliteAction nextAction = findNextAction(sCopy.getT(),rewardGrid);
        try {
            value = rewardFunction(sCopy, nextAction, gamma, rewardGrid);
        } catch (Exception e) {
            value = Double.NEGATIVE_INFINITY;
        }
        res = new ActionResult(nextAction,value);
        return res;
    }
    public static ArrayList<StateAction> sequentialSimulation(SatelliteState initialState, int dSolve, double gamma, HashMap<double[],GeodeticPoint> accessLibrary,HashMap<GeodeticPoint,Double> rewardGrid,HashMap<GeodeticPoint,Double> actualRewardGrid) {
        ArrayList<StateAction> resultList = new ArrayList<>();
        HashMap<GeodeticPoint,Double> newRewardGrid;
        ActionResult initRes = SelectAction(initialState,dSolve,gamma,accessLibrary,rewardGrid);
        try{
            newRewardGrid = updateRewardGrid(rewardGrid,initRes.getA().getLocation(),actualRewardGrid);
        } catch (Exception e) {
            newRewardGrid = rewardGrid;
            System.out.println(e);
        }
        SatelliteState newSatelliteState = transitionFunction(initialState,initRes.getA(),newRewardGrid);
        resultList.add(new StateAction(initialState,initRes.getA()));
        double value = initRes.getV();
        double totalScore = 0;
        while(value != Double.NEGATIVE_INFINITY) {
            ActionResult res = SelectAction(newSatelliteState,dSolve,gamma,accessLibrary,newRewardGrid);
            try{
                newRewardGrid = updateRewardGrid(newRewardGrid,res.getA().getLocation(),actualRewardGrid);
                value = res.getV();
            } catch (Exception e) {
                System.out.println(e);
                value = Double.NEGATIVE_INFINITY;
            }
            totalScore = totalScore + rewardFunction(newSatelliteState,res.getA(),gamma,newRewardGrid);
            newSatelliteState = transitionFunction(newSatelliteState,res.getA(),newRewardGrid);
            System.out.println(newSatelliteState.getT());
            resultList.add(new StateAction(newSatelliteState,res.getA()));


            System.out.println(totalScore);
            System.out.println(resultList.size());
        }
        return resultList;
    }

    public static double rewardFunction (SatelliteState s, SatelliteAction a, double gamma,HashMap<GeodeticPoint,Double> rewardGrid){
        ArrayList<GeodeticPoint> newImageSet = s.getImages();
        double reward = 0;
        if(!newImageSet.contains(a.getLocation()) && rewardGrid.containsKey(a.getLocation())) {
            reward = rewardGrid.get(a.getLocation());
            reward = (1-reward)*Math.pow(gamma,a.gettStart()-s.getT());
        }
        return reward;
    }
    public static HashMap<GeodeticPoint,Double> updateRewardGrid (HashMap<GeodeticPoint,Double> rewardGrid, GeodeticPoint pointOfInterest,HashMap<GeodeticPoint,Double> actualRewardGrid) throws IOException {
        long start = System.nanoTime();
        double cloudCover = actualRewardGrid.get(pointOfInterest);
        double multiplier = 0;
        GeodeticPoint updatePoint = new GeodeticPoint(pointOfInterest.getLatitude(),pointOfInterest.getLongitude(),0);
        rewardGrid.put(updatePoint,cloudCover);
        long end = System.nanoTime();
        System.out.printf("updateRewardGrid took %.4f sec\n", (end - start) / Math.pow(10, 9));
        return rewardGrid;
    }

    public static SatelliteState transitionFunction(SatelliteState s, SatelliteAction a, HashMap<GeodeticPoint,Double> rewardGrid) {
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
    public static SatelliteAction findNextAction(double time, HashMap<GeodeticPoint,Double> pointLibrary) {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("D:/Documents/VASSAR/orekit/results/xd_gdt_sat1.gdt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
        GeodeticPoint closestPoint = new GeodeticPoint(0,0,0);
        double minimumTime = 1000000000000.0;
        double timeDiff = 0;
        for(int i = 1; i < records.size(); i++) {
            double savedTime = parseDouble(records.get(i).get(0));
            int latitude = (int)Math.round(parseDouble(records.get(i).get(1)));
            int longitude = (int)Math.round(parseDouble(records.get(i).get(2)));
            GeodeticPoint testPoint = new GeodeticPoint(Math.toRadians(latitude),Math.toRadians(longitude),0);
            if(Math.abs(time-savedTime) < minimumTime && savedTime > time && pointLibrary.containsKey(testPoint)) {
                minimumTime = Math.abs(time-savedTime);
                timeDiff = savedTime-time;
                closestPoint = new GeodeticPoint(Math.toRadians(Math.round(parseDouble(records.get(i).get(1)))),Math.toRadians(Math.round(parseDouble(records.get(i).get(2)))),0);
            }
        }
        SatelliteAction nextAction = new SatelliteAction(time+timeDiff,time+timeDiff+100,closestPoint);

        return nextAction;
    }
    private static boolean lineOfSightPotential(CoveragePoint pt, Orbit orbit, double latitudeMargin) throws OrekitException {
        //this computation assumes that the orbit frame is in ECE
        double distance2Pt = pt.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition().getNorm();
        double distance2Sat = orbit.getPVCoordinates().getPosition().getNorm();
        double subtendedAngle = FastMath.acos(distance2Pt / distance2Sat);

        return FastMath.abs(pt.getPoint().getLatitude()) - latitudeMargin < subtendedAngle + orbit.getI();
    }

}
