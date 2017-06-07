/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.analysis.events;

import java.util.Collection;
import seak.orekit.analysis.AbstractAnalysis;
import seak.orekit.analysis.Record;
import seak.orekit.events.FOVDetector;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;

/**
 * This analysis will record the g() function values at each time step
 * @author SEAK1
 */
public class EventAnalysis extends AbstractAnalysis<GValues>{
    private static final long serialVersionUID = -6588467353574801952L;

    private final Collection<FOVDetector> detectors;

    public EventAnalysis(double timeStep, Collection<FOVDetector> detectors) {
        super(timeStep);
        this.detectors = detectors;
    }

    @Override
    public String getExtension() {
        return "event";
    }

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        GValues<EventDetector> gvals = new GValues();
        for(EventDetector ed : detectors){
            gvals.put(ed, ed.g(currentState));
        }
        Record<GValues> e = new Record(currentState.getDate(), gvals);
        history.add(e);
    }
}
