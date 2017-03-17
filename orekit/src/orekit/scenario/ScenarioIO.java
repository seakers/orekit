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
import orekit.analysis.Analysis;
import orekit.analysis.Record;
import orekit.coverage.access.RiseSetTime;
import orekit.coverage.access.TimeIntervalArray;
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
    public static boolean save(Path path, String filename, Scenario3 scenario) {
        File file;
//        if (scenario instanceof SubScenario) {
//            file = new File(path.toFile(), String.format("%s.subscen", filename));
//        } else {
            file = new File(path.toFile(), String.format("%s.scen", filename));
//        }
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
    public static boolean saveAccess(Path path, String filename, Scenario3 scenario, CoverageDefinition covdef) {
        File file = new File(path.toFile(), String.format("%s_%s_%s.cva", filename, scenario.getName(), covdef.getName()));
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
     * Saves all the computed analyses run during the scenario
     *
     * @param path
     * @param scenario
     * @return true if the analyses are successfuly saved
     */
    public static boolean saveAnalyses(Path path, Scenario3 scenario) {
        for (Analysis analysis : scenario.getAnalyses()) {
            HashSet<Satellite> sats = scenario.getUniqueSatellites();
            for (Satellite s : sats) {
                File file = new File(path.toFile(), String.format("%s_%s.%s", scenario.getName(), s.getName(), analysis.getExtension()));
                Iterator<Record> histIter = scenario.getAnalysisResult(analysis, s).iterator();
                try (FileWriter fw = new FileWriter(file)) {
                    fw.append("#Epoch time," + analysis.getHeader() + "\n");
                    while (histIter.hasNext()) {
                        Record r = histIter.next();
                        fw.append(String.format("%f,%s\n",
                                r.getDate().durationFrom(scenario.getStartDate()),
                                r.getValue()));
                    }
                    fw.flush();
                } catch (IOException ex) {
                    Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
                System.out.println(String.format("Saved %s for satellite %s in %s", analysis.getClass().getSimpleName(), s.getName(), file.toString()));
            }
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
    public static Scenario3 load(Path path, String filename) {
        Scenario3 scenario = null;
        File file = new File(path.toFile(), filename);
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))) {
            scenario = (Scenario3) is.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            System.err.println(ex);
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Successfully loaded scenario: " + file.toString());
        return scenario;
    }

    /**
     * Saves a human read-able text file that contains input parameters that
     * define the scenario such as the satellites, their payload, the coverage
     * definition, the propagation type, and the scenario dates
     *
     * @param path
     * @param filename
     * @param scenario
     * @return
     */
    public static boolean saveReadMe(Path path, String filename, Scenario3 scenario) {
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

    /**
     * This method looks to see if the satellites in the scenario have already
     * been run and saved in the database. If so, the previously run scenario is
     * loaded with the access times.
     *
     * @param scenarioToRun
     * @return　null if the scenario does not exist in the database. Otherwise,
     * the scenario that is stored in the database
     */
    public static Scenario3 checkDatabase(Scenario3 scenarioToRun) {
        String fileName = String.format("%d.scen", scenarioToRun.hashCode());
        File covDB = new File(System.getProperty("CoverageDatabase"));
        File file = new File(covDB, fileName);
        if (file.exists()) {
            return ScenarioIO.load(covDB.toPath(), fileName);
        } else {
            return null;
        }
    }

    /**
     * This method saves a simulated scenario to the database. If the scenario
     * is not run yet, it will not be saved. In addition, if the scenario
     * already exists in the database, the given scenario will not be saved
     *
     * @param scenario the scenario that has already been simulated
     * @return　true if the scenario was successfully saved. else false
     */
    public static boolean saveToDatabase(Scenario3 scenario) {
        if (!scenario.isDone()) {
            return false;
        }

        String fileName = String.format("%d.scen", scenario.hashCode());
        File covDB = new File(System.getProperty("CoverageDatabase"));
        File file = new File(covDB, fileName);
        if (file.exists()) {
            return false;
        } else {
            ScenarioIO.save(covDB.toPath(), String.valueOf(scenario.hashCode()), scenario);
            return true;
        }
    }
}
