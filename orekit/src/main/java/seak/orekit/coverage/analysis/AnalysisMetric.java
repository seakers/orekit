/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.coverage.analysis;

/**
 *
 * @author nhitomi
 */
public enum AnalysisMetric {
    
    DURATION,
    MEAN_TIME_TO_T, //used for mean time to access or mean response time
    TIME_AVERAGE, //Analytically, it is simply double the the mean time to T
    PERCENT_TIME, //the quotient between the sum of the durations when the event is occuring and the total simulation time
    
}
