/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.events;

import orekit.object.CoveragePoint;
import orekit.object.linkbudget.LinkBudget;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_MAXCHECK;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_MAX_ITER;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_THRESHOLD;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author paugarciabuzzi
 */
public class LBDetector2 extends AbstractDetector<LBDetector2> {

    private static final long serialVersionUID = -6354518769897706495L;
    
    /**
     * The ground point the satellite wants to comunciate with
     */
    private final CoveragePoint pt;
    
    private final LinkBudget lb;
    
    /**
     * Constructor for the detector.
     * Threshold for event detection is set to default 1e-6 seconds
     *
     * @param pt ground point the satellite wants to comunciate with
     * @param lb link budget params provider
     */
    public LBDetector2(CoveragePoint pt, LinkBudget lb) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                new StopOnIncreasing<>(), pt, lb);
    }

    /**
     * Constructor for the detector. Can set
     * the resolution at which access time is computed
     *
     * @param threshold threshold in seconds that determines the temporal
     * @param pt ground point the satellite wants to comunciate with
     * @param lb link budget params provider
     */
    public LBDetector2(double threshold, CoveragePoint pt, LinkBudget lb) {
        this(DEFAULT_MAXCHECK, threshold, DEFAULT_MAX_ITER,
                new StopOnIncreasing<>(), pt, lb);
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
    private LBDetector2(final double maxCheck, final double threshold, final int maxIter,
            final EventHandler<? super LBDetector2> handler,
            final CoveragePoint pt, final LinkBudget lb) {
        super(maxCheck, threshold, maxIter, handler);

        this.pt = pt;
        this.lb = lb;
    }

    /**
     * This g() function is positive when the link budget is closed (distance smaller
     * than dmax) and negative if the satellite cannot communicate with the ground point
     * with enough power margin.
     *
     * @param s Spacecraft state
     * @return
     * @throws OrekitException
     */
    @Override
    public double g(SpacecraftState s) throws OrekitException {
        double distance1=s.getPVCoordinates().getPosition().distance1(pt.getPVCoordinates(s.getDate(),s.getFrame()).getPosition());
        double maxdistance=this.lb.getMaxDistance();
        return maxdistance-distance1;
    }

    @Override
    protected LBDetector2 create(double newMaxCheck, double newThreshold, int newMaxIter, EventHandler<? super LBDetector2> newHandler) {
        return new LBDetector2(newMaxCheck, newThreshold, newMaxIter, newHandler, pt, lb);
    }
    
}