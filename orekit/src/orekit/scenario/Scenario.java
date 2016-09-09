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
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.Satellite;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.events.FieldOfViewDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Stores information about the scenario or simulation including information of
 * start and stop times, time scales, epoch, etc.
 *
 * @author nozomihitomi
 */
public class Scenario implements Callable<Scenario>, Serializable {

    private static final long serialVersionUID = 8350171762084530278L;

    private final String scenarioName;
    private final TimeScale timeScale;
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;
    private final double simulationDuration;
    private final Frame inertialFrame;
    private final PropagatorBuilder propagatorBuilder;
    private HashSet<Constellation> uniqueConstellations;
    private HashSet<Satellite> uniqueSatellites;

    private HashSet<CoverageDefinition> covDefs;

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
    public Scenario(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame,
            PropagatorBuilder propagatorBuilder) {
        this.scenarioName = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.simulationDuration = endDate.offsetFrom(startDate, timeScale);
        this.timeScale = timeScale;
        this.inertialFrame = inertialFrame;
        this.propagatorBuilder = propagatorBuilder;

        this.covDefs = new HashSet<>();
        this.uniqueConstellations = new HashSet<>();
        this.uniqueSatellites = new HashSet<>();

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
    public Scenario call() throws OrekitException {
        System.out.println(String.format("Running scenario: %s...", this.scenarioName));
        if (!isDone) {
            //Create propagators for each satellite (only create one per satellite)
            HashMap<Satellite, Propagator> propagators = new HashMap<>(uniqueSatellites.size());
            for (Satellite sat : uniqueSatellites) {
                Orbit orbit = sat.getOrbit();
                if (!orbit.getType().equals(propagatorBuilder.getOrbitType())) {
                    throw new IllegalArgumentException(String.format("Orbit type of "
                            + "satelite %s does not match propagator. "
                            + "Expected %s, found %s", sat.toString(),
                            orbit.getType(), propagatorBuilder.getOrbitType()));
                }

                //error for driver is arbitrarily set to 0 because only need the number of drivers
                double[] orbParams = new double[orbit.getType().getDrivers(0, orbit, PositionAngle.TRUE).getNbParams()];
                orbit.getType().mapOrbitToArray(orbit, PositionAngle.TRUE, orbParams);

                Propagator prop = propagatorBuilder.buildPropagator(orbParams);
                prop.setSlaveMode();
                
                //add an attitude provider (e.g. nadir pointing)
                if(sat.getAttProv() != null)
                    prop.setAttitudeProvider(sat.getAttProv());
                propagators.put(sat, prop);
                
                if(repProp == null)
                    repProp = prop;
            }

            System.out.println("Setting up Event Detectors...");
            for (CoverageDefinition cdef : covDefs) {
                createFOVDetectors(cdef, propagators);
            }

            //propogate each satellite individually
            System.out.println("Propogating...");
            int cpt = 1;
            for (Satellite sat : propagators.keySet()) {
                Propagator prop = propagators.get(sat);
                for (AbsoluteDate extrapDate = startDate;
                        extrapDate.compareTo(endDate) <= 0;
                        extrapDate = extrapDate.shiftedBy(60)) {
                    SpacecraftState currentState = prop.propagate(extrapDate);
                }
            }

            isDone = true;
            System.out.println("Finished simulation...");
        }
        return this;
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
    private void createFOVDetectors(CoverageDefinition covDef, HashMap<Satellite, Propagator> propMap) {
        for (Constellation constel : covDef.getConstellations()) {
            for (Satellite sat : constel.getSatellites()) {
                for (TopocentricFrame point : covDef.getPoints()) {
                    for (Instrument inst : sat.getPayload()) {
                        FieldOfViewDetector eventDec = new FieldOfViewDetector(point, inst.getFov()).withMaxCheck(1).withHandler(new FOVHandler());
                        propMap.get(sat).addEventDetector(eventDec);
                    }
                }
            }
        }
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
                out += "\t\t\tPayload {";
                for (Instrument inst : sat.getPayload()) {
                    out += "\t\t\t\tInstrument name: " + inst.getName() + "{\n";
                    out += "\t\t\t\t\tFOV: " + inst.getName();
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
            out += "\t\tViewers: {";
            for (Constellation constel : covdef.getConstellations()) {
                out += "\t\t\t" + constel + "\n";
            }
            out += "\t\t}\n";
            out += "\t\tNumber of Points: " + covdef.getNumberOfPoints() + "\n";
            out += "\t\tLatitude\tLongitude\tAltitude:\n";
            for (TopocentricFrame topoPt : covdef.getPoints()) {
                GeodeticPoint pt = topoPt.getPoint();
                out += String.format("\t\t%1$03.5f", FastMath.toDegrees(pt.getLatitude()));
                out += String.format("\t%1$03.5f", FastMath.toDegrees(pt.getLongitude()));
                out += String.format("\t%1$03.5f\n", FastMath.toDegrees(pt.getAltitude()));
            }
            out += "\t}\n";
        }
        out += "}\n";
        return out;
    }

    @Override
    public String toString() {
        return "Scenario{" + "scenarioName=" + scenarioName + '}';
    }

    private static class FOVHandler implements EventHandler<FieldOfViewDetector> {

        public EventHandler.Action eventOccurred(final SpacecraftState s, final FieldOfViewDetector detector,
                final boolean increasing) {
            //the g() function for the FieldOfViewDetector asumes that the
            //target enters the FieldOfView when the value is negative and
            //exits when the value is positive. Therefore, when an event is 
            //detected and the value is decreasing, the target is entering the 
            //FieldOfView.
            
            CoveragePoint target = (CoveragePoint) detector.getPVTarget();
            if (increasing){
                //Access ends
                target.addSetTime(s.getDate());
                System.out.println("Access End: " + s.getDate());
            } else{
                //Access Begins
                target.addRiseTime(s.getDate());
                System.out.println("Access Starts: " + s.getDate());
            }

            return EventHandler.Action.CONTINUE;
        }

    }
}
