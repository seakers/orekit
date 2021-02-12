package seakers.orekit.event.detector;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

public class SatLOSDetector extends AbstractDetector<SatLOSDetector> {

    private static final long serialVersionUID = 3765360560924280705L;

    private final Satellite target;

    private final Propagator pfTarget;

    private final Frame inertialFrame;

    /**
     * The minimum radius of the earth (north-south direction)
     */
    private final double minRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING);

    /**
     * Constructor for the detector. Must use a instrument/target pair. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param target the target to attach the detector to
     */
    public SatLOSDetector(final Satellite target, Propagator pfTarget, final Frame inertialFrame) {
        this(target, pfTarget, inertialFrame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, new StopOnIncreasing<>());
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
     */
    private SatLOSDetector(final Satellite target, Propagator pfTarget, final Frame inertialFrame,
                        final double maxCheck, final double threshold, final int maxIter,
                        final EventHandler<? super SatLOSDetector> handler) {
        super(maxCheck, threshold, maxIter, handler);

        this.pfTarget = pfTarget;
        this.target = target;
        this.inertialFrame = inertialFrame;
    }

    /**
     * Get the position/velocity provider of the target .
     *
     * @return the position/velocity provider of the target
     */
    public Satellite getPVTarget() {
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
        AbsoluteDate date = s.getDate();

        // The spacecraft position in the inertial frame
        Vector3D satPosInert = s.getPVCoordinates(inertialFrame).getPosition();
        Vector3D targetPosInert = pfTarget.propagate(date).getPVCoordinates(inertialFrame).getPosition();

        double th = Math.acos( satPosInert.dotProduct(targetPosInert)/( satPosInert.getNorm() * targetPosInert.getNorm() ) );

        //the maximum allowable angle between two satellites to be in line of sight of each other
        double maxTh = Math.acos( minRadius / satPosInert.getNorm() ) + Math.acos( minRadius / targetPosInert.getNorm());

        //losVal > 0 means that sat has line of sight
        return maxTh - th;
    }

    @Override
    protected SatLOSDetector create(final double newMaxCheck, final double newThreshold,
                                 final int newMaxIter, final EventHandler<? super SatLOSDetector> newHandler) {
        return new SatLOSDetector(target, pfTarget, inertialFrame, newMaxCheck, newThreshold, newMaxIter, newHandler);
    }
}
