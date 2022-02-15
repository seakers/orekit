package seakers.orekit;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.analysis.Record;
import seakers.orekit.analysis.ephemeris.GroundTrackAnalysis;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.event.CrossLinkEventAnalysis;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.FieldOfViewEventAnalysis;
import seakers.orekit.event.GndStationEventAnalysis;
import seakers.orekit.object.*;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.util.OrekitConfig;

import java.io.*;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Double.parseDouble;

@SuppressWarnings({"unchecked"})

public class Precomputer {

    public String plannerRepoFilePath;
    public double durationDays;
    public AbsoluteDate startDate;

    public Precomputer(String plannerRepo) {
        plannerRepoFilePath = plannerRepo;
        durationDays = 0.1;
        OrekitConfig.init(4);
        File orekitData = new File("./resources");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        TimeScale utc = TimeScalesFactory.getUTC();
        startDate = new AbsoluteDate(2020, 1, 1, 10, 30, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU;

        // Initializing
        ArrayList<Satellite> imagers = new ArrayList<>();
        Collection<Instrument> ssPayload = new ArrayList<>();
        double ssCrossFOVRadians = Math.toRadians(30.0);
        double ssAlongFOVRadians = Math.toRadians(1.0);
        NadirRectangularFOV ssFOV = new NadirRectangularFOV(ssCrossFOVRadians,ssAlongFOVRadians,0.0,earthShape);
        Instrument ssImager = new Instrument("Smallsat imager", ssFOV, 100.0, 100.0);
        ssPayload.add(ssImager);
        int r = 4;
        int s = 4;
        for(int m = 0; m < r; m++) {
            for(int n = 0; n < s; n++) {
                int pu = 360 / (r*s);
                int delAnom = pu * r; //in plane spacing between satellites
                int delRAAN = pu * s; //node spacing
                int RAAN = delRAAN * m;
                int f = 1;
                int phasing = pu * f;
                int anom = (n * delAnom + phasing * m);
                Orbit ssOrbit = new KeplerianOrbit(6378000+500000, 0.0, FastMath.toRadians(90), 0.0, FastMath.toRadians(RAAN), FastMath.toRadians(anom), PositionAngle.MEAN, inertialFrame, startDate, mu);
//                TransmitterAntenna tx = new TransmitterAntenna(1.0,Collections.singleton(S));
//                ReceiverAntenna rx = new ReceiverAntenna(1.0,Collections.singleton(S));
                Satellite smallsat = new Satellite("smallsat"+m+n, ssOrbit, ssPayload);
                imagers.add(smallsat);
            }
        }
        Map<GeodeticPoint,Double> covPointRewards = loadCoveragePoints();
        NearEarthNetwork nen = new NearEarthNetwork();
        Set<GndStation> nenStations = nen.getGroundStations();

        int satelliteNumber = 0;
        ArrayList<Constellation> constellationArrayList = new ArrayList<>();
        constellationArrayList.add(new Constellation("satellites", imagers));
        Map<Satellite, TimeIntervalArray> crosslinks = getCrosslinks(constellationArrayList);
        for (Satellite imager : imagers) {
            File file = new File(plannerRepoFilePath+"/satellite"+satelliteNumber);
            file.mkdir();
            ArrayList<Observation> observations = observationsBySatellite(imager, covPointRewards, getGroundTrack(imager.getOrbit()));
            Map<Satellite, Set<GndStation>> satelliteSetMap = new HashMap<>();
            satelliteSetMap.put(imager,nenStations);
            HashMap<GndStation,TimeIntervalArray> downlinks = downlinksBySatellite(satelliteSetMap);
            TimeIntervalMerger merger = new TimeIntervalMerger(downlinks.values());
            TimeIntervalArray mergedDownlinks = merger.orCombine();
            TimeIntervalArray crosslinkTimes = crosslinks.get(imager);
            saveObject(observations,"/satellite"+satelliteNumber+"/observations.dat");
            saveObject(mergedDownlinks,"/satellite"+satelliteNumber+"/downlinks.dat");
            saveObject(crosslinkTimes,"/satellite"+satelliteNumber+"/crosslinks.dat");
            satelliteNumber = satelliteNumber + 1;
        }
        OrekitConfig.end();
    }

    public void saveObject(Object obj, String filepath) {
        try {
            File file = new File(plannerRepoFilePath+filepath);
            FileOutputStream fos=new FileOutputStream(file);
            ObjectOutputStream oos=new ObjectOutputStream(fos);

            oos.writeObject(obj);
            oos.flush();
            oos.close();
            fos.close();
        } catch (Exception e) {
            System.out.println("Exception in saveObject: "+e);
        }
    }

    public ArrayList<Observation> observationsBySatellite(Satellite satellite, Map<GeodeticPoint, Double> covPoints, Map<Double, GeodeticPoint> groundTrack) {
        long start = System.nanoTime();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate endDate = startDate.shiftedBy(durationDays*86400);
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2);

        //create a coverage definition
        CoverageDefinition covDef = new CoverageDefinition("covdef1", covPoints.keySet(), earthShape);
        //CoverageDefinition covDef = new CoverageDefinition("Whole Earth", granularity, earthShape, UNIFORM);
        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        ArrayList<Satellite> satelliteList = new ArrayList<>();
        satelliteList.add(satellite);
        Constellation constellation = new Constellation("Constellation", satelliteList);
        covDef.assignConstellation(constellation);
        covDefs.add(covDef);

        ArrayList<EventAnalysis> eventAnalyses = new ArrayList<>();
        FieldOfViewEventAnalysis fovea = new FieldOfViewEventAnalysis(startDate, endDate, inertialFrame,covDefs,pf,true, true);
        eventAnalyses.add(fovea);

        Scenario scene = new Scenario.Builder(startDate, endDate, utc).eventAnalysis(eventAnalyses).covDefs(covDefs).name("CoverageBySatellite").propagatorFactory(pf).build();

        try {
            scene.call();
        } catch (Exception e) {
            e.printStackTrace();
        }

        GroundEventAnalyzer gea = new GroundEventAnalyzer(fovea.getEvents(covDef));

        Map<TopocentricFrame, TimeIntervalArray> gpEvents = gea.getEvents();
        ArrayList<Observation> observations = new ArrayList<>();
        for (TopocentricFrame tf : gpEvents.keySet()) {
            GeodeticPoint gp = tf.getPoint();
            TimeIntervalArray tia = gpEvents.get(tf);
            for (int i = 0; i < tia.numIntervals(); i++) {
                double riseTime = tia.getRiseAndSetTimesList()[2*i];
                double setTime = tia.getRiseAndSetTimesList()[2*i+1];
                double incidenceAngle = getIncidenceAngle(gp,riseTime,setTime,satellite,groundTrack);
                double reward = covPoints.get(gp);
                Observation obs = new Observation(gp,riseTime,setTime,reward,incidenceAngle);
                observations.add(obs);
            }
        }
        long end = System.nanoTime();
        System.out.printf("observationsBySatellite took %.4f sec\n", (end - start) / Math.pow(10, 9));
        observations = sortObservations(observations);
        return observations;
    }

