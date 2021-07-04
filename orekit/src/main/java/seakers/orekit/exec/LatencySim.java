package seakers.orekit.exec;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.analysis.GSAccessAnalyser;
import seakers.orekit.coverage.analysis.LatencyGroundEventAnalyzer;
import seakers.orekit.event.FieldOfViewAndGndStationEventAnalysis;
import seakers.orekit.object.*;

import java.util.*;

import static java.lang.Integer.parseInt;

/**
 * Simulates latency metrics for a given constellation and list of ground stations
 * @author a.aguilar
 */
public class LatencySim {
    /**1
     * List of constellations being simulated
     */
    private final Constellation constellation;

    /**
     * Ground station network
     */
    private final HashSet<GndStation> gsNetwork;

    /**
     * Toggle for crosslinks being enabled in latency calcs
     */
    private final boolean crossLinks;

    /**
     * Cost of use of ground station network
     */
    private final double cost;

    /**
     * Toggle for the type of cost per access
     * if true, cost is calculated per access to the ground station network
     * if false, cost is calculated per minute of access to the ground station network
     */
    private final boolean costPerAccess;

    private final GSAccessAnalyser.Strategy strategy;

    /**
     * Start and end date of the simulation
     */
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    private final FieldOfViewAndGndStationEventAnalysis events;
    private final HashMap<Satellite,HashMap<TopocentricFrame, TimeIntervalArray>> fovEvents;
    private final HashMap<Satellite,HashMap<GndStation, TimeIntervalArray>> gsEvents;
    private final HashSet<CoverageDefinition> covDefs;

    public LatencySim(FieldOfViewAndGndStationEventAnalysis events, Constellation constellation, HashSet<CoverageDefinition> covDefs, ArrayList<GndStation> gsNetwork, boolean crossLinks, double cost, boolean costPerAccess, GSAccessAnalyser.Strategy strategy,  AbsoluteDate startDate, AbsoluteDate endDate) {
        this.events = events;
        this.fovEvents = events.getAllAccesses().get(covDefs.iterator().next());
        this.gsEvents = new HashMap<>();{
            for(Satellite sat : events.getAllAccessesGS().keySet()){
                gsEvents.put(sat, new HashMap<>());
                for(GndStation gnd : gsNetwork){
                    gsEvents.get(sat).put(gnd, events.getAllAccessesGS().get(sat).get(gnd));
                }
            }
        }

        this.constellation = constellation;
        this.covDefs = covDefs;
        this.crossLinks = crossLinks;
        this.cost = cost;
        this.costPerAccess = costPerAccess;
        this.strategy = strategy;
        this.startDate = startDate;
        this.endDate = endDate;
        this.gsNetwork = new HashSet<>(gsNetwork);
    }

    public LatencyResults calcResults() throws Exception {
        // read results
        Properties props=new Properties();// -Latency
        LatencyGroundEventAnalyzer latencyAnalyzer = new LatencyGroundEventAnalyzer(fovEvents,gsEvents,crossLinks);
        DescriptiveStatistics latencyStats = latencyAnalyzer.getStatistics();
        // -Gap and Access Times
        GSAccessAnalyser gsAccessAnalyser = new GSAccessAnalyser(gsEvents,startDate,endDate,crossLinks, strategy);
        HashMap<Satellite, DescriptiveStatistics> accessStats = gsAccessAnalyser.getAccessTimesStatistics();
        HashMap<Satellite, DescriptiveStatistics> gapStats = gsAccessAnalyser.getGapTime();
        HashMap<Satellite, Double> meanResponseTime = gsAccessAnalyser.getMeanResponseTime();
        HashMap<Satellite, Double> numPasses = gsAccessAnalyser.getNumberOfPasses();

        // -Operational Costs
        DescriptiveStatistics dailyCost = readDailyCosts();

        // -Daily Access Duration
        HashMap<Satellite, DescriptiveStatistics> dailyAccessDuration = readDailyAccessDuration();

        // -SatCost
        double satCost = 0.0;


        return new LatencyResults(latencyStats, gapStats, accessStats, dailyAccessDuration, meanResponseTime, numPasses, dailyCost, satCost);
    }

