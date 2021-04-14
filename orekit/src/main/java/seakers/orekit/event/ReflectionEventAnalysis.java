/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;
import seakers.orekit.event.detector.ReflectorDetector;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Satellite;
import seakers.orekit.parallel.ParallelRoutine;
import seakers.orekit.parallel.SubRoutine;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.util.RawSafety;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An analysis for recording when the angle between a direction centered at a
 * geodetic point and a celestial body are above a specified angle threshold
 *
 * @author nhitomi
 */
public class ReflectionEventAnalysis extends AbstractGroundEventAnalysis {

    private final PropagatorFactory propagatorFactory;

    private final boolean saveAllAccesses;

    /**
     * flag to dictate whether the coverage accesses of individual satellites
     * should be saved to the coverage database
     */
    private boolean saveToDB = false;

    /**
     * Stores all the accesses of each satellite if saveAllAccesses is true.
     */
    private HashMap<CoverageDefinition, HashMap<Satellite, HashMap<TopocentricFrame, TimeIntervalArray>>> allAccesses;


    private final Constellation rxConstel;

    private final Constellation txConstel;

    public ReflectionEventAnalysis(AbsoluteDate startDate,
                                   AbsoluteDate endDate, Frame inertialFrame,
                                   Constellation rxConstel, Constellation txConstel, Set<CoverageDefinition> covDefs, PropagatorFactory propagatorFactory, boolean saveAllAccesses,
                                   boolean saveToDB) {
        super(startDate, endDate, inertialFrame, covDefs);
        this.rxConstel = rxConstel;
        this.txConstel = txConstel;
        this.propagatorFactory = propagatorFactory;
        this.saveAllAccesses = saveAllAccesses;
        if (saveAllAccesses) {
            this.allAccesses = new HashMap<>();
            for (CoverageDefinition cdef : covDefs) {
                allAccesses.put(cdef, new HashMap<>());
            }
        }

        this.saveToDB = saveToDB;
    }
    private class ReflectionSubRoutine implements SubRoutine {
        private final Satellite sat;
        private final Propagator prop;
        private final CoverageDefinition cdef;
        private final Constellation txConstellation;
        private final double fovStepSize;
        private final double threshold;
        private final HashMap<TopocentricFrame, TimeIntervalArray> satAccesses;

        public ReflectionSubRoutine(Satellite sat, Propagator prop, CoverageDefinition cdef, Constellation txConstellation, double fovStepSize, double threshold) {
            this.sat = sat;
            this.prop = prop;
            this.cdef = cdef;
            this.txConstellation = txConstellation;
            this.fovStepSize = fovStepSize;
            this.threshold = threshold;
            this.satAccesses = new HashMap<>(cdef.getNumberOfPoints());
            for (CoveragePoint pt : cdef.getPoints()) {
                satAccesses.put(pt, getEmptyTimeArray());
            }

        }

        @Override
        public ReflectionSubRoutine call() throws Exception {
            Logger.getGlobal().finer(String.format("Propagating satellite%s...", sat));
            singlePropagate();
            return this;
        }

