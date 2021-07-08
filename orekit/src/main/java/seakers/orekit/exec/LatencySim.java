package seakers.orekit.exec;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.analysis.GSAccessAnalyser;
import seakers.orekit.event.GndStationEventAnalysis;
import seakers.orekit.object.*;

import java.util.*;

import static java.lang.Integer.parseInt;

/**
 * Simulates latency metrics for a given constellation and list of ground stations
 * @author a.aguilar
 */
public class LatencySim {

    /**
     * Toggle for crosslinks being enabled in latency calcs
     */
    private final boolean crossLinks;

    /**
     * Name of ground station network used
     */
    private final GndStationNetwork.GSNetwork network;

    private final String networkBin;

    /**
     * Accessing strategy
     */
    private final GSAccessAnalyser.Strategy strategy;

    /**
     * Start and end date of the simulation
     */
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    private final GndStationEventAnalysis events;
    private final HashMap<Satellite,HashMap<TopocentricFrame, TimeIntervalArray>> fovEvents;
    private final HashMap<Satellite,HashMap<GndStation, TimeIntervalArray>> gsEvents;

    public LatencySim(GndStationEventAnalysis events, ArrayList<GndStation> gsNetwork, String networkBin, GndStationNetwork.GSNetwork networkName, GSAccessAnalyser.Strategy downLinkStrategy, boolean crossLinks, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.events = events;
        this.fovEvents = null;
        this.networkBin = networkBin;

        this.gsEvents = new HashMap<>();{
            for(Satellite sat : events.getAllAccesses().keySet()){
                gsEvents.put(sat, new HashMap<>());
                for(GndStation gnd : gsNetwork){
                    gsEvents.get(sat).put(gnd, events.getAllAccesses().get(sat).get(gnd));
                }
            }
        }

        this.crossLinks = crossLinks;
        this.network = networkName;
        this.strategy = downLinkStrategy;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LatencyResults calcResults() throws Exception {
        // read results
        // -Gap and Access Times
        GSAccessAnalyser gsAccessAnalyser = new GSAccessAnalyser(gsEvents,startDate,endDate,crossLinks, strategy, network);
        DescriptiveStatistics accessStats = gsAccessAnalyser.getAccessTimesStatisticsAll();
        DescriptiveStatistics gapStats = gsAccessAnalyser.getGapTimeStatisticsAll();
        DescriptiveStatistics numPasses = gsAccessAnalyser.getDailyNumberOfPassesStatisticsAll();

        // -Operational Costs
        DescriptiveStatistics dailyCost = gsAccessAnalyser.getDailyCostStatisticsAll();

        // -Data Captured by Constellation
//        DescriptiveStatistics dataCaptured  = gsAccessAnalyser.getDailyDataDownLinkedStatisticsAll();
        DescriptiveStatistics dataCaptured = null;

        // -Daily Access Duration
        DescriptiveStatistics dailyAccessDuration = gsAccessAnalyser.getDailyAccessDurationStatisticsAll();

        // -SatCost
        double satCost = 0.0;

        return new LatencyResults(null, gapStats, accessStats, dailyAccessDuration, numPasses, dataCaptured, dailyCost, satCost, networkBin, network, strategy, crossLinks);
    }
}
