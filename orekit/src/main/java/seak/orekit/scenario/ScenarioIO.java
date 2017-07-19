/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.scenario;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.frames.TopocentricFrame;
import seak.orekit.analysis.Analysis;
import seak.orekit.analysis.CompoundSpacecraftAnalysis;
import seak.orekit.analysis.Record;
import seak.orekit.coverage.access.RiseSetTime;
import seak.orekit.coverage.access.TimeIntervalArray;
import seak.orekit.coverage.analysis.AnalysisMetric;
import seak.orekit.coverage.analysis.GroundEventAnalyzer;
import seak.orekit.event.GroundEventAnalysis;
import seak.orekit.object.Constellation;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.object.CoveragePoint;
import seak.orekit.object.Satellite;

/**
 * Saves and loads scenarios
 *
 * @author nozomihitomi
 */
public class ScenarioIO {

    /**
     * Saves the accesses of the coverage definition from the scenario in a
     * desired directory. Searches the scenario for the computed metrics
     * belonging to the specified coverage definition
     *
     * @param path to the directory to save the file
     * @param filename name of the file without the extension
     * @param scenario Scenario that was simulated
     * @param covdef the coverage definition of interest
     * @param analysis the analysis to save
     * @return
     */
    public static boolean saveGroundEventAnalysis(Path path, String filename,
            Scenario scenario, CoverageDefinition covdef, GroundEventAnalysis analysis) {

        Map<TopocentricFrame, TimeIntervalArray> groundEvents = analysis.getEvents(covdef);
        File file = new File(path.toFile(),
                String.format("%s_%s_%s.res", filename, scenario.getName(), covdef.getName()));
        try (FileWriter fw = new FileWriter(file)) {
            fw.append(String.format("Start Date: %s\n\n", scenario.getStartDate()));
            fw.append(String.format("End Date: %s\n\n", scenario.getEndDate()));
            fw.append(analysis.getHeader());
            fw.flush();

            int i = 0;
            ArrayList<CoveragePoint> gridPoints = new ArrayList(groundEvents.keySet());
            Collections.sort(gridPoints);
            for (CoveragePoint pt : gridPoints) {
                fw.append(String.format("PointNumber:        %d\n", i));
                fw.append(String.format("Lat:                %.14e\n", pt.getPoint().getLatitude()));
                fw.append(String.format("Lon:                %.14e\n", pt.getPoint().getLongitude()));
                fw.append(String.format("Alt:                %.14e\n", pt.getPoint().getAltitude()));
                fw.append(String.format("NumberOfEvents  :   %d\n", groundEvents.get(pt).numIntervals()));
                Iterator<RiseSetTime> iter = groundEvents.get(pt).getRiseSetTimes().iterator();
                while (iter.hasNext()) {
                    fw.append(String.format("%.14e", iter.next().getTime()));
                    try {
                        fw.append(String.format("   %.14e\n", iter.next().getTime()));
                    } catch (NoSuchElementException ex) {
                        Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, "Expected all intervals to be closed. Found an open one", ex);
                    }
                }
                fw.append("\n");
                fw.flush();
                i++;
            }
        } catch (IOException ex) {
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        Logger.getGlobal().finest(String.format("Saved accesses in %s", file.toString()));
        return true;
    }

