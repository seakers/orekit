/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit;

import java.util.ArrayList;
import java.util.Collection;
import org.orekit.bodies.GeodeticPoint;

/**
 * For testing purposes only
 *
 * @author nozomihitomi
 */
public class STKGRID {

    public static Collection<GeodeticPoint> getPoints() {
        ArrayList<GeodeticPoint> points = new ArrayList<>();
        
        points.add(new GeodeticPoint(-1.570796, 0.000000, 0.0));
//        points.add(new GeodeticPoint(-1.221730, 0.523599, 0.0));
//        points.add(new GeodeticPoint(-1.221730, 1.570796, 0.0));
//        points.add(new GeodeticPoint(-1.221730, 2.617994, 0.0));
//        points.add(new GeodeticPoint(-1.221730, 3.665191, 0.0));
//        points.add(new GeodeticPoint(-1.221730, 4.712389, 0.0));
//        points.add(new GeodeticPoint(-1.221730, 5.759587, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 0.209440, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 0.628319, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 1.047198, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 0.209440, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 0.628319, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 1.047198, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 0.209440, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 0.628319, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 1.047198, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 1.466077, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 1.884956, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 2.303835, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 1.466077, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 1.884956, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 2.303835, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 1.466077, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 1.884956, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 2.303835, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 2.722714, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 3.141593, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 3.560472, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 2.722714, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 3.141593, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 3.560472, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 2.722714, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 3.141593, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 3.560472, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 3.979351, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 4.398230, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 4.817109, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 3.979351, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 4.398230, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 4.817109, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 3.979351, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 4.398230, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 4.817109, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 5.235988, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 5.654867, 0.0));
//        points.add(new GeodeticPoint(-0.872665, 6.073746, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 5.235988, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 5.654867, 0.0));
//        points.add(new GeodeticPoint(-0.523599, 6.073746, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 5.235988, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 5.654867, 0.0));
//        points.add(new GeodeticPoint(-0.174533, 6.073746, 0.0));
//        points.add(new GeodeticPoint(0.174533, 0.209440, 0.0));
//        points.add(new GeodeticPoint(0.174533, 0.628319, 0.0));
//        points.add(new GeodeticPoint(0.174533, 1.047198, 0.0));
//        points.add(new GeodeticPoint(0.523599, 0.209440, 0.0));
//        points.add(new GeodeticPoint(0.523599, 0.628319, 0.0));
//        points.add(new GeodeticPoint(0.523599, 1.047198, 0.0));
//        points.add(new GeodeticPoint(0.872665, 0.209440, 0.0));
//        points.add(new GeodeticPoint(0.872665, 0.628319, 0.0));
//        points.add(new GeodeticPoint(0.872665, 1.047198, 0.0));
//        points.add(new GeodeticPoint(0.174533, 1.466077, 0.0));
//        points.add(new GeodeticPoint(0.174533, 1.884956, 0.0));
//        points.add(new GeodeticPoint(0.174533, 2.303835, 0.0));
//        points.add(new GeodeticPoint(0.523599, 1.466077, 0.0));
//        points.add(new GeodeticPoint(0.523599, 1.884956, 0.0));
//        points.add(new GeodeticPoint(0.523599, 2.303835, 0.0));
//        points.add(new GeodeticPoint(0.872665, 1.466077, 0.0));
//        points.add(new GeodeticPoint(0.872665, 1.884956, 0.0));
//        points.add(new GeodeticPoint(0.872665, 2.303835, 0.0));
//        points.add(new GeodeticPoint(0.174533, 2.722714, 0.0));
//        points.add(new GeodeticPoint(0.174533, 3.141593, 0.0));
//        points.add(new GeodeticPoint(0.174533, 3.560472, 0.0));
//        points.add(new GeodeticPoint(0.523599, 2.722714, 0.0));
//        points.add(new GeodeticPoint(0.523599, 3.141593, 0.0));
//        points.add(new GeodeticPoint(0.523599, 3.560472, 0.0));
//        points.add(new GeodeticPoint(0.872665, 2.722714, 0.0));
//        points.add(new GeodeticPoint(0.872665, 3.141593, 0.0));
//        points.add(new GeodeticPoint(0.872665, 3.560472, 0.0));
//        points.add(new GeodeticPoint(0.174533, 3.979351, 0.0));
//        points.add(new GeodeticPoint(0.174533, 4.398230, 0.0));
//        points.add(new GeodeticPoint(0.174533, 4.817109, 0.0));
//        points.add(new GeodeticPoint(0.523599, 3.979351, 0.0));
//        points.add(new GeodeticPoint(0.523599, 4.398230, 0.0));
//        points.add(new GeodeticPoint(0.523599, 4.817109, 0.0));
//        points.add(new GeodeticPoint(0.872665, 3.979351, 0.0));
//        points.add(new GeodeticPoint(0.872665, 4.398230, 0.0));
//        points.add(new GeodeticPoint(0.872665, 4.817109, 0.0));
//        points.add(new GeodeticPoint(0.174533, 5.235988, 0.0));
//        points.add(new GeodeticPoint(0.174533, 5.654867, 0.0));
//        points.add(new GeodeticPoint(0.174533, 6.073746, 0.0));
//        points.add(new GeodeticPoint(0.523599, 5.235988, 0.0));
//        points.add(new GeodeticPoint(0.523599, 5.654867, 0.0));
//        points.add(new GeodeticPoint(0.523599, 6.073746, 0.0));
//        points.add(new GeodeticPoint(0.872665, 5.235988, 0.0));
//        points.add(new GeodeticPoint(0.872665, 5.654867, 0.0));
//        points.add(new GeodeticPoint(0.872665, 6.073746, 0.0));
//        points.add(new GeodeticPoint(1.221730, 0.523599, 0.0));
//        points.add(new GeodeticPoint(1.221730, 1.570796, 0.0));
//        points.add(new GeodeticPoint(1.221730, 2.617994, 0.0));
//        points.add(new GeodeticPoint(1.221730, 3.665191, 0.0));
//        points.add(new GeodeticPoint(1.221730, 4.712389, 0.0));
//        points.add(new GeodeticPoint(1.221730, 5.759587, 0.0));
//        points.add(new GeodeticPoint(1.570796, 0.000000, 0.0));
         
        return points;
    }
}
