/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object;

import orekit.access.TimeIntervalArray;
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
public class CoveragePoint extends TopocentricFrame implements OrekitObject {

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
     * @return
     */
    public TimeIntervalArray getAccesses() {
        return accesses;
    }
    
    /**
     * Adds the rise time (start time) of an access
     * @param riseTime time when the access begins
     */
    public void addRiseTime(AbsoluteDate riseTime) {
        accesses.addRiseTime(riseTime);
    }
    
    /**
     * Adds the set time (end time) of an access
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
        accesses = new TimeIntervalArray(endDate, endDate);
    }
}