    public static boolean saveGroundEventAnalysisMetrics(Path path, String filename,
            Scenario scenario, GroundEventAnalyzer gea, AnalysisMetric metric, boolean accesses) {
        String acc;
        if (accesses) {
            acc = "access";
        } else {
            acc = "gap";
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(
                new File(path.toFile(),
                        String.format("%s_%s_%s_%s.csv", filename, scenario, metric, acc))))) {
            //write the headers
            bw.append("#");
            bw.append("latitude[deg]").append(",").append("longitude[deg]").append(",");
            bw.append(String.format("min %s[s]",acc)).append(",");
            bw.append(String.format("max %s[s]",acc)).append(",");
            bw.append(String.format("mean %s[s]",acc)).append(",");
            bw.append(String.format("std dev %s[s]",acc));
            for (int i = 1; i <= 100; i++) {
                bw.append(",");
                bw.append(String.format("%d percentile %s[s]", i,acc));
            }
            bw.newLine();

            for (CoveragePoint pt : gea.getCoveragePoints()) {
                bw.append(String.valueOf(FastMath.toDegrees(pt.getPoint().getLatitude())));
                bw.append(",");
                bw.append(String.valueOf(FastMath.toDegrees(pt.getPoint().getLongitude())));
                bw.append(",");
                DescriptiveStatistics stats = gea.getStatistics(metric, accesses, pt);
                bw.append(String.valueOf(stats.getMin()));
                bw.append(",");
                bw.append(String.valueOf(stats.getMax()));
                bw.append(",");
                bw.append(String.valueOf(stats.getMean()));
                bw.append(",");
                bw.append(String.valueOf(stats.getStandardDeviation()));
                bw.append(",");
                for (int i = 1; i <= 100; i++) {
                    bw.append(String.valueOf(stats.getPercentile(i)));
                    bw.append(",");
                }
                bw.newLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

//    /**
//     * Saves the link budget intervals of the coverage definition from the
//     * scenario in a desired directory
//     *
//     * @param path to the directory to save the file
//     * @param filename name of the file without the extension
//     * @param scenario Scenario that was simulated
//     * @param covdef the coverage definition of interest
//     * @return
//     */
//    public static boolean saveLinkBudget(Path path, String filename, Scenario scenario, CoverageDefinition covdef) {
//        File file = new File(path.toFile(), String.format("%s_%s_%s.link", filename, scenario.getName(), covdef.getName()));
//        HashMap<CoveragePoint, TimeIntervalArray> cvaa = scenario.getMergedLinkBudgetIntervals(covdef);
//        try (FileWriter fw = new FileWriter(file)) {
//            fw.append(String.format("EpochTime: %s\n\n", scenario.getStartDate()));
//            fw.append("Assigned Constellations:\n");
//            for (Constellation constel : covdef.getConstellations()) {
//                fw.append(String.format("\tConstelation %s: %d satellites\n", constel.getName(), constel.getSatellites().size()));
//                for (Satellite sat : constel.getSatellites()) {
//                    fw.append(String.format("\t\tSatellite %s\n", sat.toString()));
//                }
//            }
//            fw.flush();
//
//            int i = 0;
//            ArrayList<CoveragePoint> girdPoints = new ArrayList(cvaa.keySet());
//            Collections.sort(girdPoints);
//            for (CoveragePoint pt : girdPoints) {
//                fw.append(String.format("PointNumber:        %d\n", i));
//                fw.append(String.format("Lat:                %.14e\n", pt.getPoint().getLatitude()));
//                fw.append(String.format("Lon:                %.14e\n", pt.getPoint().getLongitude()));
//                fw.append(String.format("Alt:                %.14e\n", pt.getPoint().getAltitude()));
//                fw.append(String.format("NumberOfIntervals:   %d\n", cvaa.get(pt).numIntervals()));
//                Iterator<RiseSetTime> iter = cvaa.get(pt).getRiseSetTimes().iterator();
//                while (iter.hasNext()) {
//                    fw.append(String.format("%.14e   %.14e\n", iter.next().getTime(), iter.next().getTime()));
//                }
//                fw.append("\n");
//                fw.flush();
//                i++;
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
//            return false;
//        }
//        System.out.println("Saved link Budget intervals in " + file.toString());
//        return true;
//    }
    /**
     * Saves the recorded history from the given analysis
     *
     * @param path path to save the results
     * @param fileName name of the analysis file to save
     * @param analysis analysis to save
     * @return true if the analysis is successfully saved
     */
    public static boolean saveAnalysis(Path path, String fileName, Analysis analysis) {
        if (analysis instanceof CompoundSpacecraftAnalysis) {
            boolean out = true;
            for (Analysis a : ((CompoundSpacecraftAnalysis) analysis).getAnalyses()) {
                out = Boolean.logicalAnd(out, saveSingleAnalysis(path, fileName, a));
            }
            return out;
        } else {
            return saveSingleAnalysis(path, fileName, analysis);
        }
    }

    /**
     * Saves the recorded history from the given analysis that is not a compound
     * analysis
     *
     * @param path path to save the results
     * @param fileName name of the analysis file to save
     * @param analysis analysis to save
     * @return true if the analysis is successfully saved
     */
    private static boolean saveSingleAnalysis(Path path, String fileName, Analysis analysis) {
        File file = new File(path.toFile(), String.format("%s.%s", fileName, analysis.getExtension()));
        Iterator<Record> histIter = analysis.getHistory().iterator();
        try (FileWriter fw = new FileWriter(file)) {
            fw.append("#Epoch time," + analysis.getHeader() + "\n");
            while (histIter.hasNext()) {
                Record r = histIter.next();
                fw.append(String.format("%f,%s\n",
                        r.getDate().durationFrom(analysis.getStartDate()),
                        r.getValue()));
            }
            fw.flush();
        } catch (IOException ex) {
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        Logger.getGlobal().finest(String.format("Saved %s in %s", analysis.getClass().getSimpleName(), file.toString()));

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
        Logger.getGlobal().finest(String.format("Successfully loaded scenario: %s", file.toString()));
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
        Logger.getGlobal().finest(String.format("Saved readme in %s", file.toString()));
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
    public static Scenario checkDatabase(Scenario scenarioToRun) {
        String fileName = String.format("%d.scen", scenarioToRun.hashCode());
        File covDB = new File(System.getProperty("CoverageDatabase"));
        File file = new File(covDB, fileName);
        if (file.exists()) {
            return ScenarioIO.load(covDB.toPath(), fileName);
        } else {
            return null;
        }
    }

//    /**
//     * This method saves a simulated scenario to the database. If the scenario
//     * is not run yet, it will not be saved. In addition, if the scenario
//     * already exists in the database, the given scenario will not be saved
//     *
//     * @param scenario the scenario that has already been simulated
//     * @return　true if the scenario was successfully saved. else false
//     */
//    public static boolean saveToDatabase(Scenario scenario) {
//        if (!scenario.isDone()) {
//            return false;
//        }
//
//        String fileName = String.format("%d.scen", scenario.hashCode());
//        File covDB = new File(System.getProperty("CoverageDatabase"));
//        File file = new File(covDB, fileName);
//        if (file.exists()) {
//            return false;
//        } else {
//            ScenarioIO.save(covDB.toPath(), String.valueOf(scenario.hashCode()), scenario);
//            return true;
//        }
//    }
}
