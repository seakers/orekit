/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import seak.orekit.analysis.Analysis;
import seak.orekit.analysis.ephemeris.OrbitalElementsAnalysis;
import seak.orekit.constellations.Walker;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.object.Instrument;
import seak.orekit.object.Satellite;
import seak.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seak.orekit.object.linkbudget.LinkBudget;
import seak.orekit.propagation.PropagatorFactory;
import seak.orekit.propagation.PropagatorType;
import seak.orekit.scenario.Scenario;
import seak.orekit.scenario.ScenarioIO;
import seak.orekit.util.OrekitConfig;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seak.orekit.analysis.AbstractSpacecraftAnalysis;
import seak.orekit.analysis.CompoundSpacecraftAnalysis;
import seak.orekit.analysis.ephemeris.LifetimeAnalysis;
import seak.orekit.analysis.vectors.VectorAnalysis;
import seak.orekit.analysis.vectors.VectorAngleAnalysis2;
import seak.orekit.coverage.analysis.AnalysisMetric;
import seak.orekit.coverage.analysis.FastCoverageAnalysis;
import seak.orekit.coverage.analysis.GroundEventAnalyzer;
import seak.orekit.event.EventAnalysis;
import seak.orekit.event.EclipseIntervalsAnalysis;
import seak.orekit.event.EventAnalysisEnum;
import seak.orekit.event.EventAnalysisFactory;
import seak.orekit.event.FieldOfViewEventAnalysis;
import seak.orekit.object.Constellation;
import seak.orekit.object.CoveragePoint;
import seak.orekit.object.fieldofview.NadirRectangularFOV;
import seak.orekit.orbit.J2KeplerianOrbit;
//import seak.orekit.sensitivity.CoverageVersusOrbitalElements;

/**
 *
 * @author nozomihitomi
 */
public class Orekit_Pau_HYPMAS {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));
        long start = System.nanoTime();

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "analysis";
        }

        OrekitConfig.init(4);
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2021, 1, 1, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient
        
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        CelestialBody sun = CelestialBodyFactory.getSun();
        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame inertialFrame = FramesFactory.getEME2000();
        //Enter satellite orbital parameters
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400000;
        double i = FastMath.toRadians(51.64);

        ArrayList<Instrument> payload = new ArrayList<>();
        Collection<Satellite> constel = new ArrayList<>();
        Satellite sat1 = new Satellite("sat1", new KeplerianOrbit(a, 0.0001, i, 0, 0, 0, PositionAngle.MEAN, inertialFrame, startDate, mu), null, payload);
        constel.add(sat1);


        Constellation constellation = new Constellation("constel", constel);

        Properties propertiesPropagator = new Properties();
        propertiesPropagator.setProperty("orekit.propagator.mass", "10");
        propertiesPropagator.setProperty("orekit.propagator.atmdrag", "true");
        //propertiesPropagator.setProperty("orekit.propagator.dragarea", "0.14");
        //propertiesPropagator.setProperty("orekit.propagator.dragarea", "0.10485");
        propertiesPropagator.setProperty("orekit.propagator.dragarea", "0.130866");
        propertiesPropagator.setProperty("orekit.propagator.dragcoeff", "2.2");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.sun", "true");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.moon", "true");
        propertiesPropagator.setProperty("orekit.propagator.solarpressure", "false");
        propertiesPropagator.setProperty("orekit.propagator.solararea", "0.058");

        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2, propertiesPropagator);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.NUMERICAL, propertiesPropagator);

        //set the analyses
        double analysisTimeStep = 60;
        Collection<Analysis> analyses = new ArrayList<>();
        Collection<EventAnalysis> eventAnalyses = new ArrayList<>();
        for (final Satellite sat : constellation.getSatellites()) {
            EclipseIntervalsAnalysis eclEvent = new EclipseIntervalsAnalysis(startDate, endDate, inertialFrame, sat,  pf);
            eventAnalyses.add(eclEvent);
//            analyses.add(new LifetimeAnalysis(startDate, endDate, analysisTimeStep, sat, PositionAngle.MEAN, pf, earthShape));
//            analyses.add(new VectorAngleAnalysis2(startDate,endDate,analysisTimeStep,sat,pf,inertialFrame, sun) {
//                private static final long serialVersionUID = 4680062066885650976L;
//                @Override
//                public Vector3D getVector1(SpacecraftState currentState, Frame frame) throws OrekitException {
//                    return currentState.getPVCoordinates(frame).getVelocity();
//                }
//                @Override
//                public Vector3D getVector2(SpacecraftState currentState, Frame frame) throws OrekitException {
//                    Vector3D sunCoord = sun.getPVCoordinates(currentState.getDate(), frame).getPosition();
//                    Vector3D satCoord = currentState.getPVCoordinates(frame).getPosition();
//                    return new Vector3D(sunCoord.getX()-satCoord.getX(),
//                                        sunCoord.getY()-satCoord.getY(),
//                                        sunCoord.getZ()-satCoord.getZ());
//                }
//                @Override
//                public String getName() {
//                    return String.format("SCOUT_angle_velocity_sun_%s", sat.getName());
//                }
//            });
            analyses.add(new VectorAngleAnalysis2(startDate,endDate,analysisTimeStep,sat,pf,inertialFrame, sun) {
                private static final long serialVersionUID = 4680062066885650976L;
                @Override
                public Vector3D getVector1(SpacecraftState currentState, Frame frame) throws OrekitException {
                    Vector3D antiVelocity = currentState.getPVCoordinates(frame).getVelocity().normalize().negate();
                    Vector3D antinadir = currentState.getPVCoordinates(frame).getPosition().normalize();
                    Vector3D v2 = antinadir.crossProduct(antiVelocity).normalize();
                    return new Vector3D(antiVelocity.getX()*0.414213562373095+v2.getX(),
                                        antiVelocity.getY()*0.414213562373095+v2.getY(),
                                        antiVelocity.getZ()*0.414213562373095+v2.getZ());
                }
                @Override
                public Vector3D getVector2(SpacecraftState currentState, Frame frame) throws OrekitException {
                    Vector3D sunCoord = sun.getPVCoordinates(currentState.getDate(), frame).getPosition();
                    Vector3D satCoord = currentState.getPVCoordinates(frame).getPosition();
                    return new Vector3D(sunCoord.getX()-satCoord.getX(),
                                        sunCoord.getY()-satCoord.getY(),
                                        sunCoord.getZ()-satCoord.getZ());
                }
                @Override
                public String getName() {
                    return String.format("RAVAN2_angle_velocity_sun_%s", sat.getName());
                }
            });
            analyses.add(new VectorAngleAnalysis2(startDate,endDate,analysisTimeStep,sat,pf,inertialFrame, sun) {
                private static final long serialVersionUID = 4680062066885650976L;
                @Override
                public Vector3D getVector1(SpacecraftState currentState, Frame frame) throws OrekitException {
                    Vector3D antiVelocity = currentState.getPVCoordinates(frame).getVelocity().normalize().negate();
                    Vector3D antinadir = currentState.getPVCoordinates(frame).getPosition().normalize();
                    Vector3D v1 = antiVelocity.crossProduct(antinadir).normalize();
                    return new Vector3D(antiVelocity.getX()*0.414213562373095+v1.getX(),
                                        antiVelocity.getY()*0.414213562373095+v1.getY(),
                                        antiVelocity.getZ()*0.414213562373095+v1.getZ());
                }
                @Override
                public Vector3D getVector2(SpacecraftState currentState, Frame frame) throws OrekitException {
                    Vector3D sunCoord = sun.getPVCoordinates(currentState.getDate(), frame).getPosition();
                    Vector3D satCoord = currentState.getPVCoordinates(frame).getPosition();
                    return new Vector3D(sunCoord.getX()-satCoord.getX(),
                                        sunCoord.getY()-satCoord.getY(),
                                        sunCoord.getZ()-satCoord.getZ());
                }
                @Override
                public String getName() {
                    return String.format("RAVAN1_angle_velocity_sun_%s", sat.getName());
                }
            });
        }
        
