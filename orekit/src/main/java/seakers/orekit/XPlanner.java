/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.*;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.*;
import org.geotools.swing.JMapFrame;
import org.geotools.util.factory.Hints;
import org.hipparchus.util.FastMath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.ContrastMethod;
import org.orekit.utils.TimeStampedPVCoordinates;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.analysis.ephemeris.GroundTrackAnalysis;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.FieldOfViewEventAnalysis;
import seakers.orekit.event.GndStationEventAnalysis;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Satellite;
import seakers.orekit.object.Instrument;
import seakers.orekit.analysis.Record;
import seakers.orekit.object.communications.Receiver;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.Transmitter;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.parallel.ParallelRoutine;
import seakers.orekit.propagation.*;
import seakers.orekit.object.*;
import seakers.orekit.coverage.access.*;
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
import seakers.orekit.scenario.Scenario;
import seakers.orekit.util.OrekitConfig;

import javax.imageio.ImageIO;
import javax.swing.*;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.parseDouble;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.io.FileUtils.getFile;
import static seakers.orekit.object.CommunicationBand.S;    

public class XPlanner {

    public static void main(String[] args) {

        // Orekit initialization needs
        OrekitConfig.init(4);
        String greedyPlannerFilePath = "./orekit/src/main/java/seakers/orekit/output/greedyplanner/";
        File orekitData = new File("./orekit/resources");
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
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 10, 30, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU;

        // Initializing
        ArrayList<Satellite> imagers = new ArrayList<>();
        Collection<Instrument> ssPayload = new ArrayList<>();
        double ssCrossFOVRadians = Math.toRadians(30.0);
        double ssAlongFOVRadians = Math.toRadians(1.0);
        NadirRectangularFOV ssFOV = new NadirRectangularFOV(ssCrossFOVRadians,ssAlongFOVRadians,0.0,earthShape);
        Instrument ssImager = new Instrument("Smallsat imager", ssFOV, 100.0, 100.0);
        ssPayload.add(ssImager);
        int r = 1;
        int s = 1;
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
        double duration = 0.1;
        Map<Double,Map<GeodeticPoint,Double>> covPointRewards = new HashMap<>();
        if(!new File("coverageRewardsUnweighted").exists()) {
            covPointRewards = loadCoveragePoints();
        } else {
            try {
                File toRead=new File("coverageRewardsUnweighted");
                FileInputStream fis=new FileInputStream(toRead);
                ObjectInputStream ois=new ObjectInputStream(fis);

                covPointRewards=(HashMap<Double,Map<GeodeticPoint,Double>>)ois.readObject();

                ois.close();
                fis.close();
            } catch(Exception e) {}
        }
        ArrayList<GeodeticPoint> updatedPoints = new ArrayList<>();
        try {
            File toRead=new File("updatedPoints");
            FileInputStream fis=new FileInputStream(toRead);
            ObjectInputStream ois=new ObjectInputStream(fis);

            updatedPoints = (ArrayList<GeodeticPoint>)ois.readObject();

            ois.close();
            fis.close();
        } catch(Exception e) {}

        ArrayList<GeodeticPoint> allGPs = new ArrayList<>();
        Map<GeodeticPoint, Double> initialCovPointRewards = covPointRewards.get(0.0);
        allGPs.addAll(initialCovPointRewards.keySet());
        Set<GndStation> nenStations = initializeNEN(earthShape);


        HashMap<Satellite, Map<TopocentricFrame, TimeIntervalArray>> satelliteGPContacts = new HashMap<>();
        ArrayList<GeodeticPoint> possibleGPs = new ArrayList<>();
        HashMap<Satellite,TimeIntervalArray> downlinkOpps = new HashMap<>();
        for (Satellite imager : imagers) {
            GroundEventAnalyzer gpContacts = coverageBySatellite(imager, duration, initialCovPointRewards.keySet(), startDate);
            Map<Satellite, Set<GndStation>> satelliteSetMap = new HashMap<>();
            satelliteSetMap.put(imager,nenStations);
            HashMap<GndStation,TimeIntervalArray> downlinks = downlinksBySatellite(satelliteSetMap,duration,startDate);
            TimeIntervalMerger merger = new TimeIntervalMerger(downlinks.values());
            TimeIntervalArray mergedDownlinks = merger.orCombine();
            downlinkOpps.put(imager, mergedDownlinks);
            System.out.println(Arrays.toString(mergedDownlinks.getRiseAndSetTimesList()));
            Map<TopocentricFrame, TimeIntervalArray> gpAccesses = gpContacts.getEvents();
            Map<TopocentricFrame, TimeIntervalArray> sortedGPAccesses = sortAccesses(gpAccesses, duration);
            for(TopocentricFrame tf : sortedGPAccesses.keySet()) {
                possibleGPs.add(tf.getPoint());
            }
            satelliteGPContacts.put(imager, sortedGPAccesses);
        }
        OrekitConfig.end();
        ArrayList<ArrayList<Observation>> individualPlans = new ArrayList<>();
        ArrayList<Observation> allObservations = new ArrayList<>();

        for (Satellite imager : imagers) {
            Map<TopocentricFrame, TimeIntervalArray> sortedGPAccesses = satelliteGPContacts.get(imager);
            TimeIntervalArray downlinks = downlinkOpps.get(imager);
            Collection<Record<String>> groundTrack = getGroundTrack(imager.getOrbit(),duration,startDate);
            SMDPPlanner smdpPlanner = new SMDPPlanner(imager,sortedGPAccesses,downlinks,startDate,covPointRewards,groundTrack,duration);
            ArrayList<Observation> smdpOutput = smdpPlanner.getResults();
            //ArrayList<Observation> planOutput = greedyPlanner(imager,sortedGPAccesses,downlinks,startDate,covPointRewards,groundTrack,);
            //ArrayList<Observation> planOutput = smarterGreedyPlanner(imager,sortedGPAccesses,downlinks,startDate,covPointRewards,groundTrack,duration);
            //ArrayList<Observation> planOutput = nadirEval(imager,sortedGPAccesses,downlinks,startDate,covPointRewards,groundTrack);
            individualPlans.add(smdpOutput);
            allObservations.addAll(smdpOutput);
        }
        ArrayList<GeodeticPoint> observedGPs = new ArrayList<>();
        for(Observation obs : allObservations) {
            observedGPs.add(obs.getObservationPoint());
            System.out.println(obs.toString());
        }
        processGroundTracks(imagers,greedyPlannerFilePath+"groundtracks.shp",startDate,duration);
        processGPs(observedGPs,greedyPlannerFilePath+"observed.shp");
        processGPs(possibleGPs,greedyPlannerFilePath+"possible.shp");
        processGPs(allGPs,greedyPlannerFilePath+"all.shp");
        processGPs(updatedPoints,greedyPlannerFilePath+"rainupdated.shp");
        Map<GeodeticPoint,Double> rainPoints = loadRainfallData();
        processRain(rainPoints,greedyPlannerFilePath+"rainfall.shp");

        plotResults(greedyPlannerFilePath);
    }

