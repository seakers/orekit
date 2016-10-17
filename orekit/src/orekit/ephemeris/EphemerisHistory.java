/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.ephemeris;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Stores the ephemeris over the history of the spacecraft
 *
 * @author nozomihitomi
 */
public class EphemerisHistory extends ArrayList<Ephemeris> {

    private static final long serialVersionUID = 7950854139353538464L;

    /**
     * Container for each ephemeris data point
     */
    private final ArrayList<Ephemeris> history;
    
    /**
     * flag to keep track if the history is sorted by date
     */
    private boolean sorted;

    public EphemerisHistory() {
        this.history = new ArrayList();
    }

    @Override
    public boolean addAll(int index, Collection<? extends Ephemeris> c) {
        sorted = false;
        return super.addAll(index, c); 
    }

    @Override
    public boolean addAll(Collection<? extends Ephemeris> c) {
        sorted = false;
        return super.addAll(c); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void add(int index, Ephemeris element) {
        sorted = false;
        super.add(index, element); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean add(Ephemeris e) {
        sorted = false;
        return super.add(e); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Sorts the ephemeris history by date, starting from the earliest event.
     */
    public void sortByDate() {
        Collections.sort(this);
        sorted = true;
    }

    /**
     * Gets the time ordered history of the semi-major axis
     * @return 
     */
    public ArrayList<Double> getSA() {
        if(!sorted)
            sortByDate();
        ArrayList<Double> out = new ArrayList<>(this.size());
        for(Ephemeris e : this){
            out.add(e.getSa());
        }
        return out;
    }
    
    /**
     * Gets the time ordered history of the eccentricity
     * @return 
     */
    public ArrayList<Double> getEcc() {
        if(!sorted)
            sortByDate();
        ArrayList<Double> out = new ArrayList<>(this.size());
        for(Ephemeris e : this){
            out.add(e.getEcc());
        }
        return out;
    }
    
    /**
     * Gets the time ordered history of the inclination
     * @return 
     */
    public ArrayList<Double> getInc() {
        if(!sorted)
            sortByDate();
        ArrayList<Double> out = new ArrayList<>(this.size());
        for(Ephemeris e : this){
            out.add(e.getInc());
        }
        return out;
    }
    
    /**
     * Gets the time ordered history of the right ascension of the ascending node
     * @return 
     */
    public ArrayList<Double> getRaan() {
        if(!sorted)
            sortByDate();
        ArrayList<Double> out = new ArrayList<>(this.size());
        for(Ephemeris e : this){
            out.add(e.getRaan());
        }
        return out;
    }
    
    /**
     * Gets the time ordered history of the argument of perigee
     * @return 
     */
    public ArrayList<Double> getArgPer() {
        if(!sorted)
            sortByDate();
        ArrayList<Double> out = new ArrayList<>(this.size());
        for(Ephemeris e : this){
            out.add(e.getArgPer());
        }
        return out;
    }
    
    /**
     * Gets the time ordered history of the mean anomaly
     * @return 
     */
    public ArrayList<Double> getMA() {
        if(!sorted)
            sortByDate();
        ArrayList<Double> out = new ArrayList<>(this.size());
        for(Ephemeris e : this){
            out.add(e.getMa());
        }
        return out;
    }
}
