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
import orekit.object.CoverageDefinition;
import orekit.scenario.*;
import org.orekit.errors.OrekitException;
/**
 *
 * @author paugarciabuzzi
 */
public class ParallelCoverage {
    
    public void CreateSubScenarios(Scenario s, int ndivisions, File file){
        /*
        We first get the different coverage definitions included inside the original scenario
        */
        HashSet<CoverageDefinition> coverageCollection=s.getCoverageDefinitions();
        
        
        /*
        For each coverage definition of the original scenario, we create ndivisions subscenarios
        and save them
        */
        Iterator iter1 = coverageCollection.iterator();
        int counter=1;
        while (iter1.hasNext()) {
            Collection<CoverageDefinition> covs=iter1.next().divideCoverageGrid(ndivisions);
            Iterator iter2=covs.iterator();
            while (iter2.hasNext()){
                try {
                    Scenario subscen = s.clone();
                    subscen.addCoverageDefinition((CoverageDefinition) iter2.next());
                    ScenarioIO.save(file.toPath(), String.format("subscen%d", counter), subscen);
                    counter++;
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(ParallelCoverage.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
    }
    
    public void LoadRunAndSave(File file, String filename){
        /*
        Loads, runs and saves the subscenario stored in filename
        */
        try {
            Scenario s=ScenarioIO.load(file.toPath(), filename);
            s.call();
            ScenarioIO.save(file.toPath(),filename,s);
        } catch (OrekitException ex) {
            Logger.getLogger(ParallelCoverage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
