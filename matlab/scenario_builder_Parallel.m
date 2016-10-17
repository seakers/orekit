function results = scenario_builder_Parallel(savepath, analysis_name)
%This function searches over a range of orbital parameters for some
%specified orbits and computes coverage metrics

%% initialize orekit simulator
orekit_init();

%% Set parameters
%the number of threads to use to parallelize simulations
n_threads = 3;

orekit.util.OrekitConfig.init([pwd,filesep,'orekit']);

utc = org.orekit.time.TimeScalesFactory.getUTC();
startDate = org.orekit.time.AbsoluteDate(2016, 1, 1, 16, 00, 00.000, utc);
endDate   = org.orekit.time.AbsoluteDate(2016, 3, 1, 16, 00, 00.000, utc);
tSimulation = endDate.durationFrom(startDate);

mu = org.orekit.utils.Constants.EGM96_EARTH_MU; % gravitation coefficient

%must use these frames to be consistent with STK
earthFrame = org.orekit.frames.FramesFactory.getITRF(org.orekit.utils.IERSConventions.IERS_2003, true);
inertialFrame = org.orekit.frames.FramesFactory.getEME2000();

earth_radius = org.orekit.utils.Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
earthShape = org.orekit.bodies.OneAxisEllipsoid(earth_radius,...
    org.orekit.utils.Constants.WGS84_EARTH_FLATTENING, earthFrame);
cov_grid_bounds = [-90, 90, 0, 360]; %-/+ latitude, -/+ longitude in degrees
coverageGridGranularity = 20; % separation of points by degree

%% set up instrument parameters
rect_fov =  [57, 2.5]; %[cross track half aperture, along track half aperature]
%all satellites will carry the same tyo=pe of instrument with above fov

%% set up gap and access threshold values of interest [sec]
access_threshold = 60; %accesses shorter than threshold = violation
gap_threshold = 120*60; %gaps longer than threshold = violation

%% set up the Design of experiments of n orbital plane constelation
n_orbits = 3;
n_sats_per_plane = 4;
alts = [500, 600]; %all satellites will have the same altitude
eccs = [0]; %all satellites will have the same eccentricity
incs = [30]; %inclination in degrees. All satellites will have the same inclination
arg_pers = [0]; %all satellites will have the same argument of perigee
delta_raan = [120]; %difference in raan between two adjacent planes in degrees
delta_mean_anomaly = [90]; %difference in mean anomaly between two adjacent satellites within the same plane in degrees

%% Enter satellites for constellation
nsats = n_orbits*n_sats_per_plane; %number of total satellites

experiments = fullfact([length(alts), length(eccs), length(incs), length(arg_pers), length(delta_raan), length(delta_mean_anomaly)]);
results = cell(size(experiments, 1),1);

