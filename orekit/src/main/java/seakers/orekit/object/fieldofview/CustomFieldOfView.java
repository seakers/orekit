/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.fieldofview;

import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.Region;
import org.hipparchus.geometry.spherical.twod.Edge;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.geometry.spherical.twod.Vertex;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.FieldOfView;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nozomihitomi
 */
public class CustomFieldOfView extends AbstractFieldOfViewDefinition {
    private static final long serialVersionUID = -9001535864555179340L;

    private final FieldOfView fov;

    public CustomFieldOfView(FieldOfView fov) {
        super(0.0);
        this.fov = fov;
    }

    public FieldOfView getFov() {
        return fov;
    }

    @Override
    public String toString() {
        return "CustomFieldOfView{}";
    }

    /**
     * <p>
     * The g function value is the angular offset between the target and the {@link FieldOfView#offsetFromBoundary(Vector3D)
     * Field Of View boundary}. It is positive if the target is visible within
     * the Field Of View and negative if it is outside of the Field Of View,
     * including the margin.
     * </p>
     * <p>
     * As per the previous definition, when the target enters the Field Of View,
     * a decreasing event is generated, and when the target leaves the Field Of
     * View, an increasing event is generated.
     * </p>
     * @param s current state of the spacecraft
     * @param target the target to view
     * @return 
     * @throws org.orekit.errors.OrekitException
     */
    @Override
    protected double g(SpacecraftState s, TopocentricFrame target) throws OrekitException {
        // get line of sight in spacecraft frame
        final Vector3D targetPosInert
                = target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D lineOfSightSC = s.toTransform().transformPosition(targetPosInert);

        return -fov.offsetFromBoundary(lineOfSightSC);

    }

    @Override
    public double offsetFromBoundary(Vector3D lineOfSight) {
        return fov.offsetFromBoundary(lineOfSight);
    }

    @Override
    public RealVector g_FOV(RealMatrix lineOfSight) {
        double[] out = new double[lineOfSight.getColumnDimension()];
        for(int i=0; i <lineOfSight.getColumnDimension(); i++){
            out[i] = -offsetFromBoundary(new Vector3D(lineOfSight.getColumn(i)));
        }
        return new ArrayRealVector(out);
    }

