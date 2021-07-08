package seakers.orekit.coverage.analysis;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.time.*;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.GndStationNetwork;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class GSAccessAnalyser {
    /**
     * Collection of GroundStations
     */
    private final HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> gsEvents;

    /**
     * Collection of GroundStations
     */
    private final HashMap<Satellite, TimeIntervalArray> gsEventsProcessed;

    /**
     * Daily accesses by each satellite in the constellation
     */
    private final ArrayList<HashMap<Satellite,TimeIntervalArray>> dailyAccesses;

    /**
     * Ordered access of each satellite to the GS network (no Cross-Links)
     */
    private final HashMap<Satellite, TimeIntervalArray> orderedAccess;

    /**
     * Start of simulation
     */
    private final AbsoluteDate startDate;

    /**
     * End of simulation
     */
    private final AbsoluteDate endDate;

    /**
     * True if satellites communicate with each other, false otherwise.
     */
    private final boolean allowCrossLinks;

    /**
     * Stores the type of ground station access strategy chosen by the user
     */
    private final Strategy accessStrategy;

    /**
     * Determines the ground station network used in this analyzer
     */
    private final GndStationNetwork.GSNetwork network;

    /**
     * Types of access strategy that can be chosen by the used
     *  CONSERVATIVE: satellites will only download data when memory is full
     *  EVERY_ACCESS: satellites will download data every time they are accessing a ground station
     */
    public enum Strategy{
        CONSERVATIVE,
        EVERY_ACCESS
    }

    /**
     * Creates an analyzer from a set of coverage points and time interval array
     * of event occurrences assigned for each satellite in the constellation
     * and a set of GS locations and time interval array of access to these GS
     * for each satellite in the constellation.
     * of contacts
     *
     * @param gsEvents accesses of all  satellites in the constellation to a set of ground stations
     * @param network
     */
    public GSAccessAnalyser(HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> gsEvents,
                            AbsoluteDate startDate, AbsoluteDate endDate, boolean allowCrossLinks, Strategy strategy, GndStationNetwork.GSNetwork network) throws Exception {
        this.gsEvents = gsEvents;
        this.startDate = startDate;
        this.endDate = endDate;
        this.allowCrossLinks = allowCrossLinks;
        this.accessStrategy = strategy;
        this.network = network;
        this.orderedAccess = new HashMap<>();
        for(Satellite sat : gsEvents.keySet()){
            orderedAccess.put(sat, getOrderedAccess(sat));
        }
        this.gsEventsProcessed = processGSEvents();
        this.dailyAccesses = getDailyAccesses();
    }

    /**
     * Gets the collection of satellites in this analysis.
     * @return
     */
    public Set<Satellite> getSatellites() {
        return gsEvents.keySet();
    }

    /**
     * Computes the gs access statistics over all points given to the analyzer
     * @return latency statistics per satellite
     */
    public HashMap<Satellite, DescriptiveStatistics> getAccessTimesStatistics() {
        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();

        for(Satellite sat : gsEventsProcessed.keySet()){
            DescriptiveStatistics ds = new DescriptiveStatistics();

            double rise = -1;
            double set = -1;
            for(RiseSetTime time : gsEventsProcessed.get(sat).getRiseSetTimes()){
                if(time.isRise()) {
                    rise = time.getTime();
                }
                else{
                    double duration;
                    if(rise < 0.0){
                        duration = time.getTime();
                    }
                    else {
                        set = time.getTime();
                        duration = set - rise;
                    }
                    ds.addValue(duration);
                }
            }

            lat.put(sat,ds);
        }

        for(Satellite sat : lat.keySet()) {
            DescriptiveStatistics ds = lat.get(sat);
            if (ds.getN() == 0) {
                ds.addValue(0.0);
            }
        }

        return lat;
    }

    /**
     * Computes the gs access statistics over all points given to the analyzer
     * @return latency statistics of all satellites
     */
    public DescriptiveStatistics getAccessTimesStatisticsAll(){
        DescriptiveStatistics ds = new DescriptiveStatistics();
        for(Satellite sat : gsEventsProcessed.keySet()){
            double rise = -1;
            double set = -1;
            for(RiseSetTime time : gsEventsProcessed.get(sat).getRiseSetTimes()){
                if(time.isRise()) {
                    rise = time.getTime();
                }
                else{
                    double duration;
                    if(rise < 0.0){
                        duration = time.getTime();
                    }
                    else {
                        set = time.getTime();
                        duration = set - rise;
                    }
                    ds.addValue(duration);
                }
            }
        }

        if (ds.getN() == 0) {
            ds.addValue(0.0);
        }
        return ds;
    }

    /**
     * Computes total daily access duration statistics over all points given to the analyzer
     * @return daily access duration per satellite
     */
    public HashMap<Satellite, DescriptiveStatistics> getDailyAccessDurationStatistics() {
        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();

        for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses){
            for(Satellite sat : dailyAccess.keySet()){
                if(!lat.containsKey(sat)) lat.put(sat,new DescriptiveStatistics());

                double rise = -1;
                double set = -1;
                double accessDuration = 0;
                for(RiseSetTime time : dailyAccess.get(sat).getRiseSetTimes()){
                    if(time.isRise()) {
                        rise = time.getTime();
                    }
                    else{
                        double duration;
                        if(rise < 0.0){
                            duration = time.getTime();
                            accessDuration += duration;
                        }
                        else {
                            set = time.getTime();
                            duration = set - rise;
                            accessDuration += duration;
                        }
                    }
                }

                lat.get(sat).addValue(accessDuration);
            }
        }
        return lat;
    }

    /**
     * Computes total daily access duration statistics over all points given to the analyzer
     * @return latency statistics
     */
    public DescriptiveStatistics getDailyAccessDurationStatisticsAll() {
        DescriptiveStatistics ds = new DescriptiveStatistics();

        for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses){
            for(Satellite sat : dailyAccess.keySet()){

                double rise = -1;
                double set = -1;
                double accessDuration = 0;
                for(RiseSetTime time : dailyAccess.get(sat).getRiseSetTimes()){
                    if(time.isRise()) {
                        rise = time.getTime();
                    }
                    else{
                        double duration;
                        if(rise < 0.0){
                            duration = time.getTime();
                            accessDuration += duration;
                        }
                        else {
                            set = time.getTime();
                            duration = set - rise;
                            accessDuration += duration;
                        }
                    }
                }

                ds.addValue(accessDuration);
            }
        }
        return ds;
    }

    /**
     * Computes the gap-time over all ground station accesses given to the analyzer
     * @return gap-time (latency) statistics
     */
    public HashMap<Satellite, DescriptiveStatistics> getGapTimeStatistics() {
        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();

        for(Satellite sat : gsEventsProcessed.keySet()){
            DescriptiveStatistics ds = new DescriptiveStatistics();

            double rise = -1;
            double set = -1;
            for(RiseSetTime time : gsEventsProcessed.get(sat).getRiseSetTimes()){
                if(time.isRise()) {
                    double duration;
                    if(set < 0.0){
                        duration = time.getTime();
                    }
                    else {
                        rise = time.getTime();
                        duration = rise - set;
                    }
                    ds.addValue(duration);
                }
                else{
                    set = time.getTime();
                }
            }

            lat.put(sat,ds);
        }

        for(Satellite sat : lat.keySet()) {
            DescriptiveStatistics ds = lat.get(sat);
            if (ds.getN() == 0) {
                ds.addValue(endDate.durationFrom(startDate));
            }
        }

        return lat;
    }

    /**
     * Computes the gap-time over all ground station accesses given to the analyzer
     * @return gap-time (latency) statistics of all satellites
     */
    public DescriptiveStatistics getGapTimeStatisticsAll() {
        DescriptiveStatistics ds = new DescriptiveStatistics();

        for(Satellite sat : gsEventsProcessed.keySet()){
            double rise = -1;
            double set = -1;
            for(RiseSetTime time : gsEventsProcessed.get(sat).getRiseSetTimes()){
                if(time.isRise()) {
                    double duration;
                    if(set < 0.0){
                        duration = time.getTime();
                    }
                    else {
                        rise = time.getTime();
                        duration = rise - set;
                    }
                    ds.addValue(duration);
                }
                else{
                    set = time.getTime();
                }
            }

        }

        if (ds.getN() == 0) {
            ds.addValue(endDate.durationFrom(startDate));
        }

        return ds;
    }

    /**
     * Computes total access duration statistics over all points given to the analyzer
     * @return latency statistics
     */
    public HashMap<Satellite, Double> getMeanResponseTime() {
        HashMap<Satellite, Double> lat = new HashMap<>();

        for(Satellite sat : gsEventsProcessed.keySet()) {
            double meanResponseTime = 0.0;
            double rise = -1;
            double set = -1;
            for(RiseSetTime time : gsEventsProcessed.get(sat).getRiseSetTimes()){
                if (time.isRise()) {
                    double duration;
                    if (set < 0.0) {
                        duration = time.getTime();
                    } else {
                        rise = time.getTime();
                        duration = rise - set;
                    }
                    meanResponseTime += (duration * duration);
                } else {
                    set = time.getTime();
                }
            }
            meanResponseTime /= (2 * endDate.durationFrom(startDate));

            lat.put(sat,meanResponseTime);
        }

        for(Satellite sat : lat.keySet()) {
            double ds = lat.get(sat);
            if (lat.get(sat) == null) {
                lat.put(sat, endDate.durationFrom(startDate));
            }
        }

        return lat;
    }

    /**
     * Computes total access duration statistics over all points given to the analyzer
     * @return latency statistics of all satellites
     */
    public DescriptiveStatistics getMeanResponseTimeAll() {
        DescriptiveStatistics ds = new DescriptiveStatistics();

        for(Satellite sat : gsEventsProcessed.keySet()) {
            double meanResponseTime = 0.0;
            double rise = -1;
            double set = -1;
            for(RiseSetTime time : gsEventsProcessed.get(sat).getRiseSetTimes()){
                if (time.isRise()) {
                    double duration;
                    if (set < 0.0) {
                        duration = time.getTime();
                    } else {
                        rise = time.getTime();
                        duration = rise - set;
                    }
                    meanResponseTime += (duration * duration);
                } else {
                    set = time.getTime();
                }
            }
            meanResponseTime /= (2 * endDate.durationFrom(startDate));

            ds.addValue(meanResponseTime);
        }
        return ds;
    }

    /**
     * Computes total access duration statistics over all points given to the analyzer
     * @return latency statistics
     */
    public HashMap<Satellite, Double> getTimeAverageGap() {
        HashMap<Satellite, Double> lat = new HashMap<>();

        HashMap<Satellite, Double> mrt = getMeanResponseTime();
        for(Satellite sat : mrt.keySet()) {
            if (mrt.get(sat) == null) {
                lat.put(sat, endDate.durationFrom(startDate));
            }
            else{
                lat.put(sat, 2 * mrt.get(sat));
            }
        }

        return lat;
    }

    /**
     * Computes total access duration statistics over all points given to the analyzer
     * @return latency statistics of all sats
     */
    public DescriptiveStatistics getTimeAverageGapAll(){
        DescriptiveStatistics ds = new DescriptiveStatistics();
        DescriptiveStatistics meanResponseTime = getMeanResponseTimeAll();

        for(Double val : meanResponseTime.getValues()){
            ds.addValue(val*2);
        }

        return ds;
    }

    /**
     * Computes total access duration statistics over all points given to the analyzer
     * @return latency statistics
     */
    public HashMap<Satellite, DescriptiveStatistics> getDailyNumberOfPassesStatistics() {
        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();

        for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses){
            for(Satellite sat : dailyAccess.keySet()){
                if(!lat.containsKey(sat)) lat.put(sat,new DescriptiveStatistics());

                lat.get(sat).addValue(dailyAccess.get(sat).getRiseSetTimes().size()/2.0);
            }
        }

        return lat;
    }

    /**
     * Computes total access duration statistics over all points given to the analyzer
     * @return latency statistics for all sats
     */
    public DescriptiveStatistics getDailyNumberOfPassesStatisticsAll() {
        DescriptiveStatistics ds = new DescriptiveStatistics();

        for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses){
            for(Satellite sat : dailyAccess.keySet()){
                ds.addValue(dailyAccess.get(sat).getRiseSetTimes().size()/2.0);
            }
        }

        return ds;
    }

    /**
     * Calculates the daily operational costs of the satellite network
     * @return cost statistics per satellite
     * @throws Exception
     */
    public HashMap<Satellite, DescriptiveStatistics> getDailyCostStatistics() throws Exception{
        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();

        if(allowCrossLinks){
            // if one or more satellites are accessing the network via a single sat, only charge for that sat's access
            double day = 3600*24;
            double day_curr = 0;

            switch(network){
                case NEN:
                    for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses) {
                        for (Satellite satAcc : orderedAccess.keySet()) {
                            double dailyCost = 0.0;

                            double riseAcc = 0.0;
                            double setAcc = 0.0;

                            for (RiseSetTime time : orderedAccess.get(satAcc).getRiseSetTimes()) {
                                boolean isDownLinking = false;

                                if (time.isRise()) riseAcc = time.getTime();
                                else {
                                    setAcc = time.getTime();

                                    for(Satellite satDL : dailyAccess.keySet()){
                                        double riseDL = 0.0;
                                        double setDL = 0.0;

                                        for(RiseSetTime timeDL : dailyAccess.get(satDL).getRiseSetTimes()){
                                            if(timeDL.isRise()) riseDL = timeDL.getTime();
                                            else{
                                                setDL = timeDL.getTime();

                                                if(riseAcc <= riseDL+day_curr*day && riseDL+day_curr*day <= setAcc){
                                                    isDownLinking = true;
                                                    break;
                                                }
                                            }
                                        }

                                        if(isDownLinking) dailyCost += (new GndStationNetwork()).getCost(network);
                                    }
                                }
                            }
                            lat.get(satAcc).addValue(dailyCost);
                        }
                        day_curr++;
                    }
                    break;
                case AWS:
                    for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses) {
                        for (Satellite satAcc : orderedAccess.keySet()) {
                            double dailyCost = 0.0;

                            double riseAcc = 0.0;
                            double setAcc = 0.0;

                            for (RiseSetTime time : orderedAccess.get(satAcc).getRiseSetTimes()) {
                                boolean isDownLinking = false;

                                if (time.isRise())
                                    riseAcc = time.getTime();
                                else {
                                    setAcc = time.getTime();

                                    ArrayList<double[]> intervalsDL = new ArrayList<>();

                                    for(Satellite satDL : dailyAccess.keySet()){
                                        double riseDL = 0.0;
                                        double setDL = 0.0;

                                        for(RiseSetTime timeDL : dailyAccess.get(satDL).getRiseSetTimes()){
                                            if(timeDL.isRise()) riseDL = timeDL.getTime();
                                            else{
                                                setDL = timeDL.getTime();

                                                if(riseAcc <= riseDL+day_curr*day && riseDL+day_curr*day <= setAcc){

                                                    double[] interval = {riseDL, setDL};
                                                    intervalsDL.add(interval);

                                                    isDownLinking = true;
                                                    break;
                                                }
                                            }
                                        }

                                        if(isDownLinking) {
                                            ArrayList<double[]> mergedIntervals = new ArrayList<>();

                                            for(double[] interval : intervalsDL){
                                                if(mergedIntervals.isEmpty()) {
                                                    mergedIntervals.add(interval);
                                                    continue;
                                                }

                                                boolean isMerged = false;
                                                for(double[] merged : mergedIntervals) {
                                                    if(interval[0] <= merged[0] && interval[1] <= merged[1]){
                                                        merged[0] = interval[0];
                                                        merged[1] = interval[1];
                                                        isMerged = true;
                                                    }
                                                    else if(merged[0] <= interval[0] && interval[1] <= merged[1]){
                                                        isMerged = true;
                                                    }
                                                    else if(interval[0] <= merged[0] && merged[0] <= interval[1]){
                                                        merged[0] = interval[0];
                                                        isMerged = true;
                                                    }
                                                    else if(merged[0] <= interval[0] && interval[0] <= merged[1]){
                                                        merged[1] = interval[1];
                                                        isMerged = true;
                                                    }

                                                    if(isMerged) break;
                                                }
                                                if(!isMerged) mergedIntervals.add(interval);
                                            }

                                            for(double[] interval : mergedIntervals) {
                                                double duration = interval[1] - interval[0];
                                                dailyCost += duration * (new GndStationNetwork()).getCost(network);
                                            }
                                        }
                                    }
                                }
                            }
                            lat.get(satAcc).addValue(dailyCost);
                        }
                        day_curr++;
                    }
                    break;
                default:
                    throw new Exception("Network not yet supported");
            }
        }
        else{
            for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses){
                for(Satellite sat : dailyAccess.keySet()){
                    double dailyCost = 0.0;
                    if(!lat.containsKey(sat)) lat.put(sat,new DescriptiveStatistics());
                    double rise = 0.0;
                    double set = 0.0;
                    for(RiseSetTime time : dailyAccess.get(sat).getRiseSetTimes()){
                        if(time.isRise()){
                            rise = time.getTime();
                        }
                        else{
                            set = time.getTime();
                            double duration = set-rise;
                            switch (network) {
                                case NEN:
                                    dailyCost += (new GndStationNetwork()).getCost(network);
                                    break;
                                case AWS:
                                    dailyCost += duration/60 * (new GndStationNetwork()).getCost(network);
                                    break;
                                default:
                                    throw new Exception("Ground Station Network not yet supported");
                            }
                        }
                    }
                    lat.get(sat).addValue(dailyCost);
                }
            }
        }

