/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.analysis.ephemeris;

import seak.orekit.analysis.Record;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seak.orekit.analysis.AbstractSpacecraftAnalysis;
import seak.orekit.object.Satellite;
import seak.orekit.propagation.PropagatorFactory;

/**
 * This class will record the orbital parameters of a satellite over the course
 * of the whole scenario
 *
 * @author nozomihitomi
 */
public class OrbitalElementsAnalysis extends AbstractSpacecraftAnalysis<OrbitalElements> {
    
    private final PositionAngle type;
   
    /**
     * 
     * @param startDate start date of the analysis
     * @param endDate end date of the analysis
     * @param timeStep the time step at which to record the orbital elements
     * @param sat the satellite of interest
     * @param type the type of anomaly to record (True or Mean)
     * @param propagatorFactory the propagator factory that will create the appropriate propagator for the satellite of interest
     */
    public OrbitalElementsAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, double timeStep, Satellite sat, PositionAngle type, PropagatorFactory propagatorFactory) {
        super(startDate, endDate, timeStep, sat, propagatorFactory);
        this.type = type;
    }

    @Override
    public String getHeader() {
        return super.getHeader()+",semimajor axis[deg],ecc.,inc.[deg],raan[deg],arg. per.[deg]," + type.toString() + " anom.[deg]"; 
    }

    @Override
    public String getExtension() {
        return "eph";
    }

    /**
     * At each step, the ephemeris of the satellite is recorded
     *
     * @param currentState
     */
    @Override
    protected void handleStep(SpacecraftState currentState) {
        KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
        Record<OrbitalElements> e = new Record(currentState.getDate(), new OrbitalElements(o.getA(), o.getE(), o.getI(),
                o.getRightAscensionOfAscendingNode(), o.getPerigeeArgument(),
                o.getAnomaly(type)));
        addRecord(e);
    }

    @Override
    public String getName() {
        return String.format("%s_%s","eph",getSatellite().getName());
    }

}
