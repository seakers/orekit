/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import orekit.object.fieldofview.FOVDetector;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import orekit.analysis.Analysis;
import orekit.analysis.CompoundAnalysis;
import orekit.coverage.access.CoverageAccessMerger;
import orekit.coverage.access.FOVHandler;
import orekit.coverage.access.TimeIntervalArray;
import orekit.coverage.parallel.CoverageDivider;
import orekit.coverage.parallel.ParallelCoverage;
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.Satellite;
import orekit.propagation.PropagatorFactory;
import orekit.propagation.PropagatorType;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * Stores information about the scenario or simulation including information of
 * start and stop times, time scales, epoch, etc.
 *
 * @author nozomihitomi
 */
public class Scenario implements Callable<Scenario>, Serializable {

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
     * A set of analyses in which values are recorded during the simulation at
     * fixed time steps
     */
    private final Analysis analyses;

    /**
     * The values recorded
     */
    private final HashMap<Analysis, HashMap<Satellite, Collection>> analysisResults;

    /**
     * Creates a new scenario.
     *
     * @param name of scenario
     * @param startDate of scenario
     * @param endDate of scenario
     * @param timeScale of scenario
     * @param inertialFrame
     * @param propagatorFactory
     * @param covDefs
     * @param saveAllAccesses true if user wants to maintain all the accesses
     * from each individual satellite. false if user would like to only get the
     * merged accesses between all satellites (this saves memory).
     * @param analyses the analyses to conduct during the propagation of this
     * scenario
     * @param numThreads number of threads to uses in parallelization of the
     * scenario by dividing up the coverage grid points across multiple threads
     */
    public Scenario(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame, PropagatorFactory propagatorFactory,
            HashSet<CoverageDefinition> covDefs, boolean saveAllAccesses, Analysis analyses,
            int numThreads) {
        this.scenarioName = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.timeScale = timeScale;
        this.inertialFrame = inertialFrame;
        this.propagatorFactory = propagatorFactory;
        this.saveAllAccesses = saveAllAccesses;
        if (saveAllAccesses) {
            this.allAccesses = new HashMap();
        }

        this.covDefs = covDefs;
        //record all unique satellite and constellations
        this.uniqueSatsAssignedToCovDef = new HashMap<>();
        this.uniqueConstellations = new HashSet<>();
        this.uniqueSatellites = new HashSet<>();
        this.finalAccesses = new HashMap<>();
        includeCovDef(covDefs);

        this.analyses = analyses;
        this.analysisResults = new HashMap();
        if (analyses != null) {
            includeAnalysis(analyses);
        }

        this.isDone = false;
        this.numThreads = numThreads;
        this.accessMerger = new CoverageAccessMerger();
        this.futureTasks = new ArrayList<>();
    }

