/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.coverage.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import seak.orekit.coverage.access.TimeIntervalArray;
import seak.orekit.object.CoveragePoint;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;

/**
 * This class computes several standard metrics regarding events (occurring and
 * not occurring) computed during a coverage simulation
 *
 * @author nozomihitomi
 */
public class GroundEventAnalyzer {

    /**
     * Collection of the CoveragePoints
     */
    private final Map<CoveragePoint, TimeIntervalArray> events;

    /**
     * Creates an analyzer from a set of coverage points and time interval array
     * of event occurrences
     *
     * @param events
     */
    public GroundEventAnalyzer(Map<CoveragePoint, TimeIntervalArray> events) {
        this.events = events;
    }

    /**
     * Gets the collection of coverage points covered in this analysis. The
     * returned collection is sorted by latitude first then by longitude in
     * ascending order
     *
     * @return
     */
    public ArrayList<CoveragePoint> getCoveragePoints() {
        ArrayList<CoveragePoint> points = new ArrayList(events.keySet());
        Collections.sort(points);
        return points;
    }

    /**
     * Computes the statistics for a specified metric over all points given to
     * the analyzer
     *
     * @param metric the metric to compute
     * @param occurrences flag to declare whether to use the saved time
     * intervals [true] or its complement [false].
     * @return statistics computed on the specified metric
     */
    public DescriptiveStatistics getStatistics(AnalysisMetric metric, boolean occurrences) {
        return this.getStatistics(metric, occurrences,
                new double[]{-Math.PI / 2, Math.PI / 2},
                new double[]{-Math.PI, Math.PI});
    }

    /**
     * Computes the statistics for a specified metric on the points in the given
     * bounds for latitude and longitude
     *
     * @param metric the metric to compute
     * @param occurrences flag to declare whether to use the saved time
     * intervals [true] or its complement [false].
     * @param latBounds latitude bounds
     * @param lonBounds longitude bounds
     * @return statistics computed on the specified metric
     */
    public DescriptiveStatistics getStatistics(AnalysisMetric metric, boolean occurrences, double[] latBounds, double[] lonBounds) {
        if (latBounds[0] > latBounds[1] || latBounds[0] < -Math.PI / 2 || latBounds[0] > Math.PI / 2
                || latBounds[1] < -Math.PI / 2 || latBounds[1] > Math.PI / 2) {
            throw new IllegalArgumentException(
                    String.format("Expected latitude bounds to be within [-pi/2,pi/2]. Found [%f,%f]", latBounds[0], latBounds[1]));
        }
        if (lonBounds[0] > lonBounds[1] || lonBounds[0] < -Math.PI || lonBounds[0] > Math.PI
                || lonBounds[1] < -Math.PI || lonBounds[1] > Math.PI) {
            throw new IllegalArgumentException(
                    String.format("Expected longitude bounds to be within [-pi,pi]. Found [%f,%f]", lonBounds[0], lonBounds[1]));
        }
        ArrayList<CoveragePoint> points = new ArrayList<>();
        for (CoveragePoint pt : events.keySet()) {
            if (pt.getPoint().getLatitude() >= latBounds[0]
                    && pt.getPoint().getLatitude() <= latBounds[1]
                    && pt.getPoint().getLongitude() >= lonBounds[0]
                    && pt.getPoint().getLongitude() <= lonBounds[1]) {
                points.add(pt);
            }
        }
        return this.getStatistics(metric, occurrences, points);
    }
    
    /**
     * Computes the statistics for a specified metric on the specified point
     *
     * @param metric the metric to compute
     * @param occurrences flag to declare whether to use the saved time
     * intervals [true] or its complement [false].
     * @param point the specific point to compute the metrics on
     
     * @return statistics computed on the specified metric
     */
    public DescriptiveStatistics getStatistics(AnalysisMetric metric, boolean occurrences, CoveragePoint point) {
        ArrayList<CoveragePoint> list = new ArrayList<>();
        list.add(point);
        return this.getStatistics(metric, occurrences, list);
    }

    /**
     * Computes the statistics for a specified metric on the specified subset of
     * given points
     *
     * @param metric the metric to compute
     * @param occurrences flag to declare whether to use the saved time
     * intervals [true] or its complement [false].
     * @param points the subset of points to compute the metrics on
     
     * @return statistics computed on the specified metric
     */
    public DescriptiveStatistics getStatistics(AnalysisMetric metric, boolean occurrences, Collection<CoveragePoint> points) {
        Map<CoveragePoint, TimeIntervalArray> data = new HashMap<>();
        for (CoveragePoint cp : points) {
            if (occurrences) {
                data.put(cp, events.get(cp));
            } else {
                data.put(cp, events.get(cp).complement());
            }
        }

        DescriptiveStatistics ds = new DescriptiveStatistics();

        switch (metric) {
            case DURATION:
                for (CoveragePoint cp : data.keySet()) {
                    for (double duration : data.get(cp).getDurations()) {
                        ds.addValue(duration);
                    }
                }
                break;
            case MEAN_TIME_TO_T:
                DescriptiveStatistics responseTime = new DescriptiveStatistics();
                for (CoveragePoint cp : data.keySet()) {
                    for (double duration : data.get(cp).getDurations()) {
                        responseTime.addValue(duration);
                    }
                    double sumsquares = responseTime.getSumOfSquares();
                    double totalTime = data.get(cp).getTail().durationFrom(
                            data.get(cp).getHead());
                    ds.addValue(sumsquares / (2 * totalTime));
                }
                break;
            case TIME_AVERAGE:
                DescriptiveStatistics respTime = new DescriptiveStatistics();
                for (CoveragePoint cp : data.keySet()) {
                    for (double duration : data.get(cp).getDurations()) {
                        respTime.addValue(duration);
                    }
                    double sumsquares = respTime.getSumOfSquares();
                    double totalTime = data.get(cp).getTail().durationFrom(
                            data.get(cp).getHead());
                    ds.addValue(sumsquares / totalTime); //only thing different than mean time to T
                }
                break;
            case PERCENT_TIME:
                double sumDuration = 0;
                for (CoveragePoint cp : data.keySet()) {
                    for (double duration : data.get(cp).getDurations()) {
                        sumDuration += duration;
                    }
                    double totalTime = data.get(cp).getTail().durationFrom(
                            data.get(cp).getHead());
                    ds.addValue(sumDuration / totalTime);
                }
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupported metric %s.", metric));
        }
        return ds;
    }
}
