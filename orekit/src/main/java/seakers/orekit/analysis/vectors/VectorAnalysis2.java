/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.vectors;

import java.util.logging.Level;
import java.util.logging.Logger;
import seakers.orekit.analysis.Record;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.analysis.AbstractSpacecraftAnalysis;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * This analysis will record how a vector changes in the given frame of reference. 
 * The vector must be accessible from the spacecraft state or this CelestialBody.
 * This class is very similar to VectorAnalysis but we can have access to a 
 * CelestialBody such as sun or moon.
 *
 * @author paugarciabuzzi
 */
public abstract class VectorAnalysis2 extends VectorAnalysis {

    /**
     * Celestial Body needed to define vector. For instance, the Sun to compute the
     * Satellite-Sun vector
     */
    private final CelestialBody body;

    /**
     * Constructor to obtain a 3D vector in the specified frame at the specified time step.
     *
     * @param startDate
     * @param endDate
     * @param timeStep
     * @param sat
     * @param propagatorFactory
     * @param frame
     * @param body
     *
     */
    public VectorAnalysis2(AbsoluteDate startDate, AbsoluteDate endDate,
            double timeStep, Satellite sat, PropagatorFactory propagatorFactory, Frame frame, CelestialBody body) {
        super(startDate, endDate, timeStep, sat, propagatorFactory,frame);
        this.body = body;
    }

}
