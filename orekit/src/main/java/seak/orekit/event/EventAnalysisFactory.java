/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.event;

import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
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
            case GND_BODY_ANGLE:
                //Option to set the angle threshold for the angle between a 
                //specified topocentric direction and the celestial body
                double threshold = Double.parseDouble(prop.getProperty("gndbodyangle.threshold", "1.570795"));
                //Option to set the topocentric, where +X is east, +Y is north, and +Z is zenith
                double x = Double.parseDouble(prop.getProperty("gndbodyangle.x", "0"));
                double y = Double.parseDouble(prop.getProperty("gndbodyangle.y", "0"));
                double z = Double.parseDouble(prop.getProperty("gndbodyangle.z", "1"));

                Vector3D direction = new Vector3D(x, y, z);

                String bodyStr = prop.getProperty("gndbodyangle.body", "SUN");
                CelestialBody body;
                try {
                    body = CelestialBodyFactory.getBody(bodyStr);
                    ea = new GroundBodyAngleEventAnalysis(startDate, endDate, inertialFrame, covDefs, body, threshold, direction);
                } catch (OrekitException ex) {
                    Logger.getLogger(EventAnalysisFactory.class.getName()).log(Level.SEVERE, null, ex);
                    throw new UnsupportedOperationException(String.format("No known celestial body: %s", bodyStr));
                }

                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Analysis type %s is unsupported.", type));
        }

        return ea;
    }

}
