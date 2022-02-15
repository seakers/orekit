package seakers.orekit;

import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.Satellite;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Planner {
    private String filepath;
    public Map<String, Map<String, TimeIntervalArray>> crosslinkEvents;
    public Map<String, TimeIntervalArray> downlinkEvents;
    public Map<String, ArrayList<Observation>> observationEvents;

    public Planner() {
        filepath = "./src/test/plannerData";
        loadCrosslinks();
        loadDownlinks();
        loadObservations();

        System.out.println("Done!");
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
                            System.out.println("Common access!");
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


}
