/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.analysis.vectors;

import java.lang.reflect.Method;
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
public abstract class VectorAngleAnalysis extends AbstractAnalysis<Double> {
    private static final long serialVersionUID = 7181097236602721128L;

    /**
     * The desired frame to use. Frame used to transform vectors into the same
     * frame.
     */
    private final Frame frame;


    /**
     * Constructor for when one of the vectors does not need to be retrieved
     * from the spacecraft state. Vector 1, which is a non-spacecraft state
     * vector must be provided in the Frame that is provided or results may be
     * nonsensical. The other vector is defined by a method call to the class
     * SpacecraftState, and the frame will be used to transform the spacecraft
     * state vector to the desired frame.
     *
     * @param frame
     * @param timeStep
     */
    public VectorAngleAnalysis(Frame frame, double timeStep) {
        super(timeStep);
        this.frame = frame;
    }
    
    public abstract Vector3D getVector1(SpacecraftState currentState, Frame frame);
    
    public abstract Vector3D getVector2(SpacecraftState currentState, Frame frame);

    @Override
    public String getHeader() {
        return super.getHeader() + ",angle[rad]"; //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String getExtension() {
        return  "vecang";
    } 

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        Vector3D v1 = getVector1(currentState, frame);
        Vector3D v2 = getVector2(currentState, frame);
        addRecord(new Record(currentState.getDate(),Vector3D.angle(v1, v2)));
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
        final VectorAngleAnalysis other = (VectorAngleAnalysis) obj;
        if (!Objects.equals(this.frame, other.frame)) {
            return false;
        }
        return true;
    }

    
    
    
    

}
