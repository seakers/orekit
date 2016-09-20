/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object.fieldofview;

import java.io.Serializable;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;

/**
 * This class is responsible for making field of view detection more
 * computationally efficient by computing the elevation between ground targets
 * and the spacecraft. The user can decide to use a minimum elevation (from the
 * perspective of the target) that must be exceeded before attempting a more
 * expensive field of view computation.
 *
 * @author nozomihitomi
 */
public abstract class AbstractFieldOfViewDefinition implements FieldOfViewDefinition, Serializable {
    private static final long serialVersionUID = -2441300006537852331L;

    private final double minElevation;

    /**
     * Use this constructor to set the minimum elevation that must be exceeded
     * before the more expensive field of view computation is carried out.
     *
     * @param minElevation the minimum elevation threshold [radians] that must
     * be met before the FieldOfViewDetector g() function is computed
     */
    public AbstractFieldOfViewDefinition(double minElevation) {
        this.minElevation = minElevation;
    }

    /**
     * This constructor assumes that line of sight is required before the more
     * expensive field of view computation is carried out.
     */
    public AbstractFieldOfViewDefinition() {
        this(0.0);
    }

    /**
     * Function to see if the given target is in line of sight with the
     * spacecraft (regardless of attitude). Implementation is similar to the
     * g(s) function from the elevation detector. That is, the g_lineOfSight
     * function value is the difference between the current elevation (and
     * azimuth if necessary) and the reference mask or minimum value (i.e. set
     * to 0degrees). This function will return a positive value if the satellite
     * and the target have line of sight. The function will return a negative
     * value if the satellite and the target do not have line of sight
     *
     * @param s the current spacecraft state
     * @param target The target to be viewed
     * @return This function will return a positive value if the satellite and
     * the target have line of sight. The function will return a negative value
     * if the satellite and the target do not have line of sight
     * @throws org.orekit.errors.OrekitException
     */
    private double g_lineOfSight(SpacecraftState s, TopocentricFrame target) throws OrekitException {

        final double trueElevation = target.getElevation(s.getPVCoordinates().getPosition(),
                s.getFrame(), s.getDate());
        return trueElevation - minElevation;
    }

    /**
     * The unique g function for a given field of view shape.
     *
     * @param s the spacecraft's current state
     * @param target the target of interest
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    protected abstract double g(SpacecraftState s, TopocentricFrame target) throws OrekitException;

    /**
     * This g() function is positive when the given target enters the field of
     * view and negative if the target is outside the field of view
     *
     * @param s the spacecraft's current state
     * @param target the target of interest
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    @Override
    public double g_FOV(SpacecraftState s, TopocentricFrame target) throws OrekitException {
        double los = g_lineOfSight(s, target);
        if (los > 0) {
            return g(s, target);
        } else {
            return los;
        }
    }
}
