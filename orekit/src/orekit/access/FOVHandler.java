/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.access;

import orekit.object.CoveragePoint;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.FieldOfViewDetector;
import org.orekit.propagation.events.handlers.EventHandler;

/**
 * the g() function for the FieldOfViewDetector assumes that the target enters
 * the FieldOfView when the value is negative and exits when the value is
 * positive. Therefore, when an event is detected and the value is decreasing,
 * the target is entering the FieldOfView.
 *
 * @author nozomihitomi
 */
public class FOVHandler implements EventHandler<FieldOfViewDetector> {

    @Override
    public EventHandler.Action eventOccurred(final SpacecraftState s, final FieldOfViewDetector detector,
            final boolean increasing) {

        CoveragePoint target = (CoveragePoint) detector.getPVTarget();
        if (increasing) {
            //Access ends
            target.addSetTime(s.getDate());
        } else {
            //Access Begins
            target.addRiseTime(s.getDate());
        }

        return EventHandler.Action.CONTINUE;
    }

}
