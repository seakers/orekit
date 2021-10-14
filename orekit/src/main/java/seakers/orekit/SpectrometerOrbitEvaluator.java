package seakers.orekit;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import seakers.orekit.coverage.analysis.AnalysisMetric;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.EventAnalysisEnum;
import seakers.orekit.event.EventAnalysisFactory;
import seakers.orekit.event.FieldOfViewEventAnalysis;
import seakers.orekit.examples.CoverageExample;
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
import seakers.orekit.scenario.Scenario;
import seakers.orekit.util.OrekitConfig;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static seakers.orekit.object.CoverageDefinition.GridStyle.UNIFORM;

/**
 *
 * @author ben_gorr
 */
public class SpectrometerOrbitEvaluator {

    /**
     * @param args the command line arguments
     * @throws OrekitException
     */
    public static void main(String[] args) {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\Ben\\Desktop\\Selva Research\\python code\\python code\\repeatSSOs_400_900.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        } catch (Exception e) {
            System.out.println("Error reading CSV");
        }
        ArrayList<Double> altitudes = new ArrayList<>();
        ArrayList<Double> inclinations = new ArrayList<>();
        ArrayList<Double> FOVs = new ArrayList<>();
        ArrayList<Integer> repeatDays = new ArrayList<>();
        for(int i = 0; i < records.size(); i++) {
            int repeatDay = parseInt(records.get(i).get(0));
            double alt = parseDouble(records.get(i).get(1));
            double inc = parseDouble(records.get(i).get(4));
            alt = (double)Math.round(alt * 100d) / 100d;
            altitudes.add(alt);
            inclinations.add(inc);
            repeatDays.add(repeatDay);
        }
        for(int j = 1; j < 20; ++j) {
            FOVs.add((double)j);
        }
        JSONObject results = new JSONObject();
        JSONArray arches = new JSONArray();
        for (int i = 0; i < inclinations.size(); i++) {
            for(int j = 0; j < FOVs.size(); j++) {
                ArrayList<Double> coverage = coverageGivenAltIncFOV(altitudes.get(i),inclinations.get(i),FOVs.get(j),repeatDays.get(i));
                //ArrayList<Double> overlap = overlapGivenAltInc(altitudes.get(i),inclinations.get(i),FOVs.get(j),30);
                JSONObject arch = new JSONObject();
                arch.put("altitude",altitudes.get(i));
                arch.put("inclination",inclinations.get(i));
                arch.put("FOV",FOVs.get(j));
                arch.put("Repeat cycle (days)",repeatDays.get(i));
                arch.put("Avg revisit time",coverage.get(0));
                arch.put("Max revisit time",coverage.get(1));
                arch.put("Percent coverage",coverage.get(2));
                arches.add(arch);
                results.put("architectures",arches);
                try{
                    System.out.println("writing to file");
                    FileWriter writer = new FileWriter("D:/Documents/VASSAR/spectrometer.json"); // may want to change this!
                    writer.write(results.toJSONString());
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("DONE");
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

    public static ArrayList<Double> coverageGivenAltIncFOV(double alt, double inc, double FOV,int durationDays) {
        ArrayList<Double> coverage = new ArrayList<>();
        long start = System.nanoTime();
        OrekitConfig.init(4);
        File orekitData = new File("D:/Documents/VASSAR/orekit/resources");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));
        Level level = Level.WARNING;
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

        NadirSimpleConicalFOV mainSatFOV = new NadirSimpleConicalFOV(FastMath.toRadians(FOV),earthShape);

        Instrument mainSatPayload = new Instrument("view2", mainSatFOV, 100, 100);

        Properties propertiesPropagator = new Properties();
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);

        double a_mainSat = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+alt*1000;
        double i_mainSat = FastMath.toRadians(inc);
        ArrayList<Satellite> mainSats=new ArrayList<>();
        double mainSatMass = 100;

        Collection<Instrument> mainSatInstruments = new ArrayList<>();
        mainSatInstruments.add(mainSatPayload);
        Orbit orb = new KeplerianOrbit(a_mainSat, 0.0001, i_mainSat, 0.0, FastMath.toRadians(257.8), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat = new Satellite("Satellite", orb, mainSatInstruments);
        Propagator prop = pf.createPropagator(orb, mainSatMass);

        mainSats.add(sat);

        Constellation mainSatConstellation = new Constellation ("Main Sat",mainSats);

        CoverageDefinition covDef1 = new CoverageDefinition("Whole Earth", 20,  earthShape, UNIFORM);
        System.out.println("Number of points: "+covDef1.getNumberOfPoints());
        covDef1.assignConstellation(mainSatConstellation);

        SpacecraftState initialState1 = prop.getInitialState();

        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef1);

        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.saveAccess", "false");

        EventAnalysisFactory eaf = new EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
        ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
        FieldOfViewEventAnalysis fovEvent = (FieldOfViewEventAnalysis) eaf.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
        eventanalyses.add(fovEvent);

        //build the scenario
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses).covDefs(covDefs).
                name("Test").properties(propertiesEventAnalysis).
                propagatorFactory(pf).build();
        try {
            System.out.println(String.format("Running Scenario %s", scen));
            System.out.println(String.format("Number of points:     %d", covDef1.getNumberOfPoints()));
            scen.call();
        } catch (Exception ex) {
            Logger.getLogger(CoverageExample.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }
        long end = System.nanoTime();
        System.out.printf("Took %.4f sec", (end - start) / Math.pow(10, 9));
        Map<TopocentricFrame, TimeIntervalArray> accesses = fovEvent.getEvents(covDef1);
        GroundEventAnalyzer eventAnalyzer = new GroundEventAnalyzer(accesses);
        DescriptiveStatistics rev;
        DescriptiveStatistics cov;
        rev = eventAnalyzer.getStatistics(AnalysisMetric.DURATION, false, propertiesPropagator);
        cov = eventAnalyzer.getStatistics(AnalysisMetric.PERCENT_COVERAGE, true, propertiesPropagator);
        coverage.add(rev.getMean()/3600);
        coverage.add(rev.getPercentile(95)/3600);
        coverage.add(cov.getMean());
        System.out.println(coverage);
        return coverage;

    }
    public static ArrayList<Double> overlapGivenAltInc(double alt, double inc, double FOV, double durationDays) {
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
        NadirSimpleConicalFOV subSatFOV = new NadirSimpleConicalFOV(FastMath.toRadians(FOV), earthShape); // 0.06 deg
        NadirSimpleConicalFOV mainSatFOV = new NadirSimpleConicalFOV(FastMath.toRadians(20),earthShape); // 0.38 deg

        Instrument subSatPayload = new Instrument("view1", subSatFOV, 100, 100);
        Instrument mainSatPayload = new Instrument("view2", mainSatFOV, 100, 100);

        Properties propertiesPropagator = new Properties();

        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN,propertiesPropagator);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);
        //PropagatorFactory pf =  new PropagatorFactory(PropagatorType.NUMERICAL,propertiesPropagator);

        double subSatHeight=alt*1000;
        int SWOTHeight=890600;
        double a_subSat = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+subSatHeight;
        double a_SWOT = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+SWOTHeight;
        double subSat_i = FastMath.toRadians(inc);
        double SWOT_i = FastMath.toRadians(77.6);
        //Enter satellite orbital parameters
        ArrayList<Satellite> subSats=new ArrayList<>();
        ArrayList<Satellite> mainSats=new ArrayList<>();
        double subSatMass = 100;
        double mainSatMass = 100;

        Collection<Instrument> subSatInstruments = new ArrayList<>();
        subSatInstruments.add(subSatPayload);
        Collection<Instrument> mainSatInstruments = new ArrayList<>();
        mainSatInstruments.add(mainSatPayload);
        Orbit orb1 = new KeplerianOrbit(a_subSat, 0.0001, subSat_i, 0.0, FastMath.toRadians(257.8), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat1 = new Satellite("ICESat", orb1, subSatInstruments);
        Orbit orb2 = new KeplerianOrbit(a_SWOT, 0.0001, SWOT_i, 0.0, FastMath.toRadians(257.8), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat2 = new Satellite("Cryosat2", orb2, mainSatInstruments);
        Propagator prop1 = pf.createPropagator(orb1, subSatMass);
        Propagator prop2 = pf.createPropagator(orb2, mainSatMass);


        subSats.add(sat1);
        mainSats.add(sat2);

        Constellation subSatConstellation = new Constellation ("Sub Sat",subSats);
        Constellation mainSatConstellation = new Constellation ("Main Sat",mainSats);

        CoverageDefinition covDef1 = new CoverageDefinition("Whole Earth", 20,  earthShape, UNIFORM);
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
        for (CoveragePoint pt : covDef1.getPoints()) {
            long pointStart = System.nanoTime();
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
