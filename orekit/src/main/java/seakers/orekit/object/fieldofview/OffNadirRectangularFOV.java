/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.fieldofview;

import org.hipparchus.geometry.Vector;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;

/**
 * This class models a rectangular field of view. The field of view is defined
 * with a center axis, two axes to define the aperture directions, and two half
 * aperture angles.
 *
 * @author paugarciabuzzi
 */
public class OffNadirRectangularFOV extends RectangularFieldOfView {
    private static final long serialVersionUID = 1559069965493414756L;

    private double lookAngle;

    private final BodyShape shape;

    /**
     * Build a Field Of View with dihedral shape (i.e. rectangular shape).
     * @param lookAngle or off nadir angle [rad] that defines the direction of the FOV center, in spacecraft frame.
     * Assumption: center axis is in the the plane perpendicular to the velocity vector (1,0,0).
     * @param acrossTrackHalfAperture FOV half aperture angle in the across-track direction,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param alongTrackHalfAperture FOV half aperture angle in the along-track direction,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     * @param shape the shape of the body to define the nadir direction
     * @throws OrekitException
     */
    public OffNadirRectangularFOV(double lookAngle, double acrossTrackHalfAperture, double alongTrackHalfAperture,
                                  double margin, BodyShape shape) throws OrekitException {
        this(new Vector3D(0,Math.sin(lookAngle),Math.cos(lookAngle)),
                Vector3D.PLUS_I, acrossTrackHalfAperture,
                new Vector3D(0, Math.cos(lookAngle),-Math.sin(lookAngle)), alongTrackHalfAperture,
                margin, shape);
        this.lookAngle=lookAngle;
    }

    /**
     * Build a Field Of View with dihedral shape (i.e. rectangular shape).
     * @param axis0 that defines the direction of the FOV center, in spacecraft frame
     * @param axis1 FOV dihedral axis 1, in spacecraft frame
     * @param halfAperture1 FOV dihedral half aperture angle 1,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param axis2 FOV dihedral axis 2, in spacecraft frame
     * @param halfAperture2 FOV dihedral half aperture angle 2,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     * @param shape the shape of the body to define the nadir direction
     * @throws OrekitException
     */
    public OffNadirRectangularFOV(Vector3D axis0,
            Vector3D axis1, double halfAperture1,
            Vector3D axis2, double halfAperture2,
            double margin, BodyShape shape) throws OrekitException {
        super(axis0,axis1, halfAperture1, axis2, halfAperture2, margin);
        this.shape = shape;
    }

    
        @Override
    protected double g(SpacecraftState s, TopocentricFrame target) throws OrekitException {
        // get line of sight in spacecraft frame
        final Vector3D targetPosInert
                = target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        Vector3D spacecraftPosInert = s.getPVCoordinates(s.getFrame()).getPosition();
        Vector3D losInert = spacecraftPosInert.subtract(targetPosInert);
        Rotation rot = alignWithNadirAndNormal(Vector3D.PLUS_K, Vector3D.PLUS_J, s, s.getOrbit(), shape, s.getFrame());
        Vector3D lineOfSightSC = rot.applyTo(losInert);
        //TODO find out why need to take the negative
        return -getFov().offsetFromBoundary(lineOfSightSC.negate());
    }

    public double getLookAngle() {
        return lookAngle;
    }

    @Override
    public String toString() {
        return "OffNadirFieldOfView{ LookAngle="+getLookAngle() + ", HalfAngle1=" + getHalfAperture1() + ", HalfAngle2=" + getHalfAperture2() + "}";
    }

    
    
}
