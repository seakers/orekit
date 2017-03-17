/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.events;

import orekit.coverage.access.TimeIntervalArray;
import orekit.object.CoveragePoint;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/**
 * the g() function for the FOVDetector assumes that the target enters the
 * FieldOfView when the value is positive and exits when the value is negative.
 * Therefore, when an event is detected and the value is increasing, the target
 * is entering the FieldOfView.
 *
 * The access times are stored within the handler and can be retrieved at any
 * time during the simulation
 *
 * @author nozomihitomi
 */
public class FOVHandler implements EventHandler<FOVDetector> {

    private final CoveragePoint covPt;

    private final TimeIntervalArray timeArray;

    /**
     * Creates a field of view handler for a specific coverage point.
     *
     * @param covPt The coverage point that the fov handler is assigned to
     * @param startDate the start date of the simulation
     * @param endDate the end date of the simulation
     */
    public FOVHandler(CoveragePoint covPt, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.covPt = covPt;
        this.timeArray = new TimeIntervalArray(startDate, endDate);
    }

    @Override
    public EventHandler.Action eventOccurred(final SpacecraftState s, final FOVDetector detector,
            final boolean increasing) throws OrekitException {

        if (increasing) {
            //Access begins
            timeArray.addRiseTime(s.getDate());
        } else {
            //Access ends
            timeArray.addSetTime(s.getDate());
        }

        return EventHandler.Action.CONTINUE;
    }

    public CoveragePoint getCovPt() {
        return covPt;
    }

    public TimeIntervalArray getTimeArray() {
        return timeArray;
    }

}
