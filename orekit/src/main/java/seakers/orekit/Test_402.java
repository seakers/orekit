package seakers.orekit;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.ReflectometerEventAnalysis;
import seakers.orekit.object.*;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.object.fieldofview.OffNadirRectangularFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static seakers.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA;

public class Test_402 {

    static public void main(String[] args) {
        //initializes the look up tables for planeteary position (required!)
        OrekitConfig.init(4);

        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));

        File orekitData = new File("orekit\\resources");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "tropics";
        }

        //setup logger
        Level level = Level.FINER;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2021, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2021, 1, 1, 12, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        // define orbits
        double a = 6771*1e3;
        double e = 0.0001;
        double i = FastMath.toRadians(98);
        double w = 0.0;

        double raan1 = 0;
        double raan2 = 0;
        double raan3 = FastMath.toRadians(90);
        double raan4 = FastMath.toRadians(90);

        double anom1 = 0;
        double anom2 = FastMath.toRadians(180);
        double anom3 = FastMath.toRadians(90);
        double anom4 = FastMath.toRadians(270);

        Orbit orb1 = new KeplerianOrbit(a, e, i, w, raan1, anom1, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Orbit orb2 = new KeplerianOrbit(a+1e3, e, i, w, raan1, anom1, PositionAngle.MEAN, inertialFrame, startDate, mu);
        OffNadirRectangularFOV fov1 = new OffNadirRectangularFOV(FastMath.toRadians(0), FastMath.toRadians(30),FastMath.toRadians(15),0,earthShape);
        OffNadirRectangularFOV fov2 = new OffNadirRectangularFOV(FastMath.toRadians(0), FastMath.toRadians(30),FastMath.toRadians(30),0,earthShape);

        ArrayList<Instrument> payload1 = new ArrayList<>();
        Instrument view11 = new Instrument("view1", fov1, 100, 100);
        payload1.add(view11);

        ArrayList<Satellite> satellitesRx =new ArrayList<>();
        ArrayList<Satellite> satellitesTx =new ArrayList<>();

        HashSet<CommunicationBand> satBands = new HashSet<>();
        satBands.add(CommunicationBand.UHF);

        Satellite satRx = new Satellite("sat1", orb1, null, payload1,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);
        Satellite satTx = new Satellite("sat1", orb2, null, payload1,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

        satellitesRx.add(satRx);
        satellitesTx.add(satTx);
        Constellation constelRx = new Constellation ("Reflectometers",satellitesRx);
        Constellation constelTx = new Constellation ("GPS", satellitesTx);

        ArrayList<Constellation> constellations = new ArrayList<>();
        constellations.add(constelRx);

        // coverage definition
        CoverageDefinition covDef = new CoverageDefinition("covdef1", 9, earthShape, EQUAL_AREA);
        covDef.assignConstellation(constellations);
        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef);

        // set up propagator
        Properties propertiesPropagator = new Properties();
        PropagatorFactory pfJ2 = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);

        //can set the properties of the analyses
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");
        propertiesEventAnalysis.setProperty("fov.saveAccess", "true");

        // Script Test
        ArrayList<EventAnalysis> eventAnalysesTest = new ArrayList<>();
        ReflectometerEventAnalysis refEventTest = new ReflectometerEventAnalysis(startDate, endDate,
                inertialFrame, covDefs, pfJ2, true, true, constelTx);
        eventAnalysesTest.add(refEventTest);


        ArrayList<Analysis<?>> analyses = new ArrayList<>();

        Scenario scenTest = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventAnalysesTest).analysis(analyses).
                covDefs(covDefs).name("ReflectometerTest").properties(propertiesEventAnalysis).
                propagatorFactory(pfJ2).build();

        try {
            long start1 = System.nanoTime();
            scenTest.call();
            long end1 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end1 - start1) / Math.pow(10, 9)));

        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""),
                String.format("%s_%s","ReflectometerTest","coverage"), scenTest, covDef, refEventTest);

        OrekitConfig.end();
    }
}