    private static Set<GndStation> initializeNEN(BodyShape earthShape) {
        Set<GndStation> gs = new HashSet<>();
        ReceiverAntenna singaporeRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna singaporeTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint singapore = new GeodeticPoint(Math.toRadians(1),Math.toRadians(103),0.0);
        TopocentricFrame singaporeTF = new TopocentricFrame(earthShape,singapore,"Singapore NEN Station");
        GndStation singaporeGS = new GndStation(singaporeTF,singaporeRX,singaporeTX,Math.toRadians(10.0));
        gs.add(singaporeGS);
        ReceiverAntenna svalbardRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna svalbardTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint svalbard = new GeodeticPoint(Math.toRadians(78),Math.toRadians(15),0.0);
        TopocentricFrame svalbardTF = new TopocentricFrame(earthShape,svalbard,"Svalbard NEN Station");
        GndStation svalbardGS = new GndStation(svalbardTF,svalbardRX,svalbardTX,Math.toRadians(10.0));
        gs.add(svalbardGS);
        ReceiverAntenna trollsatRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna trollsatTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint trollsat = new GeodeticPoint(Math.toRadians(-72),Math.toRadians(2),0.0);
        TopocentricFrame trollsatTF = new TopocentricFrame(earthShape,trollsat,"Trollsat NEN Station");
        GndStation trollsatGS = new GndStation(trollsatTF,trollsatRX,trollsatTX,Math.toRadians(10.0));
        gs.add(trollsatGS);
        return gs;

    }

