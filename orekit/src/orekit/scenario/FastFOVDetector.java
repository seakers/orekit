/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.FieldOfView;
import org.orekit.propagation.events.FieldOfViewDetector;
import org.orekit.propagation.events.handlers.EventHandler;

/**
 *
 * @author nozomihitomi
 */
public class FastFOVDetector extends FieldOfViewDetector {

    private static final long serialVersionUID = 3765360560924280705L;

    private final TopocentricFrame target;

    private final double minElevation;

    /**
     * The elevation threshold is by default set to 0degrees (i.e. satellite and
     * target must have line of sight before FieldOfViewDetector g() function is
     * computed)
     *
     * @param target the target to attach the detector to
     * @param fov the field of view that will observe the target
     */
    public FastFOVDetector(TopocentricFrame target, FieldOfView fov) {
        this(target, fov, 0.0);
    }

    /**
     *
     * @param target the target to attach the detector to
     * @param fov the field of view that will observe the target
     * @param elevationThreshold the minimum elevation threshold [radians] that
     * must be met before the FieldOfViewDetector g() function is computed
     */
    public FastFOVDetector(TopocentricFrame target, FieldOfView fov, double elevationThreshold) {
        super(target, fov);
        this.target = target;
        this.minElevation = FastMath.toRadians(0);
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
     * @param s
     * @return
     */
    private double g_lineOfSight(SpacecraftState s) throws OrekitException {

        final double trueElevation = target.getElevation(s.getPVCoordinates().getPosition(),
                s.getFrame(), s.getDate());
        return trueElevation - minElevation;
    }

    /**
     * The implementation of this g() function relies on the same implementation
     * of the FieldOfViewDetector but first computes line of sight. If there is
     * no line of sight between the satellite and the target, the more expensive
     * computation of the FieldOfViewDetector g function is not executed.
     *
     * @param s
     * @return
     * @throws OrekitException
     */
    @Override
    public double g(SpacecraftState s) throws OrekitException {
        double gLOS = g_lineOfSight(s);
        if (gLOS >= 0) {
            //only compute the more expensive FieldOfViewDetector g function if 
            //the target and satellite meet the minimum elevation threshold
            return super.g(s);
        }else
            return gLOS;
    }

    @Override
    public FieldOfViewDetector withHandler(EventHandler<? super FieldOfViewDetector> newHandler) {
        return super.withHandler(newHandler); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FieldOfViewDetector withThreshold(double newThreshold) {
        return super.withThreshold(newThreshold); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FieldOfViewDetector withMaxIter(int newMaxIter) {
        return super.withMaxIter(newMaxIter); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FieldOfViewDetector withMaxCheck(double newMaxCheck) {
        return super.withMaxCheck(newMaxCheck); //To change body of generated methods, choose Tools | Templates.
    }
    
    

}
