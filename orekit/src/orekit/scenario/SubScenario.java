/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import orekit.analysis.CompoundAnalysis;
import orekit.coverage.parallel.CoverageDivider;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import org.hipparchus.util.FastMath;

/**
 * This class extends Scenario and is intended to be used as a means to
 * parallelize the simulation by decomposing the original scenario into several
 * subscenarios. Each subscenario is limited to run a part of one coverage
 * definition.
 *
 * @author nozomihitomi
 */
public class SubScenario extends Scenario {

    private static final long serialVersionUID = 8941923902147656132L;

    /**
     * The hash value of the parent scenario
     */
    private final int parentScenarioHash;
    
    /**
     * The hash value of the parent scenario
     */
    private final String parentScenarioName;

    /**
     * The hash value of the original coverage definition. Used to place parts
     * of the coverage definition in the same bin
     */
    private final int origCovDefHash;
    
    /**
     * The name of the original coverage definition. 
     */
    private final String origCovDefName;

    /**
     * The id given to the subscenario
     */
    private final int subscenarioID;

    /**
     * The total number of subscenarios that make up the parent scenario
     */
    private final int totalSubscenarios;
    

    /**
     *
     * @param s The parent scenario
     * @param subscenarioID the id that identifies this subscenario
     * @param totalSubscenarios the total number of subscenarios that compose the original scenario
     * @param origCovDefHash the hash value of the original coverage definition
     * @param origCovDefName the name of the original coverage definition
     * @param covDefs the coverage definitions assigned to this subscenario
     * @param numThreads the number of threads to use to run the subscenario
     */
    private SubScenario(Scenario s, int subscenarioID, int totalSubscenarios, int origCovDefHash, String origCovDefName, CoverageDefinition covDef, int numThreads) {
        super(String.format(s.getName() + "_subscen%d", subscenarioID), 
                s.getStartDate(), s.getEndDate(), s.getTimeScale(), 
                s.getFrame(), s.getPropagatorFactory(), 
                new HashSet<>(Arrays.asList(covDef)), s.isSaveAllAccesses(),
                new CompoundAnalysis(s.getAnalyses()), numThreads);
        this.parentScenarioHash = s.hashCode();
        this.parentScenarioName = s.getName();
        this.subscenarioID = subscenarioID;
        this.totalSubscenarios = totalSubscenarios;
        this.origCovDefHash = origCovDefHash;
        this.origCovDefName = origCovDefName;
    }

    /**
     * Builder that helps create subscenarios to distribute task of propagation
     */
    public static class SubBuilder implements Serializable{
        private static final long serialVersionUID = 3730918796247122257L;

        //required fields
        private final Scenario parentScenario;

        //optional parameters - initialized to default parameters
        /**
         * the number of threads to use in parallel processing
         */
        private int numThreads = 1;

        /**
         * Subscenario builder is created from a parent scenario
         *
         * @param scenario
         */
        public SubBuilder(Scenario scenario) {
            this.parentScenario = scenario;
        }

        /**
         * sets the number of threads to use when running each subscenario. By
         * default it is set to 1.
         *
         * @param numThreads
         * @return
         */
        public SubBuilder numThreads(int numThreads) {
            this.numThreads = numThreads;
            return this;
        }

        /**
         * Creates ndivisions of subscenarios from the parent Scenario s.
         *
         * @param numDivisions can specify the number of divisions to make from
         * each coverage definition in the scenario.
         * @return a queue of subscenarios to be processed. Queue is threadsafe
         * for insertion and extraction
         * @throws java.lang.InterruptedException
         */
        public LinkedBlockingQueue<SubScenario> build(int numDivisions) throws InterruptedException {
            /*
             For each coverage definition of the original scenario, we create ndivisions subscenarios
             and save them
             */
            LinkedBlockingQueue<SubScenario> out = new LinkedBlockingQueue();
            Iterator<CoverageDefinition> iter1 = parentScenario.getCoverageDefinitions().iterator();
            int totalNumberOfSubscenarios = parentScenario.getCoverageDefinitions().size()*numDivisions;
            int counter = 0;
            while (iter1.hasNext()) {
                CoverageDefinition parentCovDef = iter1.next();
                Collection<CoverageDefinition> covs = CoverageDivider.divide(parentCovDef, numDivisions);
                Iterator<CoverageDefinition> iter2 = covs.iterator();
                while (iter2.hasNext()) {
                    out.put(new SubScenario(parentScenario, counter, 
                            totalNumberOfSubscenarios, parentCovDef.hashCode(),
                            parentCovDef.getName(),iter2.next(), numThreads));
                    counter++;
                }
            }
            return out;
        }
    }

    public int getParentScenarioHash() {
        return parentScenarioHash;
    }

    public int getSubscenarioID() {
        return subscenarioID;
    }

    public int getTotalSubscenarios() {
        return totalSubscenarios;
    }

    public int getOrigCovDefHash() {
        return origCovDefHash;
    }

    public String getOrigCovDefName() {
        return origCovDefName;
    }

    public String getParentScenarioName() {
        return parentScenarioName;
    }
    
    /**
     * Gets the one partial coverage definition stored in this subscenario
     * @return 
     */
    public CoverageDefinition getPartialCoverageDefinition(){
        return getCoverageDefinitions().iterator().next();
    }
    

    @Override
    public String toString() {
        return "Sub" + super.toString();
    }
}
