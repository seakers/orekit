/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.analysis;

import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seak.orekit.object.Satellite;
import seak.orekit.propagation.PropagatorFactory;

/**
 *
 * @author nhitomi
 * @param <T> The generic for the record object to record 
 */
public abstract class AbstractSpacecraftAnalysis<T> extends AbstractAnalysis<T>{
    
    /**
     * Satellite for the analysis
     */
    private final Satellite sat;
    
    /**
     * Factory to create propagators to conduct the analysis
     */
    protected final PropagatorFactory propagatorFactory;
    
    public AbstractSpacecraftAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, double timeStep, Satellite sat, PropagatorFactory propagatorFactory) {
        super(startDate, endDate, timeStep, propagatorFactory);
        this.sat = sat;
        this.propagatorFactory = propagatorFactory;
    }
    
    @Override
    public AbstractAnalysis<T> call() throws Exception {
        Propagator prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());
        prop.setSlaveMode();

        for (AbsoluteDate extrapDate = getStartDate();
                extrapDate.compareTo(getEndDate()) <= 0;
                extrapDate = extrapDate.shiftedBy(getTimeStep())) {
            handleStep(prop.propagate(extrapDate));
        }
        
        return this;
    }

    /**
     * Gets the satellite involved in this analysis
     * @return the satellite involved in this analysis
     */
    public Satellite getSatellite() {
        return sat;
    }
    
    protected abstract void handleStep(SpacecraftState state);   
    
    
}
