/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.events;

import orekit.object.CoveragePoint;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * Calculates when a satellite has line of sight to a ground point
 *
 * @author nozomihitomi
 */
public class LOSDetector extends AbstractDetector<LOSDetector> {

    /**
     * The ground point that needs to be viewed
     */
    private final CoveragePoint pt;

    /**
     * The shape of the body on which the point lies
     */
    private final BodyShape shape;

    /**
     * The inertial frame used in the scenario
     */
    private final Frame inertialFrame;

    /**
     * The minimum radius of the earth (north-south direction)
     */
    private final double minRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING);

    /**
     * Build a new line of sight detector.
     * <p>
     * This simple constructor takes default values for maximal checking
     * interval ({@link #DEFAULT_MAXCHECK}) and convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     *
     * @param pt The ground point that needs to be viewed
     * @param shape The shape of the body on which the point lies
     * @param inertialFrame The inertial frame used in the scenario evaluated
     */
    public LOSDetector(final CoveragePoint pt, final BodyShape shape, final Frame inertialFrame) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, pt, shape, inertialFrame);
    }

    /**
     * Build a new line of sight detector.
     * <p>
     * This simple constructor takes default value for convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * <p>
     * The maximal interval between line of sight checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     *
     * @param maxCheck maximal checking interval (s)
     * @param pt The ground point that needs to be viewed
     * @param shape The shape of the body on which the point lies
     * @param inertialFrame The inertial frame used in the scenario evaluated
     */
    public LOSDetector(final double maxCheck,
            final CoveragePoint pt, final BodyShape shape, final Frame inertialFrame) {
        this(maxCheck, DEFAULT_THRESHOLD, pt, shape, inertialFrame);
    }

    /**
     * Build a new line of sight detector.
     * <p>
     * The maximal interval between line of sight checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     *
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param pt The ground point that needs to be viewed
     * @param shape The shape of the body on which the point lies
     * @param inertialFrame The inertial frame used in the scenario evaluated
     */
    public LOSDetector(final double maxCheck,
            final double threshold,
            final CoveragePoint pt, final BodyShape shape, final Frame inertialFrame) {
        this(maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnDecreasing<LOSDetector>(),
                pt, shape, inertialFrame);
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
     * @param pt The ground point that needs to be viewed
     * @param shape The shape of the body on which the point lies
     * @param inertialFrame The inertial frame used in the scenario evaluated
     */
    private LOSDetector(final double maxCheck, final double threshold,
            final int maxIter, final EventHandler<? super LOSDetector> handler,
            final CoveragePoint pt, final BodyShape shape, final Frame inertialFrame) {
        super(maxCheck, threshold, maxIter, handler);
        this.pt = pt;
        this.shape = shape;
        this.inertialFrame = inertialFrame;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LOSDetector create(final double newMaxCheck, final double newThreshold,
            final int newMaxIter, final EventHandler<? super LOSDetector> newHandler) {
        return new LOSDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                pt, shape, inertialFrame);
    }

    @Override
    public double g(SpacecraftState s) throws OrekitException {
        // The spacecraft position in the inertial frame
        Vector3D satPosInertNorm = s.getPVCoordinates(inertialFrame).getPosition().normalize();

        Vector3D ptPosInertNorm = pt.getPVCoordinates(s.getDate(), inertialFrame).getPosition().normalize();

        double cosThetas = ptPosInertNorm.dotProduct(satPosInertNorm);

        //the mininum cos(theta) value required for line of sight
        double minCosTheta = minRadius / s.getA();

        //losVal > 0 means that sat has line of sight
        return cosThetas - minCosTheta;
    }

}
