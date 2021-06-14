package seakers.orekit.coverage.analysis;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.GndStation;
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
     * Creates an analyzer from a set of coverage points and time interval array
     * of event occurrences assigned for each satellite in the constellation
     * and a set of GS locations and time interval array of access to these GS
     * for each satellite in the constellation.
     * of contacts
     *
     * @param gsEvents accesses of all  satellites in the constellation to a set of ground stations
     */
    public GSAccessAnalyser(HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> gsEvents,
                           AbsoluteDate startDate, AbsoluteDate endDate, boolean allowCrossLinks){
        this.gsEvents = gsEvents;
        this.startDate = startDate;
        this.endDate = endDate;
        this.allowCrossLinks=allowCrossLinks;
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
        HashMap<Satellite, DescriptiveStatistics> lat;

        if (allowCrossLinks) lat = calcAccessTimesCL();
        else lat = calcAccessTimes();

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
        HashMap<Satellite, Double> lat;

        if (allowCrossLinks) lat = calcAccessDurationCL();
        else lat = calcAccessDuration();

        return lat;
    }

    /**
     * Computes the gap-time over all ground station accesses given to the analyzer
     * @return gap-time (latency) statistics
     */
    public HashMap<Satellite, DescriptiveStatistics>  getLatency() throws Exception {
        HashMap<Satellite, DescriptiveStatistics> lat;

        if (allowCrossLinks) lat = calcLatencyCL();
        else lat = calcLatency();

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
        HashMap<Satellite, Double> lat;

        if (allowCrossLinks) lat = calcMeanResponseTimeCL();
        else lat = calcMeanResponseTime();

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

        if (allowCrossLinks) lat = calcNumberOfPassesCL();
        else lat = calcNumberOfPasses();

        for(Satellite sat : lat.keySet()) {
            double ds = lat.get(sat);
            if (lat.get(sat) == null) {
                lat.put(sat, endDate.durationFrom(startDate));
            }
        }

        return lat;
    }

    private HashMap<Satellite, Double> calcNumberOfPassesCL() throws Exception{
        HashMap<Satellite, Double> lat = new HashMap<>();

        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(Satellite sat : gsEvents.keySet()) {
            TimeIntervalArray satAccesses = getOrderedAccess(sat);
            accesses.add(satAccesses);
        }

        for(Satellite sat : gsEvents.keySet()){
            lat.put(sat, mergeAccesses(accesses).getRiseSetTimes().size()/2.0);
        }
        return lat;
    }

    private HashMap<Satellite, Double> calcNumberOfPasses() throws Exception{
        HashMap<Satellite, Double> lat = new HashMap<>();

        for(Satellite sat : gsEvents.keySet()){
            lat.put(sat, getOrderedAccess(sat).getRiseSetTimes().size()/2.0);
        }
        return lat;
    }

    private HashMap<Satellite, DescriptiveStatistics> calcAccessTimesCL() throws Exception {
        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(Satellite sat : gsEvents.keySet()) {
            TimeIntervalArray satAccesses = getOrderedAccess(sat);
            accesses.add(satAccesses);
        }

        DescriptiveStatistics ds = new DescriptiveStatistics();
        double rise = -1;
        double set = -1;
        for(RiseSetTime time : mergeAccesses(accesses).getRiseSetTimes()){
            if (time.isRise()) {
                rise = time.getTime();
            } else {
                double duration;
                if (rise < 0.0) {
                    duration = time.getTime();
                } else {
                    set = time.getTime();
                    duration = set - rise;
                }
                ds.addValue(duration);
            }
        }

        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();
        for(Satellite sat : gsEvents.keySet()) {
            lat.put(sat,ds);
        }

        return lat;
    }

    private HashMap<Satellite, DescriptiveStatistics> calcAccessTimes() throws Exception {
        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();

        for(Satellite sat : gsEvents.keySet()){
            DescriptiveStatistics ds = new DescriptiveStatistics();

            double rise = -1;
            double set = -1;
            for(RiseSetTime time : getOrderedAccess(sat).getRiseSetTimes()){
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
        return lat;
    }

    private HashMap<Satellite, Double> calcAccessDurationCL() throws Exception {
        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(Satellite sat : gsEvents.keySet()) {
            TimeIntervalArray satAccesses = getOrderedAccess(sat);
            accesses.add(satAccesses);
        }

        double rise = -1;
        double set = -1;
        double accessDuration = 0;
        for(RiseSetTime time : mergeAccesses(accesses).getRiseSetTimes()){
            if(time.isRise()){
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

        HashMap<Satellite, Double> lat = new HashMap<>();
        for(Satellite sat : gsEvents.keySet()) {
            lat.put(sat,accessDuration);
        }

        return lat;
    }

    private HashMap<Satellite, Double> calcAccessDuration() throws Exception {
        HashMap<Satellite, Double> lat = new HashMap<>();

        for(Satellite sat : gsEvents.keySet()){
            double rise = -1;
            double set = -1;
            double accessDuration = 0;
            for(RiseSetTime time : getOrderedAccess(sat).getRiseSetTimes()){
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

//        if(Math.floorMod(orderedTimes.size(),2) != 0){
//            throw new Exception("Interval Array of uneven length");
//        }

        boolean done = false;
        while(!done){
            // merge intervals
            for(int i = 0; i < orderedTimes.size()-1; i++){
                RiseSetTime t1 = orderedTimes.get(i);
                RiseSetTime t2 = orderedTimes.get(i+1);

                if(t1.isRise() && t2.isRise()){
                    orderedTimes.remove(i+1);
                    break;
                }
                if(!t1.isRise() && !t2.isRise()){
                    orderedTimes.remove(i);
                    break;
                }
            }


            if(Math.floorMod(orderedTimes.size(),2) == 0){
                // check if all intervals have been merged
                done = true;
                for(int i = 0; i < orderedTimes.size(); i = i+2){
                    RiseSetTime t1 = orderedTimes.get(i);
                    RiseSetTime t2 = orderedTimes.get(i+1);

                    if(!t1.isRise() || t2.isRise()){
                        done = false;
                        break;
                    }
                }
            }
        }

        for(RiseSetTime time : orderedTimes){
            if(time.isRise()) orderedAccess.addRiseTime(time.getTime());
            else orderedAccess.addSetTime(time.getTime());

        }

        return orderedAccess;
    }

    private HashMap<Satellite, Double> calcMeanResponseTimeCL() throws Exception {
        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(Satellite sat : gsEvents.keySet()) {
            TimeIntervalArray satAccesses = getOrderedAccess(sat);
            accesses.add(satAccesses);
        }

        double meanResponseTime = 0.0;
        double rise = -1;
        double set = -1;
        for(RiseSetTime time : mergeAccesses(accesses).getRiseSetTimes()){
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

        HashMap<Satellite, Double> lat = new HashMap<>();
        for(Satellite sat : gsEvents.keySet()) {
            lat.put(sat,meanResponseTime);
        }

        return lat;
    }

    private HashMap<Satellite, Double> calcMeanResponseTime() throws Exception {
        HashMap<Satellite, Double> lat = new HashMap<>();

        for(Satellite sat : gsEvents.keySet()) {
            double meanResponseTime = 0.0;
            double rise = -1;
            double set = -1;
            for(RiseSetTime time : getOrderedAccess(sat).getRiseSetTimes()){
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

        return lat;
    }

    private HashMap<Satellite, DescriptiveStatistics> calcLatencyCL() throws Exception {
        ArrayList<TimeIntervalArray> accesses = new ArrayList<>();
        for(Satellite sat : gsEvents.keySet()) {
            TimeIntervalArray satAccesses = getOrderedAccess(sat);
            accesses.add(satAccesses);
        }

        DescriptiveStatistics ds = new DescriptiveStatistics();
        double rise = -1;
        double set = -1;
        for(RiseSetTime time : mergeAccesses(accesses).getRiseSetTimes()){
            if (time.isRise()) {
                double duration;
                if (set < 0.0) {
                    duration = time.getTime();
                } else {
                    rise = time.getTime();
                    duration = rise - set;
                }
                ds.addValue(duration);
            } else {
                set = time.getTime();
            }
        }

        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();
        for(Satellite sat : gsEvents.keySet()) {
            lat.put(sat,ds);
        }

        return lat;
    }

    private HashMap<Satellite,DescriptiveStatistics> calcLatency() throws Exception {
        HashMap<Satellite, DescriptiveStatistics> lat = new HashMap<>();

        for(Satellite sat : gsEvents.keySet()){
            DescriptiveStatistics ds = new DescriptiveStatistics();

            double rise = -1;
            double set = -1;
            for(RiseSetTime time : getOrderedAccess(sat).getRiseSetTimes()){
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
        return lat;
    }
}
