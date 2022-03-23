/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.event.*;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.orbit.J2KeplerianOrbit;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
//import seak.orekit.sensitivity.CoverageVersusOrbitalElements;

/**
 *
 * @author nozomihitomi
 */
public class PowerSim {

    /**
     * @param args the command line arguments
     * @throws OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        Locale.setDefault(new Locale("en", "US"));
        long start = System.nanoTime();
        OrekitConfig.init(4);
        File orekitData = new File("./src/main/resources");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2020, 1, 2, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        //Enter satellite orbital parameters
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 600000.;
        double i = FastMath.toRadians(30.);

        ArrayList<Instrument> payload = new ArrayList<>();
        Orbit orbit = new J2KeplerianOrbit(a,0.0,i,0.0,0.0,0.0,PositionAngle.TRUE,inertialFrame,startDate,mu);
        Satellite satellite = new Satellite("Radarsat", orbit,payload);

        Properties propertiesPropagator = new Properties();
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN, propertiesPropagator);
        Properties propertiesEventAnalysis = new Properties();

        //set the event analyses
        ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
        EclipseIntervalsAnalysis eclipseEvents = new EclipseIntervalsAnalysis(startDate,endDate,inertialFrame,satellite,pf);
        eventanalyses.add(eclipseEvents);

        ArrayList<Analysis<?>> analyses = new ArrayList<>();

        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses).analysis(analyses).name("PowerSim").properties(propertiesEventAnalysis).
                propagatorFactory(pf).build();
        try {
            Logger.getGlobal().finer(String.format("Running Scenario %s", scen));
            scen.call();
        } catch (Exception ex) {
            Logger.getLogger(PowerSim.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        for (Analysis<?> analysis : analyses) {
            ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                    String.format("%s_%s", scen.toString(), "analysis"), analysis);
        }
        TimeIntervalArray eclipseIntervals = eclipseEvents.getTimeIntervalArray();
        double[] riseSets = eclipseIntervals.getRiseAndSetTimesList();
        double simDuration = endDate.durationFrom(startDate);
        double batteryCapacity = 373*3600; // Ws
        double depthOfDischarge = 0.4;
        double batteryPower = 373*3600;
        double solarArrayPower = 501; // W
        double idlePower = 243; // W
        double payloadOnPower = 443; // W
        boolean daylight = true;
        boolean chargeMode = false;
        ArrayList<Double> powerTracking = new ArrayList<>();
        ArrayList<Integer> eclipseTracking = new ArrayList<>();
        int payloadTimeOn = 0;
        int counter = 2;
        for(int j = 0; j < (int)simDuration; j++) {
            if (j > riseSets[counter]) {
                counter = counter+1;
                if(daylight){
                    daylight = false;
                } else {
                    daylight = true;
                }
            }
            if(daylight){
                eclipseTracking.add(1);
            } else {
                eclipseTracking.add(0);
            }
            if(daylight){
                if(chargeMode) {
                    batteryPower = batteryPower + solarArrayPower - idlePower;
                } else {
                    batteryPower = batteryPower + solarArrayPower - payloadOnPower;
                    payloadTimeOn = payloadTimeOn + 1;
                }
            } else {
                if(chargeMode) {
                    batteryPower = batteryPower - idlePower;
                } else {
                    batteryPower = batteryPower - payloadOnPower;
                    payloadTimeOn = payloadTimeOn + 1;
                }
            }
            if(batteryPower > batteryCapacity) {
                batteryPower = batteryCapacity;
                chargeMode = false;
            }
            if(batteryPower < batteryCapacity*(1-depthOfDischarge)) {
                chargeMode = true;
            }
            powerTracking.add(batteryPower);
            if(counter >= riseSets.length)
                break;
        }
        System.out.println("Payload on for " + payloadTimeOn + " seconds");
        try{
            FileWriter csvWriter = new FileWriter("./src/test/output/power_sim.csv");
            for (int r = 0; r<powerTracking.size(); r++) {
                String rowData = Double.toString(powerTracking.get(r))+","+Double.toString(eclipseTracking.get(r));
                csvWriter.append(rowData);
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (Exception e) {
            System.out.println("Error writing to CSV");
        }
        long end = System.nanoTime();
        System.out.println("bruh i'm done");
        Logger.getGlobal().finest(String.format("Took %.4f sec", (end - start) / Math.pow(10, 9)));

        OrekitConfig.end();
    }
    public static String getListAsCsvString(ArrayList<Double> list){

        StringBuilder sb = new StringBuilder();
        for(Double str:list){
            if(sb.length() != 0){
                sb.append(",");
            }
            sb.append(Double.toString(str));
        }
        return sb.toString();
    }

}