%loop through all experiments
for exp_i = 1:size(experiments, 1)
    
    %create vectors of orbital parameters
    experiment_i = experiments(exp_i,:);
    a =         (alts(experiment_i(1))*1000+earth_radius)*ones(nsats,1);
    e =         eccs(experiment_i(2))*ones(nsats,1);
    inc =       incs(experiment_i(3))*ones(nsats,1);
    argper =    arg_pers(experiment_i(4))*ones(nsats,1);
    
    del_raan = delta_raan(experiment_i(5));
    raan = zeros(1,nsats);
    sat_i = 1;
    for r = 0 : del_raan : del_raan * (n_orbits-1)
        adjusted_r = mod(r, 360); %adjust the raan to be [0. 360)
        raan(sat_i : sat_i + n_sats_per_plane - 1) = adjusted_r * ones(1, n_sats_per_plane);
        sat_i = sat_i + n_sats_per_plane;
    end
    
    del_m_anom = delta_mean_anomaly(experiment_i(6));
    m_anom = zeros(1,nsats);
    sat_i = 1;
    for m_a = 0 : del_m_anom : del_m_anom * (n_sats_per_plane - 1)
        adjusted_ma = mod(m_a, 360); %adjust the anomaly to be [0. 360)
        m_anom(sat_i: n_sats_per_plane : end) = adjusted_ma;
        sat_i = sat_i + 1;
    end
    
    %Collection of each satellite's coverage/access file
    access_collection = java.util.ArrayList;
    
    %run each satellite individually and save the results in a database
    for i=1:nsats
        %look to see if this satellite has already been simulated
        tmps = strrep(char(startDate.toString()), ':', '-');
        tmpe = strrep(char(endDate.toString()), ':', '-');
        filename = sprintf('a%.3f_e%.3f_i%.3f_w%.3f_o%.3f_v%.3f_fov%.3fx%.3f_start%s_end%s',...
            a(i), e(i), inc(i), argper(i), raan(i), m_anom(i),rect_fov(1),rect_fov(2),...
            strrep(tmps,'.','-'), strrep(tmpe,'.','-'));
        files = dir(strcat(savepath,filesep,filename,'.scen'));
        
        %Only run satellites that have not been propogated yet
        if isempty(files)
            initial_orbit = org.orekit.orbits.KeplerianOrbit(a(i), e(i), ...
                inc(i), argper(i), raan(i), m_anom(i), org.orekit.orbits.PositionAngle.MEAN, inertialFrame, startDate, mu);
            sat = orekit.object.Satellite(sprintf('sat%d',i), initial_orbit);
            inst =   orekit.object.Instrument(sprintf('view%d',i), ...
                orekit.object.fieldofview.NadirRectangularFOV(earthShape,...
                org.hipparchus.geometry.euclidean.threed.Vector3D.PLUS_K, ...
                deg2rad(rect_fov(1)), deg2rad(rect_fov(2)) , 0));
            sat.addInstrument(inst);
            satGroup = java.util.ArrayList;
            satGroup.add(sat);
            
            constel = orekit.object.Constellation('constel', satGroup);
            covDef =  orekit.object.CoverageDefinition('covdef', coverageGridGranularity, ...
                cov_grid_bounds(1),cov_grid_bounds(2),cov_grid_bounds(3),cov_grid_bounds(4),...
                earthShape, startDate, endDate);
            covDef.assignConstellation(constel);
            
            %set up the type of propagator here {KEPLERIAN, J2}
            pf =  orekit.propagation.PropagatorFactory(orekit.propagation.PropagatorType.J2, initial_orbit);
            scen = orekit.scenario.Scenario('test', startDate, endDate, utc, inertialFrame, pf, false, n_threads);
            scen.addCoverageDefinition(covDef);
            
            %parallelization of the scenario
            parallelCoverage=orekit.coverage.parallel.ParallelCoverage();
            ndivisions=4;
            parallelCoverage.createSubScenarios(scen,ndivisions,java.io.File(savepath));
            
            %run each of the subscenarios(need to be improved with future tasks)
            subscenarios=dir(strcat(savepath,filesep,'*.subscen'));
            nsubscenarios=size(subscenarios,1);
            for ind=1:nsubscenarios
