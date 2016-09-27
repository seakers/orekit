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

        points.add(new GeodeticPoint(-1.5707963267949001, 0.0000000000000000, 0.0));
        points.add(new GeodeticPoint(-1.2217304763960299, 0.5235987755982990, 0.0));
        points.add(new GeodeticPoint(-1.2217304763960299, 1.5707963267949001, 0.0));
        points.add(new GeodeticPoint(-1.2217304763960299, 2.6179938779914900, 0.0));
        points.add(new GeodeticPoint(-1.2217304763960299, 3.6651914291880900, 0.0));
        points.add(new GeodeticPoint(-1.2217304763960299, 4.7123889803846897, 0.0));
        points.add(new GeodeticPoint(-1.2217304763960299, 5.7595865315812897, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 0.2094395102393200, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 0.6283185307179590, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 1.0471975511966001, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 0.2094395102393200, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 0.6283185307179590, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 1.0471975511966001, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 0.2094395102393200, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 0.6283185307179590, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 1.0471975511966001, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 1.4660765716752400, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 1.8849555921538801, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 2.3038346126325200, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 1.4660765716752400, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 1.8849555921538801, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 2.3038346126325200, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 1.4660765716752400, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 1.8849555921538801, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 2.3038346126325200, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 2.7227136331111499, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 3.1415926535897900, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 3.5604716740684301, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 2.7227136331111499, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 3.1415926535897900, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 3.5604716740684301, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 2.7227136331111499, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 3.1415926535897900, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 3.5604716740684301, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 3.9793506945470698, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 4.3982297150257104, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 4.8171087355043500, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 3.9793506945470698, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 4.3982297150257104, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 4.8171087355043500, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 3.9793506945470698, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 4.3982297150257104, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 4.8171087355043500, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 5.2359877559829897, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 5.6548667764616303, 0.0));
//        points.add(new GeodeticPoint(-0.8726646259971650, 6.0737457969402699, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 5.2359877559829897, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 5.6548667764616303, 0.0));
//        points.add(new GeodeticPoint(-0.5235987755982990, 6.0737457969402699, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 5.2359877559829897, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 5.6548667764616303, 0.0));
//        points.add(new GeodeticPoint(-0.1745329251994330, 6.0737457969402699, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 0.2094395102393200, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 0.6283185307179590, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 1.0471975511966001, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 0.2094395102393200, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 0.6283185307179590, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 1.0471975511966001, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 0.2094395102393200, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 0.6283185307179590, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 1.0471975511966001, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 1.4660765716752400, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 1.8849555921538801, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 2.3038346126325200, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 1.4660765716752400, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 1.8849555921538801, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 2.3038346126325200, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 1.4660765716752400, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 1.8849555921538801, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 2.3038346126325200, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 2.7227136331111499, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 3.1415926535897900, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 3.5604716740684301, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 2.7227136331111499, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 3.1415926535897900, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 3.5604716740684301, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 2.7227136331111499, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 3.1415926535897900, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 3.5604716740684301, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 3.9793506945470698, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 4.3982297150257104, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 4.8171087355043500, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 3.9793506945470698, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 4.3982297150257104, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 4.8171087355043500, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 3.9793506945470698, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 4.3982297150257104, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 4.8171087355043500, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 5.2359877559829897, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 5.6548667764616303, 0.0));
//        points.add(new GeodeticPoint(0.1745329251994330, 6.0737457969402699, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 5.2359877559829897, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 5.6548667764616303, 0.0));
//        points.add(new GeodeticPoint(0.5235987755982990, 6.0737457969402699, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 5.2359877559829897, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 5.6548667764616303, 0.0));
//        points.add(new GeodeticPoint(0.8726646259971650, 6.0737457969402699, 0.0));
//        points.add(new GeodeticPoint(1.2217304763960299, 0.5235987755982990, 0.0));
//        points.add(new GeodeticPoint(1.2217304763960299, 1.5707963267949001, 0.0));
//        points.add(new GeodeticPoint(1.2217304763960299, 2.6179938779914900, 0.0));
//        points.add(new GeodeticPoint(1.2217304763960299, 3.6651914291880900, 0.0));
//        points.add(new GeodeticPoint(1.2217304763960299, 4.7123889803846897, 0.0));
//        points.add(new GeodeticPoint(1.2217304763960299, 5.7595865315812897, 0.0));
//        points.add(new GeodeticPoint(1.5707963267949001, 0.0000000000000000, 0.0));

        return points;
    }
}
