/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import orekit.object.CoverageDefinition;
import orekit.propagation.PropagatorFactory;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * This class extends Scenario and is intended to be used as a means to
 * parallelize the simulation by decomposing the original scenario into several
 * subscenarios
 *
 * @author nozomihitomi
 */
public class SubScenario extends Scenario{

    private static final long serialVersionUID = 8941923902147656132L;
    
    private final int parentScenarioHash;

    /**
     * 
     * @param parentScenarioHash The hashcode of the parent scenario
     * @param name
     * @param startDate
     * @param endDate
     * @param timeScale
     * @param inertialFrame
     * @param propagatorFactory
     * @param saveAllAccesses 
     */
    public SubScenario(int parentScenarioHash, String name, AbsoluteDate startDate, AbsoluteDate endDate, TimeScale timeScale, Frame inertialFrame, PropagatorFactory propagatorFactory, boolean saveAllAccesses) {
        super(name, startDate, endDate, timeScale, inertialFrame, propagatorFactory, saveAllAccesses, 1);
        this.parentScenarioHash = parentScenarioHash;
    }

    public SubScenario(int parentScenarioHash, String name, AbsoluteDate startDate, AbsoluteDate endDate, TimeScale timeScale, Frame inertialFrame, PropagatorFactory propagatorFactory, boolean saveAllAccesses, CoverageDefinition covdef) {
        this(parentScenarioHash, name, startDate, endDate, timeScale, inertialFrame, propagatorFactory, saveAllAccesses);
        super.addCoverageDefinition(covdef);
    }

    public int getParentScenarioHash() {
        return parentScenarioHash;
    }
   
    /**
     * Subscenarios are restricted to only have one coverage definition. If more
     * than one coverage definition is added, an error is thrown
     *
     * @param covDef
     * @return
     */
    @Override
    public boolean addCoverageDefinition(CoverageDefinition covDef) {
        if (getCoverageDefinitions().size() > 1) {
            throw new IllegalArgumentException("This subscenario already has a coverage definition assigned. Subscenarios can only have one coverage definition");
        } else {
            return super.addCoverageDefinition(covDef);
        }
    }

    @Override
    public String toString() {
        return "Sub" + super.toString();
    }
}