    /**
     * Constructor that merges a collection of subscenarios previously run in
     * parallel into their parent Scenario
     *
     * @param subscenarios the collection of subscenarios that we want to merge
     * into the parents scenario
     * @throws java.lang.IllegalArgumentException
     */
    public Scenario(Collection<SubScenario> subscenarios) throws IllegalArgumentException {
        this(subscenarios.iterator().next().getParentScenarioName(),
                subscenarios.iterator().next().getStartDate(),
                subscenarios.iterator().next().getEndDate(),
                subscenarios.iterator().next().getTimeScale(),
                subscenarios.iterator().next().getFrame(),
                subscenarios.iterator().next().getPropagatorFactory(),
                new HashSet<>(),
                subscenarios.iterator().next().isSaveAllAccesses(),
                new CompoundAnalysis(subscenarios.iterator().next().getAnalyses()), 1);
        /*
         Check if all the subscenarios are run and all come from the same Parent Scenario. 
         Otherwise throw an Exception
         */
        SubScenario ref = subscenarios.iterator().next();

        int scenHash = ref.getParentScenarioHash();
        BitSet subscenSet = new BitSet(ref.getTotalSubscenarios());
        for (SubScenario subscenario : subscenarios) {
            if (!subscenario.isDone()) {
                throw new IllegalArgumentException(String.format("Subscenario %s are not run yet", subscenario.getName()));
            }
            if (scenHash != subscenario.getParentScenarioHash()) {
                throw new IllegalArgumentException("The subscenarios are from different Parent Scenarios");
            }
            if (subscenSet.get(subscenario.getSubscenarioID())) {
                throw new IllegalArgumentException(String.format("Found a duplicate subscenario: %s.", subscenario));
            } else {
                subscenSet.set(subscenario.getSubscenarioID());
            }
        }
        //check that all subscenarios have been provided to recreate original scenario
        if (subscenSet.cardinality() != ref.getTotalSubscenarios()) {
            String str = "{";
            for (int i = subscenSet.nextSetBit(0); i >= 0; i = subscenSet.nextSetBit(i + 1)) {
                str += String.format("%d ", i);
            }
            throw new IllegalArgumentException(String.format("Missing the following subscenarios %s}", str));
        }

        /*
         For every subscenario, we get its stored Accesses and merge them all in 
         the Parent Scenario Accesses Hashmap (psa)
         */
        HashMap<Integer, HashMap<CoveragePoint, TimeIntervalArray>> allCovDefs = new HashMap<>();
        HashMap<Integer, String> allCovDefNames = new HashMap<>();
        HashMap<Integer, Collection<Constellation>> allCovDefConstels = new HashMap<>();
        ArrayList<CoveragePoint> pts2 = new ArrayList<>();
        for (SubScenario subscenario : subscenarios) {
            CoverageDefinition partCovDef = subscenario.getPartialCoverageDefinition();
            pts2.addAll(partCovDef.getPoints());
            int covHash = subscenario.getOrigCovDefHash();
            if (!allCovDefs.containsKey(covHash)) {
                allCovDefs.put(covHash, new HashMap<CoveragePoint, TimeIntervalArray>());
                allCovDefNames.put(covHash, subscenario.getOrigCovDefName());
                allCovDefConstels.put(covHash, partCovDef.getConstellations());
            }
            allCovDefs.get(covHash).putAll(subscenario.getMergedAccesses(partCovDef));

            if (saveAllAccesses) {
                allAccesses.putAll(subscenario.getAllAccesses());
            }
        }

        //create new coverage definitions
        HashSet<CoverageDefinition> coverageDefinitions = new HashSet<>(allCovDefs.keySet().size());
        HashMap<CoverageDefinition, Integer> covDefToHash = new HashMap<>(allCovDefs.keySet().size());
        for (Integer i : allCovDefs.keySet()) {
            String name = allCovDefNames.get(i);
            Collection<CoveragePoint> pts = new HashSet(allCovDefs.get(i).keySet());
            CoverageDefinition cov = new CoverageDefinition(name, pts);
            cov.assignConstellation(allCovDefConstels.get(i));
            coverageDefinitions.add(cov);
            covDefToHash.put(cov, i);
        }
        includeCovDef(coverageDefinitions);
        //collect the merged accesses from each coverage point
        for (CoverageDefinition cdef : finalAccesses.keySet()) {
            HashMap<CoveragePoint, TimeIntervalArray> accesses = allCovDefs.get(covDefToHash.get(cdef));
            finalAccesses.get(cdef).putAll(accesses);
        }

        //collect the computed analyses
        for (SubScenario subscenario : subscenarios) {
            for (Analysis analysis : subscenario.getAnalyses()) {
                includeAnalysis(analysis);
                for (Satellite sat : subscenario.getUniqueSatellites()) {
                    analysisResults.get(analysis).put(sat, subscenario.getAnalysisResult(analysis, sat));
                }
            }
        }

        this.isDone = true;
    }

    private void includeCovDef(HashSet<CoverageDefinition> c) {
        for (CoverageDefinition cdef : c) {
            covDefs.add(cdef);
            
            uniqueSatsAssignedToCovDef.put(cdef, new HashSet());
            for (Constellation constel : cdef.getConstellations()) {
                uniqueConstellations.add(constel);
                for (Satellite satellite : constel.getSatellites()) {
                    uniqueSatellites.add(satellite);
                    uniqueSatsAssignedToCovDef.get(cdef).add(satellite);
                }
            }

            //create a new time interval array for each point in the coverage definition
            HashMap<CoveragePoint, TimeIntervalArray> ptAccesses = new HashMap<>();
            for (CoveragePoint pt : cdef.getPoints()) {
                if (cdef.getAccesses().containsKey(pt)) {
                    ptAccesses.put(pt, cdef.getAccesses().get(pt));
                } else {
                    ptAccesses.put(pt, new TimeIntervalArray(startDate, endDate));
                }
            }
            finalAccesses.put(cdef, ptAccesses);
        }
    }

