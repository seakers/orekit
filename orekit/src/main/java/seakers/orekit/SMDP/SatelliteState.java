package seakers.orekit.SMDP;

import org.orekit.bodies.GeodeticPoint;

import java.util.ArrayList;

public class SatelliteState {
    private double t;
    private double tPrevious;
    private ArrayList<GeodeticPoint> images;
    public SatelliteState() {
        this.t = 0;
        this.tPrevious = 0;
        this.images = new ArrayList<>();
    }
    public SatelliteState (double t, double tPrevious, ArrayList<GeodeticPoint> images) {
        this.t = t;
        this.tPrevious = tPrevious;
        this.images = images;
    }
    public double getT() { return t; }
    public double gettPrevious() { return t; }
    public ArrayList<GeodeticPoint> getImages() { return images; }
}
