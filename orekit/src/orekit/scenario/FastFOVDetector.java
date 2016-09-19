/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.FieldOfView;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/**
 *
 * @author nozomihitomi
 */
public class FastFOVDetector extends AbstractDetector<FastFOVDetector> {

    private static final long serialVersionUID = 3765360560924280705L;

    private final TopocentricFrame target;

    private final double minElevation;

    private final FieldOfView fov;

    /**
     * The elevation threshold is by default set to 0degrees (i.e. satellite and
     * target must have line of sight before FieldOfViewDetector g() function is
     * computed)
     *
     * @param target the target to attach the detector to
     * @param fov the field of view that will observe the target
     */
    public FastFOVDetector(TopocentricFrame target, FieldOfView fov) {
        this(target, fov, 0.0);
    }

    /**
     *
     * @param target the target to attach the detector to
     * @param fov the field of view that will observe the target
     * @param elevationThreshold the minimum elevation threshold [radians] that
     * must be met before the FieldOfViewDetector g() function is computed
     */
    public FastFOVDetector(TopocentricFrame target, FieldOfView fov, double elevationThreshold) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                new StopOnIncreasing<>(), target, fov);
    }

    /**
     * Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder API
     * with the various {@code withXxx()} methods to set up the instance in a
     * readable manner without using a huge amount of parameters.
     * </p>
     *
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param pvTarget Position/velocity provider of the considered target
     * @param fov Field Of View
     */
    private FastFOVDetector(final double maxCheck, final double threshold, final int maxIter,
            final EventHandler<? super FastFOVDetector> handler,
            final TopocentricFrame target, final FieldOfView fov) {
        super(maxCheck, threshold, maxIter, handler);

        this.fov = fov;
        this.target = target;
        this.minElevation = FastMath.toRadians(0);
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
     * Get the Field Of View.
     *
     * @return Field Of View
     */
    public FieldOfView getFieldOfView() {
        return fov;
    }

    /**
     * Function to see if the given target is in line of sight with the
     * spacecraft (regardless of attitude). Implementation is similar to the
     * g(s) function from the elevation detector. That is, the g_lineOfSight
     * function value is the difference between the current elevation (and
     * azimuth if necessary) and the reference mask or minimum value (i.e. set
     * to 0degrees). This function will return a positive value if the satellite
     * and the target have line of sight. The function will return a negative
     * value if the satellite and the target do not have line of sight
     *
     * @param s
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    private double g_lineOfSight(SpacecraftState s) throws OrekitException {

        final double trueElevation = target.getElevation(s.getPVCoordinates().getPosition(),
                s.getFrame(), s.getDate());
        return trueElevation - minElevation;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The g function value is for circular field of views and is the difference
     * between FOV half aperture and the absolute value of the angle between
     * target direction and field of view center. It is positive inside the FOV
     * and negative outside.
     * </p>
     */
    private double g_circFOV(final SpacecraftState s) throws OrekitException {

        // Compute target position/velocity at date in spacecraft frame */
        final Vector3D targetPosInert = new Vector3D(1, target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                -1, s.getPVCoordinates().getPosition());
        final Vector3D targetPosSat = s.getAttitude().getRotation().applyTo(targetPosInert);

        // Target is in the field of view if the absolute value that angle is smaller than FOV half aperture.
        // g function value is the difference between FOV half aperture and the absolute value of the angle between
        // target direction and field of view center. It is positive inside the FOV and negative outside.
        return FastMath.toRadians(45) - Vector3D.angle(targetPosSat, Vector3D.PLUS_K);
    }

    /**
     *
     * <p>
     * The g function value is the angular offset between the target and the {@link FieldOfView#offsetFromBoundary(Vector3D)
     * Field Of View boundary}. It is negative if the target is visible within
     * the Field Of View and positive if it is outside of the Field Of View,
     * including the margin.
     * </p>
     * <p>
     * As per the previous definition, when the target enters the Field Of View,
     * a decreasing event is generated, and when the target leaves the Field Of
     * View, an increasing event is generated.
     * </p>
     */
    private double g_fov(SpacecraftState s) throws OrekitException {
        // get line of sight in spacecraft frame
        final Vector3D targetPosInert
                = target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D lineOfSightSC = s.toTransform().transformPosition(targetPosInert);

        return fov.offsetFromBoundary(lineOfSightSC);

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
        double gLOS = g_lineOfSight(s);
        if (gLOS >= 0) {
            //only compute the more expensive FieldOfViewDetector g function if 
            //the target and satellite meet the minimum elevation threshold
            return g_circFOV(s);
        } else {
            return gLOS;
        }
    }

    @Override
    protected FastFOVDetector create(double newMaxCheck, double newThreshold, int newMaxIter, EventHandler<? super FastFOVDetector> newHandler) {
        return new FastFOVDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, target, fov);
    }
}
