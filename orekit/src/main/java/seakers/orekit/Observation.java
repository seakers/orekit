package seakers.orekit;

import org.orekit.bodies.GeodeticPoint;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Observation implements Serializable {
    private GeodeticPoint observationPoint;
    private double observationStart;
    private double observationEnd;
    private double observationReward;
    private double observationAngle;
    public Observation(GeodeticPoint observationPoint, double observationStart, double observationEnd, double observationReward) {
        this.observationPoint = observationPoint;
        this.observationStart = observationStart;
        this.observationEnd = observationEnd;
        this.observationReward = observationReward;
    }
    public Observation(GeodeticPoint observationPoint, double observationStart, double observationEnd, double observationReward, double observationAngle) {
        this.observationPoint = observationPoint;
        this.observationStart = observationStart;
        this.observationEnd = observationEnd;
        this.observationReward = observationReward;
        this.observationAngle = observationAngle;
    }
    public double getObservationStart() {
        return observationStart;
    }
    public double getObservationEnd() {
        return observationEnd;
    }
    public double getObservationReward() {
        return observationReward;
    }
    public GeodeticPoint getObservationPoint() {
        return observationPoint;
    }
    public double getObservationAngle() { return observationAngle; }
    public void setObservationStart(double observationStart) {
        this.observationStart = observationStart;
    }
    public void setObservationEnd(double observationEnd) {
        this.observationEnd = observationEnd;
    }
    public void setObservationReward(double observationReward) {
        this.observationReward = observationReward;
    }
    public void setObservationPoint(GeodeticPoint observationPoint) {
        this.observationPoint = observationPoint;
    }
    public String toString() {
        return this.observationPoint.toString()+", Observation Start: "+this.observationStart+", Observation End: "+this.observationEnd+", Observation Reward: "+this.observationReward;
    }

}
