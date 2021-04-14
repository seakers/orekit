/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoveragePoint;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects when specular reflection between two satellites occurs.
 *
 * @author ben
 */
public class ReflectorDetector extends AbstractEventDetector<ReflectorDetector> {

    private static final long serialVersionUID = 1L;

    /**
     * The ground target
     */
    private final CoveragePoint target;

    /**
     * The Transmitter state
     */
    private final Propagator pfTransmitter;

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable angle (e.g. max elevation angle). This constructor
     * assumes that the threshold is with respect to the Zenith direction. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @throws OrekitException
     */
    public ReflectorDetector(SpacecraftState initialState,
                             AbsoluteDate startDate, AbsoluteDate endDate,
                             CoveragePoint target, Propagator pfTransmitter) throws OrekitException {
        this(initialState, startDate, endDate, target, pfTransmitter, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable angle (e.g. max elevation angle). This constructor
     * assumes that the threshold is with respect to the Zenith direction. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @throws OrekitException
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public ReflectorDetector(SpacecraftState initialState,
                             AbsoluteDate startDate, AbsoluteDate endDate, CoveragePoint target, Propagator pfTransmitter, double maxCheck,
                             double threshold) throws OrekitException {
        this(initialState, startDate, endDate,
                target, pfTransmitter, Action.CONTINUE,
                DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER);
    }

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable angle (e.g. max elevation angle). This constructor
     * assumes that the threshold is with respect to the Zenith direction. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @throws OrekitException
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action specifies action after event is detected.
     */
    public ReflectorDetector(SpacecraftState initialState,
                             AbsoluteDate startDate, AbsoluteDate endDate, CoveragePoint target, Propagator pfTransmitter, Action action,
                             double maxCheck, double threshold) throws OrekitException {
        this(initialState, startDate, endDate,
                target, pfTransmitter, action,
                DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER);
    }

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable angle (e.g. max elevation angle). This constructor
     * assumes that the threshold is with respect to the Zenith direction. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action specifies action after event is detected.
     * @param maxIter maximum number of iterations in the event time search
     * @throws OrekitException
     */
    public ReflectorDetector(SpacecraftState initialState,
                             AbsoluteDate startDate, AbsoluteDate endDate,
                             CoveragePoint target, Propagator pfTransmitter,
                             Action action, double maxCheck, double threshold,
                             int maxIter) throws OrekitException {
        super(initialState, startDate, endDate, action, maxCheck, threshold, maxIter);
        this.target = target;
        this.pfTransmitter = pfTransmitter;
    }

    @Override
    public double g(SpacecraftState s) throws OrekitException {
        //the target position in the inertial frame
        final Vector3D targetPosInert
                = target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D txPosInert
                = s.getPVCoordinates(s.getFrame()).getPosition();
        final Vector3D rxPosInert
                = pfTransmitter.propagate(s.getDate()).getPVCoordinates(s.getFrame()).getPosition();
        final double coplanar
                = Math.abs(targetPosInert.normalize().dotProduct(txPosInert.normalize().crossProduct(rxPosInert.normalize())));
        double inView = 0.0;
        if(coplanar < 0.05) {
            final Vector3D targetToTX
                    = txPosInert.subtract(targetPosInert);
            final Vector3D targetToRX
                    = rxPosInert.subtract(targetPosInert);
            final double thetaRX
                    = Vector3D.angle(targetToRX,rxPosInert);
            final double thetaTX
                    = Vector3D.angle(targetToTX,txPosInert);
            final double heightratio = pfTransmitter.getInitialState().getA()/s.getA();
            final double angleratio = Math.sin(thetaRX)/Math.sin(thetaTX);
            final double spec = Math.abs(heightratio - angleratio);
            //System.out.println(s.getDate());
            if(spec < 0.05) {
                inView = 1.0;
            }
        } else {
            inView = -1.0;
        }
        return inView;
    }

    @Override
    protected ReflectorDetector create(SpacecraftState initialState,
                                       AbsoluteDate startDate, AbsoluteDate endDate,
                                       Action action, double maxCheck, double threshold, int maxIter) {
        try {
            return new ReflectorDetector(initialState, startDate, endDate, target, pfTransmitter, action, maxCheck, threshold, maxIter);
        } catch (OrekitException ex) {
            Logger.getLogger(ReflectorDetector.class.getName()).log(Level.SEVERE, null, ex);
        }
        throw new IllegalStateException("Could not create Reflector Detector");
    }
}

