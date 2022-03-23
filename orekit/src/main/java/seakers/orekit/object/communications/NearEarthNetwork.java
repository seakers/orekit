package seakers.orekit.object.communications;

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
        ReceiverAntenna southafricaRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna southafricaTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint southafrica = new GeodeticPoint(Math.toRadians(-25),Math.toRadians(27),0.0);
        TopocentricFrame southafricaTF = new TopocentricFrame(earthShape,southafrica,"southafrica NEN Station");
        GndStation southafricaGS = new GndStation(southafricaTF,southafricaRX,southafricaTX,Math.toRadians(10.0));
        gs.add(southafricaGS);
        ReceiverAntenna fairbanksRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna fairbanksTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint fairbanks = new GeodeticPoint(Math.toRadians(64.8),Math.toRadians(-147.8),0.0);
        TopocentricFrame fairbanksTF = new TopocentricFrame(earthShape,fairbanks,"fairbanks NEN Station");
        GndStation fairbanksGS = new GndStation(fairbanksTF,fairbanksRX,fairbanksTX,Math.toRadians(10.0));
        gs.add(fairbanksGS);
        ReceiverAntenna kennedyRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna kennedyTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint kennedy = new GeodeticPoint(Math.toRadians(28.5),Math.toRadians(-80.6),0.0);
        TopocentricFrame kennedyTF = new TopocentricFrame(earthShape,kennedy,"kennedy NEN Station");
        GndStation kennedyGS = new GndStation(kennedyTF,kennedyRX,kennedyTX,Math.toRadians(10.0));
        gs.add(kennedyGS);
        ReceiverAntenna mcmurdoRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna mcmurdoTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint mcmurdo = new GeodeticPoint(Math.toRadians(-77),Math.toRadians(166),0.0);
        TopocentricFrame mcmurdoTF = new TopocentricFrame(earthShape,mcmurdo,"mcmurdo NEN Station");
        GndStation mcmurdoGS = new GndStation(mcmurdoTF,mcmurdoRX,mcmurdoTX,Math.toRadians(10.0));
        gs.add(mcmurdoGS);
        ReceiverAntenna wallopsRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna wallopsTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint wallops = new GeodeticPoint(Math.toRadians(37.9),Math.toRadians(-75.5),0.0);
        TopocentricFrame wallopsTF = new TopocentricFrame(earthShape,wallops,"wallops NEN Station");
        GndStation wallopsGS = new GndStation(wallopsTF,wallopsRX,wallopsTX,Math.toRadians(10.0));
        gs.add(wallopsGS);
        ReceiverAntenna whitesandsRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna whitesandsTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint whitesands = new GeodeticPoint(Math.toRadians(33.2),Math.toRadians(-106.3),0.0);
        TopocentricFrame whitesandsTF = new TopocentricFrame(earthShape,whitesands,"whitesands NEN Station");
        GndStation whitesandsGS = new GndStation(whitesandsTF,whitesandsRX,whitesandsTX,Math.toRadians(10.0));
        gs.add(whitesandsGS);
        ReceiverAntenna kirunaRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna kirunaTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint kiruna = new GeodeticPoint(Math.toRadians(67.8),Math.toRadians(20.2),0.0);
        TopocentricFrame kirunaTF = new TopocentricFrame(earthShape,kiruna,"kiruna NEN Station");
        GndStation kirunaGS = new GndStation(kirunaTF,kirunaRX,kirunaTX,Math.toRadians(10.0));
        gs.add(kirunaGS);
        ReceiverAntenna santiagoRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna santiagoTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint santiago = new GeodeticPoint(Math.toRadians(-33),Math.toRadians(-70),0.0);
        TopocentricFrame santiagoTF = new TopocentricFrame(earthShape,santiago,"santiago NEN Station");
        GndStation santiagoGS = new GndStation(santiagoTF,santiagoRX,santiagoTX,Math.toRadians(10.0));
        gs.add(santiagoGS);
        ReceiverAntenna northpoleRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna northpoleTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint northpole = new GeodeticPoint(Math.toRadians(64),Math.toRadians(-147),0.0);
        TopocentricFrame northpoleTF = new TopocentricFrame(earthShape,northpole,"northpole NEN Station");
        GndStation northpoleGS = new GndStation(northpoleTF,northpoleRX,northpoleTX,Math.toRadians(10.0));
        gs.add(northpoleGS);
        ReceiverAntenna dongaraRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna dongaraTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint dongara = new GeodeticPoint(Math.toRadians(-29),Math.toRadians(114),0.0);
        TopocentricFrame dongaraTF = new TopocentricFrame(earthShape,dongara,"dongara NEN Station");
        GndStation dongaraGS = new GndStation(dongaraTF,dongaraRX,dongaraTX,Math.toRadians(10.0));
        gs.add(dongaraGS);
        ReceiverAntenna southpointRX = new ReceiverAntenna(10.0, Collections.singleton(S));
        TransmitterAntenna southpointTX = new TransmitterAntenna(10.0, Collections.singleton(S));
        GeodeticPoint southpoint = new GeodeticPoint(Math.toRadians(18.9),Math.toRadians(-155.7),0.0);
        TopocentricFrame southpointTF = new TopocentricFrame(earthShape,southpoint,"southpoint NEN Station");
        GndStation southpointGS = new GndStation(southpointTF,southpointRX,southpointTX,Math.toRadians(10.0));
        gs.add(southpointGS);
        this.groundStations = gs;
    }

    public Set<GndStation> getGroundStations() { return groundStations; }
}
