/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit;

import java.nio.file.Paths;
import java.util.ArrayList;
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
import seak.orekit.coverage.access.TimeIntervalArray;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.object.CoveragePoint;
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
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;
import seak.orekit.analysis.ephemeris.HohmannTransferAnalysis;
import seak.orekit.analysis.vectors.VectorAnalisysEclipseSunlightDiffDrag;
import seak.orekit.coverage.analysis.AnalysisMetric;
import seak.orekit.coverage.analysis.GroundEventAnalyzer;
import seak.orekit.event.EventAnalysis;
import seak.orekit.event.EventAnalysisEnum;
import seak.orekit.event.EventAnalysisFactory;
import seak.orekit.event.FieldOfViewEventAnalysis;
import seak.orekit.event.GroundBodyAngleEventAnalysis;
import seak.orekit.object.Constellation;
import static seak.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA;
import static seak.orekit.object.CoverageDefinition.GridStyle.UNIFORM;
import seak.orekit.object.OrbitWizard;
import seak.orekit.object.fieldofview.NadirRectangularFOV;
import seak.orekit.propagation.PropagatorFactory;
/**
 *
 * @author paugarciabuzzi
 */
public class LTAN2RAAN {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));
        OrekitConfig.init();
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        
        double h=600000;
        double inc=OrbitWizard.SSOinc(Constants.WGS84_EARTH_EQUATORIAL_RADIUS+h, 0);
        AbsoluteDate date=new AbsoluteDate(2020, 1, 2, 06, 30, 00.000, utc);
        Orbit SSO = new KeplerianOrbit(Constants.WGS84_EARTH_EQUATORIAL_RADIUS+h, 0.0001, FastMath.toRadians(inc),0.0,
                FastMath.toRadians(197.811), 0.0, PositionAngle.MEAN, inertialFrame, date, mu);
        GeodeticPoint p = new GeodeticPoint(0, 0, 0);
        CoveragePoint point=new CoveragePoint(earthShape, p, "");
        Vector3D pt1=SSO.getPVCoordinates().getPosition();
        Vector3D pt2=point.getPVCoordinates(date, inertialFrame).getPosition();
        double angle=Vector3D.angle(pt1, pt2);
        
        Logger.getGlobal().finest(String.format("ANGLE=%.4f", angle));
        
    }
    
}
