/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.ephemeris;

import java.io.Serializable;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;

/**
 * 
 * @author nozomihitomi
 */
public class Ephemeris implements Serializable, Comparable<Ephemeris>{
    private static final long serialVersionUID = -3726051095120448229L;
    
    /**
     * Semi-major axis[km]
     */
    private final double sa;
    
    /**
     * Eccentricity
     */
    private final double ecc;
    
    /**
     * Inclination[rad]
     */
    private final double inc;
    
    /**
     * Right ascension of the ascending node[rad]
     */
    private final double raan;
    
    /**
     * argument of perigee [rad]
     */
    private final double argPer;
    
    /**
     * mean anomaly [rad]
     */
    private final double ma;
    
    /**
     * Date of the ephemeris
     */
    private final AbsoluteDate date;

    /**
     * 
     * @param sa Semi-major axis[km]
     * @param ecc Eccentricity
     * @param inc Inclination[rad]
     * @param raan Right ascension of the ascending node[rad]
     * @param argPer argument of perigee [rad]
     * @param ma mean anomaly [rad]
     * @param date Date of the ephemeris
     */
    public Ephemeris(double sa, double ecc, double inc, double raan, double argPer, double ma, AbsoluteDate date) {
        this.sa = sa;
        this.ecc = ecc;
        this.inc = inc;
        this.raan = raan;
        this.argPer = argPer;
        this.ma = ma;
        this.date = date;
    }

    /**
     * Returns the semi-major axis in km
     * @return 
     */
    public double getSa() {
        return sa;
    }

    /**
     * Returns the eccentricity
     * @return 
     */
    public double getEcc() {
        return ecc;
    }

    /**
     * Returns the inclination in degrees
     * @return 
     */
    public double getInc() {
        return FastMath.toDegrees(inc);
    }

    /**
     * Returns the right ascension of the ascending node in degrees
     * @return 
     */
    public double getRaan() {
        return FastMath.toDegrees(raan);
    }

    /**
     * Returns the argument of perigee in degrees
     * @return 
     */
    public double getArgPer() {
        return FastMath.toDegrees(argPer);
    }

    /**
     * Returns the mean anomaly in degrees
     * @return 
     */
    public double getMa() {
        return FastMath.toDegrees(ma);
    }

    /**
     * Returns the date of the ephemeris
     * @return 
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /**
     * Use to compare the dates of two ephemeris data points
     * @param o
     * @return 
     */
    @Override
    public int compareTo(Ephemeris o) {
        return this.getDate().compareTo(o.getDate());
    }
    
    
}