%                 parallelCoverage.loadRunAndSave(java.io.File(savepath).toPath,subscenarios(ind).name);
                parallelCoverage.loadRunAndSave(java.io.File(savepath).toPath,4);
            end
            
            %load all run subscenarios and put them in a Collection
            subscen_collection = java.util.ArrayList;
            subscenarios=dir(strcat(savepath,filesep,'*.subscen'));
            nsubscenarios=size(subscenarios,1);
            for ind=1:nsubscenarios
                subscen_collection.add(orekit.scenario.ScenarioIO.loadSubScenario(java.io.File(savepath).toPath,subscenarios(ind).name));
            end
            
            scen.mergeSubscenarios(subscen_collection);
            
            %save Parent Scenario with meaningful filename
            orekit.scenario.ScenarioIO.save(java.io.File(savepath).toPath, filename, scen);
            covDef=scen.getCoverageDefinition([char(scen.getName()),'_final']);
            access_collection.add(scen.getMergedAccesses(covDef));
            
        else
            scen = orekit.scenario.ScenarioIO.load(java.io.File(savepath).toPath, strcat(filename,'.scen'));
            covDef = scen.getCoverageDefinition([char(scen.getName()),'_final']);
            access_collection.add(scen.getMergedAccesses(covDef));
        end
        
        
    end
    
    %compute metrics of the overall constellation
    cam = orekit.coverage.access.CoverageAccessMerger();
    mergedAccesses = cam.mergeCoverageDefinitionAccesses(access_collection,false);
    ca = orekit.coverage.analysis.CoverageAnalyzer(mergedAccesses);
    
    %%%%%%%%%%%%%%%%%%%%%%%
    % Collect access stats%
    %%%%%%%%%%%%%%%%%%%%%%%
    
    %collect global access metrics
    results{exp_i}.accesscdf = ca.getCDFAccess;
    results{exp_i}.accessstats.mean = ca.getMeanAccess;
    results{exp_i}.accessstats.max = ca.getMaxAccess;
    results{exp_i}.accessstats.min = ca.getMinAccess;
    results{exp_i}.accessstats.variance = ca.getVarianceAccess;
    results{exp_i}.accessstats.sumTotal = ca.getSumAccess;
    results{exp_i}.accessstats.durations = ca.getSortedAccess;
    sorted_accesses = ca.getSortedAccess;
    violating_accesses = sorted_accesses(sorted_accesses > access_threshold);
    chrc_access = sum(violating_accesses)/(tSimulation*double(ca.getCoveragePoints.size));
    results{exp_i}.accessstats = setfield(results{exp_i}.accessstats,sprintf('CHRC_%d',access_threshold), chrc_access);
    clear sorted_accesses violating_accesses %try to reduce memory footprint by removing memory intensive arrays
    
    %collect the access data by latitude
    latitudes = ca.getLatitudes;
    results{exp_i}.accesscdfByLat = zeros(latitudes.size,99);
    results{exp_i}.accessstatsByLat.mean = zeros(latitudes.size,1);
    results{exp_i}.accessstatsByLat.max = zeros(latitudes.size,1);
    results{exp_i}.accessstatsByLat.min = zeros(latitudes.size,1);
    results{exp_i}.accessstatsByLat.stddev = zeros(latitudes.size,1);
    results{exp_i}.accessstatsByLat.sumTotal = zeros(latitudes.size,1);
    results{exp_i}.accessstatsByLat.latitudes = zeros(latitudes.size,1);
    results{exp_i}.accessstatsByLat.CHRC = zeros(latitudes.size,1);
    results{exp_i}.accessstatsByLat.numPts = zeros(latitudes.size,1);
    
    iter = latitudes.iterator;
    ind = 1;
    while(iter.hasNext)
        lat = iter.next;
        results{exp_i}.accessstatsByLat.latitudes(ind)=lat;
        results{exp_i}.accesscdfByLat(ind,:) = ca.getCDFAccess(lat);
        results{exp_i}.accessstatsByLat.mean(ind) = ca.getMeanAccess(lat);
        results{exp_i}.accessstatsByLat.max(ind) = ca.getMaxAccess(lat);
        results{exp_i}.accessstatsByLat.min(ind) = ca.getMinAccess(lat);
        results{exp_i}.accessstatsByLat.variance(ind) = ca.getVarianceAccess(lat);
        results{exp_i}.accessstatsByLat.sumTotal(ind) = ca.getSumAccess(lat);
        results{exp_i}.accessstatsByLat.durations{ind} = ca.getSortedAccess(lat);
        results{exp_i}.accessstatsByLat.numPts(ind) = ca.getCoveragePoints(lat).size;
        
        sorted_accesses = ca.getSortedAccess(lat);
        violating_accesses = sorted_accesses(sorted_accesses > access_threshold);
        chrc_access = sum(violating_accesses)/(tSimulation*double(ca.getCoveragePoints.size));
        results{exp_i}.accessstats = setfield(results{exp_i}.accessstats,sprintf('CHRC_%d',access_threshold), chrc_access);
        clear sorted_accesses violating_accesses %try to reduce memory footprint by removing memory intensive arrays
        
        ind = ind + 1;
    end
    
    %collect the access data by point
    pts = ca.getCoveragePoints;
    pts_data = zeros(pts.size,10);
    iter = pts.iterator;
    ind = 1;
    while(iter.hasNext)
        pt = iter.next;
        pts_data(ind,1) = pt.getPoint.getLatitude;
        pts_data(ind,2 )= pt.getPoint.getLongitude;
        pts_data(ind,3) = ca.getMeanAccess(pt);
        pts_data(ind,4) = ca.getMaxAccess(pt);
        pts_data(ind,5) = ca.getMinAccess(pt);
        pts_data(ind,6) = ca.getPercentileAccess(85, pt);
        pts_data(ind,7) = ca.getPercentileAccess(90, pt);
        pts_data(ind,8) = ca.getPercentileAccess(95, pt);
        pts_data(ind,9) = ca.getSumAccess(pt);
        sorted_accesses = ca.getSortedAccess(pt);
        violating_accesses = sorted_accesses(sorted_accesses > access_threshold);
        pts_data(ind,10) = sum(violating_accesses)/(tSimulation*double(ca.getCoveragePoints.size));
        clear sorted_accesses violating_accesses %try to reduce memory footprint by removing memory intensive arrays
        
        ind = ind + 1;
    end
    results{exp_i}.accessstatsByPoints.metrics = pts_data;
    results{exp_i}.accessstatsByPoints.header = {'Latitude','Longitude',...
        'Mean access', 'Max access', 'Min access', '85 pct access', '90 pct access', '95 pct access',...
        'Sum access', sprintf('CHRC_%.5f access',access_threshold)};
    
    %%%%%%%%%%%%%%%%%%%%
    % Collect gap stats%
    %%%%%%%%%%%%%%%%%%%%
    
    %collect global gap metrics
    results{exp_i}.gapcdf = ca.getCDFGap;
    results{exp_i}.gapstats.mean = ca.getMeanGap;
    results{exp_i}.gapstats.max = ca.getMaxGap;
    results{exp_i}.gapstats.min = ca.getMinGap;
    results{exp_i}.gapstats.variance = ca.getVarianceGap;
    results{exp_i}.gapstats.sumTotal = ca.getSumGap;
    results{exp_i}.gapstats.durations = ca.getSortedGap;
    sorted_gaps = ca.getSortedGap;
    violating_gaps = sorted_gaps(sorted_gaps > gap_threshold);
    chrc_gap = sum(violating_gaps)/(tSimulation*double(ca.getCoveragePoints.size));
    results{exp_i}.gapstats = setfield(results{exp_i}.gapstats,sprintf('CHRC_%d',gap_threshold), chrc_gap);
    clear sorted_gaps violating_gaps %try to reduce memory footprint by removing memory intensive arrays
    
    %collect the gap data by latitude
    latitudes = ca.getLatitudes;
    results{exp_i}.gapcdfByLat = zeros(latitudes.size,99);
    results{exp_i}.gapstatsByLat.mean = zeros(latitudes.size,1);
    results{exp_i}.gapstatsByLat.max = zeros(latitudes.size,1);
    results{exp_i}.gapstatsByLat.min = zeros(latitudes.size,1);
    results{exp_i}.gapstatsByLat.stddev = zeros(latitudes.size,1);
    results{exp_i}.gapstatsByLat.sumTotal = zeros(latitudes.size,1);
    results{exp_i}.gapstatsByLat.latitudes = zeros(latitudes.size,1);
    results{exp_i}.gapstatsByLat.CHRC = zeros(latitudes.size,1);
    results{exp_i}.gapstatsByLat.numPts = zeros(latitudes.size,1);
    
    iter = latitudes.iterator;
    ind = 1;
    while(iter.hasNext)
        lat = iter.next;
        results{exp_i}.gapstatsByLat.latitudes(ind)=lat;
        results{exp_i}.gapcdfByLat(ind,:) = ca.getCDFGap(lat);
        results{exp_i}.gapstatsByLat.mean(ind) = ca.getMeanGap(lat);
        results{exp_i}.gapstatsByLat.max(ind) = ca.getMaxGap(lat);
        results{exp_i}.gapstatsByLat.min(ind) = ca.getMinGap(lat);
        results{exp_i}.gapstatsByLat.variance(ind) = ca.getVarianceGap(lat);
        results{exp_i}.gapstatsByLat.sumTotal(ind) = ca.getSumGap(lat);
        results{exp_i}.gapstatsByLat.durations{ind} = ca.getSortedGap(lat);
        results{exp_i}.gapstatsByLat.numPts(ind) = ca.getCoveragePoints(lat).size;
        
        sorted_gaps = ca.getSortedGap(lat);
        violating_gaps = sorted_gaps(sorted_gaps > gap_threshold);
        chrc_gap = sum(violating_gaps)/(tSimulation*double(ca.getCoveragePoints.size));
        results{exp_i}.gapstats = setfield(results{exp_i}.gapstats,sprintf('CHRC_%d',gap_threshold), chrc_gap);
        clear sorted_gaps violating_gaps %try to reduce memory footprint by removing memory intensive arrays
        
        ind = ind + 1;
    end
    
    %collect the access data by point
    pts = ca.getCoveragePoints;
    pts_data = zeros(pts.size,10);
    iter = pts.iterator;
    ind = 1;
    while(iter.hasNext)
        pt = iter.next;
        pts_data(ind,1) = pt.getPoint.getLatitude;
        pts_data(ind,2 )= pt.getPoint.getLongitude;
        pts_data(ind,3) = ca.getMeanGap(pt);
        pts_data(ind,4) = ca.getMaxGap(pt);
        pts_data(ind,5) = ca.getMinGap(pt);
        pts_data(ind,6) = ca.getPercentileGap(85, pt);
        pts_data(ind,7) = ca.getPercentileGap(90, pt);
        pts_data(ind,8) = ca.getPercentileGap(95, pt);
        pts_data(ind,9) = ca.getSumGap(pt);
        sorted_gaps = ca.getSortedGap(pt);
        violating_gaps = sorted_gaps(sorted_gaps > gap_threshold);
        pts_data(ind,10) = sum(violating_gaps)/(tSimulation*double(ca.getCoveragePoints.size));
        clear sorted_gaps violating_gaps %try to reduce memory footprint by removing memory intensive arrays
        
        ind = ind + 1;
    end
    results{exp_i}.gapstatsByPoints.metrics = pts_data;
    results{exp_i}.gapstatsByPoints.header = {'Latitude','Longitude',...
        'Mean gap', 'Max gap', 'Min gap', '85 pct gap', '90 pct gap', '95 pct gap',...
        'Sum gap', sprintf('CHRC_%.5f gap',gap_threshold)};
    
    %save inputs and outputs of scenarios
    results{exp_i}.a = a;
    results{exp_i}.e = e;
    results{exp_i}.inc = inc;
    results{exp_i}.argper = argper;
    results{exp_i}.raan = raan;
    results{exp_i}.m_anom = m_anom;
    results{exp_i}.fov = rect_fov;
    results{exp_i}.n_orbits = n_orbits;
    results{exp_i}.n_sats_per_plane = n_sats_per_plane;
    results{exp_i}.cov_grid_bounds = cov_grid_bounds;
    results{exp_i}.cov_grid_granularity = coverageGridGranularity;
end

filesave = [savepath analysis_name];% '-' date '-' hour '-' min];
save(filesave,'results');

%% Remove orekit jar file from matlab dynamic path
orekit_end()

end