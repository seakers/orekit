/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;
import seakers.orekit.event.detector.FOVDetector;
import seakers.orekit.event.detector.GroundStationDetector;
import seakers.orekit.event.detector.SatLOSDetector;
import seakers.orekit.event.detector.TimeIntervalHandler;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.parallel.ParallelRoutine;
import seakers.orekit.parallel.SubRoutine;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.util.RawSafety;

/**
 * This event analysis is used to compute when the satellites are in line of
 * sight of each other and create cross-link opportunities.
 *
 * @author alan aguilar
 */
public class CrossLinkEventAnalysis extends AbstractEventAnalysis {

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
    private HashMap<Constellation, HashMap<Satellite, HashMap<Satellite, TimeIntervalArray>>> allAccesses;


    private final ArrayList<Constellation> constellations;

    /**
     * Creates a new event analysis.
     *
     * @param startDate of analysis
     * @param endDate of analysis
     * @param inertialFrame the inertial frame used in the simulation
     * @param propagatorFactory the factory to create propagtors for each
     * satellite
     * @param constellations
     * @param saveAllAccesses true if user wants to maintain all the accesses
     * from each individual satellite. false if user would like to only get the
     * merged accesses between all satellites (this saves memory).
     * @param saveToDB flag to dictate whether the coverage accesses of
     * individual satellites should be saved to the coverage database
     */
    public CrossLinkEventAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, Frame inertialFrame,
                                  ArrayList<Constellation> constellations, PropagatorFactory propagatorFactory,
                                  boolean saveAllAccesses, boolean saveToDB) {
        super(startDate, endDate, inertialFrame);
        this.propagatorFactory = propagatorFactory;
        this.saveAllAccesses = saveAllAccesses;
        this.constellations = constellations;

        if (saveAllAccesses) {
            this.allAccesses = new HashMap<>();
            for (Constellation cons : constellations) {
                allAccesses.put(cons, new HashMap<>());
            }
        }

        this.saveToDB = saveToDB;
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
    public CrossLinkEventAnalysis call() throws OrekitException {
        for (Constellation cons : constellations) {
            Logger.getGlobal().finer("Acquiring inter-satellite accesses...");
            Logger.getGlobal().finer(String.format("Acquiring access times for %s...", cons));
            Logger.getGlobal().finer(
                    String.format("Simulation dates %s to %s (%.2f days)",
                            getStartDate(), getEndDate(),
                            getEndDate().durationFrom(getStartDate()) / 86400.));

            //propagate each satellite individually
            ArrayList<SubRoutine> subRoutines = new ArrayList<>();
            for (Satellite sat : cons.getSatellites()) {

                //if no precomputed times available, then propagate
                Propagator prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());

                //Set step-sizes and threshold for detectors
                double fovStepSize = sat.getOrbit().getKeplerianPeriod() / 100.;
                double losStepSize = sat.getOrbit().getKeplerianPeriod() / 100.;
                double threshold = 1e-3;

                CrossLinkSubroutine subRoutine = new CrossLinkSubroutine(sat, propagatorFactory,
                        cons,fovStepSize, losStepSize, threshold);

                subRoutines.add(subRoutine);
            }

            try {
                for (SubRoutine sr : ParallelRoutine.submit(subRoutines)) {
                    if (sr == null) {
                        throw new IllegalStateException("Subroutine failed in event analysis.");
                    }
                    CrossLinkSubroutine subr = (CrossLinkSubroutine)sr;
                    Satellite sat = subr.getSat();
                    HashMap<Satellite, TimeIntervalArray> satAccesses = subr.getSatAccesses();
                    processAccesses(sat, cons, satAccesses);

                    if (saveToDB) {
                        File file = new File(
                                System.getProperty("orekit.coveragedatabase"),
                                String.valueOf(sat.hashCode()));
                        writeAccesses(file, satAccesses);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }

            //Make all time intervals stored in finalAccesses immutable
            for (Satellite target : getEvents().keySet()) {
                getEvents().put(target, getEvents().get(target).createImmutable());
            }

        }

        return this;
    }

    /**
     * Saves the computed accesses from the satellite assigned to the coverage
     * definition.
     *
     * @param sat the satellite
     * @param cons constellation to which sat belongs to
     * @param satAccesses the accesses computed for the satellite to its
     * assigned satellites
     */
    protected void processAccesses(Satellite sat, Constellation cons,
                                   HashMap<Satellite, TimeIntervalArray> satAccesses) {
        //save the satellite accesses
        if (saveAllAccesses) {
            allAccesses.get(cons).put(sat, satAccesses);
        }
    }

    private void writeAccesses(File file, HashMap<Satellite, TimeIntervalArray> accesses) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(accesses);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private HashMap<Satellite, TimeIntervalArray> readAccesses(File file) {
        HashMap<Satellite, TimeIntervalArray> out = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            out = RawSafety.castHashMap(ois.readObject());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return out;
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
    public HashMap<Satellite, TimeIntervalArray> getSatelliteAccesses(CoverageDefinition covDef, Satellite sat) {
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
    public HashMap<Constellation, HashMap<Satellite, HashMap<Satellite, TimeIntervalArray>>> getAllAccesses() {
        return allAccesses;
    }


    /**
     * Returns the computed accesses for each satellite by any of their
     * assigned satellites.
     *
     * @return The access times to each ground station
     */
    public Map<Satellite, TimeIntervalArray> getEvents() {
        Map<Satellite, TimeIntervalArray> out = new HashMap<>();

        Map<Satellite, Collection<TimeIntervalArray>> timeArrays = new HashMap<>();
        for(Constellation cons : allAccesses.keySet()){
            for (Satellite sat : cons.getSatellites()) {
                for (Satellite target : allAccesses.get(cons).get(sat).keySet()) {
                    if(sat == target) continue;

                    //check if the ground station was already added to results
                    if (!timeArrays.containsKey(target)) {
                        timeArrays.put(target, new ArrayList<>());
                    }
                    timeArrays.get(target).add(allAccesses.get(cons).get(sat).get(target));

                }
            }
        }
        for (Satellite target : timeArrays.keySet()) {
            TimeIntervalMerger merger = new TimeIntervalMerger(timeArrays.get(target));
            out.put(target, merger.orCombine());
        }
        return out;
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

    @Override
    public String getHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cross Link Event Analysis\n");
        sb.append("Assigned Constellations:\n");
        for (Constellation constel : constellations) {
            sb.append(String.format("\tConstelation %s: %d satellites\n", constel.getName(), constel.getSatellites().size()));
            for (Satellite sat : constel.getSatellites()) {
                sb.append(String.format("\t\tSatellite %s\n", sat.toString()));
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Creates a subroutine to run the field of view event analysis in parallel
     */
    private class CrossLinkSubroutine implements SubRoutine {

        /**
         * The satellite to propagate
         */
        private final Satellite sat;

        /**
         * The propagator
         */
//        private final Propagator prop;
        private final PropagatorFactory pf;

        /**
         * The coverage definition to access
         */
        private final Constellation constellation;

        /**
         * The step size during propagation when computing the field of view
         * events. Generally, this should be a small step for accurate results.
         */
        private final double fovStepSize;

        /**
         * The step size during propagation when computing the line of sight
         * events. Generally, this can be a large step. It is used to speed up
         * the simulation.
         */
        private final double losStepSize;

        /**
         * The threshold, in seconds, when conducting root finding to determine
         * when an event occurred.
         */
        private final double threshold;

        /**
         * The times, for each point, when it is being accessed by the given
         * satellite and its payload.
         */
        private final HashMap<Satellite, TimeIntervalArray> satAccesses;


        /**
         *
         * @param sat The satellite to propagate
         * @param pf The propagator factory used for the constellation
         * @param constellation The constellation accessing itself
         * @param fovStepSize The step size during propagation when computing
         * the field of view events. Generally, this should be a small step for
         * accurate results.
         * @param threshold The threshold, in seconds, when conducting root
         * finding to determine when an event occurred.
         */
        public CrossLinkSubroutine(Satellite sat, PropagatorFactory pf, Constellation constellation,
                                   double fovStepSize, double losStepSize, double threshold) {
            this.sat = sat;
            this.pf = pf;
            this.constellation = constellation;
            this.fovStepSize = fovStepSize;
            this.losStepSize = losStepSize;
            this.threshold = threshold;

            this.satAccesses = new HashMap<>(constellation.getSatellites().size() - 1);
            for (Satellite target : constellation.getSatellites()) {
                if(target == sat) continue;
                satAccesses.put(target, getEmptyTimeArray());
            }

        }

        //NOTE: this implementation of in the field of view is a bit fragile if propagating highly elliptical orbits (>0.75). Maybe need to use smaller time steps los and fov detectors
        @Override
        public CrossLinkSubroutine call() throws Exception {
            Logger.getGlobal().finer(String.format("Propagating satellite %s...", sat));
            //if (prop instanceof NumericalPropagator) {
            singlePropagate();
            //} else {
            //    multiPropogate();
            //}
            return this;
        }

//        /**
//         * If using an analytical propagator, taking short cuts and propagating
//         * the satellite many many times is faster than packing all event
//         * detectors into one propagator.
//         *
//         * @throws OrekitException
//         */
//        private void multiPropogate() throws OrekitException {
//            SpacecraftState initialState = prop.getInitialState();
//            for (Instrument inst : sat.getPayload()) {
//                for (CoveragePoint pt : cdef.getPoints()) {
//                    if (!lineOfSightPotential(pt, initialState.getOrbit(), FastMath.toRadians(2.0))) {
//                        //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
//                        continue;
//                    }
//
//                    prop.resetInitialState(initialState);
//                    prop.clearEventsDetectors();
//
//                    //need to reset initial state of the propagators or will progate from the last stop time
//                    prop.resetInitialState(initialState);
//                    prop.clearEventsDetectors();
//                    //Next search through intervals with line of sight to compute when point is in field of view
//                    FOVDetector fovDetec = new FOVDetector(initialState, getStartDate(), getEndDate(),
//                            pt, inst, fovStepSize, threshold, Action.CONTINUE);
//                    prop.addEventDetector(fovDetec);
//                    prop.propagate(getStartDate(), getEndDate());
//
//                    TimeIntervalArray fovTimeArray = fovDetec.getTimeIntervalArray();
//                    if (fovTimeArray == null || fovTimeArray.isEmpty()) {
//                        continue;
//                    }
//                    TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
//                    satAccesses.put(pt, merger.orCombine());
//                    prop.clearEventsDetectors();
//                }
//            }
//        }

        /**
         * If using a numerical propagator, don't try to take short cuts.
         * Packing all event detectors in the propagator is faster than
         * propagating the satellite many many times.
         *
         * @throws OrekitException
         */
        private void singlePropagate() throws OrekitException {
            Propagator prop = pf.createPropagator(sat.getOrbit(), sat.getGrossMass());
            SpacecraftState initialState = prop.getInitialState();

            HashMap<Satellite, TimeIntervalHandler<SatLOSDetector>> map = new HashMap<>();

            for (Satellite target : constellation.getSatellites()) {
                // skip if trying to compute access to oneself
                if(sat == target) continue;

                Propagator pfTarget = pf.createPropagator(target.getOrbit(), target.getGrossMass());
                SatLOSDetector losDetec = new SatLOSDetector(target,pfTarget,getInertialFrame()).withMaxCheck(fovStepSize).withThreshold(threshold);

                TimeIntervalHandler<SatLOSDetector> losHandler = new TimeIntervalHandler<>(getStartDate(), getEndDate(),
                        losDetec.g(initialState), Action.CONTINUE);
                losDetec = losDetec.withHandler(losHandler);
                prop.addEventDetector(losDetec);
                map.put(target, losHandler);
            }
            prop.propagate(getStartDate(), getEndDate());

            for (Satellite target : map.keySet()) {
                TimeIntervalArray losTimeArray = map.get(target).getTimeArray().createImmutable();
                if (losTimeArray == null || losTimeArray.isEmpty()) {
                    continue;
                }
                TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(target), losTimeArray);
                satAccesses.put(target, merger.orCombine());
            }
            prop.clearEventsDetectors();
        }

        public Satellite getSat() {
            return sat;
        }

        public HashMap<Satellite, TimeIntervalArray> getSatAccesses() {
            return satAccesses;
        }
    }
}