    public static ArrayList<Observation> greedyPlanner(Satellite satellite, Map<TopocentricFrame, TimeIntervalArray> sortedAccesses, TimeIntervalArray downlinks, AbsoluteDate startDate, Map<Double,Map<GeodeticPoint,Double>> covPointRewards, Collection<Record<String>> groundTrack) {
        double currentTime = 0;
        ArrayList<Observation> observations = new ArrayList<>();
        ArrayList<TopocentricFrame> recentTFs = new ArrayList<>();
        double maxTorque = 4e-3; // Nm, BCT XACT 4e-3
        double lastAngle = 0.0;
        double energy = 10*3600; // 10 Wh to Ws
        double energyLimit = energy * 0.7; // 70% of max
        Map<GeodeticPoint,Double> latestRewardGrid = covPointRewards.get(0.0);
        double totalreward = 0.0;
        while(true) {
            int count = 1;
            double maximum = 0;
            TopocentricFrame bestTF = null;
            double bestRiseTime = Float.POSITIVE_INFINITY;
            double bestSetTime = Float.POSITIVE_INFINITY;
            for(int r = 0; r < downlinks.getRiseAndSetTimesList().length; r=r+2) {
                if(currentTime >= downlinks.getRiseAndSetTimesList()[r] && currentTime < downlinks.getRiseAndSetTimesList()[r+1]) {
                    currentTime = downlinks.getRiseAndSetTimesList()[r+1];
                    for(Double time : covPointRewards.keySet()) {
                        if(currentTime > time) {
                            latestRewardGrid = covPointRewards.get(time);
                            break;
                        }
                    }
                    break;
                }
            }
            for(TopocentricFrame tf : sortedAccesses.keySet()) {
                double riseTime = sortedAccesses.get(tf).getRiseAndSetTimesList()[0];
                double setTime = sortedAccesses.get(tf).getRiseAndSetTimesList()[1];
                boolean recent = false;
                for(TopocentricFrame rtf : recentTFs) {
                    if (rtf == tf) {
                        recent = true;
                    }
                }
                if(setTime <= currentTime || recent) {
                    continue;
                }
                double newAngle = getIncidenceAngle(tf.getPoint(),riseTime,setTime,startDate,satellite,groundTrack);
                double slewTorque = 4*Math.abs(newAngle-lastAngle)*0.05/Math.pow(Math.abs(currentTime-riseTime),2);
                double slewEnergy = slewTorque * 200 * Math.abs(currentTime-riseTime);
                double energyGain = 5 * Math.abs(currentTime-setTime); // 5 W solar panels
                double idleEnergyLoss = 4 * Math.abs(currentTime-setTime); // 4 W idle power
                double imagingEnergy = 5 * (setTime-riseTime); // 2 W camera on
                double energyChange = energyGain - idleEnergyLoss - imagingEnergy - slewEnergy;
                if(slewTorque > maxTorque) {
                    System.out.println("Can't slew! Last angle: "+lastAngle+", new angle: "+newAngle+". Last time: "+currentTime+", new time: "+setTime);
                    System.out.println("Torque required: "+slewTorque);
                    continue;
                }
                if(riseTime <= currentTime) {
                    riseTime = currentTime;
                }
                if(energy+energyChange < energyLimit) {
                    continue;
                }
                double reward = rewardFunction(tf,getIncidenceAngle(tf.getPoint(),riseTime,setTime,startDate,satellite,groundTrack),latestRewardGrid);
                if(reward > maximum) {
                    bestTF = tf;
                    maximum = reward;
                    bestRiseTime = riseTime;
                    bestSetTime = setTime;
                    lastAngle = newAngle;
                    energy = energy+energyChange;
                }
                count++;
                if(count > 1) { // ADJUST THIS TO SET "GREEDINESS"
                    break;
                }
            }
            if(bestTF == null) {
                break;
            }
            if (recentTFs.size() >= 10) {
                recentTFs.remove(0);
            }
            recentTFs.add(bestTF);
            totalreward = totalreward + maximum;
            Observation obs = new Observation(bestTF.getPoint(),bestRiseTime,bestSetTime,maximum);
            observations.add(obs);
            currentTime = bestSetTime;
            System.out.println(energy/3600);
        }
        System.out.println("Total reward: "+totalreward);
        return observations;
    }
    public static ArrayList<Observation> nadirEval(Satellite satellite, Map<TopocentricFrame, TimeIntervalArray> sortedAccesses, TimeIntervalArray downlinks, AbsoluteDate startDate, Map<Double,Map<GeodeticPoint,Double>> covPointRewards, Collection<Record<String>> groundTrack) {
        // check if reward grid is getting updated properly
        double currentTime = 0;
        ArrayList<Observation> observations = new ArrayList<>();
        Map<GeodeticPoint,Double> latestRewardGrid = covPointRewards.get(0.0);
        double totalreward = 0.0;
        for(int r = 0; r < downlinks.getRiseAndSetTimesList().length; r=r+2) {
            if(currentTime >= downlinks.getRiseAndSetTimesList()[r] && currentTime < downlinks.getRiseAndSetTimesList()[r+1]) {
                currentTime = downlinks.getRiseAndSetTimesList()[r+1];
                for(Double time : covPointRewards.keySet()) {
                    if(currentTime > time) {
                        latestRewardGrid = covPointRewards.get(time);
                        break;
                    }
                }
                break;
            }
        }
        for(TopocentricFrame tf : sortedAccesses.keySet()) {
            double riseTime = sortedAccesses.get(tf).getRiseAndSetTimesList()[0];
            double setTime = sortedAccesses.get(tf).getRiseAndSetTimesList()[1];
            double reward = rewardFunction(tf,getIncidenceAngle(tf.getPoint(),riseTime,setTime,startDate,satellite,groundTrack),latestRewardGrid);
            Observation obs = new Observation(tf.getPoint(),riseTime,setTime,reward);
            observations.add(obs);
            totalreward = totalreward + reward;
        }
        System.out.println("Landsat total reward: "+totalreward);
        return observations;
    }

