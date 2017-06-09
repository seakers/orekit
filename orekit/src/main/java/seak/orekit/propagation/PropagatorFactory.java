/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.propagation;

import org.hipparchus.ode.ODEIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
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
public class PropagatorFactory {

    /**
     * Determines the complexity of the propagator
     */
    private final PropagatorType propType;

    /**
     * the orbit type is required for numerical propagators.
     */
    private final OrbitType orbitType;

    /**
     * used when creating numerical propagators.
     */
    private final ODEIntegrator integrator;

    /**
     * force models to use with numerical propagator
     */
    private final ForceModel[] models;

    /**
     * This constructor is used when creating analytical propagators (e.g. Keplerian, J2)
     * @param propType 
     */
    public PropagatorFactory(PropagatorType propType) {
        this.propType = propType;
        this.orbitType = null;
        this.integrator = null;
        this.models = null;
    }

    /**
     * This constructor should be used if a numerical propagator is desired.
     * Need to specify the orbit type and the ode integrator.
     *
     * @param propType
     * @param orbitType the orbit type is required for numerical propagators.
     * @param integrator used when creating numerical propagators.
     * @param models the force models
     */
    public PropagatorFactory(PropagatorType propType, OrbitType orbitType, ODEIntegrator integrator, ForceModel[] models) {
        this.propType = propType;
        this.orbitType = orbitType;
        this.integrator = integrator;
        this.models = models;
    }

    public Propagator createPropagator(Orbit orbit, double mass) throws OrekitException {
        switch (propType) {
            case KEPLERIAN:
                return createKeplerianPropagator(orbit, mass);
            case J2:
                return createJ2Propagator(orbit, mass);
            case NUMERICAL:
                return createNumericalPropagator(orbit, mass, integrator, models);
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
     * Creates a J2 propagator assuming an Earth-centric orbit. Currently set
     * for using GRIM5C1 model
     *
     * @param orbit
     * @return
     * @throws OrekitException
     */
    private Propagator createJ2Propagator(Orbit orbit, double mass) throws OrekitException {

        return new EcksteinHechlerPropagator(orbit, mass, Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_MU, Constants.WGS84_EARTH_C20, 0, 0, 0, 0);
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
     * Creates a numerical propagator. Assumed that the position of the orbit is
     * given with true anomaly.
     *
     * @param orbit
     * @return
     * @throws OrekitException
     */
    private Propagator createNumericalPropagator(Orbit orbit, double mass, ODEIntegrator integrator, ForceModel[] models) throws OrekitException {
        SpacecraftState s = new SpacecraftState(orbit, mass);
        NumericalPropagator p = new NumericalPropagator(integrator);
        p.resetInitialState(s);
        p.setOrbitType(orbitType);
        p.setPositionAngleType(PositionAngle.TRUE);
        for(ForceModel fm : models){
            p.addForceModel(fm);
        }
        return p;
    }

    public OrbitType getOrbitType() {
        return orbitType;
    }

    public PropagatorType getPropType() {
        return propType;
    }

}
