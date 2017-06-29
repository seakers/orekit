/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
import seak.orekit.coverage.access.TimeIntervalArray;
import seak.orekit.coverage.access.TimeIntervalMerger;
import seak.orekit.event.detector.GroundStationDetector;
import seak.orekit.object.GndStation;
import seak.orekit.object.Satellite;
import seak.orekit.propagation.PropagatorFactory;

/**
 * This event analysis is used to compute when the given ground stations are in
 * accessible by the given satellites. The satellites are assigned one or more
 * ground stations to communicate with. A satellite and ground station can
 * communicate if they share the same communication bands and the satellites is
 * above the minimum elevation angle of the ground station.
 *
 * @author nhitomi
 */
public class GndStationEventAnalysis extends AbstractEventAnalysis {

    /**
     * Propagator factory that will create the necessary propagator for each
     * satellite
     */
    private final PropagatorFactory propagatorFactory;

    /**
     * the assignment of satellites to ground stations
     */
    private final Map<Satellite, Set<GndStation>> stationAssignment;

    /**
     * Stores all the accesses of each satellite if saveAllAccesses is true.
     */
    private HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> allAccesses;

    /**
     * the number of threads to use in parallel processing
     */
    private final int numThreads;

    /**
     * Creates a new event analysis.
     *
     * @param startDate of scenario
     * @param endDate of scenario
     * @param inertialFrame the inertial frame used in the simulation
     * @param propagatorFactory the factory to create propagtors for each
     * satellite
     * @param stationAssignment the assignment of satellites to ground stations.
     * A satellite can be assigned to one or more ground stations and ground
     * stations can be assigned to more than one satellite.
     * @param numThreads number of threads to uses in parallelization of the
     * scenario by dividing up the propagation across multiple threads
     */
    public GndStationEventAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame, Map<Satellite, Set<GndStation>> stationAssignment,
            PropagatorFactory propagatorFactory, int numThreads) {
        super(startDate, endDate, inertialFrame);
        this.propagatorFactory = propagatorFactory;
        this.allAccesses = new HashMap();

        this.numThreads = numThreads;
        this.stationAssignment = stationAssignment;
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
    public GndStationEventAnalysis call() throws OrekitException {
        //set up resource pool
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CompletionService<GndStationSubRoutine> ecs = new ExecutorCompletionService(pool);

        int nSubRoutines = 0;
        for (Satellite sat : stationAssignment.keySet()) {
            Logger.getGlobal().finer(String.format("Acquiring ground station accesses for %s...", sat));
            Logger.getGlobal().finer(
                    String.format("Simulation dates %s to %s (%.2f days)",
                            getStartDate(), getEndDate(),
                            getEndDate().durationFrom(getStartDate()) / 86400.));
            allAccesses.put(sat, new HashMap());

            //propogate each satellite individually
            //if no precomuted times available, then propagate
            Propagator prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());
            //Set stepsizes and threshold for detectors
            double losStepSize = sat.getOrbit().getKeplerianPeriod() / 10.;
            double threshold = 1e-3;

            GndStationSubRoutine subRoutine
                    = new GndStationSubRoutine(sat, prop, stationAssignment.get(sat), losStepSize, threshold);
            ecs.submit(subRoutine);
            nSubRoutines++;
        }

        for (int i = 0; i < nSubRoutines; i++) {
            GndStationSubRoutine subRoutine = null;
            try {
                subRoutine = ecs.take().get();
            } catch (InterruptedException ex) {
                Logger.getLogger(GndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(GndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (subRoutine == null) {
                pool.shutdown();
                throw new IllegalStateException("Subroutine failed in field of view event.");
            }

            Satellite sat = subRoutine.getSat();
            HashMap<GndStation, TimeIntervalArray> satAccesses = subRoutine.getSatAccesses();
            processAccesses(sat, satAccesses);
        }

        //Make all time intervals stored in accesses immutable
        for (Satellite sat : stationAssignment.keySet()) {
            for (GndStation station : stationAssignment.get(sat)) {
                TimeIntervalArray immutableArray
                        = allAccesses.get(sat).get(station).createImmutable();
                allAccesses.get(sat).put(station, immutableArray);
            }
        }

        pool.shutdown();
        return this;
    }

    /**
     * Saves the computed accesses from the satellite assigned to the ground
     * station
     *
     * @param sat the satellite
     * @param station the ground station assigned to the satellite
     * @param satAccesses the accesses computed for the satellite to its
     * assigned coverage definition
     */
    private void processAccesses(Satellite sat,
            HashMap<GndStation, TimeIntervalArray> satAccesses) {
        //save the satellite accesses 
        allAccesses.put(sat, satAccesses);
    }

    /**
     * Returns the individual accesses of a given satellite on a given ground
     * station after the scenario is finished running.
     *
     * @param sat a satellite that is assigned to the ground station
     * @return a map of ground stations and time interval array
     */
    public HashMap<GndStation, TimeIntervalArray> getSatelliteAccesses(Satellite sat) {
        return allAccesses.get(sat);
    }

    /**
     * Returns the computed accesses for each ground station by each of the
     * satellites assigned to that coverage definition
     *
     * @return
     */
    public HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> getAllAccesses() {
        return allAccesses;
    }

    @Override
    public String getHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ground Station Access Event Analysis\n");
        sb.append("Assigned Constellations:\n");
        for (Satellite satellite : stationAssignment.keySet()) {
            sb.append("satellite = ");
            sb.append(satellite.getName()).append("{");
            for (GndStation station : stationAssignment.get(satellite)) {
                sb.append(String.format("Ground Station %s\n", station));
            }
            sb.append("}\n");
        }
        sb.append("\n");
        return sb.toString();

    }

    /**
     * Creates a subroutine to run the field of view event analysis in parallel
     */
    private class GndStationSubRoutine implements Callable<GndStationSubRoutine> {

        /**
         * The satellite to propagate
         */
        private final Satellite sat;

        /**
         * The propagator
         */
        private final Propagator prop;

        /**
         * the set of ground stations assigned to the satellite
         */
        private final Set<GndStation> stations;

        /**
         * The step size during propagation when computing the line of sight
         * events. Generally, this can be a large step. It is used to speed up
         * the simulation.
         */
        private final double stepSize;

        /**
         * The threshold, in seconds, when conducting root finding to determine
         * when an event occurred.
         */
        private final double threshold;

        /**
         * The times, for each ground station, when it is being accessed by the
         * given satellite and its payload.
         */
        private final HashMap<GndStation, TimeIntervalArray> accesses;

        /**
         *
         * @param sat The satellite to propagate
         * @param prop The propagator
         * @param station The coverage definition to access
         * @param stepSize The step size during propagation
         * @param threshold The threshold, in seconds, when conducting root
         * finding to determine when an event occurred.
         */
        public GndStationSubRoutine(Satellite sat, Propagator prop,
                Set<GndStation> stations, double stepSize, double threshold) {
            this.sat = sat;
            this.prop = prop;
            this.stations = stations;
            this.stepSize = stepSize;
            this.threshold = threshold;

            this.accesses = new HashMap<>(stations.size());
            for (GndStation station : stations) {
                accesses.put(station, getEmptyTimeArray());
            }
        }

        //NOTE: this implementation of in the field of view is a bit fragile if propagating highly elliptical orbits (>0.75). Maybe need to use smaller time steps los and fov detectors
        @Override
        public GndStationSubRoutine call() throws Exception {
            SpacecraftState initialState = prop.getInitialState();
            Logger.getGlobal().finer(String.format("Propagating satellite %s...", sat));
            HashMap<GndStation, GroundStationDetector> map = new HashMap<>();
            for (GndStation station : stations) {
                GroundStationDetector gndstatDetec
                        = new GroundStationDetector(initialState,
                                getStartDate(), getEndDate(),
                                sat.getTransmitter(), sat.getReceiver(), station,
                                EventHandler.Action.CONTINUE, stepSize, threshold);

                prop.addEventDetector(gndstatDetec);
                map.put(station, gndstatDetec);

            }
            prop.propagate(getStartDate(), getEndDate());
            for (GndStation station : map.keySet()) {
                TimeIntervalArray fovTimeArray = map.get(station).getTimeIntervalArray();
                if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                    continue;
                }
                TimeIntervalMerger merger = new TimeIntervalMerger(accesses.get(station), fovTimeArray);
                accesses.put(station, merger.orCombine());
            }
            prop.clearEventsDetectors();
            return this;
        }

        public Satellite getSat() {
            return sat;
        }

        public HashMap<GndStation, TimeIntervalArray> getSatAccesses() {
            return accesses;
        }

    }

}
