package seakers.orekit.SMDP;

public class ActionResult {
    private SatelliteAction a;
    private double v;
    public ActionResult (SatelliteAction a, double v) {
        this.a = a;
        this.v = v;
    }
    public double getV() { return v; }
    public SatelliteAction getA() { return a; }
}
