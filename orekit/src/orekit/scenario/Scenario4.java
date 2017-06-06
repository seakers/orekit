/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import orekit.analysis.Analysis;
import orekit.analysis.CompoundAnalysis;
import orekit.coverage.access.CoverageAccessMerger;
import orekit.coverage.access.RiseSetTime;
import orekit.coverage.access.TimeIntervalArray;
import orekit.coverage.access.TimeIntervalMerger;
import orekit.events.FOVDetector;
import orekit.events.LBDetector2;
import orekit.events.LOSDetector;
import orekit.events.LifeTimeDetector;
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
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DTM2000;
import org.orekit.forces.drag.DTM2000InputParameters;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.HarrisPriester;
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
import org.orekit.utils.IERSConventions;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.EGMFormatReader;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import static org.orekit.forces.gravity.potential.GravityFieldFactory.EGM_FILENAME;
import static org.orekit.forces.gravity.potential.GravityFieldFactory.GRGS_FILENAME;
import static org.orekit.forces.gravity.potential.GravityFieldFactory.ICGEM_FILENAME;
import static org.orekit.forces.gravity.potential.GravityFieldFactory.SHM_FILENAME;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_MAX_ITER;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.handlers.StopOnEvent;

/**
 * Stores information about the scenario or simulation including information of
 * start and stop times, time scales, epoch, etc.
 *
 * @author nozomihitomi
 */
public class Scenario4 implements Callable<Scenario4>, Serializable {

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
     * Inertial frame used in scenario
     */
    private final Satellite sat;
    /**
     * Propagator factory that will create the necessary propagator for each
     * satellite
     */
    private final PropagatorFactory propagatorFactory;

    /**
     * flag to keep track of whether the simulation is done
     */
    private boolean isDone;

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
     * Creates a new scenario.
     *
     * @param name of scenario
     * @param startDate of scenario
     * @param endDate of scenario
     * @param timeScale of scenario
     * @param inertialFrame
     * @param sat
     * @param propagatorFactory
     * @param analyses the analyses to conduct during the propagation of this
     * scenario
     * @param numThreads number of threads to uses in parallelization of the
     * scenario by dividing up the coverage grid points across multiple threads
     */
    public Scenario4(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame, Satellite sat,PropagatorFactory propagatorFactory,
            Analysis analyses, int numThreads) {
        this.scenarioName = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.timeScale = timeScale;
        this.inertialFrame = inertialFrame;
        this.propagatorFactory = propagatorFactory;
        this.analyses = analyses;
        this.analysisResults = new HashMap();
        if (analyses != null) {
            includeAnalysis(analyses);
        }
        this.isDone = false;
        this.numThreads = numThreads;
        this.sat=sat;
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
        
        /**
         * Scenario end date
         */
        private final Satellite sat;

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
         * the number of threads to use in parallel processing
         */
        private int numThreads = 1;

        /**
         * A set of analyses in which values are recorded during the simulation
         * at fixed time steps
         */
        private Analysis analyses = null;



        /**
         * The constructor for the builder
         *
         * @param startDate the start date of the scenario
         * @param endDate the end date of the scenario
         * @param timeScale the scale used to set the dates
         * @param sat
         */
        public Builder(AbsoluteDate startDate, AbsoluteDate endDate, TimeScale timeScale,Satellite sat) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.timeScale = timeScale;
            this.sat=sat;
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
         * Builds an instance of a scenario with all the specified parameters.
         *
         * @return
         */
        public Scenario4 build() {
            return new Scenario4(scenarioName, startDate, endDate, timeScale, inertialFrame, sat, propagatorFactory, analyses, numThreads);
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
        Builder out = new Builder(startDate, endDate, timeScale,sat);
        out.analysis(analyses).frame(inertialFrame)
                .name(scenarioName).numThreads(numThreads)
                .propagatorFactory(propagatorFactory);
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
    public Scenario4 call() throws OrekitException {
        //set up resource pool
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        System.out.println(String.format("Running scenario: %s...", this));
        if (!isDone) {
            System.out.println(String.format("Initiating %d propagation tasks", numThreads));
            Propagator prop;
            SpacecraftState initialState;

            if (propagatorFactory.getPropType().equals(PropagatorType.KEPLERIAN) || propagatorFactory.getPropType().equals(PropagatorType.J2)) {

                prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());
                initialState = prop.getInitialState();

            } else if (propagatorFactory.getPropType().equals(PropagatorType.NUMERICAL)) {
                //MASS PROPAGATION
                double mass=6;

                //set integrator steps and tolerances
                final double dP = 0.001;
                final double minStep = 0.00001;
                final double maxStep = 1000;
                final double initStep = 60;
//                final double[][] tolerance = NumericalPropagator.tolerances(dP, orbit, orbit.getType());
                final double[][] tolerance = NumericalPropagator.tolerances(dP, sat.getOrbit(), OrbitType.EQUINOCTIAL);
                double[] absTolerance = tolerance[0];
                double[] relTolerance = tolerance[1];
//                double[] absTolerance = {0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001};
//                double[] relTolerance = {1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7};

                //create integrator object and set some propoerties
                AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, absTolerance, relTolerance);
                integrator.setInitialStepSize(initStep);
                prop = propagatorFactory.createPropagator(integrator,sat.getOrbit(),mass);
                prop=setNumericalPropagator(prop, 0.06, 0.058);
            
            } else {
                throw new IllegalArgumentException(String.format("Propagator type of "
                        + "satelite %s is not supported. "
                        + "Propagator found: %s", sat.toString(),
                        propagatorFactory.getPropType()));
            }

            //analysis
            if (analyses != null) {
                prop.setMasterMode(analyses.getTimeStep(), analyses);
            } else {
                prop.setSlaveMode();
            }
            
            //normal propgatation from start date to end date
            //prop.propagate(startDate, endDate);
            
            //propagation with lifetime detection
            //runLifetimeAnalysis(prop);
            
            //propagation with changes of drag area at solar/eclipse events
            runTropicsAnalysis(prop,0.015,0.075);

            //extract analysis information from propagation
            Collection<Analysis> analysislist = this.getAnalyses();
            if (analysislist!=null){
                for (Analysis analysis :analysislist){
                    if (analysis instanceof CompoundAnalysis) {
                        for (Analysis a : ((CompoundAnalysis) analysis).getAnalyses()) {
                            analysisResults.get(a).put(sat, a.getHistory());
                        }
                    } else {
                        analysisResults.get(analysis).put(sat, analysis.getHistory());
                    }
                }
            }
             
            isDone = true;      
        }

        pool.shutdown();
        return this;
    }
    
