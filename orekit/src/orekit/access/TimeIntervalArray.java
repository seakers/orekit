/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.access;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;

/**
 * Array to store intervals of rise and set times (e.g. gaps or accesses). The
 * class allows users to combine arrays with logical OR or AND to find the union
 * or intersection of intervals, respectively.
 *
 * @author nozomihitomi
 */
public class TimeIntervalArray implements Iterable<RiseSetTime>, Serializable {

    private static final long serialVersionUID = 394081713593567685L;

    private final ArrayList<RiseSetTime> timeArray;
    private boolean accessing; //boolean to track if a rise time has a corresponding set time

    /**
     * Head is the beginning date of the timeline
     */
    private final AbsoluteDate head;
    /**
     * Head is the ending date of the timeline
     */
    private final AbsoluteDate tail;

    /**
     * Creates a time interval array instance with the head and tail date times
     * to define the entire timeline
     *
     * @param head
     * @param tail
     */
    public TimeIntervalArray(AbsoluteDate head, AbsoluteDate tail) {
        this.head = head;
        this.tail = tail;
        this.timeArray = new ArrayList<>();
    }

    /**
     * Adds a new rise time to the array to the start of a time interval. If
     * previous intervals exists, the newly added rise time must occur after the
     * set time of the previous interval. If an interval remains "open" (i.e.
     * has no set time) then the interval must be "closed" before a new rise
     * time can be added.
     *
     * @param riseTime
     * @return true if the interval is open, false if it is closed.
     */
    public boolean addRiseTime(AbsoluteDate riseTime) {
        if (accessing) {
            throw new IllegalArgumentException(String.format("Cannot add rise time %s since interval is not closed yet.", riseTime));
        }

        if (riseTime.compareTo(head) < 0) {
            throw new IllegalArgumentException(String.format("Cannot add rise time %s before the head of the timeline.", riseTime));
        }

        timeArray.add(new RiseSetTime(riseTime, true));
        accessing = !accessing;
        return accessing;
    }

    /**
     * Adds a new set time to the array to the end of a time interval. The newly
     * added set time must occur after the rise time of the current interval. If
     * an interval is not "open" (i.e. has no rise time) then the interval must
     * be "opened" before a new set time can be added.
     *
     * @param setTime
     * @return true if the interval is open, false if it is closed.
     */
    public boolean addSetTime(AbsoluteDate setTime) {
        if (timeArray.isEmpty()) {
            addRiseTime(head);
        } else {

            if (!accessing) {
                throw new IllegalArgumentException(String.format("Cannot add set time %s since interval is not open yet.", setTime));
            }

            if (setTime.compareTo(tail) > 0) {
                throw new IllegalArgumentException(String.format("Cannot add set time %s after the tail of the timeline.", setTime));
            }
        }

        timeArray.add(new RiseSetTime(setTime, false));
        accessing = !accessing;
        return accessing;
    }

    /**
     * Check to see if the tail of the array is currently an open time interval
     * (i.e. ends with a rise time).
     *
     * @return
     */
    public boolean isTailOpen() {
        return accessing;
    }

    /**
     * Check to see if the head of the array is an open time interval (i.e.
     * starts with a set time).
     *
     * @return
     */
    public boolean isHeadOpen() {
        return !timeArray.get(0).isRise();
    }

    /**
     * Returns the number of closed intervals. Open intervals at the ends of the
     * array are counted as they are automatically closed with the head or tail
     * times
     *
     * @return
     */
    public int numIntervals() {
        boolean headOpen = isHeadOpen();
        boolean tailOpen = isTailOpen();

        if (headOpen && !tailOpen) {
            return FastMath.floorDiv(timeArray.size(), 2) + 1;
        } else if (!headOpen && tailOpen) {
            return FastMath.floorDiv(timeArray.size(), 2) + 1;
        } else if (headOpen && tailOpen) {
            return FastMath.floorDiv(timeArray.size(), 2);
        } else {
            return FastMath.floorDiv(timeArray.size(), 2);
        }
    }

    /**
     * Returns a copy of the time intervals ordered chronologically, and the
     * rise and set times alternate within the array.
     *
     * @return
     */
    public ArrayList<RiseSetTime> getRiseSetTimes() {
        return new ArrayList<>(timeArray);
    }

    /**
     * Returns an array of the durations of each interval stored in the array.
     * The returned array will contain the durations in chronological order of
     * the time intervals
     *
     * @return
     */
    public double[] getDurations() {
        int nIntervals = numIntervals();
        double[] durations = new double[nIntervals];
        int durationIndex = 0;

        int startInd = 0;
        int endInd = timeArray.size();

        if (isHeadOpen()) {
            startInd++;
            durationIndex++;
            durations[0] = timeArray.get(0).durationFrom(head);
        }
        if (isTailOpen()) {
            endInd--;
            durations[nIntervals - 1] = timeArray.get(endInd).durationFrom(timeArray.get(endInd - 1));
        }

        for (int i = startInd; i < endInd; i += 2) {
            durations[durationIndex] = timeArray.get(i + 1).durationFrom(timeArray.get(i));
        }

        return durations;
    }

    /**
     * Returns a time interval array that contains the gaps between the time
     * intervals stored in this array. For example, if this array was a time
     * storing rise and set times of accesses between a grid point and a
     * satellite, this method would return the gap times.
     *
     * @return Returns a time interval array that contains the gaps between the
     * time intervals stored in this array.
     */
    public TimeIntervalArray negate() {
        //if the array is empty, return new instance of a time interval array
        if (timeArray.isEmpty()) {
            return new TimeIntervalArray(head, tail);
        }

        TimeIntervalArray out = new TimeIntervalArray(head, tail);
        Iterator<RiseSetTime> iter = timeArray.iterator();

        RiseSetTime current = iter.next();
        if (head.compareTo(current) < 0) {
            out.addRiseTime(head);
            out.addSetTime(current);
        }

        while (iter.hasNext()) {
            current = iter.next();
            if (current.isRise()) {
                out.addSetTime(current);
            } else {
                out.addRiseTime(current);
            }
        }

        return out;
    }

    /**
     * Gets the head time stamp where the timeline starts
     *
     * @return the head time stamp where the timeline starts
     */
    public AbsoluteDate getHead() {
        return head;
    }

    /**
     * Gets the head time stamp where the timeline ends
     *
     * @return the head time stamp where the timeline ends
     */
    public AbsoluteDate getTail() {
        return tail;
    }

    @Override
    public Iterator<RiseSetTime> iterator() {
        return timeArray.iterator();
    }

    /**
     * Checks to see if there are any intervals in this array
     *
     * @return
     */
    public boolean isEmpty() {
        return timeArray.isEmpty();
    }

}