        private void singlePropagate() throws OrekitException {
            SpacecraftState initialState = prop.getInitialState();
            HashMap<CoveragePoint, ReflectorDetector> map = new HashMap<>();
            int count = 0;
            for(Satellite tx : txConstellation.getSatellites()) {
                long start = System.nanoTime();
                Propagator pfTransmitter = propagatorFactory.createPropagator(tx.getOrbit(),tx.getGrossMass());
                count = count + 1;
//                System.out.println(count);
                for (CoveragePoint pt : cdef.getPoints()) {
                    if (!lineOfSightPotential(pt, initialState.getOrbit(), FastMath.toRadians(2.0))) {
                        //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
                        continue;
                    }
                    ReflectorDetector reflDetec = new ReflectorDetector(initialState,getStartDate(),getEndDate(),pt,pfTransmitter).withMaxCheck(fovStepSize).withThreshold(threshold);
                    prop.addEventDetector(reflDetec);
                    map.put(pt,reflDetec);
                }
                prop.propagate(getStartDate(),getEndDate());
                for(CoveragePoint pt : map.keySet()){
                    TimeIntervalArray reflTimeArray = map.get(pt).getTimeIntervalArray();
                    if (reflTimeArray == null || reflTimeArray.isEmpty()) {
                        continue;
                    }
                    TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), reflTimeArray);
                    satAccesses.put(pt,merger.orCombine());
                }
                prop.clearEventsDetectors();
                long end = System.nanoTime();
//                System.out.printf("Took %.4f sec\n", (end - start) / Math.pow(10, 9));
            }

        }

        public Satellite getSat() {
            return sat;
        }

        public HashMap<TopocentricFrame, TimeIntervalArray> getSatAccesses() {
            return satAccesses;
        }

        private boolean lineOfSightPotential(CoveragePoint pt, Orbit orbit, double latitudeMargin) throws OrekitException {
            //this computation assumes that the orbit frame is in ECE
            double distance2Pt = pt.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition().getNorm();
            double distance2Sat = orbit.getPVCoordinates().getPosition().getNorm();
            double subtendedAngle = FastMath.acos(distance2Pt / distance2Sat);

            return FastMath.abs(pt.getPoint().getLatitude()) - latitudeMargin < subtendedAngle + orbit.getI();
        }

    }
    @Override
    public ReflectionEventAnalysis call() throws Exception {
        ArrayList<SubRoutine> subRoutines = new ArrayList<>();
        for (CoverageDefinition cdef : getCoverageDefinitions()) {
            for (Satellite rx : rxConstel.getSatellites()) {
                double fovStepSize = rx.getOrbit().getKeplerianPeriod() / 10.;
                double threshold = 1e-2;
                ReflectionSubRoutine subRoutine = new ReflectionSubRoutine(rx,propagatorFactory.createPropagator(rx.getOrbit(),rx.getGrossMass()),cdef,txConstel,fovStepSize,threshold);
                subRoutines.add(subRoutine);
            }
            try {
                for (SubRoutine sr : ParallelRoutine.blockingSubmit(subRoutines)) {
                    if (sr == null) {
                        throw new IllegalStateException("Subroutine failed in field of view event.");
                    }
                    ReflectionSubRoutine fovsr = (ReflectionSubRoutine)sr;
                    Satellite sat = fovsr.getSat();
                    HashMap<TopocentricFrame, TimeIntervalArray> satAccesses = fovsr.getSatAccesses();
                    processAccesses(sat, cdef, satAccesses);

                    if (saveToDB) {
                        File file = new File(
                                System.getProperty("orekit.coveragedatabase"),
                                String.valueOf(sat.hashCode()));
                        writeAccesses(file, satAccesses);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }
            for (TopocentricFrame pt : getEvents().get(cdef).keySet()) {
                getEvents().get(cdef).put(pt, getEvents().get(cdef).get(pt).createImmutable());
            }
        }

        return this;
    }
    protected void processAccesses(Satellite sat, CoverageDefinition cdef,
                                   HashMap<TopocentricFrame, TimeIntervalArray> satAccesses) {
        //save the satellite accesses
        if (saveAllAccesses) {
            allAccesses.get(cdef).put(sat, satAccesses);
        }

        //merge the time accesses across all satellite for each coverage definition
        if (getEvents().containsKey(cdef)) {
            Map<TopocentricFrame, TimeIntervalArray> mergedAccesses
                    = EventIntervalMerger.merge(getEvents().get(cdef), satAccesses, false);
            getEvents().put(cdef, mergedAccesses);
        } else {
            getEvents().put(cdef, satAccesses);
        }
    }

    private void writeAccesses(File file, HashMap<TopocentricFrame, TimeIntervalArray> accesses) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(accesses);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private HashMap<TopocentricFrame, TimeIntervalArray> readAccesses(File file) {
        HashMap<TopocentricFrame, TimeIntervalArray> out = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            out = RawSafety.castHashMap(ois.readObject());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return out;
    }


    @Override
    public String getHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("Reflection Event Analysis\n");
        sb.append("\tCoverageDefinitions = {");
        for(CoverageDefinition cdef : getCoverageDefinitions()){
            sb.append("\t\t").append(cdef.toString()).append("\n");
        }
        sb.deleteCharAt(sb.length()-1); //removes last comma
        sb.append("\n\t}\n\n");
        
        return sb.toString();
    }

}
