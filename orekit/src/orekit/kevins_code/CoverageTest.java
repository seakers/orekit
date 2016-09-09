/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package orekit.kevins_code;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

//import CoverageTest.VisibilityHandler;
//import fr.cs.examples.Autoconfiguration;

//Simulation of a single satellite orbit. Oribt parameters, date, length of simulation, and propagator can all be adjusted within the code
public class CoverageTest {
	//	static ArrayList<AbsoluteDate> begintime = new ArrayList<AbsoluteDate>();
	//	static ArrayList<ArrayList<Double>> allStationTimes = new ArrayList<ArrayList<Double>>();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			long startTime = System.nanoTime();					//Used to measure program runtime
			//			String mystr = "yooo*334";
			//			for( int k=1; k<=1000; k++){
			//				int asterixIndex = mystr.indexOf('*');
			//				int n = Integer.parseInt(mystr.substring(asterixIndex+1));
			//				System.out.println(n);
			//			}
			// configure Orekit
//			Autoconfiguration.configureOrekit();

			//  Initial state definition : date, orbit
			AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());    //initial orbit time
			double mu =  3.986004415e+14; // gravitation coefficient
			Frame inertialFrame = FramesFactory.getEME2000(); // inertial frame for orbit definition
			//			Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
			//			Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
			//PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
			//Orbit initialOrbit = new KeplerianOrbit(pvCoordinates, inertialFrame, initialDate, mu);


			//Orbit parameters, can be adjusted
			double a = 6971000.0;
			double e = 0.;
			double i = FastMath.toRadians(97.7592);
			double argofperigee = 0.;
			double raan = 0.;
			double anomaly = 0.;
			Orbit initialOrbit = new KeplerianOrbit(a,e,i,argofperigee,raan,anomaly,PositionAngle.TRUE, inertialFrame, initialDate, mu);
			//			

			// Propagator : could be a simple KeplerainPropagator, EcksteinHechlerPropagator, or other propagator supported by Orekit
			//			Propagator prop = new KeplerianPropagator(initialOrbit);
			Propagator prop = new EcksteinHechlerPropagator(initialOrbit, Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS, 
					Constants.EIGEN5C_EARTH_MU, Constants.EIGEN5C_EARTH_C20, Constants.EIGEN5C_EARTH_C30,
					Constants.EIGEN5C_EARTH_C40, Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);
			