//        for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses){
//            for(Satellite sat : dailyAccess.keySet()){
//                if(!lat.containsKey(sat)) lat.put(sat,new DescriptiveStatistics());
//                double dailyCost = 0.0;
//
//                double rise = 0.0;
//                double set = 0.0;
//                for(RiseSetTime time : dailyAccess.get(sat).getRiseSetTimes()){
//                    if(time.isRise()){
//                        rise = time.getTime();
//                    }
//                    else {
//                        set = time.getTime();
//                        double duration = set - rise;
//                        switch (network) {
//                            case NEN:
//                                dailyCost += (new GndStationNetwork()).getCost(network);
//                                break;
//                            case AWS:
//                                dailyCost += duration * (new GndStationNetwork()).getCost(network);
//                                break;
//                            default:
//                                throw new Exception("Ground Station Network not yet supported");
//                        }
//                    }
//                }
//
//                lat.get(sat).addValue(dailyCost);
//            }
//        }

        return lat;
    }

    /**
     * Calculates the daily operational costs of the satellite network
     * @return cost statistics per satellite
     * @throws Exception
     */
    public DescriptiveStatistics getDailyCostStatisticsAll() throws Exception{
        DescriptiveStatistics ds = new DescriptiveStatistics();

        if(allowCrossLinks){
            // if one or more satellites are accessing the network via a single sat, only charge for that sat's access
            double day = 3600*24;
            double day_curr = 0;

            switch(network){
                case NEN:
                    for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses) {
                        for (Satellite satAcc : orderedAccess.keySet()) {
                            double dailyCost = 0.0;

                            double riseAcc = 0.0;
                            double setAcc = 0.0;

                            for (RiseSetTime time : orderedAccess.get(satAcc).getRiseSetTimes()) {
                                boolean isDownLinking = false;

                                if (time.isRise()) riseAcc = time.getTime();
                                else {
                                    setAcc = time.getTime();

                                    for(Satellite satDL : dailyAccess.keySet()){
                                        double riseDL = 0.0;
                                        double setDL = 0.0;

                                        for(RiseSetTime timeDL : dailyAccess.get(satDL).getRiseSetTimes()){
                                            if(timeDL.isRise()) riseDL = timeDL.getTime();
                                            else{
                                                setDL = timeDL.getTime();

                                                if(riseAcc <= riseDL+day_curr*day && riseDL+day_curr*day <= setAcc){
                                                    isDownLinking = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if(isDownLinking) break;
                                    }
                                    if(isDownLinking) dailyCost += (new GndStationNetwork()).getCost(network);
                                }
                            }
                            ds.addValue(dailyCost);
                        }
                        day_curr++;
                    }
                    break;
                case AWS:
                    for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses) {
                        for (Satellite satAcc : orderedAccess.keySet()) {
                            double dailyCost = 0.0;

                            double riseAcc = 0.0;
                            double setAcc = 0.0;

                            for (RiseSetTime time : orderedAccess.get(satAcc).getRiseSetTimes()) {
                                boolean isDownLinking = false;

                                if (time.isRise())
                                    riseAcc = time.getTime();
                                else {
                                    setAcc = time.getTime();

                                    ArrayList<double[]> intervalsDL = new ArrayList<>();

                                    for(Satellite satDL : dailyAccess.keySet()){
                                        double riseDL = 0.0;
                                        double setDL = 0.0;

                                        for(RiseSetTime timeDL : dailyAccess.get(satDL).getRiseSetTimes()){
                                            if(timeDL.isRise()) riseDL = timeDL.getTime();
                                            else{
                                                setDL = timeDL.getTime();

                                                if(riseAcc <= riseDL+day_curr*day && riseDL+day_curr*day <= setAcc){

                                                    double[] interval = {riseDL, setDL};
                                                    intervalsDL.add(interval);

                                                    isDownLinking = true;
                                                    break;
                                                }
                                            }
                                        }

                                        if(isDownLinking) {
                                            ArrayList<double[]> mergedIntervals = new ArrayList<>();

                                            for(double[] interval : intervalsDL){
                                                if(mergedIntervals.isEmpty()) {
                                                    mergedIntervals.add(interval);
                                                    continue;
                                                }

                                                boolean isMerged = false;
                                                for(double[] merged : mergedIntervals) {
                                                    if(interval[0] <= merged[0] && interval[1] <= merged[1]){
                                                        merged[0] = interval[0];
                                                        merged[1] = interval[1];
                                                        isMerged = true;
                                                    }
                                                    else if(merged[0] <= interval[0] && interval[1] <= merged[1]){
                                                        isMerged = true;
                                                    }
                                                    else if(interval[0] <= merged[0] && merged[0] <= interval[1]){
                                                        merged[0] = interval[0];
                                                        isMerged = true;
                                                    }
                                                    else if(merged[0] <= interval[0] && interval[0] <= merged[1]){
                                                        merged[1] = interval[1];
                                                        isMerged = true;
                                                    }

                                                    if(isMerged) break;
                                                }
                                                if(!isMerged) mergedIntervals.add(interval);
                                            }

                                            for(double[] interval : mergedIntervals) {
                                                double duration = interval[1] - interval[0];
                                                dailyCost += duration/60 * (new GndStationNetwork()).getCost(network);
                                            }
                                        }
                                    }
                                }
                            }
                            ds.addValue(dailyCost);
                        }
                        day_curr++;
                    }
                    break;
                default:
                    throw new Exception("Network not yet supported");
            }
        }
        else {
            for (HashMap<Satellite, TimeIntervalArray> dailyAccess : dailyAccesses) {
                for (Satellite sat : dailyAccess.keySet()) {
                    double dailyCost = 0.0;

                    double rise = 0.0;
                    double set = 0.0;
                    for (RiseSetTime time : dailyAccess.get(sat).getRiseSetTimes()) {
                        if (time.isRise()) {
                            rise = time.getTime();
                        } else {
                            set = time.getTime();
                            switch (network) {
                                case NEN:
                                    dailyCost += (new GndStationNetwork()).getCost(network);
                                    break;
                                case AWS:
                                    dailyCost += (set - rise)/60 * (new GndStationNetwork()).getCost(network);
                                    break;
                                default:
                                    throw new Exception("Ground Station Network not yet supported");
                            }
                        }
                    }
                    ds.addValue(dailyCost);
                }
            }
        }
        return ds;
    }

    /**
     * Returns the daily amount of data down-linked by the spacecraft to the ground
     * @return data statistics per satellite
     */
    public HashMap<Satellite, DescriptiveStatistics> getDailyDataDownLinkedStatistics(){
        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();

        for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses) {
            for (Satellite sat : dailyAccess.keySet()) {
                if (!lat.containsKey(sat)) lat.put(sat, new DescriptiveStatistics());

                double dataRate = 0.0;
                for(Instrument ins : sat.getPayload()){
                    dataRate += ins.getDataRate();
                }

                double dataDownloaded = 0.0;
                double rise = 0.0;
                double set = 0.0;
                for(RiseSetTime time : dailyAccess.get(sat).getRiseSetTimes()) {
                    if (time.isRise()) {
                        rise = time.getTime();
                    } else {
                        set = time.getTime();
                        dataDownloaded = (rise - set) * getCurrentDownLink(sat, rise);
                    }
                }

                lat.get(sat).addValue(dataDownloaded);
            }
        }

        return lat;
    }

    /**
     * Returns the daily amount of data down-linked by the spacecraft to the ground
     * @return data statistics for all satellites
     */
    public DescriptiveStatistics getDailyDataDownLinkedStatisticsAll(){
        DescriptiveStatistics ds = new DescriptiveStatistics();

        for(HashMap<Satellite,TimeIntervalArray> dailyAccess : dailyAccesses) {
            for (Satellite sat : dailyAccess.keySet()) {
                double dataRate = 0.0;
                for(Instrument ins : sat.getPayload()){
                    dataRate += ins.getDataRate();
                }

                double dataDownloaded = 0.0;
                double rise = 0.0;
                double set = 0.0;
                for(RiseSetTime time : dailyAccess.get(sat).getRiseSetTimes()) {
                    if (time.isRise()) {
                        rise = time.getTime();
                    } else {
                        set = time.getTime();
                        dataDownloaded = (rise - set) * getCurrentDownLink(sat, rise);
                    }
                }

                ds.addValue(dataDownloaded);
            }
        }

        return ds;
    }

    /**
     * ------------------------------------------------------------------------
     * -------------------------HELPING FUNCTIONS------------------------------
     * ------------------------------------------------------------------------
     */

    /**
     * Generates the times in which satellites choose to down-link their onboard data based on their down-link strategy
     * @return
     * @throws Exception
     */
    private HashMap<Satellite, TimeIntervalArray> processGSEvents() throws Exception {
        // initialize processed events
        HashMap<Satellite, TimeIntervalArray> processed = new HashMap<>();
        for(Satellite sat : gsEvents.keySet()){
            processed.put(sat,new TimeIntervalArray(startDate,endDate));
        }

        switch(accessStrategy){
            case CONSERVATIVE:
                // only downloads when max memory is full
                for(Satellite sat : gsEvents.keySet()){
                    TimeIntervalArray accesses;
                    if(allowCrossLinks) accesses = getOrderedAccessCL();
                    else accesses = orderedAccess.get(sat);

                    double payloadDataRate = 0;
                    for(Instrument ins : sat.getPayload()){
                        payloadDataRate += ins.getDataRate();
                    }
                    double remData = 0.0;

                    double downRise = 0;
                    double downSet = 0;

                    double accessRise = 0;
                    double accessSet = -1;
                    for(RiseSetTime time : accesses.getRiseSetTimes()){
                        double memoryFullTime = (sat.getMaxMemory() - remData)/payloadDataRate;

                        if(time.isRise()){
                            accessRise = time.getTime();
                        }
                        else{
                            accessSet = time.getTime();

                            if(downSet + memoryFullTime <= accessSet){
                                if(accessRise <= downSet + memoryFullTime) {
                                    downRise = downSet + memoryFullTime;
                                }
                                else{
                                    downRise = accessRise;
                                }

                                double downloadTime = sat.getMaxMemory() / getCurrentDownLink(sat, downRise);

                                if (downRise + downloadTime <= accessSet){
                                    downSet = downRise + downloadTime;
                                    remData = 0.0;
                                }
                                else{
                                    downSet = accessSet;
                                    remData = (downRise + downloadTime - accessSet) * sat.getDownLink();
                                }

                                if(downSet < downRise){
                                    int x = 1;
                                }

                                processed.get(sat).addRiseTime(downRise);
                                processed.get(sat).addSetTime(downSet);
                            }
                        }
                    }
                }
                break;
            case EVERY_ACCESS:
                // downloads all data at avery access
                for(Satellite sat : gsEvents.keySet()){
                    TimeIntervalArray accesses;
                    if(allowCrossLinks) accesses = getOrderedAccessCL();
                    else accesses = orderedAccess.get(sat);

                    double payloadDataRate = 0;
                    for(Instrument ins : sat.getPayload()){
                        payloadDataRate += ins.getDataRate();
                    }
                    double remData = 0;

                    double downRise = 0;
                    double downSet = 0;

                    double accessRise = 0;
                    double accessSet = -1;
                    for(RiseSetTime time : accesses.getRiseSetTimes()){
                        if(time.isRise()){
                            accessRise = time.getTime();
                        }
                        else{
                            accessSet = time.getTime();
                            downRise = accessRise;

                            double dataCollected = (downRise - downSet) * payloadDataRate + remData;
                            if(dataCollected > sat.getMaxMemory()) dataCollected = sat.getMaxMemory();

                            double downloadTime = dataCollected / getCurrentDownLink(sat, downRise);

                            if(downRise + downloadTime < accessSet){
                                downSet = downRise + downloadTime;
                                remData = 0.0;
                            }
                            else{
                                downSet = accessSet;
                                remData = (downRise + downloadTime - accessSet) * sat.getDownLink();
                            }

                            processed.get(sat).addRiseTime(downRise);
                            processed.get(sat).addSetTime(downSet);
                        }
                    }
                }
                break;
            default:
                throw new Exception("Access strategy not yet supported");
        }

        // check for anomalies
        for(Satellite sat : processed.keySet()){
            double rise = 0.0;
            double set = 0.0;
            for(RiseSetTime time : processed.get(sat).getRiseSetTimes()){
                if(time.isRise()) rise = time.getTime();
                else{
                    set = time.getTime();

                    if(set<rise){
                        int x = 1;
                    }
                }
            }
        }

        return processed;
    }

    /**
     * Obtains the down-link speed at a given time assuming cross-links are enabled. Looks for the satellites currently
     * in access of a ground station and picks the one with highest speed or the one with the longest access time
     * remaining as the destination of its data
     * @param time current time in which accesses are searched
     * @return
     */
    private double getCurrentDownLink(Satellite currSat, double time) {
        if(!allowCrossLinks) return currSat.getDownLink();

        ArrayList<Satellite> satsInAccess = new ArrayList<>();
        HashMap<Satellite, double[]> accessInfo = new HashMap<>(); // {rise, set, data-rate}

        for(Satellite sat : gsEvents.keySet()){
            double rise = 0.0;
            double set = 0.0;
            for(RiseSetTime t : orderedAccess.get(sat).getRiseSetTimes()){
                if(t.isRise()) {
                    rise = t.getTime();
                }
                else{
                    set = t.getTime();

                    if(rise <= time && time <= set){
                        satsInAccess.add(sat);
                        double[] info = {rise, set, sat.getDownLink()};
                        accessInfo.put(sat, info);
                        break;
                    }
                }
            }
        }

        Satellite maxSat = null;
        double maxDataDownload = -1;

        double combinedDL = 0.0;

        for(Satellite sat : satsInAccess){
            double dur = (accessInfo.get(sat)[1] - time);
            double dl = sat.getDownLink();
            double dataDownload = dur * dl;

            if(dataDownload > maxDataDownload){
                maxSat = sat;
                maxDataDownload = dataDownload;
            }
            combinedDL += dl;
        }

        if(maxSat == null){
            maxSat = currSat;
        }

//        return maxSat.getDownLink();
        return combinedDL;
    }

    /**
     * Returns the time interval array of a satellite's potential access to the ground station network
     * @param sat satellite to be analysed
     * @return
     */
    private TimeIntervalArray getOrderedAccess(Satellite sat) {
        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(GndStation gndStation : gsEvents.get(sat).keySet()) {
            accesses.add(gsEvents.get(sat).get(gndStation));
        }

        return mergeAccesses(accesses);
    }

    /**
     * Returns the time interval array of a ground station's potential access to the satellite constellation
     * @param gndStation ground station to be analyzed
     * @return
     */
    private TimeIntervalArray getOrderedAccess(GndStation gndStation) {
        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(Satellite sat : gsEvents.keySet()) {
            accesses.add(gsEvents.get(sat).get(gndStation));
        }

        return mergeAccesses(accesses);
    }

    /**
     * Returns the ordered accesses of a satellite to a ground station network when cross links are enabled
     * @return
     */
    private TimeIntervalArray getOrderedAccessCL() {
        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(Satellite sat : gsEvents.keySet()) {
            for (GndStation gndStation : gsEvents.get(sat).keySet()) {
                accesses.add(gsEvents.get(sat).get(gndStation));
            }
        }
        return mergeAccesses(accesses);
    }

    /**
     * Merges Time Interval Arrays into a single one. Resolves any overlap conflicts and generates a single merged interval array
     * @param accesses List of accesses to be merged
     * @return orderedAccesses - Time interval array with newly merged intervals
     * @throws Exception
     */
    private TimeIntervalArray mergeAccesses(ArrayList<TimeIntervalArray> accesses) {
        TimeIntervalArray orderedAccess = new TimeIntervalArray(startDate, endDate);
        ArrayList<RiseSetTime> orderedTimes = new ArrayList<>();

        // order rise-set times
        for(TimeIntervalArray intervalArray : accesses){
            for(RiseSetTime time : intervalArray.getRiseSetTimes()){

                boolean minFound = false;
                for(int i = 0; i < orderedTimes.size(); i++){
                    if(time.getTime() < orderedTimes.get(i).getTime()){
                        orderedTimes.add(i,time);
                        minFound = true;
                        break;
                    }
                }

                if(!minFound || orderedTimes.isEmpty()){
                    orderedTimes.add(time);
                }
            }
        }

        boolean done = false;

        while(!done){
            // merge intervals
            boolean changesMade = false;
            for(int i = 0; i < orderedTimes.size()-1; i++){
                RiseSetTime t1 = orderedTimes.get(i);
                RiseSetTime t2 = orderedTimes.get(i+1);

                if(t1.isRise() && t2.isRise()){
                    orderedTimes.remove(i+1);
                    changesMade = true;
                    break;
                }
                if(!t1.isRise() && !t2.isRise()){
                    orderedTimes.remove(i);
                    changesMade = true;
                    break;
                }
            }

            // check if all intervals have been merge
            done = true;
            for(int i = 1; i < orderedTimes.size(); i++){
                RiseSetTime t1 = orderedTimes.get(i-1);
                RiseSetTime t2 = orderedTimes.get(i);

                if(t1.isRise() == t2.isRise()){
                    done = false;
                    break;
                }
            }
        }

        for(RiseSetTime time : orderedTimes){
            if(time.isRise()) orderedAccess.addRiseTime(time.getTime());
            else orderedAccess.addSetTime(time.getTime());
        }

        return orderedAccess;
    }

    /**
     * Separates processed accesses into daily segments and returns them in an array, which each element of the array
     * representing the contacts of each satellite in a day
     * @return
     */
    private ArrayList<HashMap<Satellite,TimeIntervalArray>> getDailyAccesses(){
        ArrayList<HashMap<Satellite,TimeIntervalArray>> dailyAccesses = new ArrayList<>();

        double day = 24*3600;
        double n_days = Math.ceil( endDate.durationFrom(startDate)/day );

        HashMap<Satellite, Double> rolloverSetTimes = new HashMap<>();
        for(int i_day = 1; i_day <= n_days; i_day++) {
            HashMap<Satellite, TimeIntervalArray> dailyAccess = new HashMap<>();

            double dayStart = (i_day-1)*day;
            double dayEnd = i_day*day;

            AbsoluteDate dateStart = startDate.shiftedBy(dayStart);
            AbsoluteDate dateEnd = startDate.shiftedBy(dayEnd);

            for (Satellite sat : gsEventsProcessed.keySet()) {
                TimeIntervalArray timeArray = new TimeIntervalArray(dateStart,dateEnd);
                double rise = 0.0;
                double set = 0.0;

                if(rolloverSetTimes.containsKey(sat)){
//                    timeArray.addRiseTime(0.0);
//                    timeArray.addSetTime(rolloverSetTimes.get(sat)-dayStart);
                    rolloverSetTimes.remove(sat);
                }

                for (RiseSetTime time : gsEventsProcessed.get(sat).getRiseSetTimes()) {
                    if (time.isRise()) {
                        rise = time.getTime();
                    } else {
                        set = time.getTime();

                        if (dayStart <= rise && rise <= dayEnd) {
                            if(set <= dayEnd){
                                timeArray.addRiseTime(rise-dayStart);
                                timeArray.addSetTime(set-dayStart);
                            }
                            else{
                                timeArray.addRiseTime(rise-dayStart);
                                timeArray.addSetTime(dateEnd);

                                rolloverSetTimes.put(sat,set);
                                break;
                            }
                        }
                    }
                }

                dailyAccess.put(sat,timeArray);
            }
            dailyAccesses.add(dailyAccess);
        }

        // check for anomalies
        for(HashMap<Satellite, TimeIntervalArray> access : dailyAccesses){
            for(Satellite sat : access.keySet()){
                double rise = 0;
                double set = 0;
                for(RiseSetTime time : access.get(sat).getRiseSetTimes()){
                    if(time.isRise()){
                        rise = time.getTime();
                    }
                    else{
                        set = time.getTime();

                        if(set < rise){
                            int x = 1;
                        }
                    }
                }
            }
        }

        return dailyAccesses;
    }
}
