/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.event;

import java.util.Map;
import seak.orekit.coverage.access.TimeIntervalArray;
import seak.orekit.object.CoverageDefinition;
import seak.orekit.object.CoveragePoint;

/**
 * Event analysis for the ground-based analyses (e.g. field-of-view access,
 * ground point sun angle)
 *
 * @author nhitomi
 */
public interface GroundEventAnalysis extends EventAnalysis {

    /**
     * Gets the time intervals when events occurred for each coverage point for
     * each coverage definition
     *
     * @return
     */
    public Map<CoverageDefinition, Map<CoveragePoint, TimeIntervalArray>> getEvents();
    
    /**
     * Gets the time intervals when events occurred for each coverage point for
     * a specified coverage definition
     *
     * @param covDef the coverage definition of interest
     * @return
     */
    public Map<CoveragePoint, TimeIntervalArray> getEvents(CoverageDefinition covDef);
}
