/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

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

    /**
     * The instruments in the payload of the satellite
     */
    private ArrayList<Instrument> payload;

    /**
     * initial orbit to position satellite
     */
    private final Orbit orbit;

    /**
     * The name of the satellite
     */
    private final String name;

    /**
     * The attitude provider that computes the attitude of the satellite at each
     * time step
     */
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

    /**
     * pretty print the orbit
     * @return 
     */
    public String ppOrbit() {
        KeplerianOrbit kepOrb = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(this.orbit);
        return kepOrb.toString();
    }

    /**
     * Pretty prints the payload
     * @return 
     */
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
        return "Satellite{" + "payload=" + payload + ", orbit=" + orbit +
                ", name=" + name + ", attProv=" + attProv + ", wetMass=" + wetMass 
                + ", dryMass=" + dryMass + ", grossMass=" + grossMass + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.payload);
        try {
            //create hash for orbit
            TimeStampedPVCoordinates pv = this.orbit.getPVCoordinates(FramesFactory.getEME2000());
            hash = 23 * hash + Objects.hashCode(pv.getMomentum());
            hash = 23 * hash + Objects.hashCode(pv.getAcceleration());
            hash = 23 * hash + Objects.hashCode(pv.getAngularVelocity());

            //get hash for attitude provider based on rotation matrix at specific time in specific frame
            if (attProv != null) {
                double[][] rotMmatrix = attProv.getAttitude(orbit, AbsoluteDate.GPS_EPOCH, FramesFactory.getEME2000()).getRotation().getMatrix();
                hash = 23 * hash + Objects.hashCode(rotMmatrix);
            }
        } catch (OrekitException ex) {
            Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
        }
        hash = 23 * hash + (int) (Double.doubleToLongBits(this.wetMass) ^ (Double.doubleToLongBits(this.wetMass) >>> 32));
        hash = 23 * hash + (int) (Double.doubleToLongBits(this.dryMass) ^ (Double.doubleToLongBits(this.dryMass) >>> 32));
        hash = 23 * hash + (int) (Double.doubleToLongBits(this.grossMass) ^ (Double.doubleToLongBits(this.grossMass) >>> 32));
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
        final Satellite other = (Satellite) obj;
        if (!Objects.equals(this.payload, other.payload)) {
            return false;
        }

        try {
            //check if orbits are equal
            TimeStampedPVCoordinates pv = this.orbit.getPVCoordinates(FramesFactory.getEME2000());
            TimeStampedPVCoordinates pv_other = this.orbit.getPVCoordinates(FramesFactory.getEME2000());
            if (!Objects.equals(pv.getMomentum(), pv_other.getMomentum())) {
                return false;
            }
            if (!Objects.equals(pv.getAcceleration(), pv_other.getAcceleration())) {
                return false;
            }
            if (!Objects.equals(pv.getAngularVelocity(), pv_other.getAngularVelocity())) {
                return false;
            }

            //check if attitude providers are equal
            if ((this.getAttProv() != null && other.getAttProv() == null)
                    || (this.getAttProv() == null && other.getAttProv() != null)) {
                return false;
            }
            if (this.getAttProv() != null && other.getAttProv() != null) {
                double[][] rotMmatrixThis = attProv.getAttitude(orbit, AbsoluteDate.GPS_EPOCH, FramesFactory.getEME2000()).getRotation().getMatrix();
                double[][] rotMmatrixOther = other.getAttProv().getAttitude(orbit, AbsoluteDate.GPS_EPOCH, FramesFactory.getEME2000()).getRotation().getMatrix();
                if(!Objects.equals(rotMmatrixThis,rotMmatrixOther)){
                    return false;
                }
            }

        } catch (OrekitException ex) {
            Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (Double.doubleToLongBits(this.wetMass) != Double.doubleToLongBits(other.wetMass)) {
            return false;
        }
        if (Double.doubleToLongBits(this.dryMass) != Double.doubleToLongBits(other.dryMass)) {
            return false;
        }
        if (Double.doubleToLongBits(this.grossMass) != Double.doubleToLongBits(other.grossMass)) {
            return false;
        }
        return true;
    }

}