    private HashMap<Satellite, DescriptiveStatistics> readDailyAccessDuration() throws Exception{
        HashMap<Satellite, DescriptiveStatistics> dailyAccessDuration = new HashMap<>();
        for(Satellite sat : gsEvents.keySet()){
            dailyAccessDuration.put(sat, new DescriptiveStatistics());
        }

        double day = 3600*24;
        AbsoluteDate startDate_i = startDate;
        AbsoluteDate endDate_i = startDate.shiftedBy(day);

        if(endDate_i.compareTo(endDate) > 0) endDate_i = endDate;

        do{
            HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> dailyEvents = new HashMap<>();

            for(Satellite sat : gsEvents.keySet()){
                dailyEvents.put(sat, new HashMap<>());
                for(GndStation gndStation : gsEvents.get(sat).keySet()){
                    TimeIntervalArray dailyInterval = new TimeIntervalArray(startDate, endDate);
                    for(RiseSetTime time :  gsEvents.get(sat).get(gndStation).getRiseSetTimes()){

                        AbsoluteDate date = startDate.shiftedBy(time.getTime());

                        if(date.compareTo(startDate_i) >= 0 && date.compareTo(endDate_i) <= 0){
                            if(time.isRise()) {
                                dailyInterval.addRiseTime(time.getTime());
                            }
                            else{
                                if(dailyInterval.isEmpty()){
                                    dailyInterval.addRiseTime(startDate_i);
                                }
                                dailyInterval.addSetTime(time.getTime());
                            }
                        }
                    }
                    if(dailyInterval.getRiseSetTimes().size() > 0
                            && dailyInterval.getRiseSetTimes().get(dailyInterval.getRiseSetTimes().size()-1).isRise()){
                        dailyInterval.addSetTime(endDate_i);
                    }
                    dailyEvents.get(sat).put(gndStation,dailyInterval);
                }
            }

            GSAccessAnalyser gsa = new GSAccessAnalyser(dailyEvents,startDate,endDate,crossLinks,strategy);
            HashMap<Satellite, Double> duration = gsa.getAccessDurationStatistics();

            for(Satellite sat : duration.keySet()){
                dailyAccessDuration.get(sat).addValue(duration.get(sat));
            }

            if(endDate_i.compareTo(endDate) == 0) break;
            startDate_i = endDate_i;
            endDate_i = endDate_i.shiftedBy(day);
            if(endDate_i.compareTo(endDate) > 0) endDate_i = endDate;
        }while( endDate_i.compareTo(endDate) <= 0);

        return dailyAccessDuration;
    }

    private DescriptiveStatistics readDailyCosts() throws Exception {
        DescriptiveStatistics dailyCosts = new DescriptiveStatistics();

        double day = 3600*24;
        AbsoluteDate startDate_i = startDate;
        AbsoluteDate endDate_i = startDate.shiftedBy(day);

        if(endDate_i.compareTo(endDate) > 0) endDate_i = endDate;

        do{
            HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> dailyEvents = new HashMap<>();

            for(Satellite sat : gsEvents.keySet()){
                dailyEvents.put(sat, new HashMap<>());
                for(GndStation gndStation : gsEvents.get(sat).keySet()){
                    TimeIntervalArray dailyInterval = new TimeIntervalArray(startDate, endDate);
                    for(RiseSetTime time :  gsEvents.get(sat).get(gndStation).getRiseSetTimes()){

                        AbsoluteDate date = startDate.shiftedBy(time.getTime());

                        if(date.compareTo(startDate_i) >= 0 && date.compareTo(endDate_i) <= 0){
                            if(time.isRise()) {
                                dailyInterval.addRiseTime(time.getTime());
                            }
                            else{
                                if(dailyInterval.isEmpty()){
                                    dailyInterval.addRiseTime(startDate_i);
                                }
                                dailyInterval.addSetTime(time.getTime());
                            }
                        }
                    }
                    if(dailyInterval.getRiseSetTimes().size() > 0
                        && dailyInterval.getRiseSetTimes().get(dailyInterval.getRiseSetTimes().size()-1).isRise()){
                        dailyInterval.addSetTime(endDate_i);
                    }
                    dailyEvents.get(sat).put(gndStation,dailyInterval);
                }
            }

            GSAccessAnalyser gsa = new GSAccessAnalyser(dailyEvents,startDate,endDate,crossLinks,strategy);
            ArrayList<Double> accessIntervals = gsa.getIntervals(true);
            ArrayList<Double> gapIntervals = gsa.getIntervals(false);

            if(costPerAccess) {
                double cost = 0;
                for(double interval : accessIntervals){
                    cost += this.cost;
                }
                dailyCosts.addValue(cost);
            }
            else{
                double cost = 0;
                for (int i = 0; i < accessIntervals.size() && i < gapIntervals.size(); i++) {
                    double gap = gapIntervals.get(i);
                    double acc = accessIntervals.get(i);
                    cost += this.cost/60 * acc;
                }
                dailyCosts.addValue(cost);
            }

            if(endDate_i.compareTo(endDate) == 0) break;
            startDate_i = endDate_i;
            endDate_i = endDate_i.shiftedBy(day);
            if(endDate_i.compareTo(endDate) > 0) endDate_i = endDate;
        }while( endDate_i.compareTo(endDate) <= 0);

        return dailyCosts;
    }

}
