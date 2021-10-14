package seakers.orekit.SMDP;

public class StateAction {
    private SatelliteState s;
    private SatelliteAction a;
    public StateAction(SatelliteState s, SatelliteAction a) {
        this.s = s;
        this.a = a;
    }
    public SatelliteState getS() { return s; }
    public SatelliteAction getA() { return a; }
}
