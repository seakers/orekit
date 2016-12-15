/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.analysis.events;

import java.util.HashMap;
import org.orekit.propagation.events.EventDetector;

/**
 *
 * @author SEAK1
 */
public class GValues<T> extends HashMap<T, Double>{
    
    @Override
    public String toString() {
        String str = "";
        for(T dec : this.keySet()){
            str += String.format("%f,",this.get(dec));
            }
        return str;
    }
    
}
