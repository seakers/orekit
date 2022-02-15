package seakers.orekit;

import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static seakers.orekit.object.CommunicationBand.S;

public class NearEarthNetwork {
    Set<GndStation> groundStations;
    public NearEarthNetwork() {
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        Set<GndStation> gs = new HashSet<>();
        ReceiverAntenna singaporeRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna singaporeTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint singapore = new GeodeticPoint(Math.toRadians(1),Math.toRadians(103),0.0);
        TopocentricFrame singaporeTF = new TopocentricFrame(earthShape,singapore,"Singapore NEN Station");
        GndStation singaporeGS = new GndStation(singaporeTF,singaporeRX,singaporeTX,Math.toRadians(10.0));
        gs.add(singaporeGS);
        ReceiverAntenna svalbardRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna svalbardTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint svalbard = new GeodeticPoint(Math.toRadians(78),Math.toRadians(15),0.0);
        TopocentricFrame svalbardTF = new TopocentricFrame(earthShape,svalbard,"Svalbard NEN Station");
        GndStation svalbardGS = new GndStation(svalbardTF,svalbardRX,svalbardTX,Math.toRadians(10.0));
        gs.add(svalbardGS);
        ReceiverAntenna trollsatRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna trollsatTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint trollsat = new GeodeticPoint(Math.toRadians(-72),Math.toRadians(2),0.0);
        TopocentricFrame trollsatTF = new TopocentricFrame(earthShape,trollsat,"Trollsat NEN Station");
        GndStation trollsatGS = new GndStation(trollsatTF,trollsatRX,trollsatTX,Math.toRadians(10.0));
        gs.add(trollsatGS);
        this.groundStations = gs;
    }

    public Set<GndStation> getGroundStations() { return groundStations; }
}
