/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Satellite;
import seakers.orekit.object.Instrument;
import seakers.orekit.propagation.*;
import seakers.orekit.object.*;
import seakers.orekit.coverage.access.*;
import seakers.orekit.event.detector.*;
import seakers.orekit.object.fieldofview.FieldOfViewDefinition;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;
import static seakers.orekit.object.CoverageDefinition.GridStyle.UNIFORM;
import java.util.Random;

/**
 *
 * @author ben_gorr
 */
public class Overlap2 {

    /**
     * @param args the command line arguments
     * @throws OrekitException
     */
    Population population = new Population();
    Individual fittest;
    Individual secondFittest;
    int generationCount = 0;

    public static void main(String[] args) {
//
//        Random rn = new Random();

        Overlap2 demo = new Overlap2();

//        //Initialize population
//        demo.population.initializePopulation(10);
//
//        //Calculate fitness of each individual
//        demo.population.calculateFitness();
//
//        System.out.println("Generation: " + demo.generationCount + " Fittest: " + demo.population.fittest);
//        System.out.print("Genes: ");
//        for (int i = 0; i < 2; i++) {
//            System.out.println(demo.population.getFittest().genes[i]);
//        }
//
//        //While population gets an individual with maximum fitness
//        while (demo.generationCount < 10) {
//            ++demo.generationCount;
//
//            //Do selection
//            demo.selection();
//
//            //Do crossover
//            demo.crossover();
//
//            //Do mutation under a random probability
//            if (rn.nextInt()%7 < 5) {
//                demo.mutation();
//            }
//
//            //Add fittest offspring to population
//            demo.addFittestOffspring();
//
//            //Calculate new fitness value
//            demo.population.calculateFitness();
//
//            System.out.println("Generation: " + demo.generationCount + " Fittest: " + demo.population.fittest);
//            System.out.print("Genes: ");
//            for (int i = 0; i < 2; i++) {
//                System.out.println(demo.population.getFittest().genes[i]);
//            }
//        }
//
//        System.out.println("\nSolution found in generation " + demo.generationCount);
//        System.out.println("Fitness: "+demo.population.getFittest().fitness);
//        System.out.print("Genes: ");
//        for (int i = 0; i < 2; i++) {
//            System.out.println(demo.population.getFittest().genes[i]);
//        }
//
//        System.out.println("");

    }

    //Selection
    void selection() {

        //Select the most fittest individual
        fittest = population.getFittest();

        //Select the second most fittest individual
        secondFittest = population.getSecondFittest();
    }

    //Crossover
    void crossover() {
        Random rn = new Random();

        //Select a random crossover point
        int crossOverPoint = rn.nextInt(population.individuals[0].geneLength);

        //Swap values among parents
        for (int i = 0; i < crossOverPoint; i++) {
            double temp = fittest.genes[i];
            fittest.genes[i] = secondFittest.genes[i];
            secondFittest.genes[i] = temp;

        }

    }

    //Mutation
    void mutation() {
        Random rn = new Random();

        //Select a random mutation point
        int mutationPoint = rn.nextInt(population.individuals[0].geneLength);

        //Flip values at the mutation point
        if (fittest.genes[mutationPoint] == 0) {
            fittest.genes[mutationPoint] = 1;
        } else {
            fittest.genes[mutationPoint] = fittest.genes[mutationPoint]+5*rn.nextDouble();
        }

        mutationPoint = rn.nextInt(population.individuals[0].geneLength);

        if (secondFittest.genes[mutationPoint] == 0) {
            secondFittest.genes[mutationPoint] = 1;
        } else {
            secondFittest.genes[mutationPoint] = secondFittest.genes[mutationPoint]+5*rn.nextDouble();
        }
    }

    //Get fittest offspring
    Individual getFittestOffspring() {
        if (fittest.fitness > secondFittest.fitness) {
            return fittest;
        }
        return secondFittest;
    }


