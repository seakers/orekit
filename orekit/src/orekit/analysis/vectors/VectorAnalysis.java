/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.analysis.vectors;

import java.util.Objects;
import orekit.analysis.AbstractAnalysis;
import orekit.analysis.Record;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;

/**
 * This analysis will record how the angle [rad] between two vector changes in the
 * given frame of reference. One of the vectors must be accessible from the
 * spacecraft state
 *
 * @author nozomihitomi
 */
public abstract class VectorAnalysis extends AbstractAnalysis<String> {
    private static final long serialVersionUID = 920486541477495142L;

    /**
     * The desired frame to use. Frame used to transform vectors into the same
     * frame.
     */
    private final Frame frame;


    /**
     * Constructor to obtain a 3D vector in the specified frame at the specified time step.
     *
     * @param frame
     * @param timeStep
     */
    public VectorAnalysis(Frame frame, double timeStep) {
        super(timeStep);
        this.frame = frame;
    }
    
    public abstract Vector3D getVector(SpacecraftState currentState, Frame frame) throws OrekitException;

    @Override
    public String getHeader() {
        return super.getHeader() + ",x,y,z"; //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String getExtension() {
        return  "vec";
    } 

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        Vector3D pos = getVector(currentState, frame);
        String str = String.format("%f,%f,%f", pos.getX(),pos.getY(),pos.getZ());
        addRecord(new Record(currentState.getDate(),str));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.frame);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VectorAnalysis other = (VectorAnalysis) obj;
        if (!Objects.equals(this.frame, other.frame)) {
            return false;
        }
        return true;
    }
}
