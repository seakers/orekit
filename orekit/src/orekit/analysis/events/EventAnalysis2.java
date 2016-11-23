/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.analysis.events;

import java.util.Collection;
import java.util.HashMap;
import orekit.analysis.AbstractAnalysis;
import orekit.analysis.Record;
import orekit.object.CoveragePoint;
import org.hipparchus.geometry.enclosing.EnclosingBall;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
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
    
    private final HashMap<TopocentricFrame, Integer> pointMap;
    
    private final Frame inertialFrame;
    
    private final RealMatrix initPointPos;

    public EventAnalysis2(double timeStep, Collection<TopocentricFrame> points, BodyShape shape, FieldOfView fov, Frame inertialFrame, AbsoluteDate startDate) throws OrekitException {
        super(timeStep);
        this.points = points;
        this.shape = shape;
        this.fov = fov;

        //build initial position vector matrix that can be reused by rotation matrix
        this.initPointPos = new Array2DRowRealMatrix(3, points.size());
        int col = 0;
        this.pointMap = new HashMap<>(points.size());
        for (TopocentricFrame pt : points) {
            initPointPos.setColumn(col, pt.getPVCoordinates(startDate, inertialFrame).getPosition().normalize().toArray());
            pointMap.put(pt, col);
            col++;
        }
        this.inertialFrame = inertialFrame;
    }

    @Override
    public String getExtension() {
        return "LOSevent";
    }

    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException {
        //The spacecraft position in the inertial frame
        RealVector satPosInert = new ArrayRealVector(currentState.getPVCoordinates().getPosition().toArray());
        RealVector satPosInertNorm = satPosInert.mapDivideToSelf(satPosInert.getNorm());
        //The normalized position vectors of the points in the inertial frame
        RealMatrix ptPosInertNorm = MatrixUtils.createRealMatrix(3, points.size());
        //The vector between the satellite and point position in the inertial frame
        RealMatrix sat2ptLineInert = MatrixUtils.createRealMatrix(3, points.size());
        
        RealMatrix pointRotation = new Array2DRowRealMatrix(shape.getBodyFrame().
                    getTransformTo(inertialFrame, currentState.getDate()).getInverse().
                    getRotation().getMatrix());
        
        RealMatrix ptPosInert = pointRotation.multiply(pointRotation);
        
        int col = 0;
        for (TopocentricFrame pt : points) {
//            Vector3D pointPos = pt.getPVCoordinates(currentState.getDate(), currentState.getFrame()).getPosition();
            ptPosInertNorm.setColumnVector(col, ptPosInert.getColumnVector(col).mapDivideToSelf(ptPosInert.getColumnVector(col).getNorm()));
            sat2ptLineInert.setColumnVector(col, satPosInert.subtract(ptPosInert.getColumnVector(col)));
            col++;
        }
        RealVector cosThetas = ptPosInertNorm.preMultiply(satPosInertNorm);

        double minCosTheta = minRadius / currentState.getA();

        //rot is rotation matrix from inertial frame to spacecraft body-center-pointing frame
        Rotation rot = alignWithNadirAndNormal(Vector3D.PLUS_K, Vector3D.PLUS_J, currentState, currentState.getOrbit(), shape, currentState.getFrame());
        RealMatrix rotMatrix = new Array2DRowRealMatrix(rot.getMatrix());
        //line of sight vectors in spacecraft frame
        RealMatrix losSC = rotMatrix.multiply(sat2ptLineInert);

        GValues<TopocentricFrame> gvals = new GValues();

        col = 0;
        for (TopocentricFrame pt : points) {
            double losVal = cosThetas.getEntry(col) - minCosTheta;
            if (losVal < 0) {
                gvals.put(pt, losVal);
            } else {
                gvals.put(pt, -fov.offsetFromBoundary((new Vector3D(losSC.getColumn(col))).negate()));
            }

            col++;
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