    // TODO: This function is a direct copy from a private Orekit function. Please remove when the Orekit devs make it public
    /** Get the footprint of the field Of View on ground.
     * <p>
     * This method assumes the Field Of View is centered on some carrier,
     * which will typically be a spacecraft or a ground station antenna.
     * The points in the footprint boundary loops are all at altitude zero
     * with respect to the ellipsoid, they correspond either to projection
     * on ground of the edges of the Field Of View, or to points on the body
     * limb if the Field Of View goes past horizon. The points on the limb
     * see the carrier origin at zero elevation. If the Field Of View is so
     * large it contains entirely the body, all points will correspond to
     * points at limb. If the Field Of View looks away from body, the
     * boundary loops will be an empty list. The points within footprint
     * the loops are sorted in trigonometric order as seen from the carrier.
     * This implies that someone traveling on ground from one point to the
     * next one will have the points visible from the carrier on his left
     * hand side, and the points not visible from the carrier on his right
     * hand side.
     * </p>
     * <p>
     * The truncation of Field Of View at limb can induce strange results
     * for complex Fields Of View. If for example a Field Of View is a
     * ring with a hole and part of the ring goes past horizon, then instead
     * of having a single loop with a C-shaped boundary, the method will
     * still return two loops truncated at the limb, one clockwise and one
     * counterclockwise, hence "closing" the C-shape twice. This behavior
     * is considered acceptable.
     * </p>
     * <p>
     * If the carrier is a spacecraft, then the {@code fovToBody} transform
     * can be computed from a {@link org.orekit.propagation.SpacecraftState}
     * as follows:
     * </p>
     * <pre>
     * Transform inertToBody = state.getFrame().getTransformTo(body.getBodyFrame(), state.getDate());
     * Transform fovToBody   = new Transform(state.getDate(),
     *                                       state.toTransform().getInverse(),
     *                                       inertToBody);
     * </pre>
     * <p>
     * If the carrier is a ground station, located using a topocentric frame
     * and managing its pointing direction using a transform between the
     * dish frame and the topocentric frame, then the {@code fovToBody} transform
     * can be computed as follows:
     * </p>
     * <pre>
     * Transform topoToBody = topocentricFrame.getTransformTo(body.getBodyFrame(), date);
     * Transform topoToDish = ...
     * Transform fovToBody  = new Transform(date,
     *                                      topoToDish.getInverse(),
     *                                      topoToBody);
     * </pre>
     * <p>
     * Only the raw zone is used, the angular margin is ignored here.
     * </p>
     * @param fovToBody transform between the frame in which the Field Of View
     * is defined and body frame.
     * @param body body surface the Field Of View will be projected on
     * @param angularStep step used for boundary loops sampling (radians)
     * @return list footprint boundary loops (there may be several independent
     * loops if the Field Of View shape is complex)
     */
    public List<List<GeodeticPoint>> getFootprint(final Transform fovToBody, final OneAxisEllipsoid body,
                                           final double angularStep) {

        final Frame bodyFrame = body.getBodyFrame();
        final Vector3D  position  = fovToBody.transformPosition(Vector3D.ZERO);
        final double    r         = position.getNorm();
        if (body.isInside(position)) {
            throw new OrekitException(OrekitMessages.POINT_INSIDE_ELLIPSOID);
        }

        final SphericalPolygonsSet zone = fov.getZone();

        final List<List<GeodeticPoint>> footprint = new ArrayList<List<GeodeticPoint>>();

        final List<Vertex> boundary = zone.getBoundaryLoops();
        for (final Vertex loopStart : boundary) {
            int count = 0;
            final List<GeodeticPoint> loop  = new ArrayList<GeodeticPoint>();
            boolean intersectionsFound      = false;
            for (Edge edge = loopStart.getOutgoing();
                 count == 0 || edge.getStart() != loopStart;
                 edge = edge.getEnd().getOutgoing()) {
                ++count;
                final int    n     = (int) FastMath.ceil(edge.getLength() / angularStep);
                final double delta =  edge.getLength() / n;
                for (int i = 0; i < n; ++i) {
                    final Vector3D awaySC      = new Vector3D(r, edge.getPointAt(i * delta));
                    final Vector3D awayBody    = fovToBody.transformPosition(awaySC);
                    final Line lineOfSight = new Line(position, awayBody, 1.0e-3);
                    GeodeticPoint  gp          = body.getIntersectionPoint(lineOfSight, position,
                            bodyFrame, null);
                    if (gp != null &&
                            Vector3D.dotProduct(awayBody.subtract(position),
                                    body.transform(gp).subtract(position)) < 0) {
                        // the intersection is in fact on the half-line pointing
                        // towards the back side, it is a spurious intersection
                        gp = null;
                    }

                    if (gp != null) {
                        // the line of sight does intersect the body
                        intersectionsFound = true;
                    } else {
                        // the line of sight does not intersect body
                        // we use a point on the limb
                        gp = body.transform(body.pointOnLimb(position, awayBody), bodyFrame, null);
                    }

                    // add the point in front of the list
                    // (to ensure the loop will be in trigonometric orientation)
                    loop.add(0, gp);

                }
            }

            if (intersectionsFound) {
                // at least some of the points did intersect the body,
                // this loop contributes to the footprint
                footprint.add(loop);
            }

        }

        if (footprint.isEmpty()) {
            // none of the Field Of View loops cross the body
            // either the body is outside of Field Of View, or it is fully contained
            // we check the center
            final Vector3D bodyCenter = fovToBody.getInverse().transformPosition(Vector3D.ZERO);
            if (zone.checkPoint(new S2Point(bodyCenter)) != Region.Location.OUTSIDE) {
                // the body is fully contained in the Field Of View
                // we use the full limb as the footprint
                final Vector3D x        = bodyCenter.orthogonal();
                final Vector3D y        = Vector3D.crossProduct(bodyCenter, x).normalize();
                final double   sinEta   = body.getEquatorialRadius() / r;
                final double   sinEta2  = sinEta * sinEta;
                final double   cosAlpha = (FastMath.cos(angularStep) + sinEta2 - 1) / sinEta2;
                final int      n        = (int) FastMath.ceil(MathUtils.TWO_PI / FastMath.acos(cosAlpha));
                final double   delta    = MathUtils.TWO_PI / n;
                final List<GeodeticPoint> loop = new ArrayList<GeodeticPoint>(n);
                for (int i = 0; i < n; ++i) {
                    final Vector3D outside = new Vector3D(r * FastMath.cos(i * delta), x,
                            r * FastMath.sin(i * delta), y);
                    loop.add(body.transform(body.pointOnLimb(position, outside), bodyFrame, null));
                }
                footprint.add(loop);
            }
        }

        return footprint;

    }
}
