/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

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
    
    public Satellite(String name, Orbit orbit, AttitudeProvider attProv) {
        this(name, orbit, attProv, Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
    }

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
        String out = "";
        ParameterDriversList paramsList;
        try {
            //error for driver is arbitrarily set to 0 because only need the name of the drivers
            paramsList = this.orbit.getType().getDrivers(0, orbit, PositionAngle.TRUE);
            double[] orbParams = new double[paramsList.getNbParams()];
            this.orbit.getType().mapOrbitToArray(orbit, PositionAngle.TRUE, orbParams);

            //print out calculations are based on javadoc provided by EquinoctialOrbit class
            int i = 0;
            for(ParameterDriver param : paramsList.getDrivers()){
                out += String.format("%s: %f\t",param.getName(),orbParams[i]);
                i++;
            }
        } catch (OrekitException ex) {
            Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
        }

        return out;
    }
    
    public String ppPayload() {
        String out = "{";
        for(Instrument inst : payload){
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
