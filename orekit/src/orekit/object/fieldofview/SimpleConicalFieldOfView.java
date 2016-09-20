/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object.fieldofview;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;

/**
 * This field of view models a simple cone. It is defined with a center axis in
 * the spacecraft frame of reference, and the half aperture angle.
 *
 * @author nozomihitomi
 */
public class SimpleConicalFieldOfView extends AbstractFieldOfViewDefinition{
    private static final long serialVersionUID = -5871573780685218252L;

    private final Vector3D centerAxis;

    private final double halfAngle;

    /**
     * Constructor to create a simple conical field of view
     *
     * @param centerAxis Direction of the FOV center, in spacecraft frame
     * @param halfAngle FOV half aperture angle, must be less than π/2.
     */
    public SimpleConicalFieldOfView(Vector3D centerAxis, double halfAngle) {
        this.centerAxis = centerAxis;
        this.halfAngle = halfAngle;
    }

    public Vector3D getCenterAxis() {
        return centerAxis;
    }

    public double getHalfAngle() {
        return halfAngle;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The g function value is for circular field of views and is the difference
     * between FOV half aperture and the absolute value of the angle between
     * target direction and field of view center. It is positive inside the FOV
     * and negative outside.
     * </p>
     * @param s the current spacecraft state
     * @param target
     * @return 
     * @throws org.orekit.errors.OrekitException
     */
    protected double g(SpacecraftState s, TopocentricFrame target) throws OrekitException {

        // Compute target position/velocity at date in spacecraft frame */
        final Vector3D targetPosInert = new Vector3D(1, target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                -1, s.getPVCoordinates().getPosition());
        final Vector3D targetPosSat = s.getAttitude().getRotation().applyTo(targetPosInert);

        // Target is in the field of view if the absolute value that angle is smaller than FOV half aperture.
        // g function value is the difference between FOV half aperture and the absolute value of the angle between
        // target direction and field of view center. It is positive inside the FOV and negative outside.
        return halfAngle - Vector3D.angle(targetPosSat, centerAxis);
    }

    @Override
    public String toString() {
        return "SimpleConicalFieldOfView{" + "halfAngle=" + halfAngle + '}';
    }

}
