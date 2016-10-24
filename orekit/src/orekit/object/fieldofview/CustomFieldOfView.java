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
import org.orekit.propagation.events.FieldOfView;

/**
 *
 * @author nozomihitomi
 */
public class CustomFieldOfView extends AbstractFieldOfViewDefinition{
    private static final long serialVersionUID = -9001535864555179340L;

    private final FieldOfView fov;

    public CustomFieldOfView(FieldOfView fov) {
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

    }
