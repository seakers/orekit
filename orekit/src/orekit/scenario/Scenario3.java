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
import orekit.analysis.Analysis;
import orekit.analysis.CompoundAnalysis;
import orekit.coverage.access.CoverageAccessMerger;
import orekit.coverage.access.RiseSetTime;
import orekit.coverage.access.TimeIntervalArray;
import orekit.coverage.access.TimeIntervalMerger;
import orekit.events.FOVDetector;
import orekit.events.HandlerTimeInterval;
import orekit.events.LBDetector;
import orekit.events.LBDetector2;
import orekit.events.LOSDetector;
import orekit.object.Constellation;
import orekit.object.CoverageDefinition;
import orekit.object.CoveragePoint;
import orekit.object.Instrument;
import orekit.object.Satellite;
import orekit.object.linkbudget.LinkBudget;
import orekit.propagation.PropagatorFactory;
import orekit.propagation.PropagatorType;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
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
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;

/**
 * Stores information about the scenario or simulation including information of
 * start and stop times, time scales, epoch, etc.
 *
 * @author nozomihitomi
 */
public class Scenario3 implements Callable<Scenario3>, Serializable {

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
     * This object stores all the merged link budget intervals for each point for each
     * coverage definition
     */
    private final HashMap<CoverageDefinition, HashMap<CoveragePoint, TimeIntervalArray>> linkBudgetIntervals;
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
    
