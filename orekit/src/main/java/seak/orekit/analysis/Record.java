/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.analysis;

import java.io.Serializable;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author nozomihitomi
 */
public class Record<T> implements Comparable<Record<T>>, Serializable{
    private static final long serialVersionUID = 1580288073315494198L;
    
    private final AbsoluteDate date;
    
    private final T value;

    public Record(AbsoluteDate date, T value) {
        this.date = date;
        this.value = value;
    }
    
    public T getValue(){
        return value;
    }

    public AbsoluteDate getDate() {
        return date;
    }
    
    /**
     * Use to compare the dates of two ephemeris data points
     * @param o
     * @return 
     */
    @Override
    public int compareTo(Record o) {
        return this.getDate().compareTo(o.getDate());
    }
    
}
