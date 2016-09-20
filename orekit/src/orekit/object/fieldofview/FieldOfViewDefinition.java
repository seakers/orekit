/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object.fieldofview;

import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;

/**
 * This interface is connects different fields of view to their proper g()
 * function that is computed in the event detectors
 *
 * @author nozomihitomi
 */
public interface FieldOfViewDefinition {

    /**
     * This g() function is positive when the given target enters the field of
     * view and negative if the target is outside the field of view
     *
     * @param s the spacecraft's current state
     * @param target the target of interest
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    public double g_FOV(SpacecraftState s, TopocentricFrame target) throws OrekitException ;

}