    /**Runs analysis where a satellite changes its drag area in Eclipse/Sunlight events
     *
     * @param prop propagator
     * @param eclipseDragArea
     * @param sunlightDragArea
     * @throws OrekitException
     */
    public void runTropicsAnalysis(Propagator prop, double eclipseDragArea, double sunlightDragArea) throws OrekitException{
        //Set stepsizes and threshold for detectors
        double StepSize = sat.getOrbit().getKeplerianPeriod()/1000;
        double threshold = 1e-3;
        final PVCoordinatesProvider occulted=CelestialBodyFactory.getSun();
        double occultedRadius=Constants.SUN_RADIUS;
        final PVCoordinatesProvider occulting=CelestialBodyFactory.getEarth();
        double occultingRadius=Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final EventHandler<? super EclipseDetector> handler=new StopOnEvent<>();
        EclipseDetector detector= new EclipseDetector(StepSize,threshold,
                                                occulted,occultedRadius,
                                                occulting,occultingRadius)
                                                .withHandler(handler);
        prop.addEventDetector(detector);
        boolean end=false;
        SpacecraftState s=prop.getInitialState();
        while (!end){
            prop.resetInitialState(s);
            ((NumericalPropagator) prop).removeForceModels();
            if (detector.g(prop.getInitialState())<0){
                prop=setNumericalPropagator(prop, eclipseDragArea, 0.058);
            }else{
                prop=setNumericalPropagator(prop, sunlightDragArea, 0.058);
            }
            s =prop.propagate(s.getDate(), endDate);
            if(s.getDate().equals(endDate)){
                end=true;
            }
        }
    }
    
