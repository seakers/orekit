/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import orekit.object.fieldofview.FOVDetector;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import orekit.coverage.access.CoverageAccessMerger;
import orekit.coverage.access.FOVHandler;
import orekit.coverage.access.TimeIntervalArray;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * Stores information about the scenario or simulation including information of
 * start and stop times, time scales, epoch, etc.
 *
 * @author nozomihitomi
 */
public class Scenario implements Callable<Scenario>, Serializable, Cloneable {

    private static final long serialVersionUID = 8350171762084530278L;

    /**
     * Name of the scenario
     */
    private final String scenarioName;

    /**
     * The time scale of the scenario
     */
    private final TimeScale timeScale;

    /**
     * Scenario start date
     */
    private final AbsoluteDate startDate;

    /**
     * Scenario end date
     */
    private final AbsoluteDate endDate;

    /**
     * Inertial frame used in scenario
     */
    private final Frame inertialFrame;

    /**
     * Propagator factory that will create the necessary propagator for each
     * satellite
     */
    private final PropagatorFactory propagatorFactory;

    /**
     * A set of the unique constellations
     */
    private final HashSet<Constellation> uniqueConstellations;

    /**
     * The mapping of the unique satellites assigned to each coverage definition
     */
    private final HashMap<CoverageDefinition, HashSet<Satellite>> uniqueSatsAssignedToCovDef;

    /**
     * A collection of the unique satellites in this scenario. Required to only
     * propagate each satellite once and once only.
     */
    private final HashSet<Satellite> uniqueSatellites;

    /**
     * The set of coverage definitions to simulate.
     */
    private final HashSet<CoverageDefinition> covDefs;

    /**
     * This object stores all the merged accesses for each point for each
     * coverage definition
     */
    private final HashMap<CoverageDefinition, HashMap<CoveragePoint, TimeIntervalArray>> finalAccesses;

    /**
     * flag to keep track of whether the simulation is done
     */
    private boolean isDone;

    /**
     * a flag set by the user to toggle whether to save the access of each
     * individual satellite or to release them from memory.
     */
    private final boolean saveAllAccesses;

    /**
     * Stores all the accesses of each satellite if saveAllAccesses is true.
     */
    private HashMap<CoverageDefinition, HashMap<Satellite, HashMap<CoveragePoint, TimeIntervalArray>>> allAccesses;

    /**
     * the number of threads to use in parallel processing
     */
    private final int numThreads;

    /**
     * Collection of future tasks to propagate
     */
    private final ArrayList<Future<PropagateTask>> futureTasks;

    /**
     * Object to merge the access from several satellite propagations
     */
    private final CoverageAccessMerger accessMerger;

    /**
     * Creates a new scenario.
     *
     * @param name of scenario
     * @param startDate of scenario
     * @param endDate of scenario
     * @param timeScale of scenario
     * @param inertialFrame
     * @param propagatorFactory
     * @param saveAllAccesses true if user wants to maintain all the accesses
     * from each individual satellite. false if user would like to only get the
     * merged accesses between all satellites (this saves memory).
     * @param numThreads number of threads to uses in parallelization of the
     * scenario by dividing up the coverage grid points across multiple threads
     */
    public Scenario(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame,
            PropagatorFactory propagatorFactory, boolean saveAllAccesses,
            int numThreads) {
        this.scenarioName = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.timeScale = timeScale;
        this.inertialFrame = inertialFrame;
        this.propagatorFactory = propagatorFactory;
        this.saveAllAccesses = saveAllAccesses;
        if (saveAllAccesses) {
            allAccesses = new HashMap();
        }

        this.covDefs = new HashSet<>();
        this.uniqueConstellations = new HashSet<>();
        this.uniqueSatellites = new HashSet<>();
        this.uniqueSatsAssignedToCovDef = new HashMap();

        this.finalAccesses = new HashMap();

        this.isDone = false;

        this.numThreads = numThreads;
        this.accessMerger = new CoverageAccessMerger();

        this.futureTasks = new ArrayList<>();
    }