    //Replace least fittest individual from most fittest offspring
    void addFittestOffspring() {

        //Update fitness values of offspring
        fittest.calcFitness();
        secondFittest.calcFitness();

        //Get index of least fit individual
        int leastFittestIndex = population.getLeastFittestIndex();

        //Replace least fittest individual from most fittest offspring
        population.individuals[leastFittestIndex] = getFittestOffspring();
    }

}


//Individual class
class Individual {
    double fitness = 0;
    double[] genes = new double[2];
    int geneLength = 2;

    public Individual() {
        Random rn = new Random();

        //Set genes randomly for each individual
        genes[0]= 400000 + (1000000 - 400000) * rn.nextDouble();
        genes[1]= 10 + (80 - 10) * rn.nextDouble();

        fitness = 0;
    }

    public static double evaluationfunc(double h ,double i) {
    	
    	long start = System.nanoTime();
    	File orekitData = new File("/home/ben/orekit-data");
    	DataProvidersManager manager = DataProvidersManager.getInstance();
    	manager.addProvider(new DirectoryCrawler(orekitData));
    	Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);
        TimeScale utc = TimeScalesFactory.getUTC();
        //AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 10, 30, 00.000, utc);
        //AbsoluteDate endDate = new AbsoluteDate(2020, 1, 8, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2020, 1, 3, 10, 30, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient
        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        
        //define instruments
        //NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
        NadirRectangularFOV SWOT_fov = new NadirRectangularFOV(FastMath.toRadians(30), FastMath.toRadians(30), 0, earthShape);
        Instrument SWOT_payload = new Instrument("view1", SWOT_fov, 100, 100);
        NadirRectangularFOV SWOTlet_fov = new NadirRectangularFOV(FastMath.toRadians(30), FastMath.toRadians(30), 0, earthShape);
        Instrument SWOTlet_VNIR = new Instrument("view1", SWOTlet_fov, 100, 100);
        