//			// steps limits
//			final double minStep  = 0.001;
//			final double maxStep  = 1000;
//			final double initStep = 60;
//
//			// error control parameters (absolute and relative)
//			final double positionError = 10.0;
//			final double[][] tolerances = NumericalPropagator.tolerances(positionError, initialOrbit, initialOrbit.getType());
//
//			// set up mathematical integrator
//			AdaptiveStepsizeIntegrator integrator =
//			    new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);
//			integrator.setInitialStepSize(initStep);
//
//			// set up space dynamics propagator
//			Propagator prop = new NumericalPropagator((ODEIntegrator) integrator);
			
			// Earth and frame
			Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
			BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
					Constants.WGS84_EARTH_FLATTENING,
					earthFrame);

			// Station
			//			final double longitude = FastMath.toRadians(45.);
			//			final double latitude  = FastMath.toRadians(25.);
			//			final double altitude  = 0.;
			//			final GeodeticPoint station1 = new GeodeticPoint(latitude, longitude, altitude);
			//			final TopocentricFrame sta1Frame = new TopocentricFrame(earth, station1, "station1");
			//
			//			// Station 2
			//			final double longitude2 = FastMath.toRadians(37.);
			//			final double latitude2  = FastMath.toRadians(-50.);
			//			final double altitude2  = 0.;
			//			final GeodeticPoint station2 = new GeodeticPoint(latitude2, longitude2, altitude2);
			//			final TopocentricFrame sta1Frame2 = new TopocentricFrame(earth, station2, "station2");
                        
			// Event definition
			final double maxcheck  = 60.0;
			final double threshold =  0.001;
			//			final double elevation = FastMath.toRadians(5.0);
			final double nadir = FastMath.toRadians(55.0);
			double sinrho = Constants.WGS84_EARTH_EQUATORIAL_RADIUS/(a);
			final double elevation = FastMath.acos( FastMath.sin(nadir)/sinrho );
			//			System.out.println(FastMath.toDegrees(elevation));
			//			PVCoordinatesProvider  ear = CelestialBodyFactory.getSun();
			//			System.out.println(ear.getPVCoordinates(initialDate, inertialFrame));
			//			final EventDetector sta1Visi =
			//					new CircularFieldOfViewDetector(600, stalFrame, new FieldOfView);
			//					new FieldOfView(Vector3D center, Vector3D meridian, double insideRadius, int n, double margin);
			//			final EventDetector sta1Visi =
			//					new ElevationDetector(maxcheck, threshold, sta1Frame).
			//					withConstantElevation(elevation).
			//					withHandler(new VisibilityHandler());
			//
			//			// Event Detector 2
			//			final EventDetector sta1Visi2 =
			//					new ElevationDetector(maxcheck, threshold, sta1Frame2).
			//					withConstantElevation(elevation).
			//					withHandler(new VisibilityHandler());

			// Add event to be detected
			//			kepler.addEventDetector(sta1Visi);
			//			EcksteinHechler.addEventDetector(sta1Visi);
			//
			//			// Add Second
			//			EcksteinHechler.addEventDetector(sta1Visi2);

			//			int stationCount = 1;								
			//			for (double lon=-90; lon<=90.001; lon = lon+6){
			//				int satsAtLon = (int)(60*FastMath.cos(FastMath.toRadians(lon))+.001);
			//				stationCount = stationCount + satsAtLon;
			//				for (double lat=0; lat<=359.999; lat=lat+360.0/satsAtLon){
			//					double longitude = FastMath.toRadians(lon);
			//					double latitude = FastMath.toRadians(lat);
			//					double altitude = 0.;
			//					GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
			//					TopocentricFrame staFrame = new TopocentricFrame(earth, station, "stationlat" + lat + "lon" + lon +"*"+stationCount);
			//					EventDetector staVisi =
			//							new ElevationDetector(maxcheck, threshold, staFrame).
			//							withConstantElevation(elevation).
			//							withHandler(new VisibilityHandler());
			//					EcksteinHechler.addEventDetector(staVisi);
			//				}
			//			}
			//			stationCount+=1;							//Adds one for stations at pole, already starts at 1
			//			System.out.println("Number of Stations" + stationCount);
			//			
			//			// Creating ArrayList for time trackers
			//			for (int n=0; n<stationCount; n++){
			//				begintime.add( initialDate );
			//			}
			//
			//			// Creating ArrayList for station data
			//			for (int n=0; n<stationCount; n++){
			//				allStationTimes.add( new ArrayList<Double>() );
			//			}


			System.out.println("propagate starting");
			// Propagate from the initial date to the first raising or for the fixed duration
			//			SpacecraftState finalState = kepler.propagate(initialDate.shiftedBy(7*86400.));
			double scenariotime = 14*86400.;					//Length of simulation
			KevCoverageGrid mygrid = new KevCoverageGrid( 6.0, earth, maxcheck, threshold, elevation, prop, initialDate, scenariotime);
			SpacecraftState finalState = prop.propagate(initialDate.shiftedBy(scenariotime));
			System.out.println("propagate done");

			mygrid.endOfScenario();
			mygrid.calculateStats();
			//			for (int n=0; n<stationCount; n++){
			//				double sum = 0.0;
			//				double max = 0.0;
			//				System.out.println(allStationTimes.get(n));
			//				for ( int n2=0; n2<allStationTimes.get(n).size(); n2++ ){
			//					sum = sum + allStationTimes.get(n).get(n2);
			//					if ( max < allStationTimes.get(n).get(n2) )
			//						max = allStationTimes.get(n).get(n2);
			//				}
			//				double avg;
			//				if (allStationTimes.get(n).size()>0)
			//					avg = sum/allStationTimes.get(n).size();
			//				else
			//					avg = scenariotime;
			//				System.out.println("Average revisit time: " + avg);
			//				System.out.println("Maximum revisit time: " + max);
			//				System.out.println(" Final state : " + finalState.getDate().durationFrom(initialDate));
			//			}

			//			System.out.println(allStationTimes.get(0));
			//			double sum = 0.0;
			//			double max = allStationTimes.get(0).get(0);
			//			for ( int n=0; n<allStationTimes.get(0).size(); n++ ){
			//				sum = sum + allStationTimes.get(0).get(n);
			//				if ( max < allStationTimes.get(0).get(n) )
			//					max = allStationTimes.get(0).get(n);
			//			}
			//			double avg = sum/allStationTimes.get(0).size();
			//			System.out.println("Average revisit time: " + avg);
			//			System.out.println("Maximum revisit time: " + max);
			//			System.out.println(" Final state : " + finalState.getDate().durationFrom(initialDate));
			//
			//			//Second
			//			System.out.println(allStationTimes.get(1));
			//			sum = 0.0;
			//			max = allStationTimes.get(1).get(0);
			//			for ( int n=0; n<allStationTimes.get(1).size(); n++ ){
			//				sum = sum + allStationTimes.get(1).get(n);
			//				if ( max < allStationTimes.get(1).get(n) )
			//					max = allStationTimes.get(1).get(n);
			//			}
			//			avg = sum/allStationTimes.get(1).size();
			//			System.out.println("Average revisit time 2: " + avg);
			//			System.out.println("Maximum revisit time 2: " + max);
			System.out.println(" Final state : " + finalState.getDate().durationFrom(initialDate));
			long endTime = System.nanoTime();
			System.out.println("Took "+(endTime - startTime) + " ns"); 
		} catch (OrekitException oe) {
			System.err.println(oe.getMessage());
		}

	}

	//	private static class VisibilityHandler implements EventHandler<ElevationDetector> {
	//
	//		public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
	//				final boolean increasing) {
	//			int asterixIndex = detector.getTopocentricFrame().getName().indexOf('*');
	//			//			System.out.println(detector.getTopocentricFrame().getName());
	//			//			System.out.println(asterixIndex);
	//			int n = Integer.parseInt(detector.getTopocentricFrame().getName().substring(asterixIndex+1));
	//			if (increasing) {
	//				begintime.set(n-1, s.getDate());
	//			} else {
	//				allStationTimes.get(n-1).add(s.getDate().durationFrom(begintime.get(n-1)));
	//			}
	//
	//			return Action.CONTINUE;
	//		}
	//
	//	}

	//	private static class VisibilityHandler implements EventHandler<ElevationDetector> {
	//
	//		public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
	//				final boolean increasing) {
	//			if (increasing) {
	//				System.out.println(" Visibility on " + detector.getTopocentricFrame().getName()
	//						+ " begins at " + s.getDate());
	//				return Action.CONTINUE;
	//			} else {
	//				System.out.println(" Visibility on " + detector.getTopocentricFrame().getName()
	//						+ " ends at " + s.getDate());
	//				return Action.CONTINUE;
	//			}
	//		}
	//
	//	}

}