    /** Runs lifetime analysis
     *
     * @param prop initialized propagator with all models added (if propagator is numerical)
     * @throws OrekitException
     */
    public void runLifetimeAnalysis(Propagator prop) throws OrekitException{
        //Set stepsizes and threshold for detectors
        double StepSize = sat.getOrbit().getKeplerianPeriod()*10;
        double threshold = 1e-3;

        //lifetime detectors
        LifeTimeDetector lifetimeDetec;
        lifetimeDetec = new LifeTimeDetector(prop.getInitialState(), startDate, endDate, StepSize, threshold, EventHandler.Action.STOP);
        prop.addEventDetector(lifetimeDetec);
        SpacecraftState s =prop.propagate(startDate, endDate);
        prop.clearEventsDetectors();
        System.out.println(String.format("Finished simulating %s...", this));
        System.out.println(String.format("Lifetime (in years) is %s", (s.getDate().durationFrom(startDate))/31556952));
    }

    /** Adds all the force models to the numerical propagator: Gravitaty, drag, third body, radiation pressure
     *
     * @param prop initialized numerical propagator without force models
     * @param dragArea
     * @param solarArea
     * @return prop with force models added to it
     */
    
    public Propagator setNumericalPropagator(Propagator prop, double dragArea, double solarArea) throws OrekitException{
        
        //Frames and Bodies creation (must use IERS_2003 and EME2000 frames to be consistent with STK)
        final Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        final CelestialBody sun  = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);

        ((NumericalPropagator) prop).setOrbitType(propagatorFactory.getOrbitType());
        ((NumericalPropagator) prop).setPositionAngleType(PositionAngle.TRUE);

        //We now add the gravity model (without Harmonics(J2))
        //                      ((NumericalPropagator) prop).setMu(Constants.WGS84_EARTH_MU);


        //We now add the gravity model (with Harmonics)
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        ICGEMFormatReader reader=new ICGEMFormatReader(ICGEM_FILENAME, false);
        //SHMFormatReader reader=new SHMFormatReader(SHM_FILENAME, false);
        //GRGSFormatReader reader=new GRGSFormatReader(GRGS_FILENAME, false);
        //EGMFormatReader reader=new EGMFormatReader(EGM_FILENAME, false);
        GravityFieldFactory.addPotentialCoefficientsReader(reader);

        final NormalizedSphericalHarmonicsProvider harmonicsProvider=GravityFieldFactory.getNormalizedProvider(21, 21);
        HolmesFeatherstoneAttractionModel model1 =new HolmesFeatherstoneAttractionModel(earthFrame,harmonicsProvider);
        ((NumericalPropagator)prop).addForceModel(model1);

        //We now add the drag model (DTM2000 model)
        //must use IERS_2003 and EME2000 frames to be consistent with STK
        String supportedNames = "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}F10\\.(?:txt|TXT)";
        MarshallSolarActivityFutureEstimation.StrengthLevel strengthlevel = MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE;
        DTM2000InputParameters parameters = new MarshallSolarActivityFutureEstimation(supportedNames,strengthlevel);
        Atmosphere atmosphere=new DTM2000(parameters, sun, earth);
        double dragCoeff=2.2;
        DragSensitive spacecraft = new IsotropicDrag(dragArea,dragCoeff);
        DragForce model2 = new DragForce(atmosphere, spacecraft);
        ((NumericalPropagator)prop).addForceModel(model2);

        //We now add the drag model (Harris Priester model)
//            Atmosphere atmosphere=new HarrisPriester(sun, earth);
//            double dragArea=0.06;
//            double dragCoeff=2.2;
//            DragSensitive spacecraft = new IsotropicDrag(dragArea,dragCoeff);
//            DragForce model2 = new DragForce(atmosphere, spacecraft);
//            ((NumericalPropagator)prop).addForceModel(model2);

        //We now add the third body attraction  (sun and moon)
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        ThirdBodyAttraction model3 = new ThirdBodyAttraction(moon);
        ((NumericalPropagator)prop).addForceModel(model3);
        ThirdBodyAttraction model4 = new ThirdBodyAttraction(sun);
        ((NumericalPropagator)prop).addForceModel(model4);

        //We now add the solar radiation pressure model
        double equatorialRadius=Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double cr=1;
        RadiationSensitive spacecraft2 = new IsotropicRadiationSingleCoefficient(solarArea, cr);
        SolarRadiationPressure model5 = new SolarRadiationPressure(sun,  equatorialRadius,  spacecraft2);
        ((NumericalPropagator)prop).addForceModel(model5);
            
        return prop;
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
     * Returns the satellite used in this scenario
     *
     * @return
     */
    public Satellite getSatellite() {
        return sat;
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
        final Scenario4 other = (Scenario4) obj;
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
        return true;

    }
}