/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.propagation;

import java.io.Serializable;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.J2DifferentialEffect;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;

/**
 * This class is an alternative to the built-in Orekit propagator builder.
 * Propagator builders rely on some conversion that I couldn't figure out. This
 * class is more straight forward and shall accomplish the desired task of
 * creating propagators for satellites
 *
 * @author nozomihitomi
 */
public class PropagatorFactory implements Serializable{
    private static final long serialVersionUID = -1149749257521567681L;
    
    private final PropagatorType propType;
    private final Orbit template;
    private final OrbitType orbitType;

    public PropagatorFactory(PropagatorType propType, Orbit template) {
        this.propType = propType;
        this.template = template;
        this.orbitType = template.getType();
    }
    
    
    public Propagator createPropagator(Orbit orbit) throws OrekitException{
        switch(propType){
            case KEPLERIAN:
                return createKeplerianPropagator(orbit, Propagator.DEFAULT_MASS);
            case J2:
//                return createJ2Propagator();
            default: 
                throw new UnsupportedOperationException(String.format("Propagator of type %s is not supported by factory.", propType));
        }
    }
    
    /**
     * Creates a Keplerian propagator
     * @param orbit to create a propagator for
     * @param mass mass of spacecraft[kg]
     * @return
     * @throws OrekitException 
     */
    private Propagator createKeplerianPropagator(Orbit orbit, double mass) throws OrekitException{
        return new KeplerianPropagator(orbit, Propagator.DEFAULT_LAW, orbit.getMu(), mass);
    }
    
//    private Propagator createJ2Propagator(SpacecraftState original,
//                                AdapterPropagator.DifferentialEffect directEffect,
//                                boolean applyBefore,
//                                double referenceRadius,  double mu,  double j2)){
//        return new J2DifferentialEffect(original, directEffect, applyBefore, referenceRadius, mu, j2);
//    }
    
    /**
     * Creates a TLE propagator
     * @param tle to create a propagator for
     * @param mass mass of spacecraft[kg]
     * @return
     * @throws OrekitException 
     */
    private Propagator createTLEPropagator(TLE tle, double mass) throws OrekitException{
        return new SGP4(tle, Propagator.DEFAULT_LAW, mass);
    }

    public OrbitType getOrbitType() {
        return orbitType;
    }
    
    
}

