/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.analysis;

import java.io.Serializable;
import java.util.Collection;

/**
 *
 * @author nozomihitomi
 */
public abstract class AbstractAnalysis<T> implements Analysis, Serializable{
    private static final long serialVersionUID = 3757497641603127042L;

    /**
     * the fixed time step at which values are recorded
     */
    private final double timeStep;
    
    protected final RecordHistory<T> history;
    
    public AbstractAnalysis(double timeStep) {
        this.timeStep = timeStep;
        this.history = new RecordHistory();
    }
    
    protected void addRecord(Record<T> record){
        history.add(record);
    }
    
    @Override
    public String getHeader(){
        return String.format("Date", timeStep);
    }

    @Override
    public double getTimeStep(){
        return timeStep;
    }
    
    /**
     * Returns the recorded data structures from the analysis
     *
     * @return
     */
    @Override
    public Collection<Record<T>> getHistory() {
        history.sortByDate();
        return history;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + (int) (Double.doubleToLongBits(this.timeStep) ^ (Double.doubleToLongBits(this.timeStep) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractAnalysis other = (AbstractAnalysis) obj;
        if (Double.doubleToLongBits(this.timeStep) != Double.doubleToLongBits(other.timeStep)) {
            return false;
        }
        return true;
    }
    
    
    
}