    public ArrayList<Observation> sortObservations(ArrayList<Observation> observations) {
        observations.sort(new sortByRiseTime());
        return observations;
    }

    class sortByRiseTime implements Comparator<Observation>
    {
        // Used for sorting in ascending order of
        // roll number
        public int compare(Observation a, Observation b)
        {
            return (int) (a.getObservationStart() - b.getObservationStart());
        }
    }

    public HashMap<GndStation, TimeIntervalArray> downlinksBySatellite(Map<Satellite,Set<GndStation>> satelliteMap) {
        long start = System.nanoTime();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate endDate = startDate.shiftedBy(durationDays*86400);
        Frame inertialFrame = FramesFactory.getEME2000();
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2);

        ArrayList<EventAnalysis> eventAnalyses = new ArrayList<>();
        GndStationEventAnalysis gsea = new GndStationEventAnalysis(startDate, endDate, inertialFrame,satelliteMap,pf);
        eventAnalyses.add(gsea);

        Scenario scene = new Scenario.Builder(startDate, endDate, utc).eventAnalysis(eventAnalyses).name("GSBySatellite").propagatorFactory(pf).build();

        try {
            scene.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Set<Satellite> satSet = satelliteMap.keySet();
        Satellite sat = satSet.iterator().next();
        HashMap<GndStation, TimeIntervalArray> gsMap = gsea.getSatelliteAccesses(sat);
        long end = System.nanoTime();
        System.out.printf("downlinksBySatellite took %.4f sec\n", (end - start) / Math.pow(10, 9));
        return gsMap;
    }

    public Map<Satellite, TimeIntervalArray> getCrosslinks(ArrayList<Constellation> constellation) {
        long start = System.nanoTime();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate endDate = startDate.shiftedBy(durationDays*86400);
        Frame inertialFrame = FramesFactory.getEME2000();
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2);

        ArrayList<EventAnalysis> eventAnalyses = new ArrayList<>();
        CrossLinkEventAnalysis clea = new CrossLinkEventAnalysis(startDate, endDate, inertialFrame,constellation,pf, true, false);
        eventAnalyses.add(clea);

        Scenario scene = new Scenario.Builder(startDate, endDate, utc).eventAnalysis(eventAnalyses).name("GSBySatellite").propagatorFactory(pf).build();

        try {
            scene.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long end = System.nanoTime();
        System.out.printf("crosslinks took %.4f sec\n", (end - start) / Math.pow(10, 9));
        return clea.getEvents();
    }

