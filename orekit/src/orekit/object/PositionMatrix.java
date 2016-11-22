/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object;

import java.util.Collection;
import java.util.HashMap;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

/**
 * An array that stores the position of multiple points over time
 *
 * @author SEAK1
 */
public class PositionMatrix {

    /**
     * Stores which row a point belongs to
     */
    private final HashMap<CoveragePoint, Integer> ptMap;

    /**
     * Stores which row a point belongs to
     */
    private final HashMap<Double, Integer> dateMap;

    /**
     * matrix of position vectors where the rows are points and the columns are
     * epoch seconds from the start date
     */
    private final Vector3D[][] positions;

    /**
     * The start date of the simulation
     */
    private final AbsoluteDate startDate;
    
    private final double timeStep;

    public PositionMatrix(Collection<CoveragePoint> pts, AbsoluteDate startDate, AbsoluteDate endDate, double timeStep) {
        this.ptMap = new HashMap<>(pts.size());
        int i = 0;
        for (CoveragePoint pt : pts) {
            this.ptMap.put(pt, i);
            i++;
        }
        double simTime = endDate.durationFrom(startDate);
        this.dateMap = new HashMap<>();
        i = 0;
        for (double t = 0; t < simTime; t = i * timeStep) {
            this.dateMap.put(t, i);
            i++;
        }
        this.positions = new Vector3D[this.ptMap.size()][this.dateMap.size()];
        this.startDate = startDate;
        this.timeStep = timeStep;
    }

    /**
     * Adds a position vector to the matrix
     *
     * @param pt
     * @param date
     * @param vec
     */
    public void add(CoveragePoint pt, AbsoluteDate date, Vector3D vec) {
        Integer row = this.ptMap.get(pt);
        if(row == null){
            throw new IllegalArgumentException(String.format("The coverage point %s does not exist in position matrix", pt));
        }
        double deltaT = date.durationFrom(startDate);
        Integer col = this.dateMap.get(deltaT);
        if(col == null){
            throw new IllegalArgumentException(String.format("The specified date %s cannot be added to position matrix. Date must be some step size %f[s] away from %s", date, timeStep, startDate));
        }
        this.positions[row][col]=vec;
    }
    
    /**
     * Gets a position vector from the matrix at the specified point and date
     *
     * @param pt
     * @param date
     * @return 
     */
    public Vector3D get(CoveragePoint pt, AbsoluteDate date) {
        Integer row = this.ptMap.get(pt);
        if(row == null){
            throw new IllegalArgumentException(String.format("The coverage point %s does not exist in position matrix", pt));
        }
        double deltaT = date.durationFrom(startDate);
        Integer col = this.dateMap.get(deltaT);
        if(col == null){
            throw new IllegalArgumentException(String.format("The specified date %s cannot be added to position matrix. Date must be some step size %f[s] away from %s", date, timeStep, startDate));
        }
        return this.positions[row][col];
    }

}
