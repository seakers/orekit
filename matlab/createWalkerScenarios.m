function createWalkerScenarios(path, alts, incs, t, fov1, fov2)
%this function will enumerate several walker constellations and create new
%orekit scenarios to simulate the constellation coverages. The function
%assumes the use of a rectangular field of view sensor, which is common
%across all satellites. Also it assumes a global coverage grid (6deg
%granularity), j2 propagator, and a 2 month simulation from
%2016 01 01 T:00:00:00.000 to 2016 03 01 T:00:00:00.000.
%
%The function %does not propagate the scenarios but will save the un-run
%scenarios in the %desired folder so that they can be run in a separate
%process, possibly %using multiple computers.
%
%parameters
%path: the path to the directory to save the un run scenarios
%alts: a discrete set of altitudes [km] to try
%incs: a discrete set of inclinations [deg] to try
%alts: a discrete set of total number of satellites to try
%fov1: across track field of view (half aperature angle)
%fov2: along track field of view (half aperature angle)

%full factorial enumeration
constels = fullfactwalker(alts, incs, t);

try
    orekit_init;
    
    orekit.util.OrekitConfig.init([pwd,filesep,'orekit']);
    
    utc = org.orekit.time.TimeScalesFactory.getUTC();
    startDate = org.orekit.time.AbsoluteDate(2016, 1, 1, 16, 00, 00.000, utc);
    endDate   = org.orekit.time.AbsoluteDate(2016, 1, 8, 16, 00, 00.000, utc);
    
    mu = org.orekit.utils.Constants.WGS84_EARTH_MU; % gravitation coefficient
    
    %must use these frames to be consistent with STK
    earthFrame = org.orekit.frames.FramesFactory.getITRF(org.orekit.utils.IERSConventions.IERS_2003, true);
    inertialFrame = org.orekit.frames.FramesFactory.getEME2000();
    
    earth_radius = org.orekit.utils.Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
    earthShape = org.orekit.bodies.OneAxisEllipsoid(earth_radius,...
        org.orekit.utils.Constants.WGS84_EARTH_FLATTENING, earthFrame);
    cov_grid_bounds = [-90, 90, 0, 360]; %-/+ latitude, -/+ longitude in degrees
    coverageGridGranularity = 20; % separation of points by degree
    
    inst =   orekit.object.Instrument('inst', ...
        orekit.object.fieldofview.NadirRectangularFOV(...
        org.hipparchus.geometry.euclidean.threed.Vector3D.PLUS_K, ...
        deg2rad(fov1), deg2rad(fov2) , 0, earthShape), 100, 100);
    
    %set up the type of propagator here {KEPLERIAN, J2}
    pf =  orekit.propagation.PropagatorFactory(orekit.propagation.PropagatorType.J2, org.orekit.orbits.OrbitType.KEPLERIAN);
    
    for i=1:size(constels,1)
        a = constels(i,1) * 1000 + org.orekit.utils.Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        inc = constels(i,2);
        t = constels(i,3);
        p = constels(i,4);
        f = constels(i,5);
        walker = orekit.constellations.Walker('walker1', inc, t, p, f, a, inertialFrame, startDate, mu);
        %add instrument to all satellites in the walker constellation
        iter = walker.getSatellites.iterator;
        while(iter.hasNext())
            iter.next().addInstrument(inst);
        end
        covDef =  orekit.object.CoverageDefinition('covdef', coverageGridGranularity, ...
            cov_grid_bounds(1),cov_grid_bounds(2),cov_grid_bounds(3),cov_grid_bounds(4),...
            earthShape);
        covDef.assignConstellation(walker);
        covDefs = java.util.HashSet();
        covDefs.add(covDef);
        
        %add a orbital element analysis
        analysisTimeStep = 600;
        analysesList = java.util.ArrayList();
        analysesList.add(orekit.analysis.ephemeris.OrbitalElementsAnalysis(analysisTimeStep));
        analyses = orekit.analysis.CompoundAnalysis(analysesList);
        
        scenBuilder = javaObject('orekit.scenario.Scenario$Builder',startDate, endDate, utc);
        scen = scenBuilder.name(sprintf('walker_%f:%d/%d/%d_%f',inc,t,p,f,a))...
            .analysis(analyses).covDefs(covDefs).propagatorFactory(pf)...
            .saveAllAccesses(true).saveToDB(true).build();
        
        savePath = java.io.File(path).toPath;
        orekit.scenario.ScenarioIO.save(savePath, sprintf('%d',scen.hashCode), scen);
    end
    
catch ME
    orekit_end();
    rethrow(ME)
end