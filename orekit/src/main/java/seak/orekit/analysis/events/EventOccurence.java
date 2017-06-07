/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.analysis.events;

/**
 * This class stores the time steps and gvalues right before and right after an event occurs.
 * @author nozomihitomi
 */
public class EventOccurence{
    
    /**
     * The time stamp in epoch seconds right before the event occurs
     */
    private final double dateBefore;
    
    /**
     * The time stamp  in epoch seconds right after the event occurs
     */
    private final double dateAfter;
    
    /**
     * The gvalue right before the event occurs
     */
    private final double valBefore;
    
    /**
     * The gvalue right after the event occurs
     */
    private final double valAfter;

    /**
     * 
     * @param dateBefore The time stamp in epoch seconds right before the event occurs
     * @param dateAfter The time stamp  in epoch seconds right after the event occurs
     * @param valBefore The gvalue right before the event occurs
     * @param valAfter The gvalue right after the event occurs
     */
    public EventOccurence(double dateBefore, double dateAfter, double valBefore, double valAfter) {
        this.dateBefore = dateBefore;
        this.dateAfter = dateAfter;
        this.valBefore = valBefore;
        this.valAfter = valAfter;
    }

    /**
     * 
     * @return The time stamp in epoch seconds right before the event occurs
     */
    public double getDateBefore() {
        return dateBefore;
    }

    /**
     * 
     * @return The time stamp  in epoch seconds right after the event occurs
     */
    public double getDateAfter() {
        return dateAfter;
    }

    /**
     * 
     * @return The gvalue right before the event occurs
     */
    public double getValBefore() {
        return valBefore;
    }

    /**
     * 
     * @return The gvalue right after the event occurs
     */
    public double getValAfter() {
        return valAfter;
    }
    
}