    public static ArrayList<Observation> smarterGreedyPlanner(Satellite satellite, Map<TopocentricFrame, TimeIntervalArray> sortedAccesses, TimeIntervalArray downlinks, AbsoluteDate startDate, Map<Double,Map<GeodeticPoint,Double>> covPointRewards, Collection<Record<String>> groundTrack, double duration) {
        double currentTime = 0;
        ArrayList<Observation> observations = new ArrayList<>();
        ArrayList<TopocentricFrame> recentTFs = new ArrayList<>();
        double maxTorque = 4e-3; // Nm, BCT XACT 4e-3
        double lastAngle = 0.0;
        double energy = 10*3600; // 10 Wh to Ws
        double energyLimit = energy * 0.7; // 70% of max
        double estimatedReward = 8000.0;
        double totalreward = 0.0;
        Map<GeodeticPoint,Double> latestRewardGrid = covPointRewards.get(0.0);
        while(true) {
            int count = 1;
            double maximum = 0;
            double trueReward = 0.0;
            TopocentricFrame bestTF = null;
            double bestRiseTime = Float.POSITIVE_INFINITY;
            double bestSetTime = Float.POSITIVE_INFINITY;
            for(int r = 0; r < downlinks.getRiseAndSetTimesList().length; r=r+2) {
                if(currentTime >= downlinks.getRiseAndSetTimesList()[r] && currentTime < downlinks.getRiseAndSetTimesList()[r+1]) {
                    currentTime = downlinks.getRiseAndSetTimesList()[r+1];
                    for(Double time : covPointRewards.keySet()) {
                        if(currentTime > time) {
                            latestRewardGrid = covPointRewards.get(time);
                            break;
                        }
                    }
                    break;
                }
            }
            for(TopocentricFrame tf : sortedAccesses.keySet()) {
                double riseTime = sortedAccesses.get(tf).getRiseAndSetTimesList()[0];
                double setTime = sortedAccesses.get(tf).getRiseAndSetTimesList()[1];
                boolean recent = false;
                for(TopocentricFrame rtf : recentTFs) {
                    if (rtf == tf) {
                        recent = true;
                    }
                }
                if(setTime <= currentTime || recent) {
                    continue;
                }
                double newAngle = getIncidenceAngle(tf.getPoint(),riseTime,setTime,startDate,satellite,groundTrack);
                double slewTorque = 4*Math.abs(newAngle-lastAngle)*0.05/Math.pow(Math.abs(currentTime-riseTime),2);
                double slewEnergy = slewTorque * 200 * Math.abs(currentTime-riseTime);
                double energyGain = 5 * Math.abs(currentTime-setTime); // 5 W solar panels
                double idleEnergyLoss = 4 * Math.abs(currentTime-setTime); // 4 W idle power
                double imagingEnergy = 5 * (setTime-riseTime); // 2 W camera on
                double energyChange = energyGain - idleEnergyLoss - imagingEnergy - slewEnergy;
                if(slewTorque > maxTorque) {
                    System.out.println("Can't slew! Last angle: "+lastAngle+", new angle: "+newAngle+". Last time: "+currentTime+", new time: "+setTime);
                    System.out.println("Torque required: "+slewTorque);
                    continue;
                }
                if(riseTime <= currentTime) {
                    riseTime = currentTime;
                }
                if(energy+energyChange < energyLimit) {
                    continue;
                }
                double reward = rewardFunction(tf,getIncidenceAngle(tf.getPoint(),riseTime,setTime,startDate,satellite,groundTrack),latestRewardGrid);
                double rho = (duration*24*3600-setTime)/(duration*24*3600);
                double e = Math.pow(rho,5) * estimatedReward;
                double adjustedReward = reward + e;
                if(adjustedReward > maximum) {
                    bestTF = tf;
                    maximum = adjustedReward;
                    trueReward = reward;
                    bestRiseTime = riseTime;
                    bestSetTime = setTime;
                    lastAngle = newAngle;
                    energy = energy+energyChange;
                }
                count++;
                if(count > 5) {
                    break;
                }
            }
            if(bestTF == null) {
                break;
            }
            if (recentTFs.size() >= 10) {
                recentTFs.remove(0);
            }
            recentTFs.add(bestTF);
            totalreward = totalreward + trueReward;
            Observation obs = new Observation(bestTF.getPoint(),bestRiseTime,bestSetTime,trueReward);
            observations.add(obs);
            currentTime = bestSetTime;
            System.out.println(energy/3600);
        }
        System.out.println("Total reward: "+totalreward);
        return observations;
    }

