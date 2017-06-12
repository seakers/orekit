/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.event;

import java.util.HashSet;
import java.util.Properties;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.propagation.PropagatorFactory;

/**
 * Creates a multitude of event analyses.
 *
 * @author nhitomi
 */
public class EventAnalysisFactory {

    /**
     * Analysis start date
     */
    private final AbsoluteDate startDate;

    /**
     * Analysis end date
     */
    private final AbsoluteDate endDate;

    /**
     * A factory to create propagators
     */
    private PropagatorFactory propagatorFactory;

    /**
     * The coverage definitions that contains informations about the target
     * regions and the constellations assigned to view/access those regions
     */
    private HashSet<CoverageDefinition> covDefs;

    /**
     * Inertial frame
     */
    private final Frame inertialFrame;

    /**
     * Constructor without a propagator assigned. It can be set later through
     * the set methods. Some analyses use a default, low-fidelity propagator
     * (e.g. sun angle to ground point)
     *
     * @param startDate the start date of the analysis
     * @param endDate the end date of the analysis
     * @param inertialFrame the inertial frame of reference
     * @param covDefs The coverage definitions to conduct analyses on
     */
    public EventAnalysisFactory(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame, HashSet<CoverageDefinition> covDefs) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.inertialFrame = inertialFrame;
    }

    /**
     * Constructor specifying the propagator factory to use,
     *
     * @param startDate the start date of the analysis
     * @param endDate the end date of the analysis
     * @param inertialFrame the inertial frame of reference
     * @param covDefs The coverage definitions to conduct analyses on
     * @param propagatorFactory the propagator factory to create the propagators
     * used in the analyses
     */
    public EventAnalysisFactory(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame, HashSet<CoverageDefinition> covDefs,
            PropagatorFactory propagatorFactory) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.propagatorFactory = propagatorFactory;
        this.covDefs = covDefs;
        this.inertialFrame = inertialFrame;
    }

    public EventAnalysis create(EventAnalysisEnum type, Properties prop) {
        EventAnalysis ea = null;

        switch (type) {
            case FOV:
                //Option to record the accesses of the individual satellites in 
                //the constellation and keep them separate from the combined 
                //access times of the constellation as a whole. By default, 
                //the accesses of the individual satellites are not kept in memory to conserve memory space
                String saveAllAccessesStr = prop.getProperty("fov.saveAccess", "false");

                //Option to set the number of threads to use to run the scenario.
                //By default it is set to 1.
                String numThreadsStr = prop.getProperty("fov.numThreads", "1");

                //Option to dictate whether the coverage accesses of individual 
                //satellites should be saved to the coverage database. 
                //By default the accesses are not stored to the database.
                String saveToDBStr = prop.getProperty("fov.saveToDB", "false");

                ea = new FieldOfViewEventAnalysis(startDate, endDate, inertialFrame,
                        covDefs, propagatorFactory,
                        Boolean.parseBoolean(saveAllAccessesStr),
                        Boolean.parseBoolean(saveToDBStr),
                        Integer.parseInt(numThreadsStr));
                break;
            case GND_SUN_ANGLE:
                //Option to set the angle threshold for the angle between a 
                //specified topocentric direction and the sun
                double threshold = Double.parseDouble(prop.getProperty("gndsunangle.threshold", "1.570795"));
                //Option to set the topocentric, where +X is east, +Y is north, and +Z is zenith
                double x = Double.parseDouble(prop.getProperty("gndsunangle.x", "0"));
                double y = Double.parseDouble(prop.getProperty("gndsunangle.y", "0"));
                double z = Double.parseDouble(prop.getProperty("gndsunangle.z", "1"));
                
                Vector3D direction = new Vector3D(x, y, z);
                
                ea = new GroundSunAngleEventAnalysis(startDate, endDate, inertialFrame, covDefs, threshold, direction);
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Analysis type %s is unsupported.", type));
        }

        return ea;
    }

}
