/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.object.communications;

import java.util.Set;
import seak.orekit.object.CommunicationBand;

/**
 * An abstract transmitter to send signals to receivers
 *
 * @author nhitomi
 */
public class AbstractTransmitter implements Transmitter {

    /**
     * the gain for transmitting signals
     */
    private final double gainR;

    /**
     * the bands supported by this transmitter
     */
    private final Set<CommunicationBand> bands;

    /**
     * Constructs a new transmitter
     *
     * @param gainR the gain for transmitting signals
     * @param bands the bands supported by this transmitter
     */
    public AbstractTransmitter(double gainR, Set<CommunicationBand> bands) {
        this.gainR = gainR;
        this.bands = bands;
    }

    @Override
    public double getGainT() {

        return gainR;
    }

    @Override
    public boolean compatible(Receiver receiver) {
        for (CommunicationBand band : getBands()) {
            if (receiver.getBands().contains(band)) {
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
