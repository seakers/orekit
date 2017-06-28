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
    private final Receiver downlink;

    /**
     * uplink antenna for this ground station
     */
    private final Transmitter uplink;

    public GndStation(TopocentricFrame topo,
            Receiver downlink, Transmitter uplink) throws OrekitException {
        super(topo);
        this.downlink = downlink;
        this.uplink = uplink;
    }

    /**
     * Gets the antenna for downlinking receiver to this ground station
     *
     * @return the antenna for downlinking receiver to this ground station
     */
    public Receiver getReceiver() {
        return downlink;
    }

    /**
     * Gets the antenna for uplinking transmitter from this ground station
     *
     * @return the antenna for uplinking transmitter from this ground station
     */
    public Transmitter getTransmitter() {
        return uplink;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 53 * hash + Objects.hashCode(this.downlink);
        hash = 53 * hash + Objects.hashCode(this.uplink);
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
        if (!Objects.equals(this.downlink, other.downlink)) {
            return false;
        }
        if (!Objects.equals(this.uplink, other.uplink)) {
            return false;
        }
        if (!Objects.equals(this.getBaseFrame().getPoint(), 
                other.getBaseFrame().getPoint())) {
            return false;
        }
        return true;
    }

    
}