    /**
     * Adds the analysis to the scenario
     *
     * @param analysis
     * @return
     */
    private Collection<Analysis> includeAnalysis(Analysis analysis) {
        ArrayList<Analysis> out = new ArrayList<>();
        if (analysis instanceof CompoundAnalysis) {
            CompoundAnalysis compoundAnalysis = (CompoundAnalysis) analysis;
            for (Analysis a : compoundAnalysis.getAnalyses()) {
                if (!analysisResults.containsKey(a)) {
                    analysisResults.put(a, new HashMap<>());
                    out.add(a);
                }
            }
        } else {
            if (!analysisResults.containsKey(analysis)) {
                analysisResults.put(analysis, new HashMap<>());
                out.add(analysis);
            }
        }
        return out;
    }

    /**
     * A builder pattern to set parameters for scenario
     */
    public static class Builder implements Serializable {

        private static final long serialVersionUID = -2447754795882563741L;

        //required fields
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

        //optional parameters - initialized to default parameters
        /**
         * Scenario name
         */
        private String scenarioName = "scenario1";

        /**
         * Inertial frame used in scenario
         */
        private Frame inertialFrame = FramesFactory.getEME2000();

        /**
         * Propagator factory that will create the necessary propagator for each
         * satellite
         */
        private PropagatorFactory propagatorFactory = new PropagatorFactory(PropagatorType.J2, OrbitType.KEPLERIAN);

        /**
         * a flag set by the user to toggle whether to save the access of each
         * individual satellite or to release them from memory.
         */
        private boolean saveAllAccesses = false;

        /**
         * the number of threads to use in parallel processing
         */
        private int numThreads = 1;

        /**
         * A set of analyses in which values are recorded during the simulation
         * at fixed time steps
         */
        private Analysis analyses = null;

        /**
         * The set of coverage definitions to simulate.
         */
        private HashSet<CoverageDefinition> covDefs = null;

        /**
         * The constructor for the builder
         *
         * @param startDate the start date of the scenario
         * @param endDate the end date of the scenario
         * @param timeScale the scale used to set the dates
         */
        public Builder(AbsoluteDate startDate, AbsoluteDate endDate, TimeScale timeScale) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.timeScale = timeScale;
        }

        public Builder saveAllAccesses(boolean b) {
            this.saveAllAccesses = b;
            return this;
        }

        public Builder numThreads(int i) {
            this.numThreads = i;
            return this;
        }

        public Builder analysis(Analysis a) {
            this.analyses = a;
            return this;
        }

        public Builder propagatorFactory(PropagatorFactory factory) {
            this.propagatorFactory = factory;
            return this;
        }

        public Builder frame(Frame frame) {
            this.inertialFrame = frame;
            return this;
        }

        public Builder covDefs(HashSet<CoverageDefinition> covDefs) {
            this.covDefs = covDefs;
            return this;
        }

        public Builder name(String name) {
            this.scenarioName = name;
            return this;
        }

