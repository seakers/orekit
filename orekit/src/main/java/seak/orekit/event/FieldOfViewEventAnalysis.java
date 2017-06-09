/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import seak.orekit.coverage.access.RiseSetTime;
import seak.orekit.coverage.access.TimeIntervalArray;
import seak.orekit.coverage.access.TimeIntervalMerger;
import seak.orekit.event.detector.FOVDetector;
import seak.orekit.event.detector.LOSDetector;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.object.CoveragePoint;
import seak.orekit.object.Instrument;
import seak.orekit.object.Satellite;
import seak.orekit.propagation.PropagatorFactory;

/**
 * This event analysis is used to compute when the given ground points are in
 * the field of view of the given satellites. The satellites can be grouped into
 * multiple constellations, where a satellite can belong to more than one
 * constellation. Specific accesses between a point and a
 * satellite/constellation can be retrieved.
 *
 * @author nhitomi
 */
public class FieldOfViewEventAnalysis extends AbstractGroundEventAnalysis {

    /**
     * Propagator factory that will create the necessary propagator for each
     * satellite
     */
    private final PropagatorFactory propagatorFactory;


    /**
     * a flag set by the user to toggle whether to save the access of each
     * individual satellite or to release them from memory.
     */
    private final boolean saveAllAccesses;

    /**
     * flag to dictate whether the coverage accesses of individual satellites
     * should be saved to the coverage database
     */
    private boolean saveToDB = false;

    /**
     * Stores all the accesses of each satellite if saveAllAccesses is true.
     */
    private HashMap<CoverageDefinition, HashMap<Satellite, HashMap<CoveragePoint, TimeIntervalArray>>> allAccesses;

    /**
     * the number of threads to use in parallel processing
     */
    private final int numThreads;

