package seakers.orekit.coverage.analysis;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.GndStation;
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
     */
    public GSAccessAnalyser(HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> gsEvents,
                           AbsoluteDate startDate, AbsoluteDate endDate, boolean allowCrossLinks, GSAccessAnalyser.Strategy strategy) throws Exception {
        this.gsEvents = gsEvents;
        this.startDate = startDate;
        this.endDate = endDate;
        this.allowCrossLinks = allowCrossLinks;
        this.accessStrategy = strategy;
        if(allowCrossLinks) this.gsEventsProcessed = processGSEventsCL();
        else this.gsEventsProcessed = processGSEvents();
    }

    private HashMap<Satellite, TimeIntervalArray> processGSEventsCL() throws Exception {
        // initialize processed events
        HashMap<Satellite, TimeIntervalArray> processed = new HashMap<>();
        for(Satellite sat : gsEvents.keySet()){
            processed.put(sat,new TimeIntervalArray(startDate,endDate));
        }

        switch(accessStrategy){
            case CONSERVATIVE:
                break;
            case EVERY_ACCESS:
                break;
            default:
                throw new Exception("Access strategy not yet supported");
        }

        return processed;
    }

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
                    TimeIntervalArray accesses = getOrderedAccess(sat);
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
                        double dt = (sat.getMaxMemory() - remData)/payloadDataRate;

                        if(time.isRise()){
                            accessRise = time.getTime();
                        }
                        else{
                            accessSet = time.getTime();

                            if(downSet + dt <= accessSet){
                                if(accessRise <= downSet + dt) {
                                    downRise = downSet + dt;
                                }
                                else{
                                    downRise = accessRise;
                                }

                                double downloadTime = sat.getMaxMemory() / sat.getDownLink();

                                if (downRise + downloadTime <= accessSet){
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
                }
                break;
            case EVERY_ACCESS:
                // downloads all data at avery access
                for(Satellite sat : gsEvents.keySet()){
                    TimeIntervalArray accesses = getOrderedAccess(sat);
                    double payloadDataRate = 0;
                    for(Instrument ins : sat.getPayload()){
                        payloadDataRate += ins.getDataRate();
                    }

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
                            double downloadTime = (accessRise - downSet) * payloadDataRate /sat.getDownLink();

                            if(downRise + downloadTime < accessSet){
                                downSet = downRise + downloadTime;
                            }
                            else{
                                downSet = accessSet;
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
        return processed;
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
     * @return latency statistics
     */
    public HashMap<Satellite, DescriptiveStatistics> getAccessTimesStatistics() throws Exception {
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
     * Computes total access duration statistics over all points given to the analyzer
     * @return latency statistics
     */
    public HashMap<Satellite, Double> getAccessDurationStatistics() throws Exception {
        HashMap<Satellite, Double> lat = new HashMap<>();

        for(Satellite sat : gsEventsProcessed.keySet()){
            double rise = -1;
            double set = -1;
            double accessDuration = 0;
            for(RiseSetTime time : gsEventsProcessed.get(sat).getRiseSetTimes()){
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

            lat.put(sat,accessDuration);
        }
        return lat;
    }

    /**
     * Computes the gap-time over all ground station accesses given to the analyzer
     * @return gap-time (latency) statistics
     */
    public HashMap<Satellite, DescriptiveStatistics> getGapTime() throws Exception {
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
     * Computes total access duration statistics over all points given to the analyzer
     * @return latency statistics
     */
    public HashMap<Satellite, Double> getMeanResponseTime() throws Exception {
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
     * @return latency statistics
     */
    public HashMap<Satellite, Double> getTimeAverageGap() throws Exception {
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
     * @return latency statistics
     */
    public HashMap<Satellite, Double> getNumberOfPasses() throws Exception {
        HashMap<Satellite, Double> lat = new HashMap<>();

        for(Satellite sat : gsEventsProcessed.keySet()){
            lat.put(sat, gsEventsProcessed.get(sat).getRiseSetTimes().size()/2.0);
        }

        for(Satellite sat : lat.keySet()) {
            double ds = lat.get(sat);
            if (lat.get(sat) == null) {
                lat.put(sat, endDate.durationFrom(startDate));
            }
        }

        return lat;
    }

    private TimeIntervalArray getOrderedAccess(Satellite sat) throws Exception {
        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(GndStation gndStation : gsEvents.get(sat).keySet()) {
            accesses.add(gsEvents.get(sat).get(gndStation));
        }

        return mergeAccesses(accesses);
    }

    private TimeIntervalArray getOrderedAccess(GndStation gndStation) throws Exception {
        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(Satellite sat : gsEvents.keySet()) {
            accesses.add(gsEvents.get(sat).get(gndStation));
        }

        return mergeAccesses(accesses);
    }

    private TimeIntervalArray mergeAccesses(ArrayList<TimeIntervalArray> accesses) throws Exception {
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

//            if(Math.floorMod(orderedTimes.size(),2) == 0){
//                // check if all intervals have been merged
//                done = true;
//                for(int i = 0; i < orderedTimes.size(); i = i+2){
//                    RiseSetTime t1 = orderedTimes.get(i);
//                    RiseSetTime t2 = orderedTimes.get(i+1);
//
//                    if(!t1.isRise() || t2.isRise()){
//                        done = false;
//                        break;
//                    }
//                }
//            }
//            else if(!done && !changesMade){
//                break;
//            }
        }

        for(RiseSetTime time : orderedTimes){
            if(time.isRise()) orderedAccess.addRiseTime(time.getTime());
            else orderedAccess.addSetTime(time.getTime());
        }

        return orderedAccess;
    }
    public ArrayList<Double> getIntervals(boolean access) throws Exception {
        ArrayList<Double> intervals = new ArrayList<>();
//        if(allowCrossLinks){
//            ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
//            for(Satellite sat : gsEvents.keySet()) {
//                TimeIntervalArray satAccesses = getOrderedAccess(sat);
//                accesses.add(satAccesses);
//            }
//
//            double rise = -1;
//            double set = -1;
//            for(RiseSetTime time : mergeAccesses(accesses).getRiseSetTimes()){
//                if(access) {
//                    if(time.isRise()) {
//                        rise = time.getTime();
//                    }
//                    else{
//                        double duration;
//                        if(rise < 0.0){
//                            duration = time.getTime();
//                        }
//                        else {
//                            set = time.getTime();
//                            duration = set - rise;
//                        }
//                        intervals.add(duration);
//                    }
//                }
//                else{
//                    if (time.isRise()) {
//                        double duration;
//                        if (set < 0.0) {
//                            duration = time.getTime();
//                        } else {
//                            rise = time.getTime();
//                            duration = rise - set;
//                        }
//                        intervals.add(duration);
//                    } else {
//                        set = time.getTime();
//                    }
//                }
//            }
//        }
//        else{
//            for(Satellite sat : gsEvents.keySet()) {
//                TimeIntervalArray satAccesses = getOrderedAccess(sat);
//
//                double rise = -1;
//                double set = -1;
//                for(RiseSetTime time : satAccesses.getRiseSetTimes()){
//                    if(access) {
//                        if(time.isRise()) {
//                            rise = time.getTime();
//                        }
//                        else{
//                            double duration;
//                            if(rise < 0.0){
//                                duration = time.getTime();
//                            }
//                            else {
//                                set = time.getTime();
//                                duration = set - rise;
//                            }
//                            intervals.add(duration);
//                        }
//                    }
//                    else{
//                        if (time.isRise()) {
//                            double duration;
//                            if (set < 0.0) {
//                                duration = time.getTime();
//                            } else {
//                                rise = time.getTime();
//                                duration = rise - set;
//                            }
//                            intervals.add(duration);
//                        } else {
//                            set = time.getTime();
//                        }
//                    }
//                }
//            }
//        }

        for(Satellite sat : gsEventsProcessed.keySet()) {
            TimeIntervalArray satAccesses = gsEventsProcessed.get(sat);

            double rise = -1;
            double set = -1;
            for(RiseSetTime time : satAccesses.getRiseSetTimes()){
                if(access) {
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
                        intervals.add(duration);
                    }
                }
                else{
                    if (time.isRise()) {
                        double duration;
                        if (set < 0.0) {
                            duration = time.getTime();
                        } else {
                            rise = time.getTime();
                            duration = rise - set;
                        }
                        intervals.add(duration);
                    } else {
                        set = time.getTime();
                    }
                }
            }
        }
        return intervals;
    }
}
