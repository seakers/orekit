/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import seak.orekit.coverage.access.TimeIntervalArray;
import seak.orekit.event.detector.GroundSunAngleDetector;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.object.CoveragePoint;

/**
 *
 * @author nhitomi
 */
public class GroundSunAngleEventAnalysis extends AbstractGroundEventAnalysis {

    /**
     * The maximum allowable angle from the sun to the desired direction.
     */
    private final double maxAngle;

    /**
     * The direction used to compute the maximum allowable angle from.
     */
    private final Vector3D direction;

    public GroundSunAngleEventAnalysis(AbsoluteDate startDate,
            AbsoluteDate endDate, Frame inertialFrame, HashSet<CoverageDefinition> covDefs, double maxAngle, Vector3D direction) {
        super(startDate, endDate, inertialFrame, covDefs);
        this.maxAngle = maxAngle;
        this.direction = direction;
    }

    @Override
    public EventAnalysis call() throws Exception {
        double stepSize = 86400. / 4.; //quarter of a day
        double threshold = 1E-3; //for root finding [s]
        for (CoverageDefinition cdef : getCoverageDefinitions()) {
            Logger.getGlobal().finest(String.format("Computing sun angles for %s...", cdef));

            KeplerianOrbit dummyOrbit = 
                    new KeplerianOrbit(1, 0, Math.PI, 0, 0, 0, 
                            PositionAngle.TRUE, getInertialFrame(), getStartDate(), 0);

            Map<CoveragePoint, TimeIntervalArray> illuminationTimes = new HashMap<>();
            for (CoveragePoint point : cdef.getPoints()) {
                KeplerianPropagator kp = new KeplerianPropagator(dummyOrbit, 0);

                GroundSunAngleDetector gsd
                        = new GroundSunAngleDetector(kp.getInitialState(),
                                getStartDate(), getEndDate(), point, 
                                maxAngle, direction, EventHandler.Action.CONTINUE, stepSize, threshold);
                kp.addEventDetector(gsd);
                kp.propagate(getStartDate(), getEndDate());
                illuminationTimes.put(point, gsd.getTimeIntervalArray());
            }
            getEvents().put(cdef, illuminationTimes);
        }

        return this;
    }

}