    /**
     * Creates a new scenario.
     *
     * @param startDate of scenario
     * @param endDate of scenario
     * @param inertialFrame
     * @param propagatorFactory
     * @param covDefs
     * @param saveAllAccesses true if user wants to maintain all the accesses
     * from each individual satellite. false if user would like to only get the
     * merged accesses between all satellites (this saves memory).
     * @param saveToDB flag to dictate whether the coverage accesses of
     * individual satellites should be saved to the coverage database
     * @param numThreads number of threads to uses in parallelization of the
     * scenario by dividing up the coverage grid points across multiple threads
     */
    public FieldOfViewEventAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame, HashSet<CoverageDefinition> covDefs,
            PropagatorFactory propagatorFactory, boolean saveAllAccesses,
            boolean saveToDB, int numThreads) {
        super(startDate, endDate, inertialFrame, covDefs);
        this.propagatorFactory = propagatorFactory;
        this.saveAllAccesses = saveAllAccesses;
        if (saveAllAccesses) {
            this.allAccesses = new HashMap();
        }
        this.saveToDB = saveToDB;

        this.numThreads = numThreads;
    }


    /**
     * Runs the scenario from the start date to the end date. Running the
     * scenario propagates the orbits of each satellite in the constellation and
     * computes the accesses between the satellites and the ground stations or
     * grid points. The accesses are stored and are accessible after the
     * simulation is run.
     *
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    @Override
    public FieldOfViewEventAnalysis call() throws OrekitException {
        //set up resource pool
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CompletionService<FieldOfViewSubRoutine> ecs = new ExecutorCompletionService(pool);

        for (CoverageDefinition cdef : getCoverageDefinitions()) {
            Logger.getGlobal().finest(String.format("Acquiring access times for %s...", cdef));
            if (saveAllAccesses) {
                allAccesses.put(cdef, new HashMap());
            }
            
            //propogate each satellite individually
            int nSubRoutines = 0;
            for (Satellite sat : getUniqueSatellites(cdef)) {
                Propagator prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());
                //Set stepsizes and threshold for detectors
                double losStepSize = sat.getOrbit().getKeplerianPeriod() / 10.;
                double fovStepSize = sat.getOrbit().getKeplerianPeriod() / 100.;
                double threshold = 1e-3;

                FieldOfViewSubRoutine subRoutine = new FieldOfViewSubRoutine(sat, prop, cdef, losStepSize, fovStepSize, threshold);
                ecs.submit(subRoutine);
                nSubRoutines++;
            }
            
            for(int i=0; i<nSubRoutines; i++){
                FieldOfViewSubRoutine subRoutine = null;
                try {
                    subRoutine = ecs.take().get();
                } catch (InterruptedException ex) {
                    Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                }

                if(subRoutine == null){
                    throw new IllegalStateException("Subroutine failed in field of view event.");
                }
                
                Satellite sat = subRoutine.getSat();
                HashMap<CoveragePoint, TimeIntervalArray> satAccesses = subRoutine.getSatAccesses();
                
                //save the satellite accesses 
                if (saveAllAccesses) {
                    allAccesses.get(cdef).put(sat, satAccesses);
                }

                //merge the time accesses across all satellite for each coverage definition
                if (getEvents().containsKey(cdef)) {
                    Map<CoveragePoint, TimeIntervalArray> mergedAccesses
                            = EventIntervalMerger.merge(getEvents().get(cdef), satAccesses, false);
                    getEvents().put(cdef, mergedAccesses);
                } else {
                    getEvents().put(cdef, satAccesses);
                }
            }

            //Make all time intervals stored in finalAccesses immutable
            for (CoveragePoint pt : getEvents().get(cdef).keySet()) {
                getEvents().get(cdef).put(pt, getEvents().get(cdef).get(pt).createImmutable());
            }
        }

        pool.shutdown();
        return this;
    }

    /**
     * Returns the individual accesses of a given satellite on a given coverage
     * definition after the scenario is finished running.
     *
     * @param covDef the coverage definition of interest
     * @param sat a satellite that is assigned to the coverage definition
     * @return If the scenario is set to save the individual satellite accesses
     * and the satellite is assigned to the coverage definition, a map of
     * coverage points and time interval array will be returned. else null
     */
    public HashMap<CoveragePoint, TimeIntervalArray> getSatelliteAccesses(CoverageDefinition covDef, Satellite sat) {
        return allAccesses.get(covDef).get(sat);
    }

    /**
     * Returns the flag that marks whether each satellite's accesses should be
     * saved.
     *
     * @return
     */
    public boolean isSaveAllAccesses() {
        return saveAllAccesses;
    }

    /**
     * Returns the computed accesses for each coverage definition by each of the
     * satellites assigned to that coverage definition
     *
     * @return
     */
    public HashMap<CoverageDefinition, HashMap<Satellite, HashMap<CoveragePoint, TimeIntervalArray>>> getAllAccesses() {
        return allAccesses;
    }

    /**
     * Returns the flag that dictates whether the individual satellite coverage
     * accesses are to be saved in the database
     *
     * @return
     */
    public boolean isSaveToDB() {
        return saveToDB;
    }

    /**
     * Gets the propagator factory used to create new propagators for this
     * scenario
     *
     * @return
     */
    public PropagatorFactory getPropagatorFactory() {
        return propagatorFactory;
    }

    /**
     * Creates a subroutine to run the field of view event analysis in parallel
     */
    private class FieldOfViewSubRoutine implements Callable<FieldOfViewSubRoutine> {

        /**
         * The satellite to propagate
         */
        private final Satellite sat;

        /**
         * The propagator
         */
        private final Propagator prop;

        /**
         * The coverage definition to access
         */
        private final CoverageDefinition cdef;

        /**
         * The step size during propagation when computing the line of sight
         * events. Generally, this can be a large step. It is used to speed up
         * the simulation.
         */
        private final double losStepSize;

        /**
         * The step size during propagation when computing the field of view
         * events. Generally, this should be a small step for accurate results.
         */
        private final double fovStepSize;

        /**
         * The threshold, in seconds, when conducting root finding to determine
         * when an event occurred.
         */
        private final double threshold;

        /**
         * The times, for each point, when it is being accessed by the given
         * satellite and its payload.
         */
        private final HashMap<CoveragePoint, TimeIntervalArray> satAccesses;

        /**
         *
         * @param sat The satellite to propagate
         * @param prop The propagator
         * @param cdef The coverage definition to access
         * @param losStepSize The step size during propagation when computing
         * the line of sight events. Generally, this can be a large step. It is
         * used to speed up the simulation.
         * @param fovStepSize The step size during propagation when computing
         * the field of view events. Generally, this should be a small step for
         * accurate results.
         * @param threshold The threshold, in seconds, when conducting root
         * finding to determine when an event occurred.
         */
        public FieldOfViewSubRoutine(Satellite sat, Propagator prop,
                CoverageDefinition cdef, double losStepSize,
                double fovStepSize, double threshold) {
            this.sat = sat;
            this.prop = prop;
            this.cdef = cdef;
            this.losStepSize = losStepSize;
            this.fovStepSize = fovStepSize;
            this.threshold = threshold;

            this.satAccesses = new HashMap<>(cdef.getNumberOfPoints());
            for (CoveragePoint pt : cdef.getPoints()) {
                satAccesses.put(pt, getEmptyTimeArray());
            }
        }

        @Override
        public FieldOfViewSubRoutine call() throws Exception {
            SpacecraftState initialState = prop.getInitialState();
            Logger.getGlobal().finest(String.format("Propagating satellite %s...", sat));
            for (Instrument inst : sat.getPayload()) {
                for (CoveragePoint pt : cdef.getPoints()) {
                    prop.resetInitialState(initialState);
                    prop.clearEventsDetectors();

                    //First find all intervals with line of sight.
                    LOSDetector losDetec = new LOSDetector(
                            prop.getInitialState(), getStartDate(), getEndDate(),
                            pt, cdef.getPlanetShape(), getInertialFrame(),
                            losStepSize, threshold, EventHandler.Action.CONTINUE);
                    prop.addEventDetector(losDetec);
                    prop.propagate(getStartDate(), getEndDate());
                    TimeIntervalArray losTimeArray = losDetec.getTimeIntervalArray();
                    if (losTimeArray == null || losTimeArray.isEmpty()) {
                        continue;
                    }

                    //need to reset initial state of the propagators or will progate from the last stop time
                    prop.resetInitialState(initialState);
                    prop.clearEventsDetectors();
                    //Next search through intervals with line of sight to compute when point is in field of view 
                    FOVDetector fovDetec = new FOVDetector(initialState, getStartDate(), getEndDate(),
                            pt, inst, fovStepSize, threshold, EventHandler.Action.STOP);
                    prop.addEventDetector(fovDetec);

                    double date0 = 0;
                    double date1 = Double.NaN;
                    for (RiseSetTime interval : losTimeArray) {
                        if (interval.isRise()) {
                            date0 = interval.getTime();
                        } else {
                            date1 = interval.getTime();
                        }

                        if (!Double.isNaN(date1)) {
                            //first propagation will find the start time when the point is in the field of view
                            SpacecraftState s = prop.propagate(getStartDate().shiftedBy(date0), getStartDate().shiftedBy(date1));

                            //prop.resetInitialState(s);
                            //second propagation will find the end time when the point is in the field of view
                            prop.propagate(s.getDate(), getStartDate().shiftedBy(date1));
                            date1 = Double.NaN;
                        }
                    }
                    TimeIntervalArray fovTimeArray = fovDetec.getTimeIntervalArray();
                    if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                        continue;
                    }
                    TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
                    satAccesses.put(pt, merger.orCombine());
                    prop.clearEventsDetectors();
                }
            }
            return this;
        }

        public Satellite getSat() {
            return sat;
        }

        public CoverageDefinition getCoverageDefinition() {
            return cdef;
        }

        public HashMap<CoveragePoint, TimeIntervalArray> getSatAccesses() {
            return satAccesses;
        }

    }

}
