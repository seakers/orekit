/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.coverage.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import orekit.coverage.access.TimeIntervalArray;
import orekit.object.CoveragePoint;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;

/**
 * This class computes several standard metrics regarding accesses and gaps
 * computed during a coverage simulation
 *
 * @author nozomihitomi
 */
public class CoverageAnalyzer {

    /**
     * Data on all accesses across entire coverage definition
     */
    private final DescriptiveStatistics accessStats;

    /**
     * Data on all gaps across entire coverage definition
     */
    private final DescriptiveStatistics gapStats;

    /**
     * Data on accesses at each point in the coverage definition
     */
    private final HashMap<CoveragePoint, DescriptiveStatistics> accessesPerPoint;

    /**
     * Data on gaps at each point in the coverage definition
     */
    private final HashMap<CoveragePoint, DescriptiveStatistics> gapsPerPoint;

    /**
     * Data on accesses at each latitude in the coverage definition
     */
    private final HashMap<Double, DescriptiveStatistics> accessesPerLatitude;

    /**
     * Data on gaps at each latitude in the coverage definition
     */
    private final HashMap<Double, DescriptiveStatistics> gapsPerLatitude;
    
    /**
     * Collection of the CoveragePoints at each latitude
     */
    private final HashMap<Double, ArrayList<CoveragePoint>> pointsAtLatitude;

    /**
     * Creates an analyzer from a set of coverage points and time interval array
     * of accesses
     *
     * @param coverageDefinitionAccesses
     */
    public CoverageAnalyzer(HashMap<CoveragePoint, TimeIntervalArray> coverageDefinitionAccesses) {
        accessStats = new DescriptiveStatistics();
        gapStats = new DescriptiveStatistics();
        accessesPerPoint = new HashMap<>(coverageDefinitionAccesses.keySet().size());
        gapsPerPoint = new HashMap<>(coverageDefinitionAccesses.keySet().size());
        accessesPerLatitude = new HashMap<>();
        gapsPerLatitude = new HashMap<>();
        pointsAtLatitude = new HashMap<>();

        for (CoveragePoint pt : coverageDefinitionAccesses.keySet()) {
            accessesPerPoint.put(pt, new DescriptiveStatistics(coverageDefinitionAccesses.get(pt).numIntervals()));
            gapsPerPoint.put(pt, new DescriptiveStatistics(coverageDefinitionAccesses.get(pt).numIntervals()));

            double latitude = pt.getPoint().getLatitude();
            if (!accessesPerLatitude.containsKey(latitude)) {
                accessesPerLatitude.put(pt.getPoint().getLatitude(), new DescriptiveStatistics());
                gapsPerLatitude.put(pt.getPoint().getLatitude(), new DescriptiveStatistics());
                pointsAtLatitude.put(pt.getPoint().getLatitude(), new ArrayList<>());
            }

            for (Double duration : coverageDefinitionAccesses.get(pt).getDurations()) {
                accessStats.addValue(duration);
                accessesPerPoint.get(pt).addValue(duration);
                accessesPerLatitude.get(latitude).addValue(duration);
            }
            for (Double duration : coverageDefinitionAccesses.get(pt).negate().getDurations()) {
                gapStats.addValue(duration);
                gapsPerPoint.get(pt).addValue(duration);
                gapsPerLatitude.get(latitude).addValue(duration);
            }
        }
    }

    /**
     * Gets the collection of coverage points covered in this analysis. The
     * returned collection is sorted by latitude first then by longitude in
     * ascending order
     *
     * @return
     */
    public ArrayList<CoveragePoint> getCoveragePoints() {
        ArrayList<CoveragePoint> points = new ArrayList(accessesPerPoint.keySet());
        Collections.sort(points);
        return points;
    }

    /**
     * Gets the collection of coverage points at a specific latitude covered in
     * this analysis. The returned collection is sorted by latitude first then
     * by longitude in ascending order
     *
     * @param latitude latitude of interest
     * @return
     */
    public ArrayList<CoveragePoint> getCoveragePoints(double latitude) {
        ArrayList<CoveragePoint> points = new ArrayList(pointsAtLatitude.get(latitude));
        Collections.sort(points);
        return points;
    }

    /**
     * Gets the collection of latitudes covered in this analysis, in ascending
     * order
     *
     * @return
     */
    public ArrayList<Double> getLatitudes() {
        ArrayList<Double> latitudes = new ArrayList(accessesPerLatitude.keySet());
        Collections.sort(latitudes);
        return latitudes;
    }

