package seakers.orekit;

import seakers.orekit.SMDP.SatelliteAction;
import seakers.orekit.SMDP.SatelliteState;

public class Key {
    public SatelliteState s;
    public SatelliteAction a;
    public Key(SatelliteState s, SatelliteAction a) {
        this.s = s;
        this.a = a;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Key))
            return false;
        Key ref = (Key) obj;
        return this.s.equals(ref.s) && this.a.equals(ref.a);
    }

    @Override
    public int hashCode() {
        return s.hashCode() ^ a.hashCode();
    }

    public SatelliteState getS() {
        return s;
    }
    public SatelliteAction getA() {
        return a;
    }
}
