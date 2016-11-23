/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.analysis.ephemeris;

import java.io.Serializable;
import orekit.analysis.AbstractAnalysis;
import orekit.analysis.Record;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;

/**
 * This class will record the orbital parameters of a satellite over the course
 * of the whole scenario
 *
 * @author nozomihitomi
 */
public class OrbitalElementsAnalysis extends AbstractAnalysis<OrbitalElements> implements Serializable{
    private static final long serialVersionUID = 2364842286699623867L;

    public OrbitalElementsAnalysis(double timeStep) {
        super(timeStep);
    }

    @Override
    public String getHeader() {
        return super.getHeader()+",semimajor axis[deg],ecc.,inc.[deg],raan[deg],arg. per.[deg],mean anom.[deg]"; 
    }

    @Override
    public String getExtension() {
        return "eph";
    }


    /**
     * At each step, the ephemeris of the satellite is recorded
     *
     * @param currentState
     * @param isLast
     * @throws OrekitException
     */
    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
        Record<OrbitalElements> e = new Record(currentState.getDate(), new OrbitalElements(o.getA(), o.getE(), o.getI(),
                o.getRightAscensionOfAscendingNode(), o.getPerigeeArgument(),
                o.getMeanAnomaly()));
        addRecord(e);
    }

    
    @Override
    public int hashCode() {
        int hash = super.hashCode();
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
        final OrbitalElementsAnalysis other = (OrbitalElementsAnalysis) obj;
        return true;
    }
    

}
