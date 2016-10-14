/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.coverage.parallel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
                SubScenario subscen = new SubScenario(s.getName(), s.getStartDate(),
                        s.getEndDate(), s.getTimeScale(), s.getFrame(),
                        s.getPropagatorFactory(), s.isSaveAllAccesses());
                subscen.addCoverageDefinition(iter2.next());
                ScenarioIO.save(file.toPath(), String.format(s.getName() + "_subscen%d", counter), subscen);
                counter++;

            }
        }
    }

    /**
     * Loads, runs, and saves an Scenario
     *
     * @param file File object containing the path of the Scenario we want to
     * run
     * @param filename String containing the name of the scenario to run
     */
    public void loadRunAndSave(File file, String filename) {
        /*
         Loads, runs and saves the subscenario stored in filename
         */
        try {
            SubScenario s = ScenarioIO.loadSubScenario(file.toPath(), filename);
            s.call();
            ScenarioIO.save(file.toPath(), filename, s);
        } catch (OrekitException ex) {
            Logger.getLogger(ParallelCoverage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
