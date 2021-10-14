package seakers.orekit.SMDP;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;
import seakers.orekit.event.detector.FOVDetector;
import seakers.orekit.event.detector.TimeIntervalHandler;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.util.OrekitConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.lang.Double.parseDouble;

public class CloudGrid {
    public static void main(String[] args) throws IOException {
        OrekitConfig.init(4);
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("D:\\Documents\\VASSAR\\orekit\\CoverageDefinition1_Grid_Point_Information.csv"))) { // CHANGE THIS FOR YOUR IMPLEMENTATION
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }

        Set<GeodeticPoint> landPoints = new HashSet<>();
        for(int idx = 0; idx < records.size(); idx++) {
            double lat = Math.round(parseDouble(records.get(idx).get(0)));
            double lon = Math.round(parseDouble(records.get(idx).get(1)));
            if(lon > 180) {
                lon = lon-360.0;
            }
            lat = Math.toRadians(lat);
            lon = Math.toRadians(lon);
            GeodeticPoint landPoint = new GeodeticPoint(lat,lon,0.0);
            landPoints.add(landPoint);
        }
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", landPoints, earthShape);
        HashMap<GeodeticPoint,Double> rewardGrid = new HashMap<>();
        for (CoveragePoint pt : covDef1.getPoints()) {
            rewardGrid.put(pt.getPoint(),0.5);
        }
        FileWriter csvWriter = new FileWriter("CloudGrid.csv");
        Iterator hmIterator = rewardGrid.entrySet().iterator();
        int iter = 0;
        while (hmIterator.hasNext()) {
            iter = iter + 1;
            Map.Entry mapElement = (Map.Entry)hmIterator.next();
            GeodeticPoint pointOfInterest = (GeodeticPoint) mapElement.getKey();
            String apiKey = "eyJjaWQiOjIxNjQ4NTAsInMiOiIxNjA2NjA2Njg5IiwiciI6ODM4LCJwIjpbImRvd25sb2FkIiwib3JkZXIiXX0=";
            String scene = EarthExplorerAPI.sceneSearch(apiKey,pointOfInterest);
            double cloudCover = EarthExplorerAPI.sceneMetadata_CC(apiKey,scene)/100;
            csvWriter.append(String.valueOf(pointOfInterest.getLatitude())+","+String.valueOf(pointOfInterest.getLongitude())+","+String.valueOf(cloudCover));
            csvWriter.append("\n");
            System.out.println(iter);
        }
        csvWriter.flush();
        csvWriter.close();

    }
}
