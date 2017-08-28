function createWalkerScenariosForTropics(path, alts, incs, t)
%this function will enumerate several walker constellations and create new
%orekit scenarios to simulate the constellation coverages.
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
    orekit_init2();
    
    seak.orekit.util.OrekitConfig.init(['..',filesep,'orekit']);
    
    utc = org.orekit.time.TimeScalesFactory.getUTC();
    startDate = org.orekit.time.AbsoluteDate(2016, 1, 1, 00, 00, 00.000, utc);
    endDate   = org.orekit.time.AbsoluteDate(2016, 1, 7, 00, 00, 00.000, utc);
    
    mu = org.orekit.utils.Constants.WGS84_EARTH_MU; % gravitation coefficient
    
    %must use these frames to be consistent with STK
    earthFrame = org.orekit.frames.FramesFactory.getITRF(org.orekit.utils.IERSConventions.IERS_2003, true);
    inertialFrame = org.orekit.frames.FramesFactory.getEME2000();
    
    earth_radius = org.orekit.utils.Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
    earthShape = org.orekit.bodies.OneAxisEllipsoid(earth_radius,...
        org.orekit.utils.Constants.WGS84_EARTH_FLATTENING, earthFrame);
    coverageGridGranularity = 6; % separation of points by degree
    
    fov=seak.orekit.object.fieldofview.NadirRectangularFOV(deg2rad(57), deg2rad(2.5), 0, earthShape);
    
    payload=java.util.ArrayList();
    mass=6;
    averagePower=10;
    view1=seak.orekit.object.Instrument('view1',fov,mass,averagePower);
    payload.add(view1);
    
    %set up the type of propagator here
    propertiesPropagator = java.util.Properties();
    propertiesPropagator.setProperty('orekit.propagator.atmdrag', 'true');
    propertiesPropagator.setProperty('orekit.propagator.dragarea', '0.13'); %worst case scenario 0.3x0.1*4+0.1x0.1
    propertiesPropagator.setProperty('orekit.propagator.dragcoeff', '2.2');
    propertiesPropagator.setProperty('orekit.propagator.thirdbody.sun', 'true');
    propertiesPropagator.setProperty('orekit.propagator.thirdbody.moon', 'true');
    propertiesPropagator.setProperty('orekit.propagator.solarpressure', 'true');
    propertiesPropagator.setProperty('orekit.propagator.solararea', '10');
    pf =  seak.orekit.propagation.PropagatorFactory(seak.orekit.propagation.PropagatorType.NUMERICAL, propertiesPropagator);
    
    for i=1:size(constels,1)
        a = constels(i,1) * 1000 + earth_radius;
        inc = constels(i,2);
        t = constels(i,3);
        p = constels(i,4);
        f = constels(i,5);
        walker = seak.orekit.constellations.Walker('walker1', payload, a, inc, t, p, f, inertialFrame, startDate, mu);

        covDef =  seak.orekit.object.CoverageDefinition('covdef', coverageGridGranularity, ...
            earthShape,seak.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA);
        covDef.assignConstellation(walker);
        covDefs = java.util.HashSet();
        covDefs.add(covDef);
        
        propertiesEventAnalysis = java.util.Properties();
        %propertiesEventAnalysis.setProperty('fov.numThreads', '4');
        
        %set the event analysis
        eaf = seak.orekit.event.EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
        eventanalyses = java.util.ArrayList();
        fovEvent = eaf.createGroundPointAnalysis(seak.orekit.event.EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
        eventanalyses.add(fovEvent);
        %set the analysis
        analyses = java.util.ArrayList();
        
        
        scenBuilder = javaObject('seak.orekit.scenario.Scenario$Builder',startDate, endDate, utc);
        scen = scenBuilder.name(sprintf('walker_%f:%d/%d/%d_%f',inc,t,p,f,a))...
            .analysis(analyses).eventAnalysis(eventanalyses).covDefs(covDefs).propagatorFactory(pf)...
            .properties(propertiesEventAnalysis).build();
        try
            scen.call();
        catch e
            e.message
            if(isa(e,'matlab.exception.JavaException'))
                ex = e.ExceptionObject;
                assert(isJava(ex));
                ex.printStackTrace;
            end
        end
        
        savePath = java.io.File(path).toPath;
        seak.orekit.scenario.ScenarioIO.saveGroundEventAnalysis(savePath, 'Tropics', scen, covDef, fovEvent);
        seak.orekit.scenario.ScenarioIO.saveGroundEventAnalysisMetrics(savePath, 'Tropics', scen, covDef, fovEvent);
        seak.orekit.scenario.ScenarioIO.saveGroundEventAnalysisObject(savePath, 'Tropics', scen, covDef, fovEvent);
    end
    
catch ME
    orekit_end2();
    rethrow(ME)
end