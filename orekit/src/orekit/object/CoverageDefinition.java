/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import orekit.coverage.access.TimeIntervalArray;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;

/**
 * Stores information about a grid of points (each point is a TopocentricFrame)
 *
 * @author nozomihitomi
 */
public class CoverageDefinition implements OrekitObject, Serializable {

    private static final long serialVersionUID = 2467782856759734909L;

    /**
     * Grid of points to define the target area
     */
    private HashSet<CoveragePoint> grid;

    /**
     * constellations responsible for monitoring the coverage grid
     */
    private HashSet<Constellation> constellations;

    /**
     * name of Coverage definition
     */
    private final String name;

    /**
     * Shape used to project CoveragePoints
     */
    private final BodyShape planet;

    /**
     * Creates a new grid of GeodeticPoints on the entire surface of a given
     * BodyShape ([-90 deg, 90 deg] in latitude and [0 deg, 360 deg] in
     * longitude).
     *
     * @param name of the coverage definition
     * @param granularity defines the resolution of the grid points. Input in
     * degrees
     * @param planet Body shape on which to project the CoveragePoints
     * @param startDate The date when the scenario starts
     * @param endDate The date when the scenario ends
     */
    public CoverageDefinition(String name, double granularity, BodyShape planet,
            AbsoluteDate startDate, AbsoluteDate endDate) {
        this(name, granularity, -90, 90, 0, 360, planet, startDate, endDate);
    }

    /**
     * Creates a new grid of GeodeticPoints on the surface of a given BodyShape
     * within the given latitude and longitude bounds.
     *
     * @param name of the coverage definition
     * @param granularity defines the resolution of the grid points. Input in
     * degrees
     * @param minLatitude Minimum latitude where coverage is defined [deg]
     * @param maxLatitude Maximum latitude where coverage is defined [deg]
     * @param minLongitdue Maximum latitude where coverage is defined [deg]
     * @param maxLongitude Maximum latitude where coverage is defined [deg]
     * @param planet Body shape on which to project the CoveragePoints
     * @param startDate The date when the scenario starts
     * @param endDate The date when the scenario ends
     */
    public CoverageDefinition(String name, double granularity, double minLatitude,
            double maxLatitude, double minLongitdue, double maxLongitude,
            BodyShape planet, AbsoluteDate startDate, AbsoluteDate endDate) {

        this.name = name;
        this.planet = planet;

        //check to see if minimum lat/lon is less than maximum lat/lon
        if (minLatitude > maxLatitude) {
            throw new IllegalArgumentException("Minimum latitude ("
                    + minLatitude + ") must be less than maximum latitude ("
                    + maxLatitude + ")");
        }
        if (minLongitdue > maxLongitude) {
            throw new IllegalArgumentException("Minimum latitude ("
                    + minLongitdue + ") must be less than maximum latitude ("
                    + maxLongitude + ")");
        }

        //Create grid points
        this.grid = new HashSet();
        int numPoints = 0;
        for (double lat = minLatitude; lat <= maxLatitude; lat += granularity) {
            //weigh the number of points by their latitude to get even 
            //distribution of points within spherical grid
            int satsAtLat = (int) (360 / granularity * FastMath.cos(FastMath.toRadians(lat)));
            for (double lon = minLongitdue; lon < maxLongitude; lon += 360.0 / satsAtLat) {
                double longitude = FastMath.toRadians(lon);
                double latitude = FastMath.toRadians(lat);
                double altitude = 0.;
                GeodeticPoint point = new GeodeticPoint(latitude, longitude, altitude);
                this.grid.add(new CoveragePoint(planet, point, String.valueOf(numPoints), startDate, endDate));
                numPoints++;
            }
        }

        System.out.println(String.format("Coverage Grid '%s' has %d points", this.name, this.getNumberOfPoints()));
    }

    /**
     * Defines a coverage shape using the given points projected onto the
     * surface of a given BodyShape
     *
     * @param name of the coverage definition
     * @param points points defining the coverage definition
     * @param planet Body shape on which to project the CoveragePoints
     * @param startDate The date when the scenario starts
     * @param endDate The date when the scenario ends
     */
    public CoverageDefinition(String name, Collection<GeodeticPoint> points,
            BodyShape planet, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.name = name;
        this.planet = planet;
        this.grid = new HashSet();
        int numPoints = 0;
        for (GeodeticPoint point : points) {
            this.grid.add(new CoveragePoint(planet, point, String.valueOf(numPoints), startDate, endDate));
        }
    }

    /**
     * Assigns the coverage grid to a constellation that will monitor the
     * coverage grid
     *
     * @param constellation
     */
    public void assignConstellation(Constellation constellation) {
        this.constellations = new HashSet();
        this.constellations.add(constellation);
    }

    /**
     * Assigns the coverage grid to a colletion of constellations that will
     * monitor the coverage grid
     *
     * @param constellation
     */
    public void assignToConstellations(Collection<Constellation> constellation) {
        this.constellations = new HashSet(constellation);
    }

    /**
     * Checks to see if a constellation is assigned to this coverage definition
     *
     * @param constellation
     * @return
     */
    public boolean isAssigned(Constellation constellation) {
        return constellations.contains(constellation);
    }

    /**
     * Checks to see if given satellite is assigned to this coverage definition
     *
     * @param satellite
     * @return
     */
    public boolean isAssigned(Satellite satellite) {
        for (Constellation constel : constellations) {
            if (constel.hasSatellite(satellite)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a hashmap of all the accesses stored in this coverage definition.
     *
     * @return a hashmap of all the accesses stored in this coverage definition
     * where the keys are the grid points and the values are the time interval
     * arrays
     */
    public HashMap<CoveragePoint, TimeIntervalArray> getAccesses() {
        HashMap<CoveragePoint, TimeIntervalArray> out = new HashMap<>(grid.size());
        for (CoveragePoint pt : grid) {
            out.put(pt, pt.getAccesses());
        }
        return out;
    }

    /**
     * Clears the access information stored within all coverage points of this
     * coverage defintion
     */
    public void clearAccesses() {
        for (CoveragePoint pt : grid) {
            pt.reset();
        }
    }

    /**
     * Gets the collection of constellations that is responsible for monitoring
     * the coverage grid
     *
     * @return
     */
    public Collection<Constellation> getConstellations() {
        return constellations;
    }

    /**
     * Returns the number of points in the grid
     *
     * @return
     */
    public int getNumberOfPoints() {
        return grid.size();
    }

    /**
     * returns a new Collection of points within the grid
     *
     * @return
     */
    public HashSet<CoveragePoint> getPoints() {
        return new HashSet<>(grid);
    }

    /**
     * Get the planet shape of the coverage definition
     *
     * @return
     */
    public BodyShape getPlanetShape() {
        return planet;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CoverageDefinition{" + "name=" + name + ", number of points=" + grid.size() + "}";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.grid);
        hash = 59 * hash + Objects.hashCode(this.constellations);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CoverageDefinition other = (CoverageDefinition) obj;
        if (!Objects.equals(this.grid, other.grid)) {
            return false;
        }
        if (!Objects.equals(this.constellations, other.constellations)) {
            return false;
        }
        return true;
    }

}
