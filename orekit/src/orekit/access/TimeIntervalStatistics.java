/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author nozomihitomi
 */
public class TimeIntervalStatistics {

    /**
     * Computes the mean duration [seconds] of the events stored in the
     * TimeIntervalArray
     *
     * @param array
     * @return
     */
    public static double mean(TimeIntervalArray array) {
        if (array.isEmpty()){
            return Double.NaN;
        }
        
        Iterator<RiseSetTime> iter = array.iterator();
        double sum = 0;
        int numIntervals = array.numIntervals();
        AbsoluteDate riseTime;
        AbsoluteDate setTime;
        while (iter.hasNext()) {
            riseTime = iter.next();
            if (iter.hasNext()) {
                setTime = iter.next();
            } else {
                setTime = array.getTail();
                numIntervals++;
            }
            sum += setTime.durationFrom(riseTime);
        }
        return sum / numIntervals;
    }

    /**
     * Computes the max duration [seconds] of the events stored in the
     * TimeIntervalArray
     *
     * @param array
     * @return
     */
    public static double max(TimeIntervalArray array) {
        if (array.isEmpty()){
            return Double.NaN;
        }
        
        Iterator<RiseSetTime> iter = array.iterator();
        double max = Double.NEGATIVE_INFINITY;
        AbsoluteDate riseTime;
        AbsoluteDate setTime;
        while (iter.hasNext()) {
            riseTime = iter.next();
            if (iter.hasNext()) {
                setTime = iter.next();
            } else {
                setTime = array.getTail();
            }
            max = FastMath.max(max, setTime.durationFrom(riseTime));
        }
        return max;
    }

    /**
     * Computes the min duration [seconds] of the events stored in the
     * TimeIntervalArray
     *
     * @param array
     * @return
     */
    public static double min(TimeIntervalArray array) {
        if (array.isEmpty()){
            return Double.NaN;
        }
        
        Iterator<RiseSetTime> iter = array.iterator();
        double min = Double.POSITIVE_INFINITY;
        AbsoluteDate riseTime;
        AbsoluteDate setTime;
        while (iter.hasNext()) {
            riseTime = iter.next();
            if (iter.hasNext()) {
                setTime = iter.next();
            } else {
                setTime = array.getTail();
            }
            min = FastMath.min(min, setTime.durationFrom(riseTime));
        }
        return min;
    }

    /**
     * Finds the xth percentile duration [seconds] of the events stored in the
     * TimeIntervalArray
     *
     * @param array
     * @param x should be in range [0,100]
     * @return
     */
    public static double percentile(TimeIntervalArray array, double x) {
        if (x < 0 || x > 100) {
            throw new IllegalArgumentException(String.format("Percentile expected to be between [0, 100]. Found %f", x));
        }
        
        if (array.isEmpty()){
            return Double.NaN;
        }

        Iterator<RiseSetTime> iter = array.iterator();
        ArrayList<Double> durations = new ArrayList(array.numIntervals() / 2 + 1);
        AbsoluteDate riseTime;
        AbsoluteDate setTime;
        while (iter.hasNext()) {
            riseTime = iter.next();
            if (iter.hasNext()) {
                setTime = iter.next();
            } else {
                setTime = array.getTail();
            }
            durations.add(setTime.durationFrom(riseTime));
        }
        Collections.sort(durations);
        int index = (int) FastMath.floor((x / 100.0) * durations.size());
        return durations.get(index);
    }
}