        public Scenario build() {
            return new Scenario(scenarioName, startDate, endDate, timeScale, inertialFrame, propagatorFactory, covDefs, saveAllAccesses, analyses, numThreads);
        }

    }

    /**
     * Creates a builder that has the same information as this scenario
     * including the name, start and end dates, the coverage definitions,
     * propagator, analyses, time scale, inertial frame, and number of threads
     * to use. None of the information of the accesses are carried over into
     * builder
     *
     * @return
     */
    public Builder builder() {
        Builder out = new Builder(startDate, endDate, timeScale);
        out.analysis(analyses).covDefs(covDefs).frame(inertialFrame)
                .name(scenarioName).numThreads(numThreads)
                .propagatorFactory(propagatorFactory)
                .saveAllAccesses(saveAllAccesses);
        return out;
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

                    //assign future tasks by dividing coverage definition into the number of threads available
                    Collection<Collection<CoveragePoint>> pointGroups = CoverageDivider.divide(cdef.getPoints(), numThreads);
                    boolean addedAnalysis = false; //only add the analyses to one task
                    System.out.println(String.format("Initiating %d propagation tasks", numThreads));
                    for(Collection<CoveragePoint> group : pointGroups){
                        PropagateTask task;
                        if(!addedAnalysis){
                            task = new PropagateTask(sat, endDate, group, analyses);
                        }else{
                            task = new PropagateTask(sat, endDate, group, null);
                        }
                        futureTasks.add(pool.submit(task));
                    }

                    //call future tasks and combine accesses from each point into one results object 
                    System.out.println(String.format("Propagating satellite %s to date %s", sat, endDate));
                    for (Future<PropagateTask> run : futureTasks) {
                        try {
                            PropagateTask finishedTask = run.get();
                            for (CoveragePoint pt : finishedTask.getAccesses().keySet()) {
                                satAccesses.put(pt, finishedTask.getAccesses().get(pt));
                            }

                            //extract analysis information from propagation
                            Analysis analysis = finishedTask.getAnalysis();
                            if (analysis != null) {
                                if (analysis instanceof CompoundAnalysis) {
                                    for (Analysis a : ((CompoundAnalysis) analysis).getAnalyses()) {
                                        analysisResults.get(a).put(sat, a.getHistory());
                                    }
                                } else {
                                    analysisResults.get(analysis).put(sat, analysis.getHistory());
                                }
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
        return new HashSet<CoverageDefinition>(covDefs);
    }

    /**
     * Gets the unique constellations that are simulated in this scenario
     *
     * @return
     */
    public HashSet<Constellation> getUniqueConstellations() {
        return new HashSet<Constellation>(uniqueConstellations);
    }

    /**
     * Gets the unique satellites that are simulated in this scenario
     *
     * @return
     */
    public HashSet<Satellite> getUniqueSatellites() {
        return new HashSet<Satellite>(uniqueSatellites);
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

    /**
     * Gets the analyses that are assigned to this scenario
     *
     * @return
     */
    public Collection<Analysis> getAnalyses() {
        return new HashSet<Analysis>(analysisResults.keySet());
    }

    /**
     * Gets the results of a particular analysis for a specific satellite
     *
     * @param analysis
     * @param satellite
     * @return
     */
    public Collection getAnalysisResult(Analysis analysis, Satellite satellite) {
        return analysisResults.get(analysis).get(satellite);
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
                out += "\t\t\tName: " + sat.getName() +"\n";
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
            ArrayList<CoveragePoint> sortedTopoPts = new ArrayList(covdef.getPoints());
            Collections.sort(sortedTopoPts);
            for (CoveragePoint topoPt : sortedTopoPts) {
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

    public String getName() {
        return scenarioName;
    }

    public AbsoluteDate getEndDate() {
        return endDate;
    }

    /**
     * Returns the flag that marks the simulation as finished.
     *
     * @return True if the simulation is done. Else false.
     */
    public boolean isDone() {
        return isDone;
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
     * Returns the computed accesses for each coverage definition by the
     * combination of satellites assigned to that coverage definition
     *
     * @return
     */
    public HashMap<CoverageDefinition, HashMap<CoveragePoint, TimeIntervalArray>> getFinalAccesses() {
        return finalAccesses;
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
     * Gets the timescale (e.g. UTC)
     *
     * @return
     */
    public TimeScale getTimeScale() {
        return timeScale;
    }

    /**
     * Returns the inertial frame used in this scenario
     *
     * @return
     */
    public Frame getFrame() {
        return inertialFrame;
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

    @Override
    public String toString() {
        return "Scenario{" + "scenarioName=" + scenarioName + ", startDate=" + startDate + ", endDate= " + endDate + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.scenarioName);
        hash = 47 * hash + Objects.hashCode(this.timeScale);
        hash = 47 * hash + Objects.hashCode(this.startDate);
        hash = 47 * hash + Objects.hashCode(this.endDate);
        hash = 47 * hash + Objects.hashCode(this.inertialFrame);
        hash = 47 * hash + Objects.hashCode(this.propagatorFactory);
        hash = 47 * hash + Objects.hashCode(this.uniqueConstellations);
        hash = 47 * hash + Objects.hashCode(this.uniqueSatsAssignedToCovDef);
        hash = 47 * hash + Objects.hashCode(this.uniqueSatellites);
        hash = 47 * hash + Objects.hashCode(this.covDefs);
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
        final Scenario other = (Scenario) obj;
        if (!Objects.equals(this.scenarioName, other.scenarioName)) {
            return false;
        }
        if (!Objects.equals(this.timeScale, other.timeScale)) {
            return false;
        }
        if (!Objects.equals(this.startDate, other.startDate)) {
            return false;
        }
        if (!Objects.equals(this.endDate, other.endDate)) {
            return false;
        }
        if (!Objects.equals(this.inertialFrame, other.inertialFrame)) {
            return false;
        }
        if (!Objects.equals(this.propagatorFactory, other.propagatorFactory)) {
            return false;
        }
        if (!Objects.equals(this.uniqueConstellations, other.uniqueConstellations)) {
            return false;
        }
        if (!Objects.equals(this.uniqueSatsAssignedToCovDef, other.uniqueSatsAssignedToCovDef)) {
            return false;
        }
        if (!Objects.equals(this.uniqueSatellites, other.uniqueSatellites)) {
            return false;
        }
        if (!Objects.equals(this.covDefs, other.covDefs)) {
            return false;
        }
        return true;
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
         * The analysis to record any values obtained during the propagation at
         * fixed time steps
         */
        private final Analysis analysis;

        /**
         * The constructor to create a new subtask to propagate forward a
         * satellite. This propagator records the ephemeris at fixed time steps.
         *
         * @param satellte the satellite to propagate forward
         * @param targetDate the target date to propagate the simulation forward
         * to
         * @param points the points the satellite is assigned to in this
         * @param tStep the fixed time step at which ephemeris is recorded
         * propagation
         */
        public PropagateTask(Satellite satellte, AbsoluteDate targetDate, Collection<CoveragePoint> points, Analysis analysis) {
            this.satellite = satellte;
            this.targetDate = targetDate;
            this.points = points;
            this.results = new HashMap<>(points.size());
            this.analysis = analysis;
        }

        /**
         * The constructor to create a new subtask to propagate forward a
         * satellite. This propagator will not record the ephemeris.
         *
         * @param satellte the satellite to propagate forward
         * @param targetDate the target date to propagate the simulation forward
         * to
         * @param points the points the satellite is assigned to in this
         * propagation
         */
        public PropagateTask(Satellite satellte, AbsoluteDate targetDate, Collection<CoveragePoint> points) {
            this(satellte, targetDate, points, null);
        }

        /**
         * Creates a propagator. Can set an option to record ephemeris
         *
         * @param recordEphmeris true if ephemeris should be recorded
         * @param tstep the fixed interval in which to record ephemeris
         * @return
         * @throws OrekitException
         */
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
            if (analysis != null) {
                prop.setMasterMode(analysis.getTimeStep(), analysis);
            } else {
                prop.setSlaveMode();
            }

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
            //attach a fov detector to each instrument-grid point pair
            for (Instrument inst : satellite.getPayload()) {
                for (CoveragePoint pt : points) {
                    FOVDetector eventDec = new FOVDetector(pt, inst).withMaxCheck(1).withHandler(new FOVHandler());
                    propagator.addEventDetector(eventDec);
                }
            }
            propagator.propagate(targetDate);

            //reset the accesses at each point after transferring information to this hashmap
            for (CoveragePoint pt : points) {
                results.put(pt, pt.getAccesses());
                pt.reset();
            }

            return this;
        }

        public HashMap<CoveragePoint, TimeIntervalArray> getAccesses() {
            return results;
        }

        /**
         * Returns the analyses
         *
         * @return
         */
        public Analysis getAnalysis() {
            return analysis;
        }

    }
}
