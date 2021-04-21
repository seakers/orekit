package seakers.orekit.event.detector;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;

public class ReflectionDetector extends AbstractDetector<ReflectionDetector> {
    private final TopocentricFrame target;

    private final HashMap<Satellite,Propagator> propagatorMap;

    private final Frame inertialFrame;

    private final Instrument instrument;

    private final Constellation txConstel;

    /**
     * The minimum radius of the earth (north-south direction)
     */
    private final double minRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING);

    /**
     * Constructor for the detector. Must use a instrument/target pair. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *  @param target the target to attach the detector to
     * @param instrument
     * @param txConstel
     */
    public ReflectionDetector(final TopocentricFrame target, HashMap<Satellite,Propagator> propagatorMap, final Frame inertialFrame, Instrument instrument, Constellation txConstel) {
        this(target, propagatorMap, inertialFrame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, new StopOnIncreasing<>(), instrument, txConstel);
    }

    /**
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param target the target to attach the detector to
     * of the access times
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param instrument
     * @param txConstel
     */
    private ReflectionDetector(final TopocentricFrame target, HashMap<Satellite,Propagator> propagatorMap, final Frame inertialFrame,
                               final double maxCheck, final double threshold, final int maxIter,
                               final EventHandler<? super ReflectionDetector> handler, Instrument instrument, Constellation txConstel) {
        super(maxCheck, threshold, maxIter, handler);

        this.propagatorMap = propagatorMap;
        this.target = target;
        this.inertialFrame = inertialFrame;
        this.instrument = instrument;
        this.txConstel = txConstel;
    }

    /**
     * Get the position/velocity provider of the target .
     *
     * @return the position/velocity provider of the target
     */
    public TopocentricFrame getPVTarget() {
        return target;
    }

    /**
     * The implementation of this g() function relies on the implementation of
     * the FieldOfViewDetector but first computes line of sight. If there is no
     * line of sight between the satellite and the target, the more expensive
     * computation of the FieldOfViewDetector g function is not executed. This
     * g() function is positive when the target enters the field of view and
     * negative if the target is outside the field of view
     *
     * @param s
     * @return
     * @throws OrekitException
     */
    @Override
    public double g(SpacecraftState s) throws OrekitException {
        // For the target to be in LOS:
        //  1- Target must be in the instrument's fov
        //  2- Target must be in the fov of any gps sat
        //  3- The rx sat must be in fov of tx sat
        //  4- The tx sat, rx sat, and the target must be co-planar

        boolean targetVisRx;
        boolean targetVisTx = false;
        boolean rxVisTx = false;
        boolean coplanar = false;

        // check if coplanar
        targetVisRx = instrument.getFOV().g_FOV(s, target) > 0;
        if(!targetVisRx) return -1;

        // check every tx sat for visibility of rx and target and check if coplanar
        AbsoluteDate date = s.getDate();
        for(Satellite txSat : txConstel.getSatellites()){
            ArrayList<Instrument> txPayload = txSat.getPayload();
            SpacecraftState sTx = propagatorMap.get(txSat).propagate(date);

            for(Instrument txIns : txPayload){
                targetVisTx = txIns.getFOV().g_FOV(sTx,target) > 0;

                // if target is visible to tx sat, check if rx is also in fov and if coplanar
                if(targetVisTx) {
                    rxVisTx = checkFOV(s,sTx);
                    coplanar = checkCoplanar(s,sTx);
                    break;
                }
            }

            if(targetVisTx && rxVisTx && coplanar) break;
        }

        if(targetVisRx && targetVisTx && rxVisTx && coplanar){
            return 1;
        }
        return -1;
    }


    @Override
    protected ReflectionDetector create(final double newMaxCheck, final double newThreshold,
                                    final int newMaxIter, final EventHandler<? super ReflectionDetector> newHandler) {
        return new ReflectionDetector(target, propagatorMap, inertialFrame, newMaxCheck, newThreshold, newMaxIter, newHandler, instrument, txConstel);
    }

    private boolean checkFOV(SpacecraftState sRx, SpacecraftState sTx){
        // The spacecraft position in the inertial frame
        Vector3D rxPosInert = sRx.getPVCoordinates(inertialFrame).getPosition();
        Vector3D txPosInert = sTx.getPVCoordinates(inertialFrame).getPosition();

        double th = Math.acos( rxPosInert.dotProduct(txPosInert)/( rxPosInert.getNorm() * txPosInert.getNorm() ) );

        //the maximum allowable angle between two satellites to be in line of sight of each other
        double maxTh = Math.acos( minRadius / rxPosInert.getNorm() ) + Math.acos( minRadius / txPosInert.getNorm());

        //losVal > 0 means that sat has line of sight
        return maxTh >= th;
    }

    private boolean checkCoplanar(SpacecraftState sRx, SpacecraftState sTx){
        AbsoluteDate date = sRx.getDate();
        Vector3D rxPosInert = sRx.getPVCoordinates(inertialFrame).getPosition().normalize();
        Vector3D txPosInert = sTx.getPVCoordinates(inertialFrame).getPosition().normalize();
        Vector3D targetPosInert = target.getPVCoordinates(date,inertialFrame).getPosition().normalize();

        final double coplanar
                = Math.abs(targetPosInert.normalize().dotProduct(txPosInert.crossProduct(rxPosInert)));
        boolean inView = false;
        if(coplanar < Math.toRadians(0.5)) {
            final Vector3D targetToTX
                    = txPosInert.subtract(targetPosInert);
            final Vector3D targetToRX
                    = rxPosInert.subtract(targetPosInert);
            final double thetaRX
                    = Vector3D.angle(targetToRX,targetPosInert);
            final double thetaTX
                    = Vector3D.angle(targetToTX,targetPosInert);

            final double spec = Math.abs(thetaRX - thetaTX);

            if(spec < Math.toRadians(0.5)) {
                inView = true;
//                System.out.println(date);
            }
        }
        return inView;
    }
}
