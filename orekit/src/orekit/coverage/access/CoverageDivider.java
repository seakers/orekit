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
     * in parallel.
     *
     * @param cdef the coverage definition to divide
     * @param numDivisions the number of divisions
     * @return a collection of coverage definitions that are all identical
     * except for the points internal to the coverage definition
     */
    public Collection<CoverageDefinition> divide(CoverageDefinition cdef, int numDivisions) {
        ArrayList<CoverageDefinition> out = new ArrayList<>(numDivisions);
        if (numDivisions == 1) {
            out.add(cdef);
        } else {
            out.addAll(divideCoverage(cdef, numDivisions));
        }
        return out;
    }

    private Collection<CoverageDefinition> divideCoverage(CoverageDefinition cdef, int numDivisions) {
        //TODO implement map coloring to efficiently distribute points
        ArrayList<ArrayList<CoveragePoint>> points = new ArrayList<>(numDivisions);
        for (int i = 0; i < numDivisions; i++) {
            points.add(new ArrayList());
        }
        int j = 0;
        for (CoveragePoint pt : cdef.getPoints()) {
            points.get(FastMath.floorMod(j, numDivisions)).add(pt);
            j++;
        }
        ArrayList<CoverageDefinition> out =  new ArrayList<>(numDivisions);
        for (int i = 0; i < numDivisions; i++) {
            out.add(new CoverageDefinition(cdef.getName() + "_i", points.get(i)));
        }
        return out;
    }
}
