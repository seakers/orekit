/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.coverage.access;

import java.util.ArrayList;
import java.util.Collection;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import org.hipparchus.util.FastMath;

/**
 * This class is responsible for dividing up the coverage definition into
 * subsets in order to decompose a simulation into multiple smaller simulations
 *
 * @author nozomihitomi
 */
public class CoverageDivider {
    
     /**
     * This method divides the coverage grid into subregions to break up the
     * simulation in parts. The subregions can be then simulated sequentially or
     * in parallel
     *
     * @param cdef
     * @param numDivisions
     * @return
     */
    public ArrayList<Collection<CoveragePoint>> divide(CoverageDefinition cdef, int numDivisions) {
        ArrayList<Collection<CoveragePoint>> out = new ArrayList<>(numDivisions);
        if (numDivisions == 1) {
            out.add(cdef.getPoints());
        } else {
            //TODO implement map coloring to efficiently distribute points
            for (int i = 0; i < numDivisions; i++) {
                out.add(new ArrayList());
            }
            int i = 0;
            for (CoveragePoint pt : cdef.getPoints()) {
                out.get(FastMath.floorMod(i, numDivisions)).add(pt);
                i++;
            }
        }
        return out;
    }
}
