/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import orekit.access.FOVHandler;
import orekit.access.TimeIntervalArray;
import orekit.access.TimeIntervalMerger;
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.Satellite;
import orekit.propagation.PropagatorFactory;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldOfViewDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * Stores information about the scenario or simulation including information of
 * start and stop times, time scales, epoch, etc.
 *
 * @author nozomihitomi
 */
public class ScenarioStepWise implements Callable<ScenarioStepWise>, Serializable {

    private static final long serialVersionUID = 8350171762084530278L;

    private final String scenarioName;
    private final TimeScale timeScale;
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;
    private final double simulationDuration;
    private final Frame inertialFrame;
    private final PropagatorFactory propagatorFactory;
    private final HashSet<Constellation> uniqueConstellations;
    private final HashSet<Satellite> uniqueSatellites;

    private final HashSet<CoverageDefinition> covDefs;

    /**
     * This object stores all the merged accesses for each point for each
     * coverage definition
     */
    private final HashMap<CoverageDefinition, HashMap<CoveragePoint, TimeIntervalArray>> finalAccesses;

    private boolean isDone;
    private Propagator repProp; //representative Propagator

    /**
     * Constructor if numerical integrator is needed (e.g. Atmospheric drag
     * model)
     *
     * @param name of scenario
     * @param startDate of scenario
     * @param endDate of scenario
     * @param timeScale of scenario
     * @param inertialFrame
     * @param propagatorBuilder
     */
    public ScenarioStepWise(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame,
            PropagatorFactory propagatorBuilder) {
        this.scenarioName = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.simulationDuration = endDate.offsetFrom(startDate, timeScale);
        this.timeScale = timeScale;
        this.inertialFrame = inertialFrame;
        this.propagatorFactory = propagatorBuilder;

        this.covDefs = new HashSet<>();
        this.uniqueConstellations = new HashSet<>();
        this.uniqueSatellites = new HashSet<>();

        this.finalAccesses = new HashMap();

        this.isDone = false;
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
    public ScenarioStepWise call() throws OrekitException {
        System.out.println(String.format("Running scenario: %s...", this));
        if (!isDone) {
            //Create propagators for each satellite (only create one per satellite)
            HashMap<Satellite, Propagator> propagators = new HashMap<>(uniqueSatellites.size());
            for (Satellite sat : uniqueSatellites) {
                Orbit orbit = sat.getOrbit();
                if (!orbit.getType().equals(propagatorFactory.getOrbitType())) {
                    throw new IllegalArgumentException(String.format("Orbit type of "
                            + "satelite %s does not match propagator. "
                            + "Expected %s, found %s", sat.toString(),
                            orbit.getType(), propagatorFactory.getOrbitType()));
                }

                Propagator prop = propagatorFactory.createPropagator(orbit, sat.getGrossMass());
                
                prop.setSlaveMode();
                
                
                //add an attitude provider (e.g. nadir pointing)
                if (sat.getAttProv() != null) {
                    prop.setAttitudeProvider(sat.getAttProv());
                }

                propagators.put(sat, prop);

                if (repProp == null) {
                    repProp = prop;
                }
            }

            System.out.println("Setting up Event Detectors...");
            for (CoverageDefinition cdef : covDefs) {
                //propogate each satellite individually
                System.out.println("Propogating...");
                for (Satellite sat : propagators.keySet()) {
                    Propagator prop = propagators.get(sat);

                    ArrayList<EventDetector> detectors = createFOVDetectors(cdef, sat);
                    HashMap<EventDetector, Double> currentGValues = new HashMap<>();
                    for (EventDetector detector : detectors) {
                        currentGValues.put(detector, null);
                    }
                    for (AbsoluteDate extrapDate = startDate;
                            extrapDate.compareTo(endDate) <= 0;
                            extrapDate = extrapDate.shiftedBy(1)) {
                        SpacecraftState currentState = prop.propagate(extrapDate);
                        for (EventDetector detector : detectors) {
                            if (currentGValues.get(detector) == null) {
                                currentGValues.put(detector, detector.g(currentState));
                            } else {
                                double gVal = currentGValues.get(detector);
                                double newGVal = detector.g(currentState);
                                if (gVal >= 0 && newGVal < 0) {
                                    detector.eventOccurred(currentState, false);
                                } else if (gVal <= 0 && newGVal > 0) {
                                    detector.eventOccurred(currentState, true);
                                }
                                currentGValues.put(detector, newGVal);
                            }
                        }
                    }
                    HashMap<CoveragePoint, TimeIntervalArray> mergedAccesses = mergeCoverageDefinitionAccesses(finalAccesses.get(cdef), cdef.getAccesses(), false);
                    finalAccesses.put(cdef, mergedAccesses);
                    cdef.clearAccesses();
                }

            }

            isDone = true;
            System.out.println(String.format("Finished simulating %s...", this));
        }
        return this;
    }

    /**
     * Merges the accesses in two sets of accesses.
     *
     * @param accesses1 a set of accesses that is to be merged. Must have the
     * same coverage points as accesses2
     * @param accesses2 a set of accesses that is to be merged. Must have the
     * same coverage points as accesses1
     * @param andCombine true if accesses should be combined with logical AND
     * (i.e. intersection). False if accesses should be combined with a logical
     * OR (i.e. union)
     */
    private HashMap<CoveragePoint, TimeIntervalArray> mergeCoverageDefinitionAccesses(HashMap<CoveragePoint, TimeIntervalArray> accesses1, HashMap<CoveragePoint, TimeIntervalArray> accesses2, boolean andCombine) {
        HashMap<CoveragePoint, TimeIntervalArray> out = new HashMap<>(accesses1.size());
        if (accesses1.keySet().equals(accesses2.keySet())) {
            for (CoveragePoint pt : accesses1.keySet()) {
                ArrayList<TimeIntervalArray> accessArrays = new ArrayList<>();
                accessArrays.add(accesses1.get(pt));
                accessArrays.add(accesses2.get(pt));
                TimeIntervalMerger merger = new TimeIntervalMerger(accessArrays);
                TimeIntervalArray mergedArray;
                if (andCombine) {
                    mergedArray = merger.andCombine();
                } else {
                    mergedArray = merger.orCombine();
                }
                out.put(pt, mergedArray);
            }
        } else {
            //The coverage definitions must contain the same grid points
            throw new IllegalArgumentException("Failed to merge access for two sets of grid points. Expected grid points between sets to be equal. Found sets containing different points.");
        }
        return out;
    }

    /**
     * Returns the merged accesses of a given coverage definition after the
     * scenario is finished running
     *
     * @param covDef
     * @return
     */
    public HashMap<CoveragePoint, TimeIntervalArray> getMergedAccesses(CoverageDefinition covDef) {
        return finalAccesses.get(covDef);
    }

    /**
     * Adds a coverage definition to the scenario
     *
     * @param covDef
     * @return true if scenario did not already contain the specified coverage
     * definition
     */
    public boolean addCoverageDefinition(CoverageDefinition covDef) {
        boolean newConstel = covDefs.add(covDef);
        if (newConstel) {
            for (Constellation constel : covDef.getConstellations()) {
                uniqueConstellations.add(constel);
                for (Satellite satellite : constel.getSatellites()) {
                    uniqueSatellites.add(satellite);
                }
            }
        }

        //create a new time interval array for each point in the coverage definition
        HashMap<CoveragePoint, TimeIntervalArray> ptAccesses = new HashMap<>();
        for (CoveragePoint pt : covDef.getPoints()) {
            ptAccesses.put(pt, new TimeIntervalArray(startDate, endDate));
        }
        finalAccesses.put(covDef, ptAccesses);
        return newConstel;
    }

    /**
     * Creates the field of view detectors for all points within the given
     * coverage definition and the satellites assigned to the coverage
     * definition
     *
     * @param covDef coverage definition to create field of view detectors for
     * @param propMap mapping between satellite and propagator
     */
    private ArrayList<EventDetector> createFOVDetectors(CoverageDefinition covDef, HashMap<Satellite, Propagator> propMap) {
        ArrayList<EventDetector> detectors = new ArrayList();
        for (Constellation constel : covDef.getConstellations()) {
            System.out.println(String.format("Creating FOV detectors for %s targeting %s...", constel, covDef));
            for (Satellite sat : constel.getSatellites()) {
                for (TopocentricFrame point : covDef.getPoints()) {
                    for (Instrument inst : sat.getPayload()) {
//                        FastFOVDetector eventDec = new FastFOVDetector(point, inst.getFov(), FastMath.toRadians(60)).withMaxCheck(1).withHandler(new FOVHandler());
////                        propMap.get(sat).addEventDetector(eventDec);
//                        detectors.add(eventDec);
                    }
                }
            }
        }
        return detectors;
    }

    /**
     * Creates the field of view detectors for all points within the given
     * coverage definition and the satellites assigned to the coverage
     * definition
     *
     * @param covDef coverage definition to create field of view detectors for
     * @param propMap mapping between satellite and propagator
     */
    private ArrayList<EventDetector> createFOVDetectors(CoverageDefinition covDef, Satellite sat) {
        ArrayList<EventDetector> detectors = new ArrayList();
        for (Constellation constel : covDef.getConstellations()) {
            System.out.println(String.format("Creating FOV detectors for %s targeting %s...", constel, covDef));

            for (TopocentricFrame point : covDef.getPoints()) {
                for (Instrument inst : sat.getPayload()) {
//                    FastFOVDetector eventDec = new FastFOVDetector(point, inst.getFov(), FastMath.toRadians(60)).withMaxCheck(1).withHandler(new FOVHandler());
////                        propMap.get(sat).addEventDetector(eventDec);
//                    detectors.add(eventDec);
                }
            }

        }
        return detectors;
    }

    /**
     * Pretty print of the dates and the time scale
     *
     * @return pretty print string of dates and time scale
     */
    public String ppDate() {
        String out = "Dates{\n";
        out += "\tStart date: " + startDate.toString(timeScale) + "\n";
        out += "\tEnd date: " + endDate.toString(timeScale) + "\n";
        out += "\tTime scale: " + timeScale.getName() + "\n";
        out += "}\n";
        return out;
    }

    /**
     * Pretty print of the inertial frame
     *
     * @return pretty print string of the inertial Frame
     */
    public String ppFrame() {
        String out = "Frame{\n";
        out += "\tInertial Frame: " + inertialFrame.getName() + "\n";
        out += "}\n";
        return out;
    }

    /**
     * Pretty print of the propagator
     *
     * @return pretty print string of the propagator
     */
    public String ppPropgator() {
        String out = "Propagator{\n";
        out += "\tPropagator: " + repProp.toString() + "\n";
        out += "\tMode: ";
        switch (repProp.getMode()) {
            case 0:
                out += Propagator.SLAVE_MODE;
                break;
            case 1:
                out += Propagator.MASTER_MODE;
                break;
            case 2:
                out += Propagator.EPHEMERIS_GENERATION_MODE;
                break;
        }
        out += "\tForceModels{\n";
//        out += String.format("\tForceModel{\n", args)
        out += "}\n";
        out += "\n}\n";
        return out;
    }

    /**
     * Pretty print of all the objects included in this scenario
     *
     * @return pretty print string of all the objects included in this scenario
     */
    public String ppConstellations() {
        String out = "Constellations{\n";
        for (Constellation constel : uniqueConstellations) {
            out += "\tName: " + constel.getName() + "{\n";
            for (Satellite sat : constel.getSatellites()) {
                out += "\t\tSatellite{\n";
                out += "\t\t\tOrbit: {" + sat.ppOrbit() + "}\n";
                out += "\t\t\tPayload {\n";
                for (Instrument inst : sat.getPayload()) {
                    out += "\t\t\t\tInstrument name: " + inst.getName() + "{\n";
                    out += "\t\t\t\t\tFOV: " + inst.getName() + "\n";
                    out += "\t\t\t\t}\n";
                }
                out += "\t\t\t}\n";
                out += "\t\t}\n";
            }
            out += "\t}\n\n";
        }
        out += "}\n";
        return out;
    }

    /**
     * Pretty print of all the coverage definitions included in this scenario
     *
     * @return pretty print string of all the coverage definitions included in
     * this scenario
     */
    public String ppCoverageDefinition() {
        String out = "CoverageDefinition{\n";
        for (CoverageDefinition covdef : this.covDefs) {
            out += "\tName: " + covdef.getName() + " {\n";
            out += "\t\tViewers: {\n";
            for (Constellation constel : covdef.getConstellations()) {
                out += "\t\t\t" + constel + "\n";
            }
            out += "\t\t}\n";
            out += "\t\tNumber of Points: " + covdef.getNumberOfPoints() + "\n";
            out += "\t\tLatitude\tLongitude\tAltitude:\n";
            for (TopocentricFrame topoPt : covdef.getPoints()) {
                GeodeticPoint pt = topoPt.getPoint();
                out += String.format("\t\t%1$03.6f", FastMath.toDegrees(pt.getLatitude()));
                out += String.format("\t%1$03.6f", FastMath.toDegrees(pt.getLongitude()));
                out += String.format("\t%1$03.6f\n", FastMath.toDegrees(pt.getAltitude()));
            }
            out += "\t}\n";
        }
        out += "}\n";
        return out;
    }

    @Override
    public String toString() {
        return "Scenario{" + "scenarioName=" + scenarioName + ", startDate=" + startDate + ", endDate= " + endDate + '}';
    }
}
