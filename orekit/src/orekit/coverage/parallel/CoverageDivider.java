/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.coverage.parallel;

import java.util.ArrayList;
import java.util.Collection;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import org.hipparchus.util.FastMath;

/**
 * This class is responsible for dividing the coverage definition into multiple
 * smaller coverage definitions to allow for quicker parallelization of the
 * simulation
 *
 * @author SEAK1
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
    public static Collection<CoverageDefinition> divide(CoverageDefinition cdef, int numDivisions) {
        ArrayList<CoverageDefinition> out = new ArrayList<>(numDivisions);
        if (numDivisions == 1) {
            out.add(cdef);
        } else {
            Collection<Collection<CoveragePoint>> pointGroups = divide(cdef.getPoints(), numDivisions);
            int groupNum = 0;
            for (Collection<CoveragePoint> group : pointGroups) {
                CoverageDefinition subDivision = new CoverageDefinition(cdef.getName() + "_" + groupNum, group);
                subDivision.assignConstellation(cdef.getConstellations());
                out.add(subDivision);
            }
        }
        return out;
    }
    /**
     * 
     * @param points a collection of points to divide into multiple collections
     * @param numDivisions the number of divisions to create
     * @return  A collection of partial coverage definitions
     */
    public static Collection<Collection<CoveragePoint>> divide(Collection<CoveragePoint> points, int numDivisions){
        //TODO implement map coloring to efficiently distribute points
        ArrayList<Collection<CoveragePoint>> pointGroups = new ArrayList<>(numDivisions);
        for (int i = 0; i < numDivisions; i++) {
            pointGroups.add(new ArrayList());
        }
        int j = 0;
        for (CoveragePoint pt : points) {
            pointGroups.get(FastMath.floorMod(j, numDivisions)).add(pt);
            j++;
        }
        return pointGroups;
    }
}
