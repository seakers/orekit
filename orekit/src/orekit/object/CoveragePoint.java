/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object;

import orekit.coverage.access.TimeIntervalArray;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;

/**
 * A point within a coverage definition. Allows user to retrieve accesses by
 * satellites
 *
 * @author nozomihitomi
 */
public class CoveragePoint extends TopocentricFrame implements OrekitObject, Comparable<CoveragePoint> {

    private static final long serialVersionUID = 5330957252938339044L;

    /**
     * stores the accesses between viewer and this point
     */
    private TimeIntervalArray accesses;
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    /**
     * A coverage point is one element that is comprised within the
     * CoverageDefinition. It is defined by the latitude, longitude, and
     * altitude along with the shape of the body on which the points are
     * projected. This class also stores the accesses between a viewer (e.g.
     * satellite) and this point.
     *
     * @param parentShape The shape of the body to position the point
     * @param point The geodetic location (latitude, longitude, altitude)
     * @param name name of the point
     * @param startDate date when the scenario starts
     * @param endDate date when the scenario ends
     */
    public CoveragePoint(BodyShape parentShape, GeodeticPoint point, String name, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(parentShape, point, name);
        this.startDate = startDate;
        this.endDate = endDate;
        this.accesses = new TimeIntervalArray(startDate, endDate);
    }

    /**
     * Gets the accesses stored
     *
     * @return
     */
    public TimeIntervalArray getAccesses() {
        return accesses;
    }

    /**
     * Adds the rise time (start time) of an access
     *
     * @param riseTime time when the access begins
     */
    public void addRiseTime(AbsoluteDate riseTime) {
        accesses.addRiseTime(riseTime);
    }

    /**
     * Adds the set time (end time) of an access
     *
     * @param setTime time when the access ends
     */
    public void addSetTime(AbsoluteDate setTime) {
        accesses.addSetTime(setTime);
    }

    /**
     * Resets the stored time interval array to a new instance of a
     * TimeIntervalArray
     */
    public void reset() {
        accesses = new TimeIntervalArray(startDate, endDate);
    }

    /**
     * This method can be used to order the coverage points first by latitude
     * and then by longitude in ascending order.
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(CoveragePoint o) {
        //first compare the latitudes
        if (this.getPoint().getLatitude() < o.getPoint().getLatitude()) {
            return -1;
        } else if (this.getPoint().getLatitude() > o.getPoint().getLatitude()) {
            return 1;
        } else {
            //next compare longitudes
            if (this.getPoint().getLongitude()< o.getPoint().getLongitude()) {
                return -1;
            } else if (this.getPoint().getLongitude()> o.getPoint().getLongitude()) {
                return 1;
            }
            else 
                return 0;
        }
    }

    @Override
    public int hashCode() {
        return this.getPoint().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CoveragePoint other = (CoveragePoint) obj;
        return this.getPoint().equals(other.getPoint());
    }
    
}
