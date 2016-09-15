/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import orekit.util.SortedLinkedList;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author nozomihitomi
 */
public class TimeIntervalMerger {

    private final ArrayList<RiseSetTime> sortedRiseSetTimes;
    private final ArrayList<Integer> overlaps;
    private final AbsoluteDate head;
    private final AbsoluteDate tail;
    private final int nTotalArrays;

    public TimeIntervalMerger(Collection<TimeIntervalArray> timeArrays) {
        this.nTotalArrays = timeArrays.size();

        //Find earliest head date and latest tail date
        Iterator<TimeIntervalArray> iter = timeArrays.iterator();
        TimeIntervalArray currentArray = iter.next();
        AbsoluteDate earliest = currentArray.getHead();
        AbsoluteDate latest = currentArray.getTail();
        while (iter.hasNext()) {
            currentArray = iter.next();
            if (currentArray.getHead().compareTo(earliest) < 0) {
                earliest = currentArray.getHead();
            }
            if (currentArray.getTail().compareTo(latest) > 0) {
                latest = currentArray.getTail();
            }
        }
        head = earliest;
        tail = latest;

        //compute the ordered/sorted rise and set times to be used for the future so they don't have to be recomputed
        this.sortedRiseSetTimes = sortRiseSetTimes(timeArrays);

        //find where the time intervals overlap
        this.overlaps = findOverlaps(sortedRiseSetTimes);
    }

    /**
     * Sorts all the rise and set times from all the time interval arrays. In
     * mode = 0, creates a sorted array for all the rise times. In mode = 1
     * creates sorted array of all the set times. A mode of any other number
     * except for 0 or 1 will cause exception to be thrown.
     *
     * @param mode
     * @return
     */
    private ArrayList<RiseSetTime> sortRiseSetTimes(Collection<TimeIntervalArray> timeArrays) {

        //Give each array an index
        HashMap<Integer, ArrayList<RiseSetTime>> timeArrayMap = new HashMap<>(timeArrays.size());
        //map to store the current index for each array <id,index>
        HashMap<Integer, Integer> id_indexMap = new HashMap<>(timeArrays.size());
        Iterator<TimeIntervalArray> iter = timeArrays.iterator();
        int arrayID = 0;
        int totalSize = 0;
        while (iter.hasNext()) {
            timeArrayMap.put(arrayID, iter.next().getRiseSetTimes());
            totalSize += timeArrayMap.get(arrayID).size();
            id_indexMap.put(arrayID, 0);
            arrayID++;
        }

        ArrayList<RiseSetTime> resultant = new ArrayList<>(totalSize);

        SortedLinkedList<RiseSetTimeID> currentDates = new SortedLinkedList<>(new TimeComparator());
        for (Integer id : id_indexMap.keySet()) {
            if(!timeArrayMap.get(id).isEmpty()){
                //skip over any time interval arrays that are empty 
                RiseSetTime time = timeArrayMap.get(id).get(id_indexMap.get(id));
                currentDates.add(new RiseSetTimeID(time, id));
            }
        }
        
        //if all arrays are empty, return an empty array
        if (currentDates.isEmpty()){
            return resultant;
        }
        
        int numOpenIntervals = 0;
        while (true) {
            //search for the next earliest rise or set time
            RiseSetTimeID earliestDate = currentDates.get(0);
            RiseSetTime minDate = earliestDate.getTime();
            Integer minID = earliestDate.getId();
            currentDates.remove(0);

            //update index
            //check if update would exceed array size
            int nextIndex = id_indexMap.get(minID) + 1;
            if (nextIndex < timeArrayMap.get(minID).size()) {
                id_indexMap.put(minID, nextIndex);
                currentDates.add(new RiseSetTimeID(timeArrayMap.get(minID).get(nextIndex), minID));
            } else if (nextIndex == timeArrayMap.get(minID).size() && minDate.isRise()) {
                numOpenIntervals++;
            }
            
            resultant.add(minDate);

            //if there are no more dates to sort, break from loop
            if (currentDates.isEmpty()) {
                break;
            }
        }
        for (int i = 0; i < numOpenIntervals; i++) {
            resultant.add(new RiseSetTime(tail, false));
        }
        return resultant;
    }

    /**
     * Keeps track of when and how many overlaps occur between the time
     * intervals. Also removes duplicate rise or set times from the given list.
     *
     * @param riseSetTimes the sorted set of rise and set times
     * @return
     */
    private ArrayList<Integer> findOverlaps(ArrayList<RiseSetTime> riseSetTimes) {
        ArrayList<Integer> overlap = new ArrayList<>();
        
        //if there are no time intervals, there are no overlapping intervals
        if(riseSetTimes.isEmpty()){
            overlap.add(0);
            return overlap;
        }

        RiseSetTime currentTime = riseSetTimes.get(0);
        int currentOverlap = 1;
        for (int i = 1; i < riseSetTimes.size(); i++) {
            RiseSetTime event = riseSetTimes.get(i);

            if (!event.equals(currentTime)) {
                currentTime = event;
                overlap.add(currentOverlap);
            } else {
                riseSetTimes.remove(i); //remove duplicate rise set times
                i--;
            }

            if (event.isRise()) {
                currentOverlap++;
            } else {
                currentOverlap--;
            }
        }

        return overlap;
    }

    /**
     * This method uses a logical AND to combine the time intervals. Resulting
     * time interval array uses the earliest head date and the latest tail date
     * in the collection of TimeIntervalSets
     *
     * @return the intersection of all the time intervals
     */
    public TimeIntervalArray andCombine() {
        return nOverlapping(this.nTotalArrays);
    }

    /**
     * This method uses a logical OR to combine the time intervals. Resulting
     * time interval array uses the earliest head date and the latest tail date
     * in the collection of TimeIntervalSets
     *
     * @return the union of all the time intervals
     */
    public TimeIntervalArray orCombine() {
        return nOverlapping(1);
    }

    /**
     * This method finds the intervals where at least n intervals overlap at a
     * given time. Resulting time interval array uses the earliest head date and
     * the latest tail date in the collection of TimeIntervalSets
     *
     * @param n the minimum number of intervals that need to overlap to be
     * included in the set.
     * @return
     */
    public TimeIntervalArray nOverlapping(int n) {
        TimeIntervalArray out = new TimeIntervalArray(head, tail);
        for (int i = 0; i < this.overlaps.size(); i++) {
            if (overlaps.get(i) >= n) {
                if (!out.isTailOpen()) {
                    out.addRiseTime(sortedRiseSetTimes.get(i));
                }
            } else {
                if (out.isTailOpen()) {
                    out.addSetTime(sortedRiseSetTimes.get(i));
                }
            }
        }
        //Need to check if last time stamp should be added.
        if (overlaps.get(overlaps.size() - 1) >= n
                && out.isTailOpen()) {
            out.addSetTime(sortedRiseSetTimes.get(sortedRiseSetTimes.size() - 1));
        }

        return out;
    }

    private class RiseSetTimeID {

        private final RiseSetTime time;
        private final int id;

        public RiseSetTimeID(RiseSetTime time, int id) {
            this.time = time;
            this.id = id;
        }

        public RiseSetTime getTime() {
            return time;
        }

        public int getId() {
            return id;
        }
    }

    private class TimeComparator implements Comparator<RiseSetTimeID> {

        @Override
        public int compare(RiseSetTimeID a, RiseSetTimeID b) {
            return a.getTime().compareTo(b.getTime());
        }
    }

}
