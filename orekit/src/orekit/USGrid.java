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
public class USGrid {

    /**
     * 6 deg granularity grid from STK
     *
     * @return
     */
    public static Collection<GeodeticPoint> getPoints6() {
        ArrayList<GeodeticPoint> points = new ArrayList<>();

        points.add(new GeodeticPoint(5.84480172878371e-001, 4.24696784651954e+000, 0.0));
        points.add(new GeodeticPoint(5.84480172878371e-001, 4.36332312998582e+000, 0.0));
        points.add(new GeodeticPoint(5.84480172878371e-001, 4.47967841345211e+000, 0.0));
        points.add(new GeodeticPoint(5.84480172878371e-001, 4.59603369691840e+000, 0.0));
        points.add(new GeodeticPoint(5.84480172878371e-001, 4.71238898038469e+000, 0.0));
        points.add(new GeodeticPoint(5.84480172878371e-001, 4.82874426385098e+000, 0.0));
        points.add(new GeodeticPoint(6.86147451950733e-001, 4.26359002987186e+000, 0.0));
        points.add(new GeodeticPoint(6.86147451950733e-001, 4.41318968004280e+000, 0.0));
        points.add(new GeodeticPoint(6.86147451950733e-001, 4.56278933021375e+000, 0.0));
        points.add(new GeodeticPoint(6.86147451950733e-001, 4.71238898038469e+000, 0.0));
        points.add(new GeodeticPoint(6.86147451950733e-001, 4.86198863055563e+000, 0.0));
        points.add(new GeodeticPoint(7.87814731023095e-001, 4.26359002987186e+000, 0.0));
        points.add(new GeodeticPoint(7.87814731023095e-001, 4.41318968004280e+000, 0.0));
        points.add(new GeodeticPoint(7.87814731023095e-001, 4.56278933021375e+000, 0.0));
        points.add(new GeodeticPoint(7.87814731023095e-001, 4.71238898038469e+000, 0.0));
        points.add(new GeodeticPoint(1.09281656824018e+000, 3.60701378745495e+000, 0.0));
        points.add(new GeodeticPoint(1.19448384731254e+000, 3.60701378745495e+000, 0.0));
        return points;
    }
}
