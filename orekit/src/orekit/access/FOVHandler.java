/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.access;

import orekit.object.CoveragePoint;
import orekit.object.fieldofview.FOVDetector;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;

/**
 * the g() function for the FOVDetector assumes that the target enters
 the FieldOfView when the value is positive and exits when the value is
 negative. Therefore, when an event is detected and the value is increasing,
 * the target is entering the FieldOfView.
 *
 * @author nozomihitomi
 */
public class FOVHandler implements EventHandler<FOVDetector> {

    @Override
    public EventHandler.Action eventOccurred(final SpacecraftState s, final FOVDetector detector,
            final boolean increasing) throws OrekitException {

        CoveragePoint target = (CoveragePoint) detector.getPVTarget();
        if (increasing) {
            //Access begins
            target.addRiseTime(s.getDate());
        } else {
            //Access ends
            target.addSetTime(s.getDate());
        }

        return EventHandler.Action.CONTINUE;
    }

}
