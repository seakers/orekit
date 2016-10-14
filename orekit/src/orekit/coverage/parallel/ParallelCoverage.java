/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.coverage.parallel;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import orekit.coverage.access.CoverageDivider;
import orekit.object.CoverageDefinition;
import orekit.scenario.*;
import org.orekit.errors.OrekitException;

/**
 *
 * @author paugarciabuzzi
 */
public class ParallelCoverage {

    /**
     * Creates ndivisions subscenarios from parent Scenario s to run them in
     * parallel
     *
     * @param s Scenario from which we want to create the subscenarios
     * @param ndivisions Number of subscenarios
     * @param file File object containing the path to save all the subscenarios
     */
    public void createSubScenarios(Scenario s, int ndivisions, File file) {
        /*
         We first get the different coverage definitions included inside the original scenario
         */
        HashSet<CoverageDefinition> coverageCollection = s.getCoverageDefinitions();

        /*
         For each coverage definition of the original scenario, we create ndivisions subscenarios
         and save them
         */
        Iterator<CoverageDefinition> iter1 = coverageCollection.iterator();
        int counter = 1;
        while (iter1.hasNext()) {
            CoverageDivider covdivider = new CoverageDivider();
            Collection<CoverageDefinition> covs = covdivider.divide(iter1.next(), ndivisions);
            Iterator<CoverageDefinition> iter2 = covs.iterator();
            while (iter2.hasNext()) {
                String subscenarioName = String.format(s.getName() + "_subscen%d", counter);
                SubScenario subscen = new SubScenario(s.hashCode(), subscenarioName, s.getStartDate(),
                        s.getEndDate(), s.getTimeScale(), s.getFrame(),
                        s.getPropagatorFactory(), s.isSaveAllAccesses());
                subscen.addCoverageDefinition(iter2.next());
                ScenarioIO.save(file.toPath(), subscen.getName(), subscen);
                counter++;

            }
        }
    }

    /**
     * Loads, runs, and saves an Scenario.
     *
     * @param savepath the path to load from and save the run file
     * @param filename String containing the name of the scenario to run
     */
    public void loadRunAndSave(Path savepath, String filename) {
        /*
         Loads, runs and saves the subscenario stored in filename
         */
        try {
            SubScenario s = ScenarioIO.loadSubScenario(savepath, filename);
            s.call();
            String[] str = filename.split("[.]");
            ScenarioIO.save(savepath, str[0], s);
        } catch (OrekitException ex) {
            Logger.getLogger(ParallelCoverage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Loads, runs, and saves an Scenario.
     *
     * @param path path containing un-run subscenarios
     */
    public void loadRunAndSave(Path path, int numThreads) {

        /**
         * pool of resources
         */
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        
        try {
            /**
             * List of future tasks to perform
             */
            ArrayList<Future<Scenario>> futures = new ArrayList<>();

            File[] matchingFiles = path.toFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".ore");
                }
            });

            for (File f : matchingFiles) {
                SubScenario s = ScenarioIO.loadSubScenario(path, f.getName());
                futures.add(pool.submit(s));
            }

            for (Future<Scenario> fut : futures) {
                Scenario s = fut.get();
                ScenarioIO.save(path, s.getName(), s);
            }
        } catch (ExecutionException ex) {
            Logger.getLogger(ParallelCoverage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ParallelCoverage.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            pool.shutdown();
        }
    }
}
