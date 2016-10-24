/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object;

import java.io.Serializable;
import java.util.ArrayList;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;

/**
 * A satellite is defined with an orbit and a payload Satellite +X axis is
 * assumed to be the velocity vector along its orbit Satellite +Y axis is
 * assumed to be normal to the orbital plane Satellite +Z axis is assumed to be
 * facing nadir
 *
 * @author nozomihitomi
 */
public class Satellite implements OrekitObject, Serializable {

    private static final long serialVersionUID = -2337716721370526426L;

    private ArrayList<Instrument> payload;
    private final Orbit orbit;
    private final String name;
    private final AttitudeProvider attProv;

    /**
     * Original wet mass of the satellite
     */
    private final double wetMass;

    /**
     * Original dry mass of the satellite
     */
    private final double dryMass;

    /**
     * Original gross mass of the satellite
     */
    private final double grossMass;

    /**
     * Constructor for new satellite instance with no attitude control and
     * default wet mass and default dry mass
     *
     * @param name satellite name
     * @param orbit initial orbit to position satellite
     */
    public Satellite(String name, Orbit orbit) {
        this(name, orbit, null, Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
    }

    /**
     * Constructor for new satellite instance with default wet mass and default
     * dry mass
     *
     * @param name satellite name
     * @param orbit initial orbit to position satellite
     * @param attProv the attitude control law
     */
    public Satellite(String name, Orbit orbit, AttitudeProvider attProv) {
        this(name, orbit, attProv, Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
    }

    /**
     * Constructor for new satellite instance with specified wet mass and
     * default dry mass
     *
     * @param name satellite name
     * @param orbit initial orbit to position satellite
     * @param attProv the attitude control law
     */
    public Satellite(String name, Orbit orbit, AttitudeProvider attProv, double wetMass, double dryMass) {
        this.orbit = orbit;
        this.name = name;
        this.payload = new ArrayList<>();
        this.attProv = attProv;
        this.wetMass = wetMass;
        this.dryMass = dryMass;
        this.grossMass = wetMass + dryMass;
    }

    public void addInstrument(Instrument instrument) {
        payload.add(instrument);
    }

    public ArrayList<Instrument> getPayload() {
        return payload;
    }

    public Orbit getOrbit() {
        return orbit;
    }

    public AttitudeProvider getAttProv() {
        return attProv;
    }

    public String getName() {
        return name;
    }

    public double getWetMass() {
        return wetMass;
    }

    public double getDryMass() {
        return dryMass;
    }

    public double getGrossMass() {
        return grossMass;
    }

    public String ppOrbit() {
        KeplerianOrbit kepOrb = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(this.orbit);
        return kepOrb.toString();
    }

    public String ppPayload() {
        String out = "{";
        for (Instrument inst : payload) {
            out += inst.getName() + ",";
        }
        out += "}";
        return out;
    }

    @Override
    public String toString() {
        return "Satellite{" + "payload=" + payload + ", orbit=" + orbit + ", name=" + name + ", attProv=" + attProv + ", wetMass=" + wetMass + ", dryMass=" + dryMass + ", grossMass=" + grossMass + '}';
    }

    }
