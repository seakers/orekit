/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import seak.orekit.coverage.access.TimeIntervalArray;
import seak.orekit.object.Constellation;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.object.CoveragePoint;
import seak.orekit.object.Satellite;

/**
 * The abstract class for an event analysis.
 *
 * @author nhitomi
 */
public abstract class AbstractGroundEventAnalysis extends AbstractEventAnalysis implements GroundEventAnalysis {

    /**
     * A set of the unique constellations
     */
    private final Set<Constellation> uniqueConstellations;

    /**
     * The mapping of the unique satellites assigned to each coverage definition
     */
    private final Map<CoverageDefinition, Set<Satellite>> uniqueSatsAssignedToCovDef;

    /**
     * A collection of the unique satellites in this scenario. Required to only
     * propagate each satellite once and once only.
     */
    private final Set<Satellite> uniqueSatellites;

    /**
     * The set of coverage definitions to simulate.
     */
    private final Set<CoverageDefinition> covDefs;

    /**
     * This object stores when the events occur at each point for each coverage
     * definition
     */
    private Map<CoverageDefinition, Map<CoveragePoint, TimeIntervalArray>> events;

    public AbstractGroundEventAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, Frame inertialFrame, HashSet<CoverageDefinition> covDefs) {
        super(startDate, endDate, inertialFrame);

        this.covDefs = Collections.unmodifiableSet(covDefs);
        //record all unique satellite and constellations
        HashMap<CoverageDefinition, Set<Satellite>> tuniqueSatsAssignedToCovDef = new HashMap<>();
        HashSet<Constellation> tuniqueConstellations = new HashSet<>();
        HashSet<Satellite> tuniqueSatellites = new HashSet<>();
        this.events = new HashMap<>();
        
        for (CoverageDefinition cdef : this.covDefs) {
            tuniqueSatsAssignedToCovDef.put(cdef, new HashSet());
            for (Constellation constel : cdef.getConstellations()) {
                tuniqueConstellations.add(constel);
                for (Satellite satellite : constel.getSatellites()) {
                    tuniqueSatellites.add(satellite);
                    tuniqueSatsAssignedToCovDef.get(cdef).add(satellite);
                }
            }

            //create a new time interval array for each point in the coverage definition
            HashMap<CoveragePoint, TimeIntervalArray> ptAccesses = new HashMap<>();
            for (CoveragePoint pt : cdef.getPoints()) {
                ptAccesses.put(pt, new TimeIntervalArray(startDate, endDate));
            }
            
            //make unique sat per covedef sets immutatble
            tuniqueSatsAssignedToCovDef.put(cdef, Collections.unmodifiableSet(tuniqueSatsAssignedToCovDef.get(cdef)));
            events.put(cdef, ptAccesses);
        }
        
        //make all sets and maps immutable
        this.uniqueSatsAssignedToCovDef = Collections.unmodifiableMap(tuniqueSatsAssignedToCovDef);
        this.uniqueConstellations = Collections.unmodifiableSet(tuniqueConstellations);
        this.uniqueSatellites = Collections.unmodifiableSet(tuniqueSatellites);
    }

    @Override
    public Map<CoverageDefinition, Map<CoveragePoint, TimeIntervalArray>> getEvents() {
        return events;
    }

    @Override
    public Map<CoveragePoint, TimeIntervalArray> getEvents(CoverageDefinition covDef) {
        return events.get(covDef);
    }

    /**
     * Gets the list of coverage definitions assigned to this scenario
     *
     * @return
     */
    public HashSet<CoverageDefinition> getCoverageDefinitions() {
        return new HashSet<>(covDefs);
    }

    /**
     * Gets the unique constellations that are simulated in this scenario
     *
     * @return
     */
    public HashSet<Constellation> getUniqueConstellations() {
        return new HashSet<>(uniqueConstellations);
    }

    /**
     * Gets the unique satellites that are simulated in this scenario
     *
     * @return
     */
    public HashSet<Satellite> getUniqueSatellites() {
        return new HashSet<>(uniqueSatellites);
    }

    /**
     * Gets the unique satellites belonging to the specified coverage definition
     *
     * @param cdef
     * @return
     */
    protected Set<Satellite> getUniqueSatellites(CoverageDefinition cdef) {
        return uniqueSatsAssignedToCovDef.get(cdef);
    }
    
    /**
     * Gets the coverage definition specified by a name
     *
     * @param name of the CoverageDefinition we want to get
     * @return
     */
    public CoverageDefinition getCoverageDefinition(String name) {
        Iterator<CoverageDefinition> i = this.covDefs.iterator();
        while (i.hasNext()) {
            CoverageDefinition c = i.next();
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }
}
