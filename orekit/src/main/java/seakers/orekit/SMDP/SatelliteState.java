package seakers.orekit.SMDP;

import org.orekit.bodies.GeodeticPoint;

import java.util.ArrayList;

public class SatelliteState {
    private double t;
    private double tPrevious;
    private double batteryCharge;
    private double dataStored;
    private double currentAngle;
    private double storedImageReward;
    private ArrayList<SatelliteAction> actionHistory;
    public SatelliteState() {
        this.t = 0.0;
        this.tPrevious = 0.0;
        this.actionHistory = new ArrayList<>();
        this.batteryCharge = 30.0;
        this.dataStored = 0.0;
        this.currentAngle = 0.0;
        this.storedImageReward = 0.0;
    }
    public SatelliteState (double t, double tPrevious, ArrayList<SatelliteAction> actionHistory, double batteryCharge, double dataStored, double currentAngle, double storedImageReward) {
        this.t = t;
        this.tPrevious = tPrevious;
        this.actionHistory = actionHistory;
        this.batteryCharge = batteryCharge;
        this.dataStored = dataStored;
        this.currentAngle = currentAngle;
        this.storedImageReward = storedImageReward;
    }
    public SatelliteState(SatelliteState another) {
        this.t = another.t;
        this.tPrevious = another.tPrevious;
        this.batteryCharge = another.batteryCharge;
        this.dataStored = another.dataStored;
        this.currentAngle = another.currentAngle;
        this.storedImageReward = another.storedImageReward;
        this.actionHistory = another.actionHistory;
    }

    public double getT() { return t; }
    public double gettPrevious() { return t; }
    public ArrayList<SatelliteAction> getHistory() { return actionHistory; }
    public double getCurrentAngle() { return currentAngle; }
    public double getStoredImageReward() { return storedImageReward; }
    public double getDataStored() { return dataStored; }
    public double getBatteryCharge() { return batteryCharge; }
}
