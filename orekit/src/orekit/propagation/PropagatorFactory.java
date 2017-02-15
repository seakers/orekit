/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.propagation;

import java.io.Serializable;
import java.util.Objects;
import org.hipparchus.ode.ODEIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.Constants;

/**
 * This class is an alternative to the built-in Orekit propagator builder.
 * Propagator builders rely on some conversion that I couldn't figure out. This
 * class is more straight forward and shall accomplish the desired task of
 * creating propagators for satellites
 *
 * @author nozomihitomi
 */
public class PropagatorFactory implements Serializable {

    private static final long serialVersionUID = -1149749257521567681L;

    private final PropagatorType propType;
    private final OrbitType orbitType;

    public PropagatorFactory(PropagatorType propType, OrbitType orbitType) {
        this.propType = propType;
        this.orbitType = orbitType;
    }

    public Propagator createPropagator(Orbit orbit, double mass) throws OrekitException {
        switch (propType) {
            case KEPLERIAN:
                return createKeplerianPropagator(orbit, mass);
            case J2:
               return createJ2Propagator(orbit, mass);
            default:
                throw new UnsupportedOperationException(String.format("Propagator of type %s is not supported by factory or by this constructor.", propType));
        }
    }

    public Propagator createPropagator(ODEIntegrator integrator) throws OrekitException {
        switch (propType) {
            case NUMERICAL:
                return createNumericalPropagator(integrator);
            default:
                throw new UnsupportedOperationException(String.format("Propagator of type %s is not supported by factory or by this constructor.", propType));
        }
    }
    /**
     * Creates a Keplerian propagator
     *
     * @param orbit to create a propagator for
     * @param mass mass of spacecraft[kg]
     * @return
     * @throws OrekitException
     */
    private Propagator createKeplerianPropagator(Orbit orbit, double mass) throws OrekitException {
        return new KeplerianPropagator(orbit, Propagator.DEFAULT_LAW, orbit.getMu(), mass);
    }

    /**
     * Creates a J2 propagator assuming an Earth-centric orbit. Currently set for using GRIM5C1 model
     * @param orbit
     * @return
     * @throws OrekitException 
     */
    private Propagator createJ2Propagator(Orbit orbit, double mass) throws OrekitException{
        
        return new EcksteinHechlerPropagator(orbit, mass, Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                        Constants.WGS84_EARTH_MU, Constants.WGS84_EARTH_C20, 0,0,0,0);
    }
    /**
     * Creates a TLE propagator
     *
     * @param tle to create a propagator for
     * @param mass mass of spacecraft[kg]
     * @return
     * @throws OrekitException
     */
    private Propagator createTLEPropagator(TLE tle, double mass) throws OrekitException {
        return new SGP4(tle, Propagator.DEFAULT_LAW, mass);
    }
    
        /**
     * Creates a numerical propagator
     * @param orbit
     * @return
     * @throws OrekitException 
     */
    private Propagator createNumericalPropagator(ODEIntegrator integrator) throws OrekitException{
        
        return new NumericalPropagator(integrator);
    }

    public OrbitType getOrbitType() {
        return orbitType;
    }

    public PropagatorType getPropType() {
        return propType;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.propType.toString());
        hash = 59 * hash + Objects.hashCode(this.orbitType.toString());
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
        final PropagatorFactory other = (PropagatorFactory) obj;
        if (this.propType != other.propType) {
            return false;
        }
        if (this.orbitType != other.orbitType) {
            return false;
        }
        return true;
    }
    
    

}
