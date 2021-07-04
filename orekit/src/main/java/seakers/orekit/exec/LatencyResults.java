package seakers.orekit.exec;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import seakers.orekit.object.Satellite;

import java.util.HashMap;

public class LatencyResults {
    private final DescriptiveStatistics latency;
    private final HashMap<Satellite, DescriptiveStatistics>  gapTime;
    private final HashMap<Satellite, DescriptiveStatistics>  accessTime;
    private final HashMap<Satellite, DescriptiveStatistics> dailyAccessDuration;
    private final HashMap<Satellite, Double> meanResponseTime;
    private final HashMap<Satellite, Double> numPasses;
    private final DescriptiveStatistics dailyCost;
    private final double satCost;

    public LatencyResults(DescriptiveStatistics latency, HashMap<Satellite, DescriptiveStatistics> gapTime, HashMap<Satellite, DescriptiveStatistics> accessTime, HashMap<Satellite, DescriptiveStatistics> dailyAccessDuration, HashMap<Satellite, Double> meanResponseTime, HashMap<Satellite, Double> numPasses, DescriptiveStatistics dailyCost, double satCost) {
        this.latency = latency;
        this.gapTime = gapTime;
        this.accessTime = accessTime;
        this.dailyAccessDuration = dailyAccessDuration;
        this.meanResponseTime = meanResponseTime;
        this.numPasses = numPasses;
        this.dailyCost = dailyCost;
        this.satCost = satCost;
    }

    public String toString(){
        StringBuilder out = new StringBuilder();

        if(latency != null){
            out.append(latency.getMax() + "," + latency.getMean() + "," + latency.getMin() + "," + latency.getPercentile(90) + "," + latency.getPercentile(50)  + ",");
        }

        for(Satellite sat : gapTime.keySet()) {
            out.append(gapTime.get(sat).getMax() + "," + gapTime.get(sat).getMean() + "," + gapTime.get(sat).getMin() + "," + gapTime.get(sat).getPercentile(90) + "," + gapTime.get(sat).getPercentile(50) + ",");
            out.append(accessTime.get(sat).getMax() + "," + accessTime.get(sat).getMean() + "," + accessTime.get(sat).getMin() + "," + accessTime.get(sat).getPercentile(90) + "," + accessTime.get(sat).getPercentile(50) + ",");
            out.append(meanResponseTime.get(sat) + ",");
            out.append(numPasses.get(sat) + ",");
        }

        for(Satellite sat : dailyAccessDuration.keySet()){
            out.append(dailyAccessDuration.get(sat).getMax() + "," + dailyAccessDuration.get(sat).getMean() + "," + dailyAccessDuration.get(sat).getMin() + "," + dailyAccessDuration.get(sat).getPercentile(90) + "," + dailyAccessDuration.get(sat).getPercentile(50) + ",");
        }

        out.append(dailyCost.getMax() + "," + dailyCost.getMean() + "," + dailyCost.getMin() + "," + dailyCost.getPercentile(90) + "," + dailyCost.getPercentile(50) + ",");
        out.append(satCost);
        return out.toString();
    }

    public String toString(String networkBin, boolean nen, boolean crosslinks){
        StringBuilder out = new StringBuilder();

        out.append(this.toString());
        out.append( "," + networkBin + "," + nen + "," + crosslinks + "\n");

        return out.toString();
    }

    public double getGapTime(){
        DescriptiveStatistics ds = new DescriptiveStatistics();
        for(Satellite sat : gapTime.keySet()){
            ds.addValue(gapTime.get(sat).getMean());
        }

        return ds.getMean();
    }
}
