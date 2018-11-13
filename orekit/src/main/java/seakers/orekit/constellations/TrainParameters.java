package seakers.orekit.constellations;

import java.util.ArrayList;

public class TrainParameters {
    double a;
    ArrayList<Double> LTANs;

    public TrainParameters(double a, ArrayList<Double> LTANs) {
        this.a = a;
        this.LTANs = LTANs;
    }

    public double getA() {
        return this.a;
    }

    public ArrayList<Double> getLTANs() {
        return LTANs;
    }
}