/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.ephemeris;

import org.orekit.errors.OrekitException;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;

/**
 *
 * @author nozomihitomi
 */
public class EphemerisStepHandler implements OrekitFixedStepHandler{
    
    private final EphemerisHistory hist;

    public EphemerisStepHandler() {
        this.hist = new EphemerisHistory();
    }

    /**
     * At each step, the ephemeris of the satellite is recorded
     * @param currentState
     * @param isLast
     * @throws OrekitException 
     */
    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
        Ephemeris e = new Ephemeris(o.getA(), o.getE(), o.getI(), 
                o.getRightAscensionOfAscendingNode(), o.getPerigeeArgument(), 
                o.getMeanAnomaly(),currentState.getDate());
        hist.add(e);
    }

    /**
     * Returns the recorded ephemeris history
     * @return 
     */
    public EphemerisHistory getEphemerisHistory() {
        return hist;
    }
    
}
