/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.propagation;

/**
 * These enums enumerate the propagator types supported by the
 * PropagatorFactory. More can be added as the code base is developed further
 *
 * @author nozomihitomi
 */
public enum PropagatorType {
    KEPLERIAN, //for keplerian propagation
    TLE, //Use if TLE is available
    NUMERICAL //numerical propagation for when using force models (i.e. atmospheric drag)
}
