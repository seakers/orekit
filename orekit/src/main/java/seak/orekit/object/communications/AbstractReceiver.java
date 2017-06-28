/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.object.communications;

import java.util.Set;
import seak.orekit.object.CommunicationBand;

/**
 * An abstract receiver to communicate with transmitters
 *
 * @author nhitomi
 */
public class AbstractReceiver implements Receiver {

    /**
     * the gain for receiving incoming signals
     */
    private final double gainR;

    /**
     * the bands supported by this receiver
     */
    private final Set<CommunicationBand> bands;

    /**
     * Constructs a new receiver
     *
     * @param gainR the gain for receiving incoming signals
     * @param bands the bands supported by this receiver
     */
    public AbstractReceiver(double gainR, Set<CommunicationBand> bands) {
        this.gainR = gainR;
        this.bands = bands;
    }

    @Override
    public double getGainR() {

        return gainR;
    }

    @Override
    public boolean compatible(Transmitter transmitter) {
        for (CommunicationBand band : getBands()) {
            if (transmitter.getBands().contains(band)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<CommunicationBand> getBands() {
        return bands;
    }

}
