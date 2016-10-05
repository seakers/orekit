/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.coverage.parallel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import orekit.object.CoverageDefinition;
import orekit.scenario.Scenario;

/**
 *
 * @author paugarciabuzzi
 */
public class ParallelCoverageMerger{
    
        /**
     * Merges subscenarios after being run in parallel into the original one. 
     * The input is the collection of subscenarios that we want to merge.
     * 
     * @param col collection of subscenarios
     * @return the merged scenario
     */
    public Scenario mergeSubscenarios(Collection<Scenario> col) throws Exception{
        /*
        Check if all the subscenarios in col are run. Otherwise throw an Exception
        */
        Iterator iter1=col.iterator();
        while(iter1.hasNext()){
            Scenario subscenario=(Scenario) iter1.next();
            if (!subscenario.isDone()){
                throw new Exception("The subscenarios are not run yet");
            }
        }
            /*
            iter3 and iter4 are useless since each subscenario only has one coverage definition.
            */
            Iterator iter2=col.iterator();
            Scenario subscenario=(Scenario) iter2.next();
            Scenario out=subscenario.clone();
            HashSet<CoverageDefinition> cdefs=subscenario.getCoverageDefinitions();
            Iterator iter3=cdefs.iterator();
            while(iter3.hasNext()){
                CoverageDefinition c=(CoverageDefinition) iter3.next();
                out.addCoverageDefinition(c);
                if(subscenario.isSaveAllAccesses()){
                    out.putAllAccesses(c, subscenario.getAllAccesses().get(c));//that works?
                } 
                out.putFinalAccesses(c, subscenario.getFinalAccesses().get(c));//that works?
            }
            while(iter2.hasNext()){
                subscenario=(Scenario) iter2.next();
                HashSet<CoverageDefinition> covdefs=subscenario.getCoverageDefinitions();
                Iterator iter4=covdefs.iterator();
                while(iter4.hasNext()){
                    CoverageDefinition c=(CoverageDefinition) iter4.next();
                    out.addCoverageDefinition(c);
                    if(subscenario.isSaveAllAccesses()){
                        out.putAllAccesses(c, subscenario.getAllAccesses().get(c));//that works?
                    } 
                    out.putFinalAccesses(c, subscenario.getFinalAccesses().get(c));//that works?
                }
            }
            /*
            Return the scenario which has all the accesses from the subscenarios but in
            size(col) diferent coverage definitions. Is this good?
            */
            return out;
            
    }
}
