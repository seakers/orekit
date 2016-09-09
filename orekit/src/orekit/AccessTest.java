/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit;

import java.util.ArrayList;
import java.util.Iterator;
import orekit.access.RiseSetTime;
import orekit.access.TimeIntervalArray;
import orekit.access.TimeIntervalMerger;
import orekit.access.TimeIntervalStatistics;
import orekit.util.OrekitConfig;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

/**
 *
 * @author nozomihitomi
 */
public class AccessTest {
    
    
    public static void main(String[] args) throws OrekitException{
        
        OrekitConfig.init();

        TimeScale utc = TimeScalesFactory.getUTC();
        
        AbsoluteDate acc1_1 = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);
        AbsoluteDate acc1_2 = acc1_1.shiftedBy(10);
        AbsoluteDate acc1_3 = acc1_1.shiftedBy(20);
        AbsoluteDate acc1_4 = acc1_1.shiftedBy(30);
        AbsoluteDate acc1_5 = acc1_1.shiftedBy(40);
        AbsoluteDate acc1_6 = acc1_1.shiftedBy(50);
        AbsoluteDate acc1_7 = acc1_1.shiftedBy(60);
        
        
        TimeIntervalArray array1 = new TimeIntervalArray(acc1_1, acc1_7.shiftedBy(10));
        array1.addRiseTime(acc1_1);
        array1.addSetTime(acc1_2);
        array1.addRiseTime(acc1_3);
        array1.addSetTime(acc1_4);
        array1.addRiseTime(acc1_5);
        array1.addSetTime(acc1_6);
        array1.addRiseTime(acc1_7);
        
        Iterator<RiseSetTime> iter = array1.iterator();
        System.out.println("Original Array:");
        while(iter.hasNext()){
            RiseSetTime time = iter.next();
            if(time.isRise()){
                System.out.println("Rise: " + time);
            }else{
                System.out.println("Set: " + time);
            }
        }
        System.out.println("Mean " + TimeIntervalStatistics.mean(array1));
        System.out.println("Max " + TimeIntervalStatistics.max(array1));
        System.out.println("Min " + TimeIntervalStatistics.min(array1));
        System.out.println("50th " + TimeIntervalStatistics.percentile(array1,50));
        
        System.out.println("\n\n");
        TimeIntervalArray array2 = array1.negate();
        Iterator<RiseSetTime> iter2 = array2.iterator();
        System.out.println("Negated Array:");
        while(iter2.hasNext()){
            RiseSetTime time = iter2.next();
            if(time.isRise()){
                System.out.println("Rise: " + time);        
            }else{
                System.out.println("Set: " + time);
            }
        }
        System.out.println("Mean " + TimeIntervalStatistics.mean(array2));
        System.out.println("Max " + TimeIntervalStatistics.max(array2));
        System.out.println("Min " + TimeIntervalStatistics.min(array2));
        System.out.println("50th " + TimeIntervalStatistics.percentile(array2,50));

        
        ArrayList<TimeIntervalArray> arrayTimes = new ArrayList();
        arrayTimes.add(array1);
        arrayTimes.add(array1);
        arrayTimes.add(array2);
        TimeIntervalMerger set = new TimeIntervalMerger(arrayTimes);
        TimeIntervalArray union = set.orCombine();
        System.out.println("\n\n");
        Iterator<RiseSetTime> iteru = union.iterator();
        System.out.println("Union Array:");
        while(iteru.hasNext()){
            RiseSetTime time = iteru.next();
            if(time.isRise()){
                System.out.println("Rise: " + time);
            }else{
                System.out.println("Set: " + time);
            }
        }
        System.out.println("Mean " + TimeIntervalStatistics.mean(union));
        System.out.println("Max " + TimeIntervalStatistics.max(union));
        System.out.println("Min " + TimeIntervalStatistics.min(union));
        System.out.println("50th " + TimeIntervalStatistics.percentile(union,50));
        
        TimeIntervalArray intersect = set.andCombine();
        System.out.println("\n\n");
        Iterator<RiseSetTime> iteri = intersect.iterator();
        System.out.println("Intersection Array:");
        while(iteri.hasNext()){
            RiseSetTime time = iteri.next();
            if(time.isRise()){
                System.out.println("Rise: " + time);
            }else{
                System.out.println("Set: " + time);
            }
        }
        System.out.println("Mean " + TimeIntervalStatistics.mean(intersect));
        System.out.println("Max " + TimeIntervalStatistics.max(intersect));
        System.out.println("Min " + TimeIntervalStatistics.min(intersect));
        System.out.println("50th " + TimeIntervalStatistics.percentile(intersect,50));
        
        TimeIntervalArray atleast2 = set.nOverlapping(2);
        System.out.println("\n\n");
        Iterator<RiseSetTime> iteratleast = atleast2.iterator();
        System.out.println("At least2 Array:");
        while(iteratleast.hasNext()){
            RiseSetTime time = iteratleast.next();
            if(time.isRise()){
                System.out.println("Rise: " + time);
            }else{
                System.out.println("Set: " + time);
            }
        }
        System.out.println("Mean " + TimeIntervalStatistics.mean(atleast2));
        System.out.println("Max " + TimeIntervalStatistics.max(atleast2));
        System.out.println("Min " + TimeIntervalStatistics.min(atleast2));
        System.out.println("50th " + TimeIntervalStatistics.percentile(atleast2,50));
    }
}
