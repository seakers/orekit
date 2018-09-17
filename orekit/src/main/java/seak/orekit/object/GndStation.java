/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.object;

import java.util.Objects;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.TopocentricFrame;
import seak.orekit.object.communications.Receiver;
import seak.orekit.object.communications.Transmitter;

/**
 * Object to conduct studies with ground stations
 *
 * @author nhitomi
 */
public class GndStation extends GroundStation implements OrekitObject {

    /**
     * downlink antenna for this ground station
     */
    private final Receiver receiver;

    /**
     * uplink antenna for this ground station
     */
    private final Transmitter transmitter;

    /**
     * Minimum elevation angle [rad]
     */
    private final double minEl;
    
    /**
     * Flag for if the ground station is designated or not
     */
    private final int designated;

    /**
     * Communication band type designation
     */
    private final String[] commBandType;


    /**
     * Creates a new ground station
     *
     * @param topo the topocentric frame defining the position of the ground
     * station
     * @param receiver the receiving antenna
     * @param transmitter the transmitting antenna
     * @param minEl the minimum elevation angle [rad]
     * @param designated the flag that returns whether this gnd station is designated or not
     * @param commBandType returns the communication bands for this ground station
     * @throws OrekitException
     */
    public GndStation(TopocentricFrame topo,
            Receiver receiver, Transmitter transmitter, double minEl, int designated, String[] commBandType) throws OrekitException {
        super(topo);
        this.receiver = receiver;
        this.transmitter = transmitter;
        this.minEl = minEl;
        this.designated = designated;
        this.commBandType = commBandType;
    }

    /**
     * Gets the antenna for downlinking receiver to this ground station
     *
     * @return the antenna for downlinking receiver to this ground station
     */
    public Receiver getReceiver() {
        return receiver;
    }

    /**
     * Gets the antenna for uplinking transmitter from this ground station
     *
     * @return the antenna for uplinking transmitter from this ground station
     */
    public Transmitter getTransmitter() {
        return transmitter;
    }

    /**
     * The minimum elevation angle that at satellite can be with respect to the
     * ground station and still communicate
     *
     * @return
     */
    public double getMinEl() {
        return minEl;
    }
    
    /**
     * Gets the flag for if the ground station is designated or not
     *
     * @return the designated flag from this ground station
     */
    public int getDesignated() {
        return designated;
    }
    
    /**
     * Gets the types of communication bands for this ground station
     *
     * @return a string with communication bands for this ground station
     */
    public String[] getCommBandType() {
        return commBandType;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 53 * hash + Objects.hashCode(this.receiver);
        hash = 53 * hash + Objects.hashCode(this.transmitter);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GndStation other = (GndStation) obj;
        if (!Objects.equals(this.receiver, other.receiver)) {
            return false;
        }
        if (!Objects.equals(this.transmitter, other.transmitter)) {
            return false;
        }
        if (!Objects.equals(this.getBaseFrame().getPoint(),
                other.getBaseFrame().getPoint())) {
            return false;
        }
        return true;
    }

}
