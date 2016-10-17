/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import orekit.coverage.access.RiseSetTime;
import orekit.coverage.access.TimeIntervalArray;
import orekit.ephemeris.Ephemeris;
import orekit.ephemeris.EphemerisHistory;
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Satellite;

/**
 * Saves and loads scenarios
 *
 * @author nozomihitomi
 */
public class ScenarioIO {

    /**
     * Saves the scenario in a desired directory. If the scenario is a
     * Subscenario, it will have the extension .subscen. If the scenario is a
     * Scenario, it will have the extension .scen
     *
     * @param path to the directory to save the file
     * @param filename name of the file without the extension
     * @param scenario Scenario object to save
     * @return
     */
    public static boolean save(Path path, String filename, Scenario scenario) {
        File file;
        if (scenario instanceof SubScenario) {
            file = new File(path.toFile(), filename + ".subscen");
        } else {
            file = new File(path.toFile(), filename + ".scen");
        }
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));) {
            os.writeObject(scenario);
            os.close();
        } catch (IOException ex) {
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        System.out.println("Saved scenario in " + file.toString());
        return true;
    }

    /**
     * Saves the accesses of the coverage definition from the scenario in a
     * desired directory
     *
     * @param path to the directory to save the file
     * @param filename name of the file without the extension
     * @param scenario Scenario that was simulated
     * @param covdef the coverage definition of interest
     * @return
     */
    public static boolean saveAccess(Path path, String filename, Scenario scenario, CoverageDefinition covdef) {
        File file = new File(path.toFile(), filename + "_" + covdef.getName() + ".cva");
        HashMap<CoveragePoint, TimeIntervalArray> cvaa = scenario.getMergedAccesses(covdef);
        try (FileWriter fw = new FileWriter(file)) {
            fw.append(String.format("EpochTime: %s\n\n", scenario.getStartDate()));
            fw.append("Assigned Constellations:\n");
            for (Constellation constel : covdef.getConstellations()) {
                fw.append(String.format("\tConstelation %s: %d satellites\n", constel.getName(), constel.getSatellites().size()));
                for (Satellite sat : constel.getSatellites()) {
                    fw.append(String.format("\t\tSatellite %s\n", sat.toString()));
                }
            }
            fw.flush();

            int i = 0;
            ArrayList<CoveragePoint> girdPoints = new ArrayList(cvaa.keySet());
            Collections.sort(girdPoints);
            for (CoveragePoint pt : girdPoints) {
                fw.append(String.format("PointNumber:        %d\n", i));
                fw.append(String.format("Lat:                %.14e\n", pt.getPoint().getLatitude()));
                fw.append(String.format("Lon:                %.14e\n", pt.getPoint().getLongitude()));
                fw.append(String.format("Alt:                %.14e\n", pt.getPoint().getAltitude()));
                fw.append(String.format("NumberOfAccesses:   %d\n", cvaa.get(pt).numIntervals()));
                Iterator<RiseSetTime> iter = cvaa.get(pt).getRiseSetTimes().iterator();
                while (iter.hasNext()) {
                    fw.append(String.format("%.14e   %.14e\n", iter.next().getTime(), iter.next().getTime()));
                }
                fw.append("\n");
                fw.flush();
                i++;
            }
        } catch (IOException ex) {
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        System.out.println("Saved accesses in " + file.toString());
        return true;
    }

    /**
     * Saves the ephemeris data for each satellite in the scenario in a desired
     * directory. File name is defined as name of scenario_name of satellite.eph
     *
     * @param path to the directory to save the file
     * @param scenario Scenario that was simulated
     * @return
     */
    public static boolean saveEphemeris(Path path, Scenario scenario) {
        HashSet<Satellite> sats = scenario.getUniqueSatellites();
        for (Satellite s : sats) {
            File file = new File(path.toFile(), String.format("%s_%s.eph",scenario.getName(),s.getName()));
            EphemerisHistory hist = scenario.getEphemerisHistory(s);
            hist.sortByDate();
            try (FileWriter fw = new FileWriter(file)) {
                fw.append("EpochTime, semimajor axis[km], eccentricty, inclination[deg], raan[deg], arg. per. [deg], mean anom. [deg]\n");
                for (int i=0; i<hist.size(); i++) {
                    Ephemeris e = hist.get(i);
                    fw.append(String.format("%f,%f,%f,%f,%f,%f,%f\n", 
                            e.getDate().durationFrom(scenario.getStartDate()),
                            e.getSa(), e.getEcc(), e.getInc(), e.getRaan(), e.getArgPer(), e.getMa()));
                    fw.flush();
                }
            } catch (IOException ex) {
                Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            System.out.println("Saved ephemeris in " + file.toString());
        }
        return true;
    }

    /**
     * Loads the Scenario instance saved by using save() from the given
     * filename.
     *
     * @param path to the directory to save the file
     * @param filename the file name (extension included)
     * @return the Scenario instance saved by using save()
     */
    public static Scenario load(Path path, String filename) {
        Scenario scenario = null;
        File file = new File(path.toFile(), filename);
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))) {
            scenario = (Scenario) is.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            System.err.println(ex);
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Successfully loaded scenario: " + file.toString());
        return scenario;
    }

    public static SubScenario loadSubScenario(Path path, String filename) {
        return (SubScenario) load(path, filename);
    }

    public static boolean saveReadMe(Path path, String filename, Scenario scenario) {
        File file = new File(path.toFile(), filename + ".txtore");

        try (FileWriter fw = new FileWriter(file)) {
            fw.append(scenario.toString()); //write scenario header
            fw.append(scenario.ppDate());
            fw.append(scenario.ppFrame());
            fw.append(scenario.ppPropgator());
            fw.append(scenario.ppConstellations());
            fw.append(scenario.ppCoverageDefinition());
            fw.flush();
        } catch (IOException ex) {
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        System.out.println("Saved readme in " + file.toString());
        return true;
    }
}
