/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;

/**
 * This
 *
 * @author nozomihitomi
 */
public class CompoundAnalysis implements Analysis, Serializable {

    private static final long serialVersionUID = 4096583420683752930L;

    private final Collection<Analysis> analyses;

    /**
     * The time step that is common to all analyses
     */
    private final double tStep;

    public CompoundAnalysis(Collection<Analysis> analyses) {
        //check if analyses is empty
        if (analyses.isEmpty()) {
            this.analyses = new ArrayList();
            this.tStep = Double.NaN;
        } else {
            //check that all analyses have the same time step
            double refTime = analyses.iterator().next().getTimeStep();
            for (Analysis a : analyses) {
                if (a.getTimeStep() != refTime) {
                    throw new IllegalArgumentException("All analyses must have the same step size");
                }
            }
            this.analyses = analyses;
            this.tStep = refTime;
        }
    }

    /**
     * Traverses all analyses recursively and obtains all analyses under this
     * compound analysis
     *
     * @return
     */
    public Collection<Analysis> getAnalyses() {
        ArrayList<Analysis> out = new ArrayList();
        for (Analysis a : analyses) {
            if (a instanceof CompoundAnalysis) {
                out.addAll(((CompoundAnalysis) a).getAnalyses());
            } else {
                out.add(a);
            }
        }
        return out;
    }

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        for (Analysis a : analyses) {
            a.handleStep(currentState, isLast);
        }
    }

    @Override
    public Collection getHistory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getTimeStep() {
        return tStep;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.analyses);
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
        final CompoundAnalysis other = (CompoundAnalysis) obj;
        if (!Objects.equals(this.analyses, other.analyses)) {
            return false;
        }
        return true;
    }

    @Override
    public String getHeader() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getExtension() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
