/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import orekit.analysis.Analysis;
import orekit.analysis.CompoundAnalysis;
import orekit.analysis.events.EventOccurence;
import orekit.analysis.events.GValues;
import orekit.coverage.access.CoverageAccessMerger;
import orekit.coverage.access.FOVHandler;
import orekit.coverage.access.TimeIntervalArray;
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.Satellite;
import orekit.propagation.PropagatorFactory;
import orekit.propagation.PropagatorType;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DTM2000;
import org.orekit.forces.drag.DTM2000InputParameters;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.drag.MarshallSolarActivityFutureEstimation;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Stores information about the scenario or simulation including information of
 * start and stop times, time scales, epoch, etc.
 *
 * @author nozomihitomi
 */
public class Scenario2 implements Callable<Scenario2>, Serializable {

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
     * The minimum radius of the earth (north-south direction)
     */
    private final double minRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING);

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
     * @param saveToDB flag to dictate whether the coverage accesses of
     * individual satellites should be saved to the coverage database
     * @param analyses the analyses to conduct during the propagation of this
     * scenario
     * @param numThreads number of threads to uses in parallelization of the
     * scenario by dividing up the coverage grid points across multiple threads
     */
    public Scenario2(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame, PropagatorFactory propagatorFactory,
            HashSet<CoverageDefinition> covDefs, boolean saveAllAccesses,
            boolean saveToDB, Analysis analyses, int numThreads) {
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
        this.saveToDB = saveToDB;

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
    public Scenario2(Collection<SubScenario> subscenarios) throws IllegalArgumentException {
        this(subscenarios.iterator().next().getParentScenarioName(),
                subscenarios.iterator().next().getStartDate(),
                subscenarios.iterator().next().getEndDate(),
                subscenarios.iterator().next().getTimeScale(),
                subscenarios.iterator().next().getFrame(),
                subscenarios.iterator().next().getPropagatorFactory(),
                new HashSet<>(),
                subscenarios.iterator().next().isSaveAllAccesses(),
                false,
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
                allCovDefs.put(covHash, new HashMap<>());
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

    /**
     * Constructor that creates a new instance of this scenario carrying over
     * all the information and setting the results to the specified results
     *
     * @param s the scenario to clone
     * @param finalAccesses the final accesses of the entire constellation
     * obtained by a simulation
     * @param allAccesses the accesses for each satellite obtained by a
     * simulation
     * @param analysisResults the results obtained by a simulation
     */
    private Scenario2(Scenario2 s,
            HashMap<CoverageDefinition, HashMap<CoveragePoint, TimeIntervalArray>> finalAccesses,
            HashMap<CoverageDefinition, HashMap<Satellite, HashMap<CoveragePoint, TimeIntervalArray>>> allAccesses,
            HashMap<Analysis, HashMap<Satellite, Collection>> analysisResults) {
        this(s.getName(), s.getStartDate(), s.getEndDate(), s.getTimeScale(),
                s.getFrame(), s.getPropagatorFactory(),
                s.getCoverageDefinitions(), s.isSaveAllAccesses(),
                s.isSaveToDB(), new CompoundAnalysis(s.getAnalyses()), 1);
        this.finalAccesses.putAll(finalAccesses);
        this.allAccesses = allAccesses;
        this.analysisResults.putAll(analysisResults);
        this.isDone = true;
    }

    /**
     * This should only be called in the constructor in order to prevent
     * changing the accesses times or the mapping between satellites to coverage
     * definitions
     *
     * @param c
     */
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
                ptAccesses.put(pt, new TimeIntervalArray(startDate, endDate));
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
         * flag to dictate whether the coverage accesses of individual
         * satellites should be saved to the coverage database
         */
        private boolean saveToDB = false;

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

        /**
         * Option to record the accesses of the individual satellites in the
         * constellation and keep them separate from the combined access times
         * of the constellation as a whole. By default, the accesses of the
         * individual satellites are not kept in memory to conserve memory space
         *
         * @param b true to record the accesses of the individual satellites
         * @return
         */
        public Builder saveAllAccesses(boolean b) {
            this.saveAllAccesses = b;
            return this;
        }

        /**
         * Option to set the number of threads to use to run the scenario. By
         * default it is set to 1.
         *
         * @param i the number of threads to use to run the scenario
         * @return
         */
        public Builder numThreads(int i) {
            this.numThreads = i;
            return this;
        }

        /**
         * Option to set the analysis to conduct during the scenario. Analyses
         * allow values to be recorded at fixed time steps during scenario. By
         * default, no analyses are conducted
         *
         * @param a
         * @return
         */
        public Builder analysis(Analysis a) {
            this.analyses = a;
            return this;
        }

        /**
         * Option to set the propagator factory that will create propagators for
         * each satellite. By default a J2 propagator is used.
         *
         * @param factory propagator factory that will create propagators for
         * each satellite
         * @return
         */
        public Builder propagatorFactory(PropagatorFactory factory) {
            this.propagatorFactory = factory;
            return this;
        }

        /**
         * Option to define the inertial frame in which the scenario is run.
         * EME2000 is used by default
         *
         * @param frame the inertial frame in which the scenario is run
         * @return
         */
        public Builder frame(Frame frame) {
            this.inertialFrame = frame;
            return this;
        }

        /**
         * Option to set the coverage definitions to assign to this scenario.
         *
         * @param covDefs coverage definitions to assign to this scenario
         * @return
         */
        public Builder covDefs(HashSet<CoverageDefinition> covDefs) {
            this.covDefs = covDefs;
            return this;
        }

        /**
         * The name to give this scenario. By default, the scenario is named as
         * "scenario1"
         *
         * @param name name to give this scenario
         * @return
         */
        public Builder name(String name) {
            this.scenarioName = name;
            return this;
        }

        /**
         * Option to dictate whether the coverage accesses of individual
         * satellites should be saved to the coverage database. By default the
         * accesses are not stored to the database.
         *
         * @param bool true if user wants to save the coverage accesses of
         * individual satellites to the coverage database
         * @return
         */
        public Builder saveToDB(boolean bool) {
            this.saveToDB = bool;
            return this;
        }

        /**
         * Builds an instance of a scenario with all the specified parameters.
         *
         * @return
         */
        public Scenario2 build() {
            return new Scenario2(scenarioName, startDate, endDate, timeScale, inertialFrame, propagatorFactory, covDefs, saveAllAccesses, saveToDB, analyses, numThreads);
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
                .saveAllAccesses(saveAllAccesses).saveToDB(saveToDB);
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
    public Scenario2 call() throws OrekitException {
        //set up resource pool
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        System.out.println(String.format("Running scenario: %s...", this));
        if (!isDone) {
            for (CoverageDefinition cdef : covDefs) {
                //first propagate all points at a fixed time step
                double stepSize = 1;
                //build initial position vector matrix that can be reused by rotation matrix

                //mapping of points to some id number to keep track of which
                //row the point is represented in the initPointPos matrix that
                //stores the initial positions of the points in the internal
                //frame
                HashMap<TopocentricFrame, Integer> pointMap = new HashMap<>(cdef.getNumberOfPoints());
                // matrix that stores the initial positions of the points in the
                // internal frame
                RealMatrix initPointPos = new Array2DRowRealMatrix(3, cdef.getNumberOfPoints());

                int col = 0;
                for (TopocentricFrame pt : cdef.getPoints()) {
                    initPointPos.setColumn(col, pt.getPVCoordinates(startDate, cdef.getPlanetShape().getBodyFrame()).getPosition().toArray());
                    pointMap.put(pt, col);
                    col++;
                }

                System.out.println(String.format("Acquiring access times for %s...", cdef));
                if (saveAllAccesses) {
                    allAccesses.put(cdef, new HashMap());
                }
                
                Collection<CoveragePoint> points = cdef.getPoints();

                //propogate each satellite individually
                for (Satellite sat : uniqueSatsAssignedToCovDef.get(cdef)) {
                    //before propagation, check that the satellite's propagation for this coverage definition is not stored in database
                    HashMap<CoveragePoint, TimeIntervalArray> satAccesses = new HashMap<>(cdef.getNumberOfPoints());
                    System.out.println(String.format("Initiating %d propagation tasks", numThreads));
                    
                    Propagator prop;
                    
                    if (propagatorFactory.getPropType().equals(PropagatorType.KEPLERIAN) || propagatorFactory.getPropType().equals(PropagatorType.J2)){
                        
                        prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());
                    
                    }else if (propagatorFactory.getPropType().equals(PropagatorType.NUMERICAL)){
                        
                        //set integrator steps and tolerances
                        final double dP       = 0.001;
                        final double minStep  = 0.001;
                        final double maxStep  = 1000;
                        final double initStep = 60;
        //                final double[][] tolerance = NumericalPropagator.tolerances(dP, orbit, orbit.getType());
                        final double[][] tolerance = NumericalPropagator.tolerances(dP, sat.getOrbit(), OrbitType.EQUINOCTIAL);
                        double[] absTolerance = tolerance[0];
                        double[] relTolerance = tolerance[1];
        //                double[] absTolerance = {0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001};
        //                double[] relTolerance = {1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7};

                        //create integrator object and set some propoerties
                        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, absTolerance,relTolerance);
                        integrator.setInitialStepSize(initStep);
                        prop = propagatorFactory.createPropagator(integrator);
                        SpacecraftState initialState=new SpacecraftState(sat.getOrbit());
                        ((NumericalPropagator)prop).setInitialState(initialState);
                        ((NumericalPropagator)prop).setMu(Constants.WGS84_EARTH_MU);
                        ((NumericalPropagator)prop).setOrbitType(propagatorFactory.getOrbitType());
                        ((NumericalPropagator)prop).setPositionAngleType(PositionAngle.TRUE);

                        //We now add the drag model (DTM2000 model)
                        CelestialBody sun  = CelestialBodyFactory.getSun();
                        BodyShape earth = points.iterator().next().getParentShape();
                        String supportedNames = "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}F10\\.(?:txt|TXT)";
                        MarshallSolarActivityFutureEstimation.StrengthLevel strengthlevel = MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE;
                        DTM2000InputParameters parameters = new MarshallSolarActivityFutureEstimation(supportedNames,strengthlevel);
                        Atmosphere atmosphere=new DTM2000(parameters, sun, earth);
                        double crossSection=100;
                        double dragCoeff=2;
                        DragSensitive spacecraft = new IsotropicDrag(crossSection,dragCoeff);
                        DragForce model = new DragForce(atmosphere, spacecraft);
                        ((NumericalPropagator)prop).addForceModel(model);

                        //We now add the drag model (Harris Priester model)
        //                CelestialBody sun  = CelestialBodyFactory.getSun();
        //                OneAxisEllipsoid earth = (OneAxisEllipsoid) points.iterator().next().getParentShape();
        //                Atmosphere atmosphere=new HarrisPriester(sun, earth);
        //                double crossSection=100;
        //                double dragCoeff=2;
        //                DragSensitive spacecraft = new IsotropicDrag(crossSection,dragCoeff);
        //                DragForce model = new DragForce(atmosphere, spacecraft);
        //                prop.addForceModel(model);
                    }else{
                            throw new IllegalArgumentException(String.format("Propagator type of "
                            + "satelite %s is not supported. "
                            + "Propagator found: %s", sat.toString(),
                            propagatorFactory.getPropType()));
                    }
                    

                    double simulationDuration = endDate.durationFrom(startDate);
                    Frame bodyFrame = cdef.getPlanetShape().getBodyFrame();

                    //First pass: obtain changes when gvalues change signs
                    HashMap<CoveragePoint, ArrayList<EventOccurence>> eventsMap = new HashMap<>(cdef.getNumberOfPoints());
                    for(CoveragePoint pt : points){
                        eventsMap.put(pt, new ArrayList<>());
                    }
                    HashMap<CoveragePoint, Double> gValMap = new HashMap<>(cdef.getNumberOfPoints());
                    for (double time = 0; time < simulationDuration; time += stepSize) {
                        AbsoluteDate currentDate = startDate.shiftedBy(time);
                        SpacecraftState s = prop.propagate(currentDate);
                        
                        // The spacecraft position in the inertial frame
                        RealVector satPosInert = new ArrayRealVector(s.getPVCoordinates().getPosition().toArray());
                        RealVector satPosInertNorm = satPosInert.copy().mapDivideToSelf(satPosInert.getNorm());
                        //The normalized position vectors of the points in the inertial frame
                        RealMatrix ptPosInertNorm = MatrixUtils.createRealMatrix(3, points.size());
                        //The vector between the satellite and point position in the inertial frame
                        RealMatrix sat2ptLineInert = MatrixUtils.createRealMatrix(3, points.size());

                        //rotate points from rotating body shape frame to inertial frame
                        RealMatrix pointRotation1 = new Array2DRowRealMatrix(bodyFrame.
                                getTransformTo(inertialFrame, s.getDate()).
                                getRotation().getMatrix());
                        RealMatrix ptPosInert = pointRotation1.multiply(initPointPos);

                        col = 0;
                        for (CoveragePoint pt : points) {
                            ptPosInertNorm.setColumnVector(col, ptPosInert.getColumnVector(col).mapDivideToSelf(ptPosInert.getColumnVector(col).getNorm()));
                            sat2ptLineInert.setColumnVector(col, ptPosInert.getColumnVector(col).subtract(satPosInert));
                            col++;
                        }
                        RealVector cosThetas = ptPosInertNorm.preMultiply(satPosInertNorm);

                        double minCosTheta = minRadius / s.getA();

                        //rot is rotation matrix from inertial frame to spacecraft body-nadir-pointing frame
                        Rotation rot = alignWithNadirAndNormal(Vector3D.PLUS_K, Vector3D.PLUS_J, s, cdef.getPlanetShape());
                        RealMatrix rotMatrix = new Array2DRowRealMatrix(rot.getMatrix());
                        //line of sight vectors in spacecraft frame
                        RealMatrix losSC = rotMatrix.multiply(sat2ptLineInert);
                        
                        col = 0;
                        for (CoveragePoint pt : points) {
                            //check if satellite has line of sight. losVal > 0 means that sat has line of sight
                            double losVal = cosThetas.getEntry(col) - minCosTheta;
                            if (losVal > 0){
                                for(Instrument inst : sat.getPayload()){
                                    double gval = -inst.getFOV().offsetFromBoundary((new Vector3D(losSC.getColumn(col))));
                                    Double oldVal = gValMap.put(pt, gval);
                                    //If the current value and the previous value have different signs, then record sign change
                                    if(oldVal!=null && FastMath.signum(oldVal)!=FastMath.signum(gval)){
                                        eventsMap.get(pt).add(new EventOccurence(time-stepSize, time, oldVal, gval));
                                    }
                                }
                            }
                            col++;
                        }
                    }
                    
                    //second pass, find when the event occured through interpolation
                    for (CoveragePoint pt : points) {
                        TimeIntervalArray tarray = new TimeIntervalArray(startDate, endDate);
                        for(EventOccurence event : eventsMap.get(pt)){
                            double deltaG = event.getValAfter()-event.getValBefore();
                            double deltaTime = event.getDateAfter()-event.getDateBefore();
                            double m = deltaG/deltaTime;
                            double b = -m*event.getDateAfter() + event.getValAfter();
                            double eventTime = -b/m;
                            if(deltaG > 0){
                                tarray.addRiseTime(eventTime);
                            }else{
                                tarray.addSetTime(eventTime);
                            }
                        }
                        satAccesses.put(pt, tarray.createImmutable());
                    }
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

                //Make all time intervals stored in finalAccesses immutable
                for (CoveragePoint pt : finalAccesses.get(cdef).keySet()) {
                    finalAccesses.get(cdef).put(pt, finalAccesses.get(cdef).get(pt).createImmutable());
                }
            }

            isDone = true;
            System.out.println(String.format("Finished simulating %s...", this));
        }

        pool.shutdown();
        return this;
    }
    

    /**
     * This method returns a rotation matrix that will transform vectors v1 and
     * v2 to point toward nadir and the vector that is normal to the orbital
     * plane, respectively. Normal vector is used instead of the velocity vector
     * because the velocity vector and nadir vector may not be orthogonal
     *
     * @param v1 Vector to line up with nadir
     * @param v2 Vector to line up with the velocity vector
     * @param s the current spacecraft state
     * @param shape the shape of the body to define nadir direction
     * @return
     * @throws OrekitException
     */
    private Rotation alignWithNadirAndNormal(Vector3D v1, Vector3D v2,
            final SpacecraftState s, final BodyShape shape) throws OrekitException {
        
        Frame frame = s.getFrame();
        
        //transform from specified reference frame to body frame
        final Transform refToBody = frame.getTransformTo(shape.getBodyFrame(), s.getDate());
        
        //Gets the nadir pointing vector at the current spacecraft state in reference frame. 
        //This method is based on the nadir pointing law NadirPointing
         final Vector3D satInBodyFrame = refToBody.transformPosition(s.getPVCoordinates().getPosition());
        // satellite position in geodetic coordinates
        final GeodeticPoint gpSat = shape.transform(satInBodyFrame, shape.getBodyFrame(), s.getDate());
        // nadir position in geodetic coordinates
        final GeodeticPoint gpNadir = new GeodeticPoint(gpSat.getLatitude(), gpSat.getLongitude(), 0.0);
        // nadir point position in body frame
        final Vector3D pNadirBody = shape.transform(gpNadir);
        // nadir point position in reference frame
        final Vector3D pNadirRef = refToBody.getInverse().transformPosition(pNadirBody);
        TimeStampedPVCoordinates nadirPosRefPV = new TimeStampedPVCoordinates(s.getDate(), pNadirRef, Vector3D.ZERO, Vector3D.ZERO);
        Vector3D nadirPosRef = nadirPosRefPV.getPosition();
        final Vector3D nadirRef = nadirPosRef.subtract(s.getPVCoordinates(frame).getPosition()).normalize();
        Vector3D velRef = s.getPVCoordinates(frame).getVelocity().normalize();
        Vector3D orbitNormal = nadirRef.crossProduct(velRef).normalize();
        return new Rotation(nadirRef, orbitNormal, v1, v2);
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
        return new HashSet<>(analysisResults.keySet());
    }

    /**
     * Gets the results of a particular analysis for a specific satellite
     *
     * @param analysis
     * @param satellite
     * @return Returns the results for the analysis for the satellite. null if
     * the analysis for the satellite does not exist.
     */
    public Collection getAnalysisResult(Analysis analysis, Satellite satellite) {
        if (analysisResults.containsKey(analysis)) {
            if (analysisResults.get(analysis).containsKey(satellite)) {
                return analysisResults.get(analysis).get(satellite);
            }
        }
        return null;
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
                out += "\t\t\tName: " + sat.getName() + "\n";
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
     * Returns the flag that dictates whether the individual satellite coverage
     * accesses are to be saved in the database
     *
     * @return
     */
    public boolean isSaveToDB() {
        return saveToDB;
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

    /**
     * Hashcode is key to filename for saved scenarios. This hashcode is a
     * function of the hashcodes to internal fields including the start date,
     * end date, time scale, inertial frame, the propagator factory, the
     * constellations and satellites, the coverage grids, and the assignment of
     * constellations to coverage grids
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.timeScale.getName());
        hash = 47 * hash + Objects.hashCode(this.startDate);
        hash = 47 * hash + Objects.hashCode(this.endDate);
        hash = 47 * hash + Objects.hashCode(this.inertialFrame.getName());
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
        final Scenario2 other = (Scenario2) obj;
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
            ArrayList<FOVHandler> fovHandlers = new ArrayList();
            //attach a fov detector to each instrument-grid point pair
//            for (Instrument inst : satellite.getPayload()) {
//                for (CoveragePoint pt : points) {
//                    FOVHandler handler = new FOVHandler(pt, startDate, endDate);
//                    FOVDetector eventDec = new FOVDetector(pt, inst).
//                            withMaxCheck(1).withHandler(handler);
//                    propagator.addEventDetector(eventDec);
//                    fovHandlers.add(handler);
//                }
//            }
            propagator.propagate(targetDate);

            //reset the accesses at each point after transferring information to this hashmap
            for (FOVHandler handler : fovHandlers) {
                results.put(handler.getCovPt(), handler.getTimeArray().createImmutable());
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
