package seakers.orekit.SMDP;

import org.orekit.bodies.GeodeticPoint;

public class SatelliteAction {
    private double tStart;
    private double tEnd;
    private GeodeticPoint location;
    private double reward;
    private String actionType;
    private String crosslinkSat;
    private double angle;
    public SatelliteAction (double tStart, double tEnd, GeodeticPoint location, String actionType) {
        this.tStart = tStart;
        this.tEnd = tEnd;
        this.location = location;
        this.actionType = actionType;
    }
    public SatelliteAction (double tStart, double tEnd, GeodeticPoint location, String actionType, String crosslinkSat) {
        this.tStart = tStart;
        this.tEnd = tEnd;
        this.location = location;
        this.actionType = actionType;
        this.crosslinkSat = crosslinkSat;
    }
    public SatelliteAction (double tStart, double tEnd, GeodeticPoint location, String actionType, double reward, double angle) {
        this.tStart = tStart;
        this.tEnd = tEnd;
        this.location = location;
        this.reward = reward;
        this.actionType = actionType;
        this.angle = angle;
    }
    public double gettStart(){ return tStart; }
    public double gettEnd(){ return tEnd; }
    public GeodeticPoint getLocation() { return location; }
    public double getReward() { return reward; }
    public String getActionType() { return actionType; }
    public String getCrosslinkSat() { return crosslinkSat; }
    public double getAngle() { return angle; }
    public void setReward(double reward) {
        this.reward = reward;
    }
}
