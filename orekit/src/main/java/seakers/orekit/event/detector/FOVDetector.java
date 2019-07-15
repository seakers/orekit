/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import seakers.orekit.object.Instrument;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;

/**
 * Detector responsible for tracking when the target enters an instrument's
 * field of view
 *
 * @author nozomihitomi
 */
public class FOVDetector extends AbstractDetector<FOVDetector> {

    private static final long serialVersionUID = 3765360560924280705L;

    private final TopocentricFrame target;

    private final Instrument instrument;

    /**
     * Constructor for the detector. Must use a instrument/target pair. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param target the target to attach the detector to
     * @param instrument the instrument that will observe the target\
     */
    public FOVDetector(final TopocentricFrame target, final Instrument instrument) {
        this(target, instrument, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, new StopOnIncreasing<>());
    }

    /**
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param target the target to attach the detector to
     * @param instrument the instrument that will observe the target resolution
     * of the access times
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    private FOVDetector(final TopocentricFrame target, final Instrument instrument,
                        final double maxCheck, final double threshold, final int maxIter,
                        final EventHandler<? super FOVDetector> handler) {
        super(maxCheck, threshold, maxIter, handler);

        this.instrument = instrument;
        this.target = target;
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
        //only compute the more expensive FieldOfViewDetector g function if 
        //the target and satellite meet the minimum elevation threshold
        return instrument.getFOV().g_FOV(s, target);
    }

    @Override
    protected FOVDetector create(final double newMaxCheck, final double newThreshold,
                                 final int newMaxIter, final EventHandler<? super FOVDetector> newHandler) {
        return new FOVDetector(target, instrument, newMaxCheck, newThreshold, newMaxIter, newHandler);
    }
}