//        Collection<Analysis> analyses = new ArrayList<>();        
//        for (final Satellite sat : constellation.getSatellites()) {
//            Collection<AbstractSpacecraftAnalysis> analyses2 = new ArrayList<>();
//            analyses2.add(new OrbitalElementsAnalysis(startDate, endDate, analysisTimeStep, sat, PositionAngle.MEAN,pf));
//            analyses2.add(new VectorAnalysis(startDate,endDate,analysisTimeStep,sat,pf,inertialFrame) {
//                private static final long serialVersionUID = 4680062066885650976L;
//                @Override
//                public Vector3D getVector(SpacecraftState currentState, Frame frame) throws OrekitException {
//                    return currentState.getPVCoordinates(frame).getPosition();
//                }
//                @Override
//                public String getName() {
//                    return String.format("position_%s", sat.getName());
//                }
//            });
//            analyses.add(new CompoundSpacecraftAnalysis(startDate, endDate,analysisTimeStep, sat, 
//                    pf, analyses2));
//        }
        
       
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                analysis(analyses).eventAnalysis(eventAnalyses).name("test1").propagatorFactory(pf).build();
        try {
            Logger.getGlobal().finer(String.format("Running Scenario %s", scen));
            Logger.getGlobal().finer(String.format("Number of satellites: %d", constellation.getSatellites().size()));
            scen.call();
        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau_HYPMAS.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }
        
        for (EventAnalysis eclEvent : eventAnalyses) {
            ScenarioIO.saveEclipseAnalysis(Paths.get(System.getProperty("results"), ""), filename, scen, (EclipseIntervalsAnalysis)eclEvent);
        }
        

        
        for (Analysis analysis : analyses) {
            if (analysis instanceof CompoundSpacecraftAnalysis){
                for (Analysis anal:((CompoundSpacecraftAnalysis) analysis).getAnalyses()){
                    ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                    String.format("%s_%s", filename, anal.getName()), anal);
                }
            } else{
                ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                    String.format("%s_%s", filename, analysis.getName()), analysis);
            }
        }
        
        for (Analysis analysis : analyses) {
            if (analysis instanceof LifetimeAnalysis){
                Logger.getGlobal().finest(String.format("Lifetime of %.2f years", ((LifetimeAnalysis) analysis).getLifetime()));
            }
        }
        long end = System.nanoTime();
        Logger.getGlobal().finest(String.format("Took %.4f sec", (end - start) / Math.pow(10, 9)));

        OrekitConfig.end();

    }

}
