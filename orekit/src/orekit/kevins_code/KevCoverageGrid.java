package orekit.kevins_code;

import java.util.ArrayList;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
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
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldOfView;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.EventHandler.Action;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

//CoverageGrid Object that given granularity, planet, maxcheck, threshold, elevation angle, propagator, 
//initialDate, and scenariotime, generates ground stations that will print out maxmax revisiti time, 
//avgmax revisit time, and avgavg revisit time. Currently runtime for a single satellite is about 23 minutes.

public class KevCoverageGrid {
	ArrayList<AbsoluteDate> begintime = new ArrayList<AbsoluteDate>();					//list of times when gap starts for all stations
	ArrayList<ArrayList<Double>> allStationTimes = new ArrayList<ArrayList<Double>>();	//ArrayList of ArrayList of all gap times for each station
	int stationCount;												//number of stations
	double scenariotime;											//length of simulation
	AbsoluteDate initialDate;										//start date
	boolean[] inview;												//Array of booleans tracking if the satellite is in view for each ground station, used for end of scenario
	ArrayList<String> maxTimes = new ArrayList<String>(); //Delete This eventually (used for debugging)
	ArrayList<String> avgTimes = new ArrayList<String>(); //Delete This eventually (used for debugging)

	public KevCoverageGrid( double granularity, BodyShape planet, double maxcheck, double threshold, double elevation, Propagator prop, AbsoluteDate initialDate, double scenariotime){
		stationCount = 0;
		this.scenariotime = scenariotime;
		this.initialDate = initialDate;
		int latcount = (int)(90/granularity+.001);
		for (double lat=-latcount*granularity; lat<=90.001; lat = lat+granularity){		//Creates grid of stations
			int satsAtLat = (int)(360/granularity*FastMath.cos(FastMath.toRadians(lat))+.001);
			for (double lon=0; lon<=359.999; lon=lon+360.0/satsAtLat){
				stationCount++;
				double longitude = FastMath.toRadians(lon);
				double latitude = FastMath.toRadians(lat);
				double altitude = 0.;
				GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
				TopocentricFrame staFrame = new TopocentricFrame(planet, station, "stationlat" + lat + "lon" + lon +"*"+stationCount);
				System.out.println("lat"+lat+"lon"+lon+"stationcCount"+stationCount);
				EventDetector staVisi =
						new ElevationDetector(maxcheck, threshold, staFrame).
						withConstantElevation(elevation).
						withHandler(new VisibilityHandler());
				prop.addEventDetector(staVisi);
			}
		}
		inview = new boolean[stationCount];
		for (int n=0; n<stationCount; n++){
			inview[n]=false;
		}

		//		stationCount++;							//Adds one for stations at pole, already starts at 1
		System.out.println("Number of Stations " + stationCount);

		// Creating ArrayList for time trackers
		for (int n=0; n<stationCount; n++){
			begintime.add( initialDate );
		}

		// Creating ArrayList for station data
		for (int n=0; n<stationCount; n++){
			allStationTimes.add( new ArrayList<Double>() );
		}
	}

	//Adds last gap time at the end of scenario. The use of this method is optional, depends on if you want to include the last gap
	public void endOfScenario(){
		for (int n=0; n<allStationTimes.size(); n++){
			if (inview[n] == false)
				allStationTimes.get(n).add(initialDate.shiftedBy(scenariotime).durationFrom(begintime.get(n)));
		}
	}

	//Calculates orbit statistics
	public void calculateStats(){
		double avgsum = 0.0;
		double maxmax = -1.0;
		double maxsum = 0.0;
		for (int n=0; n<stationCount; n++){
			double sum = 0.0;
			double max = -1.0;
//			System.out.println(allStationTimes.get(n));
			for ( int n2=0; n2<allStationTimes.get(n).size(); n2++ ){
				sum = sum + allStationTimes.get(n).get(n2);
				if ( max < allStationTimes.get(n).get(n2) )
					max = allStationTimes.get(n).get(n2);
			}
			double avg;
			if (allStationTimes.get(n).size()>0)
				avg = sum/allStationTimes.get(n).size();
			else
				avg = scenariotime;
			if (max < 0.)
				max = scenariotime;
			maxTimes.add("Station"+(n+1)+" Maxtime \t\t"+max);
			avgTimes.add("Station"+(n+1)+" AvgTime \t\t"+avg);
//			System.out.println("Station"+(n+1));
//			System.out.println("Average revisit time: " + avg);
//			System.out.println("Maximum revisit time: " + max);
//			System.out.println();
			maxsum = maxsum + max;
			avgsum = avgsum + avg;
			if (maxmax < max)
				maxmax = max;
		}
		double avgavg = avgsum/stationCount;
		double avgmax = maxsum/stationCount;
		System.out.println( "Avgavg Revisit Time " + avgavg);
		System.out.println("Maxmax Revisit Time " + maxmax);
		System.out.println("AvgMax Revisit Time " + avgmax);
		writeData( avgTimes, "C:/Users/SEAK1/Kevin/testtext.txt");
	}

	//	private static class VisibilityHandler implements EventHandler<ElevationDetector> {
	private class VisibilityHandler implements EventHandler<ElevationDetector> {

		public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
				final boolean increasing) {
			int asterixIndex = detector.getTopocentricFrame().getName().indexOf('*');
			//			System.out.println(detector.getTopocentricFrame().getName());
			//			System.out.println(asterixIndex);
			int n = Integer.parseInt(detector.getTopocentricFrame().getName().substring(asterixIndex+1));
			if (increasing) {									//Access between station and satellite begins
				allStationTimes.get(n-1).add(s.getDate().durationFrom(begintime.get(n-1)));
				inview[n-1] = true;								//in view
				//				begintime.set(n-1, s.getDate());													//REMOVE THIS
				//				System.out.println("In"+n+"DATE"+s.getDate());
			} else {											//Access ends
				begintime.set(n-1, s.getDate());				//Sets begintime to new start of gap
				inview[n-1] = false;							//no longer in view
				//				System.out.println("Out"+n+"DATE"+s.getDate());
			}

			return Action.CONTINUE;
		}

	}

	//Writes arr to a text file, used for debugging
	public void writeData( ArrayList<String> arr, String filename) {
		BufferedWriter bw = null;
		try{
			File file = new File(filename);
		
		if (!file.exists()) {
			file.createNewFile();
		}
		FileWriter fw = new FileWriter(file);
		bw = new BufferedWriter(fw);
		for (int k=0; k< arr.size(); k++){
			bw.write(arr.get(k).toString());
			bw.newLine();
		}
		System.out.println("File written Successfully");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		finally
		{ 
			try{
				if(bw!=null)
					bw.close();
			}catch(Exception ex){
				System.out.println("Error in closing the BufferedWriter"+ex);
			}
		}
	}
}