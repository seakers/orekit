package seakers.orekit.SMDP;

import java.util.ArrayList;
import java.util.HashMap;

public class SimulateResults {
    private ArrayList<SatelliteState> V;
    private HashMap<SatelliteState, HashMap<SatelliteAction, Double>> Q;
    private HashMap<SatelliteState,HashMap<SatelliteAction, Double>> N;
    public void setQ(HashMap<SatelliteState,HashMap<SatelliteAction, Double>> Q) { this.Q = Q;}
    public void setV(ArrayList<SatelliteState> V) {this.V = V;}
    public void setN(HashMap<SatelliteState,HashMap<SatelliteAction, Double>> N) { this.N = N; }
    public HashMap<SatelliteState,HashMap<SatelliteAction, Double>> getQ() { return Q; }
    public HashMap<SatelliteState,HashMap<SatelliteAction, Double>> getN() { return N; }
    public ArrayList<SatelliteState> getV() { return V; }

    public SimulateResults () {
        this.N = new HashMap<SatelliteState, HashMap<SatelliteAction, Double>>();
        this.Q = new HashMap<SatelliteState,HashMap<SatelliteAction, Double>>();
        this.V = new ArrayList<>();
    }
}
