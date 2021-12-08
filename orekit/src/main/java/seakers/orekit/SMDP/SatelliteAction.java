package seakers.orekit.SMDP;

import org.orekit.bodies.GeodeticPoint;

public class SatelliteAction {
    private double tStart;
    private double tEnd;
    private GeodeticPoint location;
    private double reward;
    public SatelliteAction (double tStart, double tEnd, GeodeticPoint location) {
        this.tStart = tStart;
        this.tEnd = tEnd;
        this.location = location;
        this.reward = 0;
    }
    public double gettStart(){ return tStart; }
    public double gettEnd(){ return tEnd; }
    public GeodeticPoint getLocation() { return location; }
    public double getReward() { return reward; }
    public void setReward(double reward) {
        this.reward = reward;
    }
}
