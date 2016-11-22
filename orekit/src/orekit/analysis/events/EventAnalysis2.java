/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.analysis.events;

import java.util.Collection;
import orekit.analysis.AbstractAnalysis;
import orekit.analysis.Record;
import org.hipparchus.geometry.enclosing.EnclosingBall;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.FieldOfView;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This analysis will record the g() function values at each time step
 *
 * @author SEAK1
 */
public class EventAnalysis2 extends AbstractAnalysis<GValues> {

    private final Collection<TopocentricFrame> points;

    private final BodyShape shape;

    private final double minRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING);

    private final FieldOfView fov;

    public EventAnalysis2(double timeStep, Collection<TopocentricFrame> points, BodyShape shape, FieldOfView fov) {
        super(timeStep);
        this.points = points;
        this.shape = shape;
        this.fov = fov;
    }

    @Override
    public String getExtension() {
        return "LOSevent";
    }

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        RealMatrix mPos = MatrixUtils.createRealMatrix(points.size(), 3);
        int row = 0;
        for (TopocentricFrame pt : points) {
            mPos.setRow(row, pt.getPVCoordinates(currentState.getDate(), currentState.getFrame()).getPosition().normalize().toArray());
            row++;
        }
        RealMatrix sPos = new Array2DRowRealMatrix(currentState.getPVCoordinates().getPosition().normalize().toArray());
        RealMatrix cosThetas = mPos.multiply(sPos);

        double minCosTheta = minRadius / currentState.getA();

        GValues<TopocentricFrame> gvals = new GValues();

        row = 0;
        for (TopocentricFrame pt : points) {
            double losVal = cosThetas.getEntry(row, 0) - minCosTheta;
            if (losVal < 0) {
                gvals.put(pt, losVal);

            } else {
                //r is rotation matrix from inertial frame to spacecraft body-center-pointing frame
                Vector3D targetPosInert = pt.getPVCoordinates(currentState.getDate(), currentState.getFrame()).getPosition();
                Vector3D spacecraftPosInert = currentState.getPVCoordinates(currentState.getFrame()).getPosition();
                Vector3D losInert = spacecraftPosInert.subtract(targetPosInert);
                Rotation rot = alignWithNadirAndNormal(Vector3D.PLUS_K, Vector3D.PLUS_J, currentState, currentState.getOrbit(), shape, currentState.getFrame());
                Vector3D lineOfSightSC = rot.applyTo(losInert);

                gvals.put(pt, -fov.offsetFromBoundary(lineOfSightSC.negate()));
            }

            row++;
        }

        Record<GValues> e = new Record(currentState.getDate(), gvals);
        history.add(e);
    }

    /**
     * Gets the nadir pointing vector at the current spacecraft state.
     *
     * @param s current spacecraft state
     * @param pvProv
     * @param shape
     * @param frame
     * @return
     * @throws OrekitException
     */
    protected Vector3D getSpacecraftToNadirPosition(final SpacecraftState s, final PVCoordinatesProvider pvProv, final BodyShape shape,
            final Frame frame) throws OrekitException {
        final Vector3D nadirPosRef = getNadirPosition(pvProv, shape, s.getDate(), frame);
        return nadirPosRef.subtract(s.getPVCoordinates(s.getFrame()).getPosition());
    }

    /**
     * Gets the nadir position in the spacecraft frame
     *
     * @param pvProv pv coordinate provider for the spacecraft
     * @param shape The shape of the body which the satellite orbits
     * @param date The date at which to obtain the nadir position
     * @param frame The frame in which to obtain the nadir vector
     * @return The position vector of the nadir point in the given frame of
     * reference
     * @throws org.orekit.errors.OrekitException
     */
    protected Vector3D getNadirPosition(final PVCoordinatesProvider pvProv, final BodyShape shape,
            final AbsoluteDate date, final Frame frame) throws OrekitException {

        // transform from specified reference frame to body frame
        final Transform refToBody = frame.getTransformTo(shape.getBodyFrame(), date);
        return nadirRef(pvProv.getPVCoordinates(date, frame), shape, refToBody).getPosition();
    }

    /**
     * Compute ground point in nadir direction, in reference frame. This method
     * is based on the nadir pointing law NadirPointin
     *
     * @param scRef spacecraft coordinates in reference frame
     * @param The shape of the body which the satellite orbits
     * @param refToBody transform from reference frame to body frame
     * @return intersection point in body frame (only the position is set!)
     * @exception OrekitException if line of sight does not intersect body
     */
    private TimeStampedPVCoordinates nadirRef(final TimeStampedPVCoordinates scRef, final BodyShape shape, final Transform refToBody)
            throws OrekitException {

        final Vector3D satInBodyFrame = refToBody.transformPosition(scRef.getPosition());

        // satellite position in geodetic coordinates
        final GeodeticPoint gpSat = shape.transform(satInBodyFrame, shape.getBodyFrame(), scRef.getDate());

        // nadir position in geodetic coordinates
        final GeodeticPoint gpNadir = new GeodeticPoint(gpSat.getLatitude(), gpSat.getLongitude(), 0.0);

        // nadir point position in body frame
        final Vector3D pNadirBody = shape.transform(gpNadir);

        // nadir point position in reference frame
        final Vector3D pNadirRef = refToBody.getInverse().transformPosition(pNadirBody);

        return new TimeStampedPVCoordinates(scRef.getDate(), pNadirRef, Vector3D.ZERO, Vector3D.ZERO);

    }

    /**
     * This method returns a rotation matrix that will transform vectors v1 and
     * v2 to point toward nadir and the vector that is normal to the orbital
     * plane, respectively. Normal vector is used instead of the velocity vector
     * because the velocity vector and nadir vector may not be orthogonal
     *
     * @param v1 Vector to line up with nadir
     * @param v2 Vector to line up with the velocity vector
     * @param s the current spacecraft state
     * @param pvProv the pv provider for the satellite
     * @param shape the shape of the body to define nadir direction
     * @param frame the reference frame to translate vectors to
     * @return
     * @throws OrekitException
     */
    protected Rotation alignWithNadirAndNormal(Vector3D v1, Vector3D v2,
            final SpacecraftState s, final PVCoordinatesProvider pvProv, final BodyShape shape,
            final Frame frame) throws OrekitException {
        final Vector3D nadirRef = getSpacecraftToNadirPosition(s, pvProv, shape, frame).normalize();
        Vector3D velRef = pvProv.getPVCoordinates(s.getDate(), frame).getVelocity().normalize();
        Vector3D orbitNormal = nadirRef.crossProduct(velRef).normalize();

        return new Rotation(nadirRef, orbitNormal, v1, v2);
    }
}
