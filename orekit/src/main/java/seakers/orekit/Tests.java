/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hipparchus.util.FastMath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import seakers.orekit.analysis.Record;
import seakers.orekit.analysis.ephemeris.GroundTrackAnalysis;
import seakers.orekit.object.Satellite;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.constellations.EnumerateConstellations;
import seakers.orekit.constellations.Walker;
import seakers.orekit.constellations.WalkerParameters;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.EventAnalysisEnum;
import seakers.orekit.event.EventAnalysisFactory;
import seakers.orekit.event.FieldOfViewEventAnalysis;
import seakers.orekit.object.CoverageDefinition;
import static seakers.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;

/**
 *
 * @author paugarciabuzzi
 */
public class Tests {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        OrekitConfig.init(1);
        String overlapFilePath = "./orekit/src/main/java/seakers/orekit/overlap/";
        File orekitData = new File("D:/Documents/VASSAR/orekit/orekit/resources");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        TimeScale utc = TimeScalesFactory.getUTC();
        double mu = Constants.WGS84_EARTH_MU;
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 10, 30, 00.000, utc);
        double duration = 0.1;
        Orbit swotOrbit = new KeplerianOrbit(6378000+891000, 0.0, FastMath.toRadians(78), 0.0, 0.0, 0.0, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Collection<Instrument> swotPayload = new ArrayList<>();
        double swotCrossFOVRadians = Math.atan(60.0/891.0);
        double swotAlongFOVRadians = Math.atan(400.0/891.0);
        NadirRectangularFOV swotFOV = new NadirRectangularFOV(swotCrossFOVRadians,swotAlongFOVRadians,0.0,earthShape);
        Instrument swotAltimeter = new Instrument("SWOT Altimeter", swotFOV, 100.0, 100.0);
        swotPayload.add(swotAltimeter);
        Satellite SWOT = new Satellite("SWOT", swotOrbit, swotPayload);
        Collection<Record<String>> coll = getGroundTrack(SWOT.getOrbit(), duration, startDate);
        for (Record<String> ind : coll) {
            String rawString = ind.getValue();
            System.out.println(rawString);
        }
        OrekitConfig.end();
    }
    public static Collection<Record<String>> getGroundTrack(Orbit orbit, double duration, AbsoluteDate startDate) {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate endDate = startDate.shiftedBy(duration*86400);

        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        ArrayList<Instrument> payload = new ArrayList<>();
        Satellite sat1 = new Satellite(orbit.toString(), orbit,  payload);
        Properties propertiesPropagator = new Properties();
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);


        Collection<Analysis<?>> analyses = new ArrayList<>();
        double analysisTimeStep = 5;
        GroundTrackAnalysis gta = new GroundTrackAnalysis(startDate, endDate, analysisTimeStep, sat1, earthShape, pf);
        analyses.add(gta);
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                analysis(analyses).name(orbit.toString()).propagatorFactory(pf).build();
        try {
            scen.call();
        } catch (Exception ex) {
            throw new IllegalStateException("Ground track scenario failed to complete.");
        }
        return gta.getHistory();
    }
    
}