    private final LinkBudget linkBudget;

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
     * @param linkBudget link budget params provider
     */
    public Scenario3(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame, PropagatorFactory propagatorFactory,
            HashSet<CoverageDefinition> covDefs, boolean saveAllAccesses,
            boolean saveToDB, Analysis analyses, int numThreads, LinkBudget linkBudget) {
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
        this.linkBudgetIntervals = new HashMap<>();
        includeCovDef(covDefs);

        this.analyses = analyses;
        this.analysisResults = new HashMap();
        if (analyses != null) {
            includeAnalysis(analyses);
        }

        this.isDone = false;
        this.numThreads = numThreads;
        this.linkBudget=linkBudget;
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
    private Scenario3(Scenario3 s,
            HashMap<CoverageDefinition, HashMap<CoveragePoint, TimeIntervalArray>> finalAccesses,
            HashMap<CoverageDefinition, HashMap<Satellite, HashMap<CoveragePoint, TimeIntervalArray>>> allAccesses,
            HashMap<Analysis, HashMap<Satellite, Collection>> analysisResults) {
        this(s.getName(), s.getStartDate(), s.getEndDate(), s.getTimeScale(),
                s.getFrame(), s.getPropagatorFactory(),
                s.getCoverageDefinitions(), s.isSaveAllAccesses(),
                s.isSaveToDB(), new CompoundAnalysis(s.getAnalyses()),1,s.getLinkBudget());
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
        
        private LinkBudget linkBudget;

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
        public Builder linkBudget(LinkBudget linkBudget) {
            this.linkBudget = linkBudget;
            return this;
        }
        

        /**
         * Builds an instance of a scenario with all the specified parameters.
         *
         * @return
         */
        public Scenario3 build() {
            return new Scenario3(scenarioName, startDate, endDate, timeScale, inertialFrame, propagatorFactory, covDefs, saveAllAccesses, saveToDB, analyses, numThreads,linkBudget);
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
                .saveAllAccesses(saveAllAccesses).saveToDB(saveToDB).linkBudget(linkBudget);
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
    public Scenario3 call() throws OrekitException {
        //set up resource pool
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        System.out.println(String.format("Running scenario: %s...", this));
        if (!isDone) {
            for (CoverageDefinition cdef : covDefs) {
                //build initial position vector matrix that can be reused by rotation matrix

                //mapping of points to some id number to keep track of which
                //row the point is represented in the initPointPos matrix that
                //stores the initial positions of the points in the internal
                //frame
                HashMap<CoveragePoint, Integer> pointColMap = new HashMap<>(cdef.getNumberOfPoints());
                // matrix that stores the initial positions of the points in the
                // internal frame
                RealMatrix initPointPos = new Array2DRowRealMatrix(3, cdef.getNumberOfPoints());

                int col = 0;
                for (CoveragePoint pt : cdef.getPoints()) {
                    initPointPos.setColumn(col, pt.getPVCoordinates(startDate, cdef.getPlanetShape().getBodyFrame()).getPosition().toArray());
                    pointColMap.put(pt, col);
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
                    HashMap<CoveragePoint, TimeIntervalArray> satlinkBudgetIntervals = new HashMap<>(cdef.getNumberOfPoints());
                    for (CoveragePoint pt : points) {
                        satAccesses.put(pt, new TimeIntervalArray(startDate, endDate));
                        satlinkBudgetIntervals.put(pt, new TimeIntervalArray(startDate, endDate));
                    }
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
//                        CelestialBody sun  = CelestialBodyFactory.getSun();
//                        BodyShape earth = points.iterator().next().getParentShape();
//                        String supportedNames = "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}F10\\.(?:txt|TXT)";
//                        MarshallSolarActivityFutureEstimation.StrengthLevel strengthlevel = MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE;
//                        DTM2000InputParameters parameters = new MarshallSolarActivityFutureEstimation(supportedNames,strengthlevel);
//                        Atmosphere atmosphere=new DTM2000(parameters, sun, earth);
//                        double crossSection=100;
//                        double dragCoeff=2;
//                        DragSensitive spacecraft = new IsotropicDrag(crossSection,dragCoeff);
//                        DragForce model = new DragForce(atmosphere, spacecraft);
//                        ((NumericalPropagator)prop).addForceModel(model);

                        //We now add the drag model (Harris Priester model)
        //                CelestialBody sun  = CelestialBodyFactory.getSun();
        //                OneAxisEllipsoid earth = (OneAxisEllipsoid) points.iterator().next().getParentShape();
        //                Atmosphere atmosphere=new HarrisPriester(sun, earth);
        //                double crossSection=100;
        //                double dragCoeff=2;
        //                DragSensitive spacecraft = new IsotropicDrag(crossSection,dragCoeff);
        //                DragForce model = new DragForce(atmosphere, spacecraft);
        //                prop.addForceModel(model);
                        
                        //We now add the third body attraction model
//                        CelestialBody moon = CelestialBodyFactory.getMoon();
//                        ThirdBodyAttraction model2 = new ThirdBodyAttraction(moon);
//                        ((NumericalPropagator)prop).addForceModel(model2);
//
//                        //We now add the solar radiation pressure model
//                        double equatorialRadius=Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
//                        double cr=0.5;
//                        RadiationSensitive spacecraft2 = new IsotropicRadiationSingleCoefficient(crossSection, cr);
//                        SolarRadiationPressure model3 = new SolarRadiationPressure(sun,  equatorialRadius,  spacecraft2);
//                        ((NumericalPropagator)prop).addForceModel(model3);
                        
                    }else{
                            throw new IllegalArgumentException(String.format("Propagator type of "
                            + "satelite %s is not supported. "
                            + "Propagator found: %s", sat.toString(),
                            propagatorFactory.getPropType()));
                    }

                    //take large steps to find when there is line of sight
                    double losStepSize = sat.getOrbit().getKeplerianPeriod() / 10;
                    //take small steps to find when points are in field of view
                    double fovStepSize = sat.getOrbit().getKeplerianPeriod() / 50000;
                    double lbStepSize=fovStepSize/100;
                    SpacecraftState initialState=new SpacecraftState(sat.getOrbit());
                    for (Instrument inst : sat.getPayload()) {
                        for (CoveragePoint pt : points) {

                            //First find all intervals with line of sight.
                            //SpacecraftState initialState=new SpacecraftState(sat.getOrbit());
                            prop.resetInitialState(initialState);
                            HandlerTimeInterval losHandler = new HandlerTimeInterval(startDate, endDate);
                            LOSDetector losDetec = new LOSDetector(losStepSize, 1.0, pt, cdef.getPlanetShape(), inertialFrame);
                            prop.addEventDetector(losDetec.withHandler(losHandler));
                            prop.propagate(startDate, endDate);
                            prop.clearEventsDetectors();

                            if(losHandler.getTimeArray().isEmpty()){
                                continue;
                            }

                            //Next search through intervals with line of sight to compute when point is in field of view
                            prop.resetInitialState(initialState);
                            HandlerTimeInterval fovHandler = new HandlerTimeInterval(startDate, endDate, EventHandler.Action.STOP);
                            FOVDetector fovDetec = new FOVDetector(fovStepSize,pt, inst);
                            prop.addEventDetector(fovDetec.withHandler(fovHandler));
                            
                            double date0 = 0;
                            double date1 = Double.NaN;
                            for (RiseSetTime interval : losHandler.getTimeArray()) {
                                if (interval.isRise()) {
                                    date0 = interval.getTime();
                                } else {
                                    date1 = interval.getTime();
                                }

                                if (!Double.isNaN(date1)) {
                                    //first propagation will find the start time when the point is in the field of view
                                    //prop.resetInitialState(losHandler.getInitialState(startDate.shiftedBy(date0)));
                                    SpacecraftState s = prop.propagate(startDate.shiftedBy(date0), startDate.shiftedBy(date1));
                                    
                                    //prop.resetInitialState(s);
                                    //second propagation will find the end time when the point is in the field of view
                                    prop.propagate(s.getDate(), startDate.shiftedBy(date1));
                                    date1 = Double.NaN;
                                }
                            }
//                            prop.clearEventsDetectors();
//                            if(fovHandler.getTimeArray().isEmpty()){
//                                continue;
//                            }
//                            
//                            //Next search through intervals with line of sight to compute when point closes the link budget 
//                            HandlerTimeInterval LBHandler = new HandlerTimeInterval(startDate, endDate, EventHandler.Action.STOP);
//                            LBDetector2 LBDetec = new LBDetector2(lbStepSize,pt, linkBudget).withHandler(LBHandler);
//                            prop.addEventDetector(LBDetec);
//                            double date00 = 0;
//                            double date11 = Double.NaN;
//                            for (RiseSetTime interval : losHandler.getTimeArray()) {
//                                if (interval.isRise()) {
//                                    date00 = interval.getTime();
//                                } else {
//                                    date11 = interval.getTime();
//                                }
//
//                                if (!Double.isNaN(date11)) {
//                                    //first propagation will find the start time when the point closes the link budget
//                                    //prop.resetInitialState(losHandler.getInitialState(startDate.shiftedBy(date00)));
//                                    SpacecraftState s = prop.propagate(startDate.shiftedBy(date00), startDate.shiftedBy(date11));
//                                    
//                                    //prop.resetInitialState(s);
//                                    //second propagation will find the end time when the point closes the link budget
//                                    prop.propagate(s.getDate(), startDate.shiftedBy(date11));
//                                    date11 = Double.NaN;
//                                }
//                            }
//                            HandlerTimeInterval LBHandler = new HandlerTimeInterval(startDate, endDate);
//                            LBDetector2 LBDetec = new LBDetector2(lbStepSize,pt, linkBudget).withHandler(LBHandler);
//                            prop.addEventDetector(LBDetec);
//                            prop.propagate(startDate, endDate);
                            
                            
                            prop.clearEventsDetectors();
                            
                            TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt),fovHandler.getTimeArray());
                            satAccesses.put(pt, merger.orCombine().createImmutable());
                            
                            //merger2 ha de tenir tant els LB intervals com els acceses intervals
//                            TimeIntervalMerger merger2 = new TimeIntervalMerger(satAccesses.get(pt),LBHandler.getTimeArray());
//                            satlinkBudgetIntervals.put(pt, merger2.andCombine().createImmutable());
                            
                        }
                    }
                    //save the satellite accesses 
                    if (saveAllAccesses) {
                        allAccesses.get(cdef).put(sat, satAccesses);
                    }

                    //merge the time accesses across all satellite for each coverage definition
                    if (finalAccesses.containsKey(cdef)) {
                        HashMap<CoveragePoint, TimeIntervalArray> mergedAccesses
                                = CoverageAccessMerger.mergeCoverageDefinitionAccesses(finalAccesses.get(cdef), satAccesses, false);
                        finalAccesses.put(cdef, mergedAccesses);
                    } else {
                        finalAccesses.put(cdef, satAccesses);
                    }
                    //merge the link budget intervals across all satellite for each coverage definition
                    if (linkBudgetIntervals.containsKey(cdef)) {
                        HashMap<CoveragePoint, TimeIntervalArray> mergedLinkBudgetIntervals
                                = CoverageAccessMerger.mergeCoverageDefinitionAccesses(linkBudgetIntervals.get(cdef), satlinkBudgetIntervals, false);
                        linkBudgetIntervals.put(cdef, mergedLinkBudgetIntervals);
                    } else {
                        linkBudgetIntervals.put(cdef, satlinkBudgetIntervals);
                    }
                }

                //Make all time intervals stored in finalAccesses immutable
                for (CoveragePoint pt : finalAccesses.get(cdef).keySet()) {
                    finalAccesses.get(cdef).put(pt, finalAccesses.get(cdef).get(pt).createImmutable());
                }
                //Make all link budget intervals stored in finalAccesses immutable
                for (CoveragePoint pt : linkBudgetIntervals.get(cdef).keySet()) {
                    linkBudgetIntervals.get(cdef).put(pt, linkBudgetIntervals.get(cdef).get(pt).createImmutable());
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
     * Returns the merged accesses of a given coverage definition after the
     * scenario is finished running
     *
     * @param covDef the coverage definition of interest
     * @return
     */
    public HashMap<CoveragePoint, TimeIntervalArray> getMergedLinkBudgetIntervals(CoverageDefinition covDef) {
        return linkBudgetIntervals.get(covDef);
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
    
    public LinkBudget getLinkBudget(){
        return this.linkBudget;
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
        final Scenario3 other = (Scenario3) obj;
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
}
