package seakers.orekit.object;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GndStationNetwork {

    public final HashSet<GndStation> NEN;
    public final double costNEN = 490.0;

    public final HashSet<GndStation> AWS;
    public final double costAWS = -1;

    public GndStationNetwork(){
        NEN = setupNEN();
        AWS = setupAWS();
    }

    private HashSet<GndStation> setupNEN(){
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        HashSet<GndStation> NEN = new HashSet<>();

        TopocentricFrame AS = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(64.8587), FastMath.toRadians(-147.8576), 0.), "AS");
        TopocentricFrame SSC = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(64.8042), FastMath.toRadians(-147.5002), 0.), "SSC");
        TopocentricFrame KUS = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(28.542064), FastMath.toRadians(-80.642953), 0.), "KUS");
        TopocentricFrame HBK = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(-25.8870), FastMath.toRadians(27.7120), 0.), "HBK");
        TopocentricFrame USHI = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(19.0140), FastMath.toRadians(-155.6633), 0.), "USHI");
        TopocentricFrame AUWA = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(-29.0457), FastMath.toRadians(115.3487), 0.), "AUWA");
        TopocentricFrame KU = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(67.8896), FastMath.toRadians(21.0657), 0.), "KU");
        TopocentricFrame MG = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(-77.8391), FastMath.toRadians(166.6671), 0.), "MG");
        TopocentricFrame TR = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(-72.0022), FastMath.toRadians(2.0575), 0.), "TR");
        TopocentricFrame SG = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(78.231), FastMath.toRadians(15.389), 0.), "SG");
        TopocentricFrame SA = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(-33.1511), FastMath.toRadians(-70.6664), 0.), "SA");
        TopocentricFrame SI = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(1.3962), FastMath.toRadians(103.8343), 0.), "SI");
        TopocentricFrame WG = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(37.9249), FastMath.toRadians(-75.4765), 0.), "WG");
        TopocentricFrame WS = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(32.5047), FastMath.toRadians(-106.6108), 0.), "WS");

        ArrayList<TopocentricFrame> points = new ArrayList<>();
        points.add(AS); points.add(SSC); points.add(KUS); points.add(HBK); points.add(USHI);
        points.add(AUWA); points.add(KU); points.add(MG); points.add(TR); points.add(SG);
        points.add(SA); points.add(SI); points.add(WG); points.add(WS);

        for(TopocentricFrame point : points){
            HashSet<CommunicationBand> bands = new HashSet<>();
            bands.add(CommunicationBand.UHF);

            NEN.add(new GndStation(point, new ReceiverAntenna(6., bands),
                    new TransmitterAntenna(6., bands), FastMath.toRadians(10.)));
        }

        return NEN;
    }

    private HashSet<GndStation> setupAWS() {
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        TopocentricFrame NVirg = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(38.880278), FastMath.toRadians(-77.108333), 0.), "North Virginia");
        TopocentricFrame Ohio = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(40.358615), FastMath.toRadians(-82.706838), 0.), "Ohio");
        TopocentricFrame NCal = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(40.583333), FastMath.toRadians(-122.366667), 0.), "Northern California");
        TopocentricFrame Org = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(45.3573), FastMath.toRadians(-122.6068), 0.), "Oregon");
        TopocentricFrame Africa = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(-33.92584), FastMath.toRadians(18.42322), 0.), "Cape Town");
        TopocentricFrame HK = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(22.27832), FastMath.toRadians( 114.17469), 0.), "Hong Kong");
        TopocentricFrame India = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(19.07283), FastMath.toRadians(72.88261), 0.), "Mumbai");
        TopocentricFrame Jap = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(34.69374), FastMath.toRadians(135.50218), 0.), "Osaka");
        TopocentricFrame Kor = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(37.566), FastMath.toRadians(126.9784), 0.), "Seoul");
        TopocentricFrame Sing = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(1.352083), FastMath.toRadians(103.819836), 0.), "Singapore");
        TopocentricFrame Aus = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(-33.86785), FastMath.toRadians(151.20732), 0.), "Sydney");
        TopocentricFrame Tok = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(35.6895), FastMath.toRadians(139.69171), 0.), "Tokyo");
        TopocentricFrame Can = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(49.8844), FastMath.toRadians(-97.14704), 0.), "Central Canada");
        TopocentricFrame Ger = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(50.11552), FastMath.toRadians(8.68417), 0.), "Frankfurt");
        TopocentricFrame Ire = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(53.41291), FastMath.toRadians(-8.24389), 0.), "Ireland");
        TopocentricFrame Ing = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(51.50853), FastMath.toRadians(-0.12574), 0.), "London");
        TopocentricFrame Itl = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(45.46427), FastMath.toRadians(9.18951), 0.), "Milan");
        TopocentricFrame Fr = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(48.85341), FastMath.toRadians(2.3488), 0.), "Paris");
        TopocentricFrame Swe = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(59.32938), FastMath.toRadians(18.06871), 0.), "Stockholm");
        TopocentricFrame Bhr = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(25.930414), FastMath.toRadians(50.637772), 0.), "Bahrain");
        TopocentricFrame Bra = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(-23.5475), FastMath.toRadians(-46.63611), 0.), "Sao Paulo");
        TopocentricFrame China = new TopocentricFrame(earthShape,
                new GeodeticPoint(FastMath.toRadians(39.9075), FastMath.toRadians(116.39723), 0.), "Beijing");

        ArrayList<TopocentricFrame> points = new ArrayList<>();
        points.add(NVirg); points.add(Ohio); points.add(NCal); points.add(Org); points.add(Africa);
        points.add(HK); points.add(India); points.add(Jap); points.add(Kor); points.add(Sing);
        points.add(Aus); points.add(Tok); points.add(Can); points.add(Ger);
        points.add(Ire); points.add(Ing); points.add(Itl); points.add(Fr);
        points.add(Swe); points.add(Bhr); points.add(Bra); points.add(China);


        HashSet<GndStation> AWS = new HashSet<>();
        for(TopocentricFrame point : points){
            HashSet<CommunicationBand> bands = new HashSet<>();
            bands.add(CommunicationBand.UHF);

            AWS.add(new GndStation(point, new ReceiverAntenna(6., bands),
                    new TransmitterAntenna(6., bands), FastMath.toRadians(10.)));
        }

        return AWS;
    }
}