    /**
     * Creates a new scenario. By default, the scenario does not parallelize the
     * simulation of the scenario
     *
     * @param name of scenario
     * @param startDate of scenario
     * @param endDate of scenario
     * @param timeScale of scenario
     * @param inertialFrame
     * @param propagatorFactory
     * @param saveAllAccesses true if user wants to maintain all the accesses
     * from each individual satellite. false if user would like to only get the
     * merged accesses between all satellites (this saves memory).
     */
    public Scenario(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame,
            PropagatorFactory propagatorFactory, boolean saveAllAccesses) {
        this(name, startDate, endDate, timeScale, inertialFrame, propagatorFactory, saveAllAccesses, 1);
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
        //set up resource pool
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        System.out.println(String.format("Running scenario: %s...", this));
        if (!isDone) {
            for (CoverageDefinition cdef : covDefs) {
                System.out.println(String.format("Acquiring access times for %s...", cdef));
                if (saveAllAccesses) {
                    allAccesses.put(cdef, new HashMap());
                }

                //propogate each satellite individually
                for (Satellite sat : uniqueSatsAssignedToCovDef.get(cdef)) {
                    HashMap<CoveragePoint, TimeIntervalArray> satAccesses = new HashMap<>(cdef.getNumberOfPoints());

                    //assign future tasks
                    PropagateTask task = new PropagateTask(sat, endDate, cdef.getPoints());
                    futureTasks.add(pool.submit(task));

                    //call future tasks and combine accesses from each point into one results object 
                    System.out.println(String.format("Propagating satellite %s to date %s", sat, endDate));
                    for (Future<PropagateTask> run : futureTasks) {
                        try {
                            PropagateTask finishedTask = run.get();
                            for (CoveragePoint pt : finishedTask.getResults().keySet()) {
                                satAccesses.put(pt, finishedTask.getResults().get(pt));
                            }
                        } catch (InterruptedException | ExecutionException ex) {
                            System.err.println(ex);
                            Logger.getLogger(Scenario.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    futureTasks.clear();

                    //save the satellite accesses 
                    if (saveAllAccesses) {
                        allAccesses.get(cdef).put(sat, satAccesses);
                    }

                    //merge the time accesses across all satellite for each coverage definition
                    if (finalAccesses.containsKey(cdef)) {
                        HashMap<CoveragePoint, TimeIntervalArray> mergedAccesses
                                = accessMerger.mergeCoverageDefinitionAccesses(finalAccesses.get(cdef), satAccesses, false);
                        finalAccesses.put(cdef, mergedAccesses);
                    } else {
                        finalAccesses.put(cdef, satAccesses);
                    }
                }
            }

            isDone = true;
            System.out.println(String.format("Finished simulating %s...", this));
        }

        pool.shutdown();
        return this;
    }

    /**
     * Clones everything in the Scenario except the coverage definitions and the
     * computed accesses
     *
     * @return Scenario cloned. The cloned instance is not flagged as run
     * @throws CloneNotSupportedException
     */
    @Override
    public Scenario clone() throws CloneNotSupportedException {
        super.clone();
        Scenario out = new Scenario(this.scenarioName, this.startDate,
                this.endDate, this.timeScale, this.inertialFrame,
                this.propagatorFactory, this.saveAllAccesses, this.numThreads);
        return out;
    }
    
    /**
     * Merges a collection of subscenarios previously run in parallel into their
     * parent Scenario
     *
     * @param subscenarios the collection of subscenarios that we want to merge
     * into the parents scenario
     * @throws java.lang.Exception
     */
    public void mergeSubscenarios(Collection<SubScenario> subscenarios) throws Exception{
        /*
        Check if all the subscenarios are run and all come from the same Parent Scenario. 
        Otherwise throw an Exception
        */
        
        for (SubScenario subscenario : subscenarios) {
            if (!subscenario.isDone()){
                throw new Exception("The subscenarios are not run yet");
            }
            if (!this.isSameScenario(subscenario)){
                throw new Exception("The subscenarios are from different Parent Scenarios");
            }
        }
        /*
        For every subscenario, we get its stored Accesses and merge them all in 
        the Parent Scenario Accesses Hashmap (psa)
        */
        HashMap<CoveragePoint,TimeIntervalArray> psa=new HashMap<>();
        for (SubScenario subscenario : subscenarios) {
            HashSet<CoverageDefinition> covs=subscenario.getCoverageDefinitions();
            Iterator iter = covs.iterator();
            CoverageDefinition c=(CoverageDefinition) iter.next();
            HashMap<CoveragePoint,TimeIntervalArray> accesses=c.getAccesses();
            psa.putAll(accesses);     
        }
        /*
        We create a new coverage definition and we add it to the Parent scenario
        */
        CoverageDefinition c=new CoverageDefinition(this.scenarioName,psa.keySet());
        this.addCoverageDefinition(c);
    }
    
        /**
     * Checks if the parameter Scenario is the same as the one calling 
     * the method
     * @param other Scenario to compare

     * @return True if both Scenarios are the same. Else false.
     */
    public boolean isSameScenario(Scenario other) {
        return this.scenarioName.equals(other.scenarioName);
    }
    
   
    /**
     * Returns the merged accesses of a given coverage definition after the
     * scenario is finished running
     *
     * @param covDef the coverage definition of interest
     * @return
     */
    public HashMap<CoveragePoint, TimeIntervalArray> getMergedAccesses(CoverageDefinition covDef) {
        return finalAccesses.get(covDef);
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
     * Gets the list of coverage definitions assigned to this scenario
     *
     * @return
     */
    public HashSet<CoverageDefinition> getCoverageDefinitions() {
        return covDefs;
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
            uniqueSatsAssignedToCovDef.put(covDef, new HashSet());
            for (Constellation constel : covDef.getConstellations()) {
                uniqueConstellations.add(constel);
                for (Satellite satellite : constel.getSatellites()) {
                    uniqueSatellites.add(satellite);
                    uniqueSatsAssignedToCovDef.get(covDef).add(satellite);
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
        out += "\tPropagator: " + propagatorFactory.getPropType() + "\n";
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

    public AbsoluteDate getStartDate() {
        return startDate;
    }

    public AbsoluteDate getEndDate() {
        return endDate;
    }

    /**
     * Returns the flag that marks the simulation as finished.
     * @return True if the simulation is done. Else false.
     */
    public boolean isDone() {
        return isDone;
    }

    /**
     * Returns the flag that marks whether each satellite's accesses should be saved.
     * @return 
     */
    public boolean isSaveAllAccesses() {
        return saveAllAccesses;
    }

    /**
     * Returns the computed accesses for each coverage definition by the combination of satellites assigned to that coverage definition
     * @return 
     */
    public HashMap<CoverageDefinition, HashMap<CoveragePoint, TimeIntervalArray>> getFinalAccesses() {
        return finalAccesses;
    }

    /**
     * Returns the computed accesses for each coverage definition by each of the satellites assigned to that coverage definition
     * @return 
     */
    public HashMap<CoverageDefinition, HashMap<Satellite, HashMap<CoveragePoint, TimeIntervalArray>>> getAllAccesses() {
        return allAccesses;
    }

    @Override
    public String toString() {
        return "Scenario{" + "scenarioName=" + scenarioName + ", startDate=" + startDate + ", endDate= " + endDate + '}';
    }

    /**
     * This class is the task to parallelize within the scenario. It propagates
     * a satellite in its own thread using a subset of the coverage grid to
     * compute accesses times
     */
    private class PropagateTask implements Callable<PropagateTask>, Serializable {

        private static final long serialVersionUID = 1078218608012519597L;

        /**
         * The satellite to propagate forward.
         */
        private final Satellite satellite;

        /**
         * The date to propagate forward to.
         */
        private final AbsoluteDate targetDate;

        /**
         * The points assigned to the satellite for coverage metrics
         */
        private final Collection<CoveragePoint> points;

        /**
         * The results will be stored here
         */
        private final HashMap<CoveragePoint, TimeIntervalArray> results;

        /**
         * The constructor to create a new subtask to propagate forward a
         * satellite.
         *
         * @param satellte the satellite to propagate forward
         * @param targetDate the target date to propagate the simulation forward
         * to
         * @param points the points the satellite is assigned to in this
         * propagation
         */
        public PropagateTask(Satellite satellte, AbsoluteDate targetDate, Collection<CoveragePoint> points) {
            this.satellite = satellte;
            this.targetDate = targetDate;
            this.points = points;
            this.results = new HashMap<>(points.size());
        }

        private Propagator createPropagator() throws OrekitException {

            //create the propagator
            Orbit orbit = satellite.getOrbit();
            if (!orbit.getType().equals(propagatorFactory.getOrbitType())) {
                throw new IllegalArgumentException(String.format("Orbit type of "
                        + "satelite %s does not match propagator. "
                        + "Expected %s, found %s", satellite.toString(),
                        orbit.getType(), propagatorFactory.getOrbitType()));
            }

            Propagator prop = propagatorFactory.createPropagator(orbit, satellite.getGrossMass());
            prop.setSlaveMode();

            //add an attitude provider (e.g. nadir pointing)
            if (satellite.getAttProv() != null) {
                prop.setAttitudeProvider(satellite.getAttProv());
            }

            return prop;
        }

        /**
         * Runs the propagation and returns the access times for all the
         * instruments aboard the satellite to those points assigned to the
         * satellite
         *
         * @return
         * @throws Exception
         * @throws OrekitException
         */
        @Override
        public PropagateTask call() throws Exception, OrekitException {
            Propagator propagator = createPropagator();
            for (Instrument inst : satellite.getPayload()) {
                for (CoveragePoint pt : points) {
                    FOVDetector eventDec = new FOVDetector(pt, inst).withMaxCheck(1).withHandler(new FOVHandler());
                    propagator.addEventDetector(eventDec);
                }
            }
            propagator.propagate(targetDate);

            for (CoveragePoint pt : points) {
                results.put(pt, pt.getAccesses());
                pt.reset();
            }
            return this;
        }

        public HashMap<CoveragePoint, TimeIntervalArray> getResults() {
            return results;
        }

    }
}