    public static double rewardFunction(TopocentricFrame point, double incidenceAngle, Map<GeodeticPoint,Double> covPointRewards){
        GeodeticPoint gp = point.getPoint();
        double score = covPointRewards.get(gp);
        return score*(1-incidenceAngle);
    }

    public static Map<Double,Map<GeodeticPoint,Double>> loadCoveragePoints() {

        // Loading river and lake constant scores
        Map<GeodeticPoint,Double> pointRewards = new HashMap<>();
        ArrayList<GeodeticPoint> updates = new ArrayList<>();
        List<List<String>> riverRecords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("./orekit/src/main/java/seakers/orekit/overlap/grwl_river_output.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                riverRecords.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println("Exception occurred in coverageByConstellation: "+e);
        }
        Set<GeodeticPoint> covPoints = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            double lon = Math.toRadians(parseDouble(riverRecords.get(i).get(0)));
            double lat = Math.toRadians(parseDouble(riverRecords.get(i).get(1)));
            double width = parseDouble(riverRecords.get(i).get(2));
            GeodeticPoint riverPoint = new GeodeticPoint(lat, lon, 0.0);
            pointRewards.put(riverPoint,width/5000.0/2);
        }
        List<List<String>> lakeRecords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("./orekit/src/main/java/seakers/orekit/overlap/hydrolakes.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                lakeRecords.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println("Exception occurred in coverageByConstellation: "+e);
        }
        for (int i = 1; i < 1000; i++) {
            double lat = Math.toRadians(parseDouble(lakeRecords.get(i).get(0)));
            double lon = Math.toRadians(parseDouble(lakeRecords.get(i).get(1)));
            double area = parseDouble(lakeRecords.get(i).get(2));
            GeodeticPoint lakePoint = new GeodeticPoint(lat, lon, 0.0);
            pointRewards.put(lakePoint,area/30000.0);
        }

        // Loading rain modifications
        String filepath = "./orekit/src/main/rain/";
        int numfiles = new File(filepath).list().length;
        Map<Double,Map<GeodeticPoint,Double>> pointRewardsOverTime = new HashMap<>();

        for (int i = 0; i < numfiles; i++) {
            int timestamp = i*30;
            double time = i*30.0;
            String timestampString = Integer.toString(timestamp);
            if(timestampString.length() == 1) {
                timestampString = "00"+timestampString;
            } else if(timestampString.length() == 2) {
                timestampString = "0"+timestampString;
            }
            Map<GeodeticPoint,Double> rainRewards = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader(filepath+"rain_"+timestampString+".csv"))) {
                String line;
                double longitude = -179.95;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    double latitude = -89.95;
                    for (int j = 0; j < values.length; j++) {
                        if (Double.parseDouble(values[j]) > 0.0) {
                            GeodeticPoint location = new GeodeticPoint(Math.toRadians(latitude),Math.toRadians(longitude),0.0);
                            rainRewards.put(location,Double.parseDouble(values[j])/10.0);
                        }
                        latitude = latitude + 0.1;
                    }
                    longitude = longitude + 0.1;
                }
            } catch (Exception e) {
                System.out.println("Exception in loadCoveragePoints: " + e);
            }
            Map<GeodeticPoint,Double> updatedPoints = new HashMap<>();
            for (GeodeticPoint gp : pointRewards.keySet()) {
                double reward = pointRewards.get(gp);
                for (GeodeticPoint rp : rainRewards.keySet()) {
                    double dist = Math.sqrt(Math.pow(gp.getLatitude() - rp.getLatitude(), 2)+Math.pow(gp.getLongitude() - rp.getLongitude(),2));
                    if(Math.toDegrees(dist) < 0.15) {
                        reward = reward + rainRewards.get(rp);
                        if(!updates.contains(gp)) {
                            updates.add(gp);
                        }
                    }
                }
                updatedPoints.put(gp,reward);
            }
            System.out.println("done with one time");
            pointRewardsOverTime.put(time,updatedPoints);
        }
        try {
            File file = new File("coverageRewardsUnweighted");
            FileOutputStream fos=new FileOutputStream(file);
            ObjectOutputStream oos=new ObjectOutputStream(fos);

            oos.writeObject(pointRewardsOverTime);
            oos.flush();
            oos.close();
            fos.close();
        } catch (Exception e) {}
        try {
            File file = new File("updatedPoints");
            FileOutputStream fos=new FileOutputStream(file);
            ObjectOutputStream oos=new ObjectOutputStream(fos);

            oos.writeObject(updates);
            oos.flush();
            oos.close();
            fos.close();
        } catch (Exception e) {}
        return pointRewardsOverTime;