        Properties propertiesPropagator = new Properties();
        propertiesPropagator.setProperty("orekit.propagator.mass", "6");
        propertiesPropagator.setProperty("orekit.propagator.atmdrag", "true");
        propertiesPropagator.setProperty("orekit.propagator.dragarea", "0.075");
        propertiesPropagator.setProperty("orekit.propagator.dragcoeff", "2.2");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.sun", "true");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.moon", "true");
        propertiesPropagator.setProperty("orekit.propagator.solarpressure", "true");
        propertiesPropagator.setProperty("orekit.propagator.solararea", "0.058");

        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN,propertiesPropagator);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);
        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.NUMERICAL,propertiesPropagator);
        
        int SWOT_height=890600;
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+SWOT_height;
        double a_SWOTlet = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+h;
        double ideg=77.6;
        double SWOT_i = FastMath.toRadians(ideg);
        //Enter satellite orbital parameters
        ArrayList<Satellite> SWOTlets=new ArrayList<>();
        ArrayList<Satellite> SWOT=new ArrayList<>();
        double SWOT_mass = 600;
        double SWOTlet_mass = 12;

        Collection<Instrument> SWOT_instruments = new ArrayList<>();
        SWOT_instruments.add(SWOT_payload);
        Collection<Instrument> SWOTlet_instruments = new ArrayList<>();
        SWOTlet_instruments.add(SWOTlet_VNIR);
        Orbit orb1 = new KeplerianOrbit(a, 0.0001, SWOT_i, 0.0, FastMath.toRadians(257.8), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat1 = new Satellite("SWOT", orb1, SWOT_instruments);
        Orbit orb2 = new KeplerianOrbit(a_SWOTlet, 0.0001, FastMath.toRadians(i), 0.0, FastMath.toRadians(257.8), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat2 = new Satellite("SWOTlet", orb2, SWOTlet_instruments);
        Propagator prop1 = pf.createPropagator(orb1, SWOT_mass);
        Propagator prop2 = pf.createPropagator(orb2, SWOTlet_mass);
        

        SWOT.add(sat1);
        SWOTlets.add(sat2);
        //CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 9, earthShape, EQUAL_AREA);
        Constellation SWOT_constel = new Constellation ("Just SWOT",SWOT);
        Constellation SWOTlet_constel = new Constellation ("SWOTlet",SWOTlets);
        CoverageDefinition covDef1 = new CoverageDefinition("Whole Earth", 10, earthShape, UNIFORM);
        covDef1.assignConstellation(SWOT_constel);
        covDef1.assignConstellation(SWOTlet_constel);
        SpacecraftState initialState1 = prop1.getInitialState();
        SpacecraftState initialState2 = prop2.getInitialState();
        HashMap<TopocentricFrame, TimeIntervalArray> satAccesses;
        satAccesses = new HashMap<>(covDef1.getNumberOfPoints());
        double coverage = 0;
        for (CoveragePoint pt : covDef1.getPoints()) {
        	TimeIntervalArray emptyTimeArray = new TimeIntervalArray(startDate, endDate);
            satAccesses.put(pt, emptyTimeArray);
        }
        for (CoveragePoint pt : covDef1.getPoints()) {
            //need to reset initial state of the propagators or will propagate from the last stop time
        	if (!lineOfSightPotential(pt, initialState1.getOrbit(), FastMath.toRadians(5.0))) {
                //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
                continue;
            }
        	prop1.resetInitialState(initialState1);
        	prop1.clearEventsDetectors();
            //Next search through intervals with line of sight to compute when point is in field of view 
        	double fovStepSize = orb1.getKeplerianPeriod() / 100.;
            double threshold = 1e-3;
            FOVDetector fovDetec = new FOVDetector(pt, SWOT_payload).withMaxCheck(fovStepSize).withThreshold(threshold);
            TimeIntervalHandler<FOVDetector> fovHandler = new TimeIntervalHandler<>(startDate, endDate, fovDetec.g(initialState1), Action.CONTINUE);
            fovDetec = fovDetec.withHandler(fovHandler);
            prop1.addEventDetector(fovDetec);
            prop1.propagate(startDate, endDate);
            FOVDetector fovDetec2 = new FOVDetector(pt, SWOTlet_VNIR).withMaxCheck(fovStepSize).withThreshold(threshold);
            TimeIntervalHandler<FOVDetector> fovHandler2 = new TimeIntervalHandler<>(startDate, endDate, fovDetec2.g(initialState1), Action.CONTINUE);
            fovDetec2 = fovDetec2.withHandler(fovHandler2);
            prop2.addEventDetector(fovDetec2);
            prop2.propagate(startDate, endDate);
            TimeIntervalArray fovTimeArray = fovHandler.getTimeArray().createImmutable();
            if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                continue;
            }
            TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
            double[] riseandsets = merger.orCombine().getRiseAndSetTimesList();
            TimeIntervalArray fovTimeArray2 = fovHandler2.getTimeArray().createImmutable();
            TimeIntervalMerger merger2 = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray2);
            double[] riseandsets2 = merger2.orCombine().getRiseAndSetTimesList();
            double coverageTime = 0;
            double totalCoverageTime = 0;
            for (int j=0;j<riseandsets.length;j=j+2) {
            	for (int k=0;k<riseandsets2.length;k=k+2) {
            		if(riseandsets[j] < riseandsets2[k] && riseandsets[j+1] > riseandsets2[k]) {
            			if(riseandsets[j] < riseandsets2[k+1] && riseandsets[j+1] > riseandsets2[k+1]) {
            				coverageTime = coverageTime + riseandsets2[k+1] - riseandsets2[k];
            			}else {
            				coverageTime = coverageTime + riseandsets[j+1] - riseandsets2[k];
            			}
            		}else if(riseandsets[j] < riseandsets2[k+1] && riseandsets[j+1] > riseandsets2[k+1]) {
            			coverageTime = coverageTime + riseandsets2[k+1] - riseandsets[j];
            		}else if(riseandsets[j]>=riseandsets2[k] && riseandsets[j+1]<=riseandsets2[k+1]) {
            			coverageTime = coverageTime + riseandsets[j+1] - riseandsets[j];
            		}else {
            			coverageTime = coverageTime;
            		}
            	}
            	totalCoverageTime = totalCoverageTime + riseandsets[j+1]-riseandsets[j];
            }
            prop1.clearEventsDetectors();
            prop2.clearEventsDetectors();
            coverage = coverage + coverageTime/totalCoverageTime;
        }
        long end = System.nanoTime();
        System.out.printf("Took %.4f sec\n", (end - start) / Math.pow(10, 9));
        System.out.println(coverage);
        return coverage;
    }
    private static boolean lineOfSightPotential(CoveragePoint pt, Orbit orbit, double latitudeMargin) throws OrekitException {
        //this computation assumes that the orbit frame is in ECE
        double distance2Pt = pt.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition().getNorm();
        double distance2Sat = orbit.getPVCoordinates().getPosition().getNorm();
        double subtendedAngle = FastMath.acos(distance2Pt / distance2Sat);

        return FastMath.abs(pt.getPoint().getLatitude()) - latitudeMargin < subtendedAngle + orbit.getI();
    }
    //Calculate fitness
    public void calcFitness() {

        fitness = evaluationfunc(genes[0],genes[1]);
    }

}

