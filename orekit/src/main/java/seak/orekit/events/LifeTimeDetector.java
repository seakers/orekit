/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.events;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_MAX_ITER;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/**
 *
 * @author paugarciabuzzi
 */
public class LifeTimeDetector extends AbstractEventDetector<LifeTimeDetector> {

    private static final long serialVersionUID = -6354518769897706495L;

    /**
     * The ground point the satellite wants to comunciate with
     */
    private final double thresholdAltitude;
    /**
     * Constructor for the detector. Threshold for event detection is set to
     * default 1e-6 seconds
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     */
    public LifeTimeDetector(final SpacecraftState initialState,
            final AbsoluteDate startDate, final AbsoluteDate endDate) {
        super(initialState, startDate, endDate);
        this.thresholdAltitude = 150000;
    }

    /**
     * Constructor for the detector. Can set the resolution at which access time
     * is computed
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param maxCheck maximum checking interval (s)
     * @param threshold threshold in seconds that determines the temporal
     * @param action specifies action after event is detected.
     */
    public LifeTimeDetector(final SpacecraftState initialState,
            final AbsoluteDate startDate, final AbsoluteDate endDate,double maxCheck, double threshold, EventHandler.Action action) {
        super(initialState, startDate, endDate, action, maxCheck, threshold, DEFAULT_MAX_ITER);
        this.thresholdAltitude = 150000;
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
     * @param pt ground point the satellite wants to comunciate with
     * @param lb link budget params provider
     */
    private LifeTimeDetector(final double maxCheck, final double threshold, final int maxIter,
            final EventHandler<? super LifeTimeDetector> handler) {
        super(maxCheck, threshold, maxIter, handler);
        this.thresholdAltitude = 150000;
    }

    /**
     * This g() function is positive when the link budget is closed (distance
     * smaller than dmax) and negative if the satellite cannot communicate with
     * the ground point with enough power margin.
     *
     * @param s Spacecraft state
     * @return
     * @throws OrekitException
     */
    @Override
    public double g(SpacecraftState s) throws OrekitException {
        double satAltitude = s.getA()-Constants.WGS84_EARTH_EQUATORIAL_RADIUS; //might need to change that for non-circular orbits
        return satAltitude - this.thresholdAltitude;
    }

    @Override
    protected EventDetector create(double newMaxCheck, double newThreshold, int newMaxIter, EventHandler newHandler) {
        return new LifeTimeDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
    }

}