//        Map<Double,Map<GeodeticPoint,Double>> pointRewardsOverTime = new HashMap<>();
//        for (int j = 0; j < 100; j++) {
//            Double time = j*duration;
//            Map<GeodeticPoint,Double> randomWeightChange = new HashMap<>();
//            for (GeodeticPoint gp : pointRewards.keySet()) {
//                randomWeightChange.put(gp,pointRewards.get(gp)+Math.random());
//            }
//            pointRewardsOverTime.put(time,randomWeightChange);
//        }
//        return pointRewardsOverTime;
    }

    public static Map<GeodeticPoint,Double> loadRainfallData() {
        Map<GeodeticPoint,Double> rainOverTime = new HashMap<>();
        String filepath = "./orekit/src/main/rain/";
        int numfiles = new File(filepath).list().length;

        for (int i = 0; i < numfiles; i++) {
            int timestamp = i*30;
            double time = i*30.0;
            String timestampString = Integer.toString(timestamp);
            if(timestampString.length() == 1) {
                timestampString = "00"+timestampString;
            } else if(timestampString.length() == 2) {
                timestampString = "0"+timestampString;
            }
            try (BufferedReader br = new BufferedReader(new FileReader(filepath+"rain_"+timestampString+".csv"))) {
                String line;
                double longitude = -179.95;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    double latitude = -89.95;
                    for (int j = 0; j < values.length; j++) {
                        if (Double.parseDouble(values[j]) > 0.0) {
                            GeodeticPoint location = new GeodeticPoint(Math.toRadians(latitude),Math.toRadians(longitude),0.0);
                            if(!rainOverTime.containsKey(location)) {
                                rainOverTime.put(location,Double.parseDouble(values[j]));
                            } else {
                                rainOverTime.put(location,rainOverTime.get(location)+Double.parseDouble(values[j]));
                            }
                        }
                        latitude = latitude + 0.1;
                    }
                    longitude = longitude + 0.1;
                }
            } catch (Exception e) {
                System.out.println("Exception in loadCoveragePoints: " + e);
            }
        }
        return rainOverTime;
    }

    public static double getIncidenceAngle(GeodeticPoint point, double riseTime, double setTime, AbsoluteDate startDate, Satellite satellite, Collection<Record<String>> groundTrack) {
        Map<Double, GeodeticPoint> sspMap = new HashMap<>();
        for (Record<String> ind : groundTrack) {
            String rawString = ind.getValue();
            AbsoluteDate date = ind.getDate();
            String[] splitString = rawString.split(",");
            double latitude = Double.parseDouble(splitString[0]);
            double longitude = Double.parseDouble(splitString[1]);
            GeodeticPoint ssp = new GeodeticPoint(Math.toRadians(latitude),Math.toRadians(longitude),0);
            double elapsedTime = date.durationFrom(startDate);
            sspMap.put(elapsedTime,ssp);
        }
        double[] times = linspace(riseTime,setTime,(int)(setTime-riseTime));
        if(times.length<2) {
            times = new double[]{riseTime, setTime};
        }

        double closestDist = 100000000000000000.0;
        for (double time : times) {
            double closestTime = 100 * 24 * 3600; // 100 days
            GeodeticPoint closestPoint = null;
            for (Double sspTime : sspMap.keySet()) {
                if (Math.abs(sspTime - time) < closestTime) {
                    closestTime = Math.abs(sspTime - time);
                    closestPoint = sspMap.get(sspTime);
                    double dist = Math.sqrt(Math.pow(LLAtoECI(closestPoint)[0] - LLAtoECI(point)[0], 2) + Math.pow(LLAtoECI(closestPoint)[1] - LLAtoECI(point)[1], 2) + Math.pow(LLAtoECI(closestPoint)[2] - LLAtoECI(point)[2], 2));
                    if (dist < closestDist) {
                        closestDist = dist;
                    }
                }
            }
        }
        return Math.atan2(closestDist,(satellite.getOrbit().getA()-6370000)/1000);
    }

    public static double[] LLAtoECI(GeodeticPoint point) {
        double re = 6370;
        double x = re * Math.cos(point.getLatitude()) * Math.cos(point.getLongitude());
        double y = re * Math.cos(point.getLatitude()) * Math.sin(point.getLongitude());
        double z = re * Math.sin(point.getLatitude());
        double[] result = {x,y,z};
        return result;
    }

    public static Map<TopocentricFrame, TimeIntervalArray> sortAccesses(Map<TopocentricFrame, TimeIntervalArray> gpAccesses, double duration) {
        Map<TopocentricFrame, TimeIntervalArray> sortedMap = new LinkedHashMap<>();
        gpAccesses.values().removeIf(TimeIntervalArray::isEmpty);
        while(!gpAccesses.isEmpty()) {
            TopocentricFrame bestTF = null;
            TimeIntervalArray bestTIA = null;
            double earliestTime = duration*3600*24;
            for (TopocentricFrame tf : gpAccesses.keySet()) {
                TimeIntervalArray tia = gpAccesses.get(tf);
                double[] tia_raslist = tia.getRiseAndSetTimesList();
                if(tia_raslist[0] < earliestTime) {
                    earliestTime = tia_raslist[0];
                    bestTF = tf;
                    bestTIA = tia;
                }
            }
            if(bestTF == null) {
                break;
            }
            sortedMap.put(bestTF,bestTIA);
            gpAccesses.remove(bestTF);
        }
        return sortedMap;
    }
    public static Collection<Record<String>> getGroundTrack(Orbit orbit, double duration, AbsoluteDate startDate) {
        OrekitConfig.init(1);
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate endDate = startDate.shiftedBy(duration*86400);

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
        OrekitConfig.end();
        return gta.getHistory();
    }

    public static void processGroundTracks(ArrayList<Satellite> satellites, String filepath, AbsoluteDate startDate, double duration) {
        try {

            final SimpleFeatureType TYPE =
                    DataUtilities.createType(
                            "Location",
                            "the_geom:Point:srid=4326,"
                    );
            List<SimpleFeature> features = new ArrayList<>();
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
            for (Satellite satellite : satellites) {
                Collection<Record<String>> coll = getGroundTrack(satellite.getOrbit(), duration, startDate);
                for (Record<String> ind : coll) {
                    String rawString = ind.getValue();
                    String[] splitString = rawString.split(",");
                    double latitude = Double.parseDouble(splitString[0]);
                    double longitude = Double.parseDouble(splitString[1]);
                    Point point = geometryFactory.createPoint(new Coordinate(latitude, longitude));
                    featureBuilder.add(point);
                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    features.add(feature);
                }
            }
            File newFile = getFile(new File(filepath));
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", newFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Uh oh");
        }
    }

    public static void processGPs(ArrayList<GeodeticPoint> gps, String filepath) {
        try {

            final SimpleFeatureType TYPE =
                    DataUtilities.createType(
                            "Location",
                            "the_geom:Point:srid=4326,"
                    );
            List<SimpleFeature> features = new ArrayList<>();
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
            for (GeodeticPoint gp : gps) {
                Point point = geometryFactory.createPoint(new Coordinate(FastMath.toDegrees(gp.getLatitude()), FastMath.toDegrees(gp.getLongitude())));
                featureBuilder.add(point);
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }
            File newFile = getFile(new File(filepath));
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", newFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Uh oh");
        }
    }
    public static void processRain(Map<GeodeticPoint,Double> gps, String filepath) {
        try {

            final SimpleFeatureType TYPE =
                    DataUtilities.createType(
                            "Location",
                            "the_geom:Point:srid=4326,"
                    );
            List<SimpleFeature> features = new ArrayList<>();
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
            for (GeodeticPoint gp : gps.keySet()) {
                Coordinate leftUpper = new Coordinate(gp.getLatitude()+0.0025,gp.getLongitude()-0.0025,0.0);
                Coordinate rightUpper = new Coordinate(gp.getLatitude()+0.0025,gp.getLongitude()+0.0025,0.0);
                Coordinate leftLower = new Coordinate(gp.getLatitude()-0.0025,gp.getLongitude()-0.0025,0.0);
                Coordinate rightLower = new Coordinate(gp.getLatitude()-0.0025,gp.getLongitude()+0.0025,0.0);
                Coordinate[] polygonPoints = {leftUpper,rightUpper,leftLower,rightLower};
                Polygon poly = geometryFactory.createPolygon(polygonPoints);
                featureBuilder.add(poly);
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }
            File newFile = getFile(new File(filepath));
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", newFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Uh oh");
        }
    }

    public static void plotResults(String overlapFilePath) {
        try {
            MapContent map = new MapContent();
            map.setTitle("Test");

            File countries_file = getFile(overlapFilePath+"50m_cultural/ne_50m_admin_0_countries_lakes.shp");
            FileDataStore countries_store = FileDataStoreFinder.getDataStore(countries_file);
            SimpleFeatureSource countriesSource = countries_store.getFeatureSource();
            Style country_style = SLD.createPolygonStyle(Color.BLACK,null,1.0f);
            Layer country_layer = new FeatureLayer(countriesSource, country_style);
            map.addLayer(country_layer);

            File groundtrack_file = getFile(overlapFilePath+"groundtracks.shp");
            FileDataStore imag_groundtrack_store = FileDataStoreFinder.getDataStore(groundtrack_file);
            SimpleFeatureSource imag_groundtrackSource = imag_groundtrack_store.getFeatureSource();
            Style imag_style = SLD.createPointStyle("Square",Color.GREEN,Color.GREEN,0.5f,6.0f);
            Layer imag_altimeter_track_layer = new FeatureLayer(imag_groundtrackSource, imag_style);
            map.addLayer(imag_altimeter_track_layer);

            File rivers_file = getFile(overlapFilePath+"GRWL_summaryStats.shp");
            FileDataStore rivers_store = FileDataStoreFinder.getDataStore(rivers_file);
            SimpleFeatureSource riversSource = rivers_store.getFeatureSource();
            Style river_style = SLD.createPolygonStyle(Color.BLACK,null,1.0f);
            Layer river_layer = new FeatureLayer(riversSource, river_style);
            map.addLayer(river_layer);

            map.addLayer(generatePointLayer(overlapFilePath+"all.shp",Color.YELLOW,0.5f,12.0f));
            map.addLayer(generatePointLayer(overlapFilePath+"possible.shp",Color.ORANGE,0.5f,12.0f));
            map.addLayer(generatePointLayer(overlapFilePath+"observed.shp",Color.RED,0.5f,12.0f));
            map.addLayer(generatePointLayer(overlapFilePath+"rainupdated.shp",Color.BLUE,0.3f,6.0f));
//            File rasterFile = new File("./rainfall.hdf5");
//            AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
//            // this is a bit hacky but does make more geotiffs work
//            GridCoverage2DReader reader = format.getReader(rasterFile);
//            Style rasterStyle = createGreyscaleStyle(1);
//            Layer rasterLayer = new GridReaderLayer(reader, rasterStyle);
//            map.addLayer(rasterLayer);

            JMapFrame.showMap(map);
            saveImage(map, "planner_map.jpeg",3000);
        } catch (Exception e) {
            System.out.println("Exception occurred in plotResults: "+e);
        }
    }

    public static Style createGreyscaleStyle(int band) {
        StyleFactory sf = CommonFactoryFinder.getStyleFactory();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
        SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(band), ce);

        RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
        ChannelSelection sel = sf.channelSelection(sct);
        sym.setChannelSelection(sel);

        return SLD.wrapSymbolizers(sym);
    }

    public static Layer generatePointLayer(String filepath, Color color, float opacity, float size) {
        try{
            File file = getFile(filepath);
            FileDataStore fds = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource sfSource = fds.getFeatureSource();
            Style style = SLD.createPointStyle("Circle",color,color,opacity,size);
            Layer layer = new FeatureLayer(sfSource,style);
            return layer;
        } catch (Exception e) {
            System.out.println("Exception occurred in generateLayer: "+e);
            return null;
        }
    }
    public static Layer generateHeatLayer(String filepath, Color fillColor, Color outlineColor, float opacity) {
        try{
            File file = getFile(filepath);
            FileDataStore fds = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource sfSource = fds.getFeatureSource();
            Style style = SLD.createPolygonStyle(outlineColor,fillColor,opacity);
            Layer layer = new FeatureLayer(sfSource,style);
            return layer;
        } catch (Exception e) {
            System.out.println("Exception occurred in generateLayer: "+e);
            return null;
        }
    }

    public static GroundEventAnalyzer coverageBySatellite(Satellite satellite, double durationDays, Collection<GeodeticPoint> covPoints, AbsoluteDate startDate) {
        long start = System.nanoTime();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate endDate = startDate.shiftedBy(durationDays*86400);
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2);