//Population class
class Population {

    int popSize = 10;
    Individual[] individuals = new Individual[10];
    double fittest = 0;

    //Initialize population
    public void initializePopulation(int size) {
        for (int i = 0; i < individuals.length; i++) {
            individuals[i] = new Individual();
        }
    }

    //Get the fittest individual
    public Individual getFittest() {
        double maxFit = Double.MIN_VALUE;
        int maxFitIndex = 0;
        for (int i = 0; i < individuals.length; i++) {
            if (maxFit <= individuals[i].fitness) {
                maxFit = individuals[i].fitness;
                maxFitIndex = i;
            }
        }
        fittest = individuals[maxFitIndex].fitness;
        return individuals[maxFitIndex];
    }

    //Get the second most fittest individual
    public Individual getSecondFittest() {
        int maxFit1 = 0;
        int maxFit2 = 0;
        for (int i = 0; i < individuals.length; i++) {
            if (individuals[i].fitness > individuals[maxFit1].fitness) {
                maxFit2 = maxFit1;
                maxFit1 = i;
            } else if (individuals[i].fitness > individuals[maxFit2].fitness) {
                maxFit2 = i;
            }
        }
        return individuals[maxFit2];
    }

    //Get index of least fittest individual
    public int getLeastFittestIndex() {
        double minFitVal = Double.MAX_VALUE;
        int minFitIndex = 0;
        for (int i = 0; i < individuals.length; i++) {
            if (minFitVal >= individuals[i].fitness) {
                minFitVal = individuals[i].fitness;
                minFitIndex = i;
            }
        }
        return minFitIndex;
    }

    //Calculate fitness of each individual
    public void calculateFitness() {

        for (int i = 0; i < individuals.length; i++) {
            individuals[i].calcFitness();
        }
        getFittest();
    }
}
//	public static double[] linspace(double min, double max, int points) {  
//	    double[] d = new double[points];  
//	    for (int i = 0; i < points; i++){  
//	        d[i] = min + i * (max - min) / (points - 1);  
//	    }  
//	    return d;  
//	}  
//
//    public static void main(String[] args) {
//    	double[] h = linspace(400000,1000000,100);
//    	double[] i = linspace(0.1,180,90);
//    	for(int r=0;r<h.length;r++) {
//    		for(int s=0;s<i.length;s++) {
//    			double res = evaluationfunc(h[r],i[s]);
//    			System.out.printf("%f%f%s%f\n", h[r], i[s],"Score:", res);
//    		}
//    	}
//    }