    /**
     * Gets the maximum access duration over all points in the coverage
     * definition
     *
     * @return
     */
    public double getMaxAccess() {
        return accessStats.getMax();
    }

    /**
     * Gets the maximum access duration of a specified point
     *
     * @param pt the point of interest
     * @return NaN if specified point is not in the coverage definition
     */
    public double getMaxAccess(CoveragePoint pt) {
        if (accessesPerPoint.containsKey(pt)) {
            return accessesPerPoint.get(pt).getMax();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the maximum access duration of a specified point
     *
     * @param latitude the latitude of interest
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getMaxAccess(double latitude) {
        if (accessesPerLatitude.containsKey(latitude)) {
            return accessesPerLatitude.get(latitude).getMax();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the minimum access duration over all points in the coverage
     * definition
     *
     * @return
     */
    public double getMinAccess() {
        return accessStats.getMin();
    }

    /**
     * Gets the minimum access duration of a specified point
     *
     * @param pt the point of interest
     * @return NaN if specified point is not in the coverage definition
     */
    public double getMinAccess(CoveragePoint pt) {
        if (accessesPerPoint.containsKey(pt)) {
            return accessesPerPoint.get(pt).getMin();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the minimum access duration of a specified point
     *
     * @param latitude the latitude of interest
     * @return NaN if specified point is not in the coverage definition
     */
    public double getMinAccess(double latitude) {
        if (accessesPerLatitude.containsKey(latitude)) {
            return accessesPerLatitude.get(latitude).getMin();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the mean access duration over all points in the coverage definition
     *
     * @return
     */
    public double getMeanAccess() {
        return accessStats.getMean();
    }

    /**
     * Gets the mean access duration of a specified point
     *
     * @param pt the point of interest
     * @return NaN if specified point is not in the coverage definition
     */
    public double getMeanAccess(CoveragePoint pt) {
        if (accessesPerPoint.containsKey(pt)) {
            return accessesPerPoint.get(pt).getMean();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the mean access duration of a specified point
     *
     * @param latitude the latitude of interest
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getMeanAccess(double latitude) {
        if (accessesPerLatitude.containsKey(latitude)) {
            return accessesPerLatitude.get(latitude).getMean();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the specified percentile access duration over all points in the
     * coverage definition
     *
     * @param pct specified percentile
     * @return
     */
    public double getPercentileAccess(double pct) {
        return accessStats.getPercentile(pct);
    }

    /**
     * Gets the specified percentile access duration of a specified point
     *
     * @param pt the point of interest
     * @param pct specified percentile
     * @return NaN if specified point is not in the coverage definition
     */
    public double getPercentileAccess(double pct, CoveragePoint pt) {
        if (accessesPerPoint.containsKey(pt)) {
            return accessesPerPoint.get(pt).getPercentile(pct);
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the specified percentile access duration of a specified point
     *
     * @param latitude the latitude of interest
     * @param pct specified percentile
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getPercentileAccess(double pct, double latitude) {
        if (accessesPerLatitude.containsKey(latitude)) {
            return accessesPerLatitude.get(latitude).getPercentile(pct);
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the empirical cdf that describes the access durations over all
     * points in the coverage definition
     *
     * @return
     */
    public double[] getCDFAccess() {
        double[] out = new double[100];
        for (int i = 0; i < 100; i++) {
            out[i] = accessStats.getPercentile(i);
        }
        return out;
    }

    /**
     * Gets the specified percentile access duration of a specified point
     *
     * @param pt the point of interest
     * @return empty double array if specified point is not in the coverage
     * definition
     */
    public double[] getCDFAccess(CoveragePoint pt) {
        if (accessesPerPoint.containsKey(pt)) {
            double[] out = new double[100];
            for (int i = 0; i < 100; i++) {
                out[i] = accessesPerPoint.get(pt).getPercentile(i);
            }
            return out;
        } else {
            return new double[0];
        }
    }

    /**
     * Gets the specified percentile access duration of a specified point
     *
     * @param latitude the latitude of interest
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double[] getCDFAccess(double latitude) {
        if (accessesPerLatitude.containsKey(latitude)) {
            double[] out = new double[100];
            for (int i = 0; i < 100; i++) {
                out[i] = accessesPerLatitude.get(latitude).getPercentile(i);
            }
            return out;
        } else {
            return new double[0];
        }
    }

    /**
     * Gets the sum of all access durations over all points in the coverage
     * definition
     *
     * @return
     */
    public double getSumAccess() {
        return accessStats.getSum();
    }

    /**
     * Gets the sum of all access durations over of a specified point
     *
     * @param pt the point of interest
     *
     * @return NaN if specified point is not in the coverage definition
     */
    public double getSumAccess(CoveragePoint pt) {
        if (accessesPerPoint.containsKey(pt)) {
            return accessesPerPoint.get(pt).getSum();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the sum of all access durations over of a specified point
     *
     * @param latitude the latitude of interest
     *
     * @return NaN if specified point is not in the coverage definition
     */
    public double getSumAccess(double latitude) {
        if (accessesPerLatitude.containsKey(latitude)) {
            return accessesPerLatitude.get(latitude).getSum();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the variance in the access durations over all points in the coverage
     * definition
     *
     * @return
     */
    public double getVarianceAccess() {
        return accessStats.getVariance();
    }

    /**
     * Gets the variance in the access durations of a specified point
     *
     * @param pt the point of interest
     *
     * @return NaN if specified point is not in the coverage definition
     */
    public double getVarianceAccess(CoveragePoint pt) {
        if (accessesPerPoint.containsKey(pt)) {
            return accessesPerPoint.get(pt).getVariance();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the variance in the access durations of a specified point
     *
     * @param latitude the latitude of interest
     *
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getVarianceAccess(double latitude) {
        if (accessesPerLatitude.containsKey(latitude)) {
            return accessesPerLatitude.get(latitude).getVariance();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets all the access durations over all points in the coverage definition
     * in ascending order
     *
     * @return
     */
    public double[] getSortedAccess() {
        return accessStats.getSortedValues();
    }

    /**
     * Gets all the access durations of a specified point
     *
     * @param pt the point of interest
     *
     * @return empty double[] if specified point is not in the coverage
     * definition
     */
    public double[] getSortedAccess(CoveragePoint pt) {
        if (accessesPerPoint.containsKey(pt)) {
            return accessesPerPoint.get(pt).getSortedValues();
        } else {
            return new double[0];
        }
    }

    /**
     * Gets all the access durations of a specified point
     *
     * @param latitude the latitude of interest
     *
     * @return empty double[] if specified latitude is not in the coverage
     * definition
     */
    public double[] getSortedAccess(double latitude) {
        if (accessesPerLatitude.containsKey(latitude)) {
            return accessesPerLatitude.get(latitude).getSortedValues();
        } else {
            return new double[0];
        }
    }

    /**
     * Gets the maximum access duration over all points in the coverage
     * definition
     *
     * @return
     */
    public double getMaxGap() {
        return gapStats.getMax();
    }

    /**
     * Gets the maximum access duration of a specified point
     *
     * @param pt the point of interest
     *
     * @return NaN if specified point is not in the coverage definition
     */
    public double getMaxGap(CoveragePoint pt) {
        if (gapsPerPoint.containsKey(pt)) {
            return gapsPerPoint.get(pt).getMax();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the maximum access duration of a specified point
     *
     * @param latitude the latitude of interest
     *
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getMaxGap(double latitude) {
        if (gapsPerLatitude.containsKey(latitude)) {
            return gapsPerLatitude.get(latitude).getMax();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the minimum access duration over all points in the coverage
     * definition
     *
     * @return
     */
    public double getMinGap() {
        return gapStats.getMin();
    }

    /**
     * Gets the minimum access duration of a specified point
     *
     * @param pt the point of interest
     *
     * @return NaN if specified point is not in the coverage definition
     */
    public double getMinGap(CoveragePoint pt) {
        if (gapsPerPoint.containsKey(pt)) {
            return gapsPerPoint.get(pt).getMin();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the minimum access duration of a specified point
     *
     * @param latitude the latitude of interest
     *
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getMinGap(double latitude) {
        if (gapsPerLatitude.containsKey(latitude)) {
            return gapsPerLatitude.get(latitude).getMin();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the mean access duration over all points in the coverage definition
     *
     * @return
     */
    public double getMeanGap() {
        return gapStats.getMean();
    }

    /**
     * Gets the mean access duration of a specified point
     *
     * @param pt the point of interest
     * @return NaN if specified point is not in the coverage definition
     */
    public double getMeanGap(CoveragePoint pt) {
        if (gapsPerPoint.containsKey(pt)) {
            return gapsPerPoint.get(pt).getMean();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the mean access duration of a specified point
     *
     * @param latitude the latitude of interest
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getMeanGap(double latitude) {
        if (gapsPerLatitude.containsKey(latitude)) {
            return gapsPerLatitude.get(latitude).getMean();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the specified percentile access duration over all points in the
     * coverage definition
     *
     * @param pct specified percentile
     * @return
     */
    public double getPercentileGap(double pct) {
        return gapStats.getPercentile(pct);
    }

    /**
     * Gets the specified percentile access duration of a specified point
     *
     * @param pt the point of interest
     * @param pct specified percentile
     * @return NaN if specified point is not in the coverage definition
     */
    public double getPercentileGap(double pct, CoveragePoint pt) {
        if (gapsPerPoint.containsKey(pt)) {
            return gapsPerPoint.get(pt).getPercentile(pct);
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the specified percentile access duration of a specified point
     *
     * @param latitude the latitude of interest
     * @param pct specified percentile
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getPercentileGap(double pct, double latitude) {
        if (gapsPerLatitude.containsKey(latitude)) {
            return gapsPerLatitude.get(latitude).getPercentile(pct);
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the empirical cdf that describes the access durations over all
     * points in the coverage definition
     *
     * @return
     */
    public double[] getCDFGap() {
        double[] out = new double[100];
        for (int i = 0; i < 100; i++) {
            out[i] = accessStats.getPercentile(i);
        }
        return out;
    }

    /**
     * Gets the empirical cdf that describes the gap durations of a specified
     * point
     *
     * @param pt the point of interest
     * @return empty double array if specified point is not in the coverage
     * definition
     */
    public double[] getCDFGap(CoveragePoint pt) {
        if (gapsPerPoint.containsKey(pt)) {
            double[] out = new double[100];
            for (int i = 0; i < 100; i++) {
                out[i] = gapsPerPoint.get(pt).getPercentile(i);
            }
            return out;
        } else {
            return new double[0];
        }
    }

    /**
     * Gets the empirical cdf that describes the gap durations of a specified
     * point
     *
     * @param latitude the latitude of interest
     * @return empty double array if specified latitude is not in coverage
     * definition
     */
    public double[] getCDFGap(double latitude) {
        if (gapsPerLatitude.containsKey(latitude)) {
            double[] out = new double[100];
            for (int i = 0; i < 100; i++) {
                out[i] = gapsPerLatitude.get(latitude).getPercentile(i);
            }
            return out;
        } else {
            return new double[0];
        }
    }

    /**
     * Gets the sum of all access durations over all points in the coverage
     * definition
     *
     * @return
     */
    public double getSumGap() {
        return gapStats.getSum();
    }

    /**
     * Gets the sum of all access durations of a specified point
     *
     * @param pt the point of interest
     *
     * @return NaN if specified point is not in the coverage definition
     */
    public double getSumGap(CoveragePoint pt) {
        if (gapsPerPoint.containsKey(pt)) {
            return gapsPerPoint.get(pt).getSum();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the sum of all access durations of a specified point
     *
     * @param latitude the latitude of interest
     *
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getSumGap(double latitude) {
        if (gapsPerLatitude.containsKey(latitude)) {
            return gapsPerLatitude.get(latitude).getSum();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the variance in the access durations over all points in the coverage
     * definition
     *
     * @return
     */
    public double getVarianceGap() {
        return gapStats.getVariance();
    }

    /**
     * Gets the variance in the access durations of a specified point
     *
     * @param pt the point of interest
     *
     * @return NaN if specified point is not in the coverage definition
     */
    public double getVarianceGap(CoveragePoint pt) {
        if (gapsPerPoint.containsKey(pt)) {
            return gapsPerPoint.get(pt).getVariance();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets the variance in the access durations of a specified point
     *
     * @param latitude the latitude of interest
     *
     * @return NaN if specified latitude is not in the coverage definition
     */
    public double getVarianceGap(double latitude) {
        if (gapsPerLatitude.containsKey(latitude)) {
            return gapsPerLatitude.get(latitude).getVariance();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Gets all the gap durations over all points in the coverage definition in
     * ascending order
     *
     * @return
     */
    public double[] getSortedGap() {
        return gapStats.getSortedValues();
    }

    /**
     * Gets all the gap durations of a specified point
     *
     * @param pt the point of interest
     *
     * @return empty double[] if specified point is not in the coverage
     * definition
     */
    public double[] getSortedGap(CoveragePoint pt) {
        if (gapsPerPoint.containsKey(pt)) {
            return gapsPerPoint.get(pt).getSortedValues();

        } else {
            return new double[0];
        }

    }

    /**
     * Gets all the gap durations of a specified point
     *
     * @param latitude the latitude of interest
     *
     * @return empty double[] if specified latitude is not in the coverage
     * definition
     */
    public double[] getSortedGap(double latitude) {
        if (gapsPerLatitude.containsKey(latitude)) {
            return gapsPerLatitude.get(latitude).getSortedValues();

        } else {
            return new double[0];
        }

    }
}