    public Map<GeodeticPoint,Double> loadCoveragePoints() {
        Map<GeodeticPoint, Double> pointRewards = new HashMap<>();
        if (!new File(plannerRepoFilePath + "/coveragePoints.dat").exists()) {
            // Loading river and lake constant scores
            List<List<String>> riverRecords = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader("./src/main/java/seakers/orekit/overlap/grwl_river_output.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    riverRecords.add(Arrays.asList(values));
                }
            } catch (Exception e) {
                System.out.println("Exception occurred in loadCoveragePoints: " + e);
            }
            for (int i = 0; i < 1000; i++) {
                double lon = Math.toRadians(parseDouble(riverRecords.get(i).get(0)));
                double lat = Math.toRadians(parseDouble(riverRecords.get(i).get(1)));
                double width = parseDouble(riverRecords.get(i).get(2));
                GeodeticPoint riverPoint = new GeodeticPoint(lat, lon, 0.0);
                pointRewards.put(riverPoint, width / 5000.0 / 2);
            }
            List<List<String>> lakeRecords = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader("./src/main/java/seakers/orekit/overlap/hydrolakes.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    lakeRecords.add(Arrays.asList(values));
                }
            } catch (Exception e) {
                System.out.println("Exception occurred in loadCoveragePoints: " + e);
            }
            for (int i = 1; i < 1000; i++) {
                double lat = Math.toRadians(parseDouble(lakeRecords.get(i).get(0)));
                double lon = Math.toRadians(parseDouble(lakeRecords.get(i).get(1)));
                double area = parseDouble(lakeRecords.get(i).get(2));
                GeodeticPoint lakePoint = new GeodeticPoint(lat, lon, 0.0);
                pointRewards.put(lakePoint, area / 30000.0);
            }
            try {
                File file = new File(plannerRepoFilePath+"/coveragePoints.dat");
                FileOutputStream fos=new FileOutputStream(file);
                ObjectOutputStream oos=new ObjectOutputStream(fos);

                oos.writeObject(pointRewards);
                oos.flush();
                oos.close();
                fos.close();
            } catch (Exception e) {
                System.out.println("Exception in loadCoveragePoints: "+e);
            }

        } else {
            try {
                File toRead=new File(plannerRepoFilePath+"/coveragePoints.dat");
                FileInputStream fis=new FileInputStream(toRead);
                ObjectInputStream ois=new ObjectInputStream(fis);

                pointRewards=(Map<GeodeticPoint,Double>)ois.readObject();

                ois.close();
                fis.close();
            } catch(Exception e) {
                System.out.println("Exception in loadCoveragePoints: "+e);
            }
        }

        return pointRewards;
    }

    public Map<Double, GeodeticPoint> getGroundTrack(Orbit orbit) {
        OrekitConfig.init(1);
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate endDate = startDate.shiftedBy(durationDays*86400);

        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        ArrayList<Instrument> payload = new ArrayList<>();
        Satellite sat1 = new Satellite(orbit.toString(), orbit,  payload);
        Properties propertiesPropagator = new Properties();
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);


        Collection<Analysis<?>> analyses = new ArrayList<>();
        double analysisTimeStep = 1;
        GroundTrackAnalysis gta = new GroundTrackAnalysis(startDate, endDate, analysisTimeStep, sat1, earthShape, pf);
        analyses.add(gta);
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                analysis(analyses).name(orbit.toString()).propagatorFactory(pf).build();
        try {
            scen.call();
        } catch (Exception ex) {
            throw new IllegalStateException("Ground track scenario failed to complete.");
        }
        Map<Double, GeodeticPoint> sspMap = new HashMap<>();
        for (Record<String> ind : gta.getHistory()) {
            String rawString = ind.getValue();
            AbsoluteDate date = ind.getDate();
            String[] splitString = rawString.split(",");
            double latitude = Double.parseDouble(splitString[0]);
            double longitude = Double.parseDouble(splitString[1]);
            GeodeticPoint ssp = new GeodeticPoint(Math.toRadians(latitude),Math.toRadians(longitude),0);
            double elapsedTime = date.durationFrom(startDate);
            sspMap.put(elapsedTime,ssp);
        }
        return sspMap;
    }

    public double getIncidenceAngle(GeodeticPoint point, double riseTime, double setTime, Satellite satellite, Map<Double, GeodeticPoint> groundTrack) {
        double time = (riseTime + setTime) / 2;

        double closestDist = 100000000000000000.0;
        double closestTime = 100 * 24 * 3600; // 100 days
        GeodeticPoint closestPoint;
        for (Double sspTime : groundTrack.keySet()) {
            if (Math.abs(sspTime - time) < closestTime) {
                closestTime = Math.abs(sspTime - time);
                closestPoint = groundTrack.get(sspTime);
                double dist = Math.sqrt(Math.pow(LLAtoECI(closestPoint)[0] - LLAtoECI(point)[0], 2) + Math.pow(LLAtoECI(closestPoint)[1] - LLAtoECI(point)[1], 2) + Math.pow(LLAtoECI(closestPoint)[2] - LLAtoECI(point)[2], 2));
                if (dist < closestDist) {
                    closestDist = dist;
                }
            }
        }
        return Math.atan2(closestDist,(satellite.getOrbit().getA()-6370000)/1000);
    }

    public double[] LLAtoECI(GeodeticPoint point) {
        double re = 6370;
        double x = re * Math.cos(point.getLatitude()) * Math.cos(point.getLongitude());
        double y = re * Math.cos(point.getLatitude()) * Math.sin(point.getLongitude());
        double z = re * Math.sin(point.getLatitude());
        double[] result = {x,y,z};
        return result;
    }

}
