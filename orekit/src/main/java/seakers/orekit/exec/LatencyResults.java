package seakers.orekit.exec;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import seakers.orekit.coverage.analysis.GSAccessAnalyser;
import seakers.orekit.object.GndStationNetwork;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;

public class LatencyResults {
    private final DescriptiveStatistics latency;
    private final DescriptiveStatistics gapTime;
    private final DescriptiveStatistics accessTime;
    private final DescriptiveStatistics dailyAccessDuration;
    private final DescriptiveStatistics numPasses;
    private final DescriptiveStatistics dataCaptured;
    private final DescriptiveStatistics dailyCost;
    private final double satCost;
    private final String networkBin;
    private final GndStationNetwork.GSNetwork network;
    private final GSAccessAnalyser.Strategy strategy;
    private final boolean crosslinks;

    public enum Stats{
        LAT,
        GAP,
        ACCESS,
        DUR_ACC,
        NUM_PASS,
        OP_COST,
        SAT_COST,
        NET_BIN,
        NETWORK,
        CL
    }

    public LatencyResults(DescriptiveStatistics latency, DescriptiveStatistics gapTime, DescriptiveStatistics accessTime, DescriptiveStatistics dailyAccessDuration, DescriptiveStatistics numPasses, DescriptiveStatistics dataCaptured, DescriptiveStatistics dailyCost, double satCost, String networkBin, GndStationNetwork.GSNetwork network, GSAccessAnalyser.Strategy strategy, boolean crossLinks) {
        this.latency = latency;
        this.gapTime = gapTime;
        this.accessTime = accessTime;
        this.dailyAccessDuration = dailyAccessDuration;
        this.numPasses = numPasses;
        this.dataCaptured = dataCaptured;
        this.dailyCost = dailyCost;
        this.satCost = satCost;
        this.networkBin = networkBin;
        this.network = network;
        this.strategy = strategy;
        this.crosslinks = crossLinks;
    }

    public String toString(){
        StringBuilder out = new StringBuilder();

        if(latency != null) out.append(latency.getMax() + "," + latency.getMean() + "," + latency.getMin() + ","
                + latency.getPercentile(75) + "," + latency.getPercentile(50)  + ",");
        else for(int i = 0; i < 5; i++) out.append(-1+ ",");

        out.append(gapTime.getMax() + "," + gapTime.getMean() + "," + gapTime.getMin() + "," + gapTime.getPercentile(75)
                + "," + gapTime.getPercentile(50) + ",");

        out.append(accessTime.getMax() + "," + accessTime.getMean() + "," + accessTime.getMin() + ","
                + accessTime.getPercentile(75) + "," + accessTime.getPercentile(50) + ",");

        out.append(dailyAccessDuration.getMax() + "," + dailyAccessDuration.getMean() + "," + dailyAccessDuration.getMin()
                + "," + dailyAccessDuration.getPercentile(75) + "," + dailyAccessDuration.getPercentile(50) + ",");

        out.append(numPasses.getMax() + "," + numPasses.getMean() + "," + numPasses.getMin() + "," + numPasses.getPercentile(75)
                + "," + numPasses.getPercentile(50) + ",");

        if(dataCaptured != null) out.append(dataCaptured.getMax() + "," + dataCaptured.getMean() + ","
                + dataCaptured.getMin() + "," + dataCaptured.getPercentile(75) + "," + dataCaptured.getPercentile(50) + ",");
        else for(int i = 0; i < 5; i++) out.append(-1+ ",");

        out.append(dailyCost.getMax() + "," + dailyCost.getMean() + "," + dailyCost.getMin() + "," + dailyCost.getPercentile(75)
                + "," + dailyCost.getPercentile(50) + ",");

        int nen;
        switch (network){
            case NEN:
                nen = 1;
                break;
            default:
                nen = 0;
        }

        int strat;
        switch (strategy){
            case CONSERVATIVE:
                strat = 0;
                break;
            case EVERY_ACCESS:
                strat = 1;
                break;
            default:
                strat = -1;
        }

        int cl;
        if(crosslinks) cl = 1;
        else cl = 0;

        out.append( networkBin + "," + nen + "," + strat + "," + cl + "\n");

        return out.toString();
    }

    public double getGapTime(){
        return gapTime.getMean();
    }
}