//        Set<GeodeticPoint> subSet = landPoints.stream()
//                // .skip(10) // Use this to get elements later in the stream
//                .limit(5000)
//                .collect(toCollection(LinkedHashSet::new));
        //create a coverage definition
        CoverageDefinition covDef = new CoverageDefinition("covdef1", covPoints, earthShape);
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
        long end = System.nanoTime();
        System.out.printf("coverageBySatellite took %.4f sec\n", (end - start) / Math.pow(10, 9));
        return gea;
    }

    public static HashMap<GndStation, TimeIntervalArray> downlinksBySatellite(Map<Satellite,Set<GndStation>> satelliteMap, double durationDays, AbsoluteDate startDate) {
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

    public static double arraySum(double[] array) {
        double sum = 0;
        for (double value : array) {
            sum += value;
        }
        return sum;
    }
    public static void saveImage(final MapContent map, final String file, final int imageWidth) {

        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(map);

        Rectangle imageBounds = null;
        ReferencedEnvelope mapBounds = null;
        try {
            mapBounds = map.getMaxBounds();
            double heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0);
            imageBounds = new Rectangle(
                    0, 0, imageWidth, (int) Math.round(imageWidth * heightToWidth));

        } catch (Exception e) {
            // failed to access map layers
            throw new RuntimeException(e);
        }

        BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);

        Graphics2D gr = image.createGraphics();
        gr.setPaint(Color.WHITE);
        gr.fill(imageBounds);

        try {
            renderer.paint(gr, imageBounds, mapBounds);
            File fileToSave = new File(file);
            ImageIO.write(image, "jpeg", fileToSave);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static double[] linspace(double min, double max, int points) {
	    double[] d = new double[points];
	    for (int i = 0; i < points; i++){
	        d[i] = min + i * (max - min) / (points - 1);
	    }
	    return d;
	}
}
