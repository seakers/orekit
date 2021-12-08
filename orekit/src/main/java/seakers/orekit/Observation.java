package seakers.orekit;

import org.orekit.frames.TopocentricFrame;

public class Observation {
    private TopocentricFrame observationPoint;
    private double observationStart;
    private double observationEnd;
    private double observationReward;
    public Observation(TopocentricFrame observationPoint, double observationStart, double observationEnd, double observationReward) {
        this.observationPoint = observationPoint;
        this.observationStart = observationStart;
        this.observationEnd = observationEnd;
        this.observationReward = observationReward;
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
    public TopocentricFrame getObservationPoint() {
        return observationPoint;
    }
    public void setObservationStart(double observationStart) {
        this.observationStart = observationStart;
    }
    public void setObservationEnd(double observationEnd) {
        this.observationEnd = observationEnd;
    }
    public void setObservationReward(double observationReward) {
        this.observationReward = observationReward;
    }
    public void setObservationPoint(TopocentricFrame observationPoint) {
        this.observationPoint = observationPoint;
    }
    public String toString() {
        return this.observationPoint.getPoint().toString()+", Observation Start: "+this.observationStart+", Observation End: "+this.observationEnd+", Observation Reward: "+this.observationReward;
    }

}
