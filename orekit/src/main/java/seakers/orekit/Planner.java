package seakers.orekit;

import org.orekit.bodies.GeodeticPoint;
import seakers.orekit.SMDP.SatelliteAction;
import seakers.orekit.SMDP.SatelliteState;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.Satellite;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Planner {
    private String filepath;
    public Map<String, Map<String, TimeIntervalArray>> crosslinkEvents;
    public Map<String, TimeIntervalArray> downlinkEvents;
    public Map<String, ArrayList<Observation>> observationEvents;
    public Map<String, ArrayList<SatelliteAction>> currentPlans;
    public Map<String, ArrayList<SatelliteAction>> actionsTaken;
    public Map<String, Map<GeodeticPoint, Double>> localRewardGrids;
    public Map<GeodeticPoint,Double> globalRewardGrid;
    public Map<String, SatelliteState> currentStates;

    public Planner() {
        filepath = "./src/test/plannerData";
        loadCrosslinks();
        loadDownlinks();
        loadObservations();
        loadRewardGrid();
        localRewardGrids = new HashMap<>();
        actionsTaken = new HashMap<>();
        currentPlans = new HashMap<>();
        currentStates = new HashMap<>();
        for (String sat : downlinkEvents.keySet()) {
            localRewardGrids.put(sat,globalRewardGrid);
            actionsTaken.put(sat,new ArrayList<>());
            SatelliteState satelliteState = new SatelliteState(0,0, new ArrayList<>(),70.0,0.0,0.0,0.0);
            currentStates.put(sat,satelliteState);
            MCTSPlanner mctsPlanner = new MCTSPlanner(observationEvents.get(sat),downlinkEvents.get(sat),crosslinkEvents.get(sat),localRewardGrids.get(sat),currentStates.get(sat));
            ArrayList<SatelliteAction> results = mctsPlanner.getResults();
            currentPlans.put(sat,results);
            //System.out.println("Initial plans for satellite "+sat+": ");
            //System.out.println(results);
        }
        double currentTime = 0.0;

        String planFlag = "";
        String replanSat = "";
        while (currentTime < 8640.0) {
            double earliestStopTime = 8640.0;
            for (String sat : downlinkEvents.keySet()) {
                PlanExecutor planExec = new PlanExecutor(currentStates.get(sat),currentTime,earliestStopTime,currentPlans.get(sat));
                double planTerminationTime = planExec.getStopTime();
                if(planTerminationTime < earliestStopTime) {
                    earliestStopTime = planTerminationTime;
                    planFlag = planExec.getReplanFlag();
                    replanSat = sat;
                }
            }
            for (String sat : downlinkEvents.keySet()) {
                PlanExecutor planExec = new PlanExecutor(currentStates.get(sat),currentTime,earliestStopTime,currentPlans.get(sat));
                ArrayList<SatelliteAction> actionsSoFar = actionsTaken.get(sat);
                actionsSoFar.addAll(planExec.getActionsTaken());
                actionsTaken.put(sat,actionsSoFar);
                currentStates.put(sat,planExec.getReturnState());
                if(Objects.equals(sat, replanSat)) {
                    if (planFlag.equals("downlink")) {
                        updateGlobalRewardGrid(planExec.getRewardGridUpdates());
                        localRewardGrids.put(sat,globalRewardGrid);
                        MCTSPlanner mctsPlanner = new MCTSPlanner(observationEvents.get(sat),downlinkEvents.get(sat),crosslinkEvents.get(sat),localRewardGrids.get(sat),currentStates.get(sat));
                        ArrayList<SatelliteAction> results = mctsPlanner.getResults();
                        currentPlans.put(sat,results);
                    } else if (planFlag.equals("image")) {
                        updateLocalRewardGrid(sat,planExec.getRewardGridUpdates());
                        MCTSPlanner mctsPlanner = new MCTSPlanner(observationEvents.get(sat),downlinkEvents.get(sat),crosslinkEvents.get(sat),localRewardGrids.get(sat),currentStates.get(sat));
                        ArrayList<SatelliteAction> results = mctsPlanner.getResults();
                        currentPlans.put(sat,results);
                    } else {
                        String crosslinkSat = "satellite"+planFlag;
                        updateLocalRewardGrid(crosslinkSat,planExec.getRewardGridUpdates());
                        updateLocalRewardGrid(sat,planExec.getRewardGridUpdates());
                        MCTSPlanner mctsPlanner = new MCTSPlanner(observationEvents.get(sat),downlinkEvents.get(sat),crosslinkEvents.get(sat),localRewardGrids.get(sat),currentStates.get(sat));
                        ArrayList<SatelliteAction> results = mctsPlanner.getResults();
                        currentPlans.put(sat,results);
                        MCTSPlanner crosslinkSatPlanner = new MCTSPlanner(observationEvents.get(crosslinkSat),downlinkEvents.get(crosslinkSat),crosslinkEvents.get(crosslinkSat),localRewardGrids.get(crosslinkSat),currentStates.get(crosslinkSat));
                        ArrayList<SatelliteAction> crosslinkResults = crosslinkSatPlanner.getResults();
                        currentPlans.put(crosslinkSat,crosslinkResults);
                    }
                }
            }
            currentTime = earliestStopTime;
        }
        System.out.println("Done!");
        for (String sat : downlinkEvents.keySet()) {
            System.out.println("Actions taken for satellite "+sat+": ");
            System.out.println(actionsTaken.get(sat));
        }
    }

    public void updateGlobalRewardGrid(Map<GeodeticPoint,Double> updates) {
        for(GeodeticPoint gp : updates.keySet()) {
            globalRewardGrid.put(gp,updates.get(gp));
        }
    }

    public void updateLocalRewardGrid(String sat, Map<GeodeticPoint,Double> updates) {
        Map<GeodeticPoint,Double> oldRewardGrid = localRewardGrids.get(sat);
        for(GeodeticPoint gp : updates.keySet()) {
            oldRewardGrid.put(gp,updates.get(gp));
        }
        localRewardGrids.put(sat,oldRewardGrid);
    }

    public void loadCrosslinks() {
        File directory = new File(filepath);
        Map<String, Map<String, TimeIntervalArray>> cl = new HashMap<>();
        Map<String, TimeIntervalArray> loadedIntervals = new HashMap<>();
        try {
            for(int i = 0; i < directory.list().length-1; i++) {
                FileInputStream fi = new FileInputStream(new File(filepath+"/satellite"+i+"/crosslinks.dat"));
                ObjectInputStream oi = new ObjectInputStream(fi);

                TimeIntervalArray crosslinkIntervals =  (TimeIntervalArray) oi.readObject();
                String satelliteName = "satellite"+i;
                loadedIntervals.put(satelliteName,crosslinkIntervals);

                oi.close();
                fi.close();
            }
        } catch (Exception e) {
            System.out.println("Exception in loadCrosslinks: "+e);
        }
        for (String s : loadedIntervals.keySet()) {
            Map<String, TimeIntervalArray> satelliteEvents = new HashMap<>();
            for (String q : loadedIntervals.keySet()) {
                if(q.equals(s)) {
                    continue;
                }
                double[] primary = loadedIntervals.get(s).getRiseAndSetTimesList();
                double[] secondary = loadedIntervals.get(q).getRiseAndSetTimesList();
                for (int i = 0; i < primary.length-1; i++) {
                    for(int j = 0; j < secondary.length-1; j++) {
                        if(primary[i] == secondary[j] && primary[i+1] == secondary[j+1]) {
                            TimeIntervalArray tia = new TimeIntervalArray(loadedIntervals.get(s).getHead(),loadedIntervals.get(s).getTail());
                            tia.addRiseTime(primary[i]);
                            tia.addSetTime(primary[i+1]);
                            satelliteEvents.put(q,tia);
                        }
                    }
                }
            }
            cl.put(s,satelliteEvents);
        }
        crosslinkEvents = cl;
    }

    public void loadDownlinks() {
        File directory = new File(filepath);
        Map<String, TimeIntervalArray> dl = new HashMap<>();
        try{
            for(int i = 0; i < directory.list().length-1; i++) {
                FileInputStream fi = new FileInputStream(new File(filepath+"/satellite"+i+"/downlinks.dat"));
                ObjectInputStream oi = new ObjectInputStream(fi);

                TimeIntervalArray downlinkIntervals =  (TimeIntervalArray) oi.readObject();
                String satelliteName = "satellite"+i;
                dl.put(satelliteName,downlinkIntervals);

                oi.close();
                fi.close();
            }
        } catch (Exception e) {
            System.out.println("Exception in loadDownlinks: "+e.getMessage());
        }
        downlinkEvents = dl;

    }

    public void loadObservations() {
        File directory = new File(filepath);
        Map<String, ArrayList<Observation>> obs = new HashMap<>();
        try{
            for(int i = 0; i < directory.list().length-1; i++) {
                FileInputStream fi = new FileInputStream(new File(filepath+"/satellite"+i+"/observations.dat"));
                ObjectInputStream oi = new ObjectInputStream(fi);

                ArrayList<Observation> observations =  (ArrayList<Observation>) oi.readObject();
                String satelliteName = "satellite"+i;
                obs.put(satelliteName,observations);

                oi.close();
                fi.close();
            }
        } catch (Exception e) {
            System.out.println("Exception in loadObservations: "+e.getMessage());
        }
        observationEvents = obs;
    }

    public void loadRewardGrid() {
        File directory = new File(filepath);
        try{
            for(int i = 0; i < directory.list().length-1; i++) {
                FileInputStream fi = new FileInputStream(new File(filepath+"/coveragePoints.dat"));
                ObjectInputStream oi = new ObjectInputStream(fi);

                globalRewardGrid =  (Map<GeodeticPoint,Double>) oi.readObject();

                oi.close();
                fi.close();
            }
        } catch (Exception e) {
            System.out.println("Exception in loadObservations: "+e.getMessage());
        }
    }


}
