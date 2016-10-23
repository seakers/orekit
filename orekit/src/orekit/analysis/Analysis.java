/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.analysis;

import java.util.Collection;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;

/**
 * An analysis is used to record certain states at fixed time steps during the
 * propagation of an orbit. The states must be observable from information
 * stored in the SpacecraftSate class. Information retrievable from
 * SpacecraftState include the spacecraft's mass, attitude, position, and
 * velocity.
 *
 * Each analysis records an object of a generic class T at each time step and
 * the history can be retrieved after propagation.
 *
 * @author nozomihitomi
 * @param <Record>
 */
public interface Analysis<Record> extends OrekitFixedStepHandler {

    /**
     * Gets the recorded value of an analysis from each time step. The returned
     * history shall be sorted in chronological order with the earliest record
     * as the first element
     *
     * @return
     */
    public Collection<Record> getHistory();

    /**
     * Gets the fixed time step at which values are recorded
     *
     * @return
     */
    public double getTimeStep();

    /**
     * The header to use in a saved file to inform a user of what the data are
     *
     * @return
     */
    public String getHeader();

    /**
     * The file extension required to save the analysis
     *
     * @return
     */
    public String getExtension();
}
