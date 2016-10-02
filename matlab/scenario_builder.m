function results = scenario_builder(savepath)
%This function searches over a range of orbital parameters for some
%specified orbits and computes coverage metrics

%% initialize orekit simulator
orekit_init();

%% Set parameters
%the number of threads to use to parallelize simulations
n_threads = 3;

OrekitConfig.init([pwd,filesep,'orekit']);

utc = TimeScalesFactory.getUTC();
startDate = AbsoluteDate(2016, 1, 1, 16, 00, 00.000, utc);
endDate   = AbsoluteDate(2016, 3, 1, 16, 00, 00.000, utc);
tSimulation = endDate.durationFrom(startDate);

mu = Constants.EGM96_EARTH_MU; % gravitation coefficient

%must use these frames to be consistent with STK
earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
inertialFrame = FramesFactory.getEME2000();

earthShape = OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
cov_grid_bounds = []; %-/+ latitude, -/+ longitude in degrees
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
results = cell(size(experiments, 1));

%loop through all experiments
for exp_i = 1:size(experiments, 1)
    
    %create vectors of orbital parameters
    experiment_i = experiments(exp_i,:);
    a =         alts(experiment_i(1))*ones(nsats,1);
    e =         eccs(experiment_i(2))*ones(nsats,1);
    inc =         incs(experiment_i(3))*ones(nsats,1);
    argper =    arg_pers(experiment_i(4))*ones(nsats,1);
    
    del_raan = delta_raan(experiment_i(5));
    raan = zeros(1,nsats);
    sat_i = 1;
    for r = 0 : del_raan : del_raan*n_orbits
        raan(sat_i) = r;
        sat_i = sat_i + 1;
    end
    
    del_m_anom = delta_mean_anomaly(experiment_i(6));
    m_anon = zeros(1,nsats);
    sat_i = 1;
    for m_a = 0 : del_m_anom : del_m_anom * n_sats_per_plane
        m_anon(sat_i) = m_a;
        sat_i = sat_i + 1;
    end
    
    %pointing law fixes satellites pointing in Nadir
    nadPoint =   NadirPointing(inertialFrame, earthShape);
    
    %Collection of each satellite's coverage/access file
    access_collection = java.util.ArrayList;
    
    %run each satellite individually and save the results in a database
    for i=1:nsats
        %look to see if this satellite has already been simulated
        filename = sprintf('a%f_e%f_i%f_w%f_o%f_v%f_fov%fx%f_start%f_end%f',...
            a(i), e(i), inc(i), argper(i), raan(i), m_anom(i),rect_fov(1),rect_fov(2),...
            strrep(tmps,'.','-'), strrep(tmpe,'.','-'));
        files = dir(strcat(filename,'.ore'));
        
        %Only run satellites that have not been propogated yet
        if isempty(files)
            initial_orbit = KeplerianOrbit(a(i), e(i), inc(i), argper(i), raan(i), m_anom(i), PositionAngle.MEAN, inertialFrame, startDate, mu);
            sat = Satellite(sprintf('sat%d',i), initial_orbit, nadPoint);
            inst =   Instrument(sprintf('view%d',i), RectangularFieldOfView(Vector3D.PLUS_K, rect_fov(1), rect_fov(2) , 0));
            sat.addInstrument(inst);
            satGroup.add(sat);
            
            constel =   Constellation('constel', satGroup);
            covDef =   CoverageDefinition('covdef', coverageGridGranularity, ...
                cov_grid_bounds(1),cov_grid_bounds(2),cov_grid_bounds(3),cov_grid_bounds(4),...
                earthShape, startDate, endDate);
            covDef.assignConstellation(constel);
            
            %set up the type of propagator here {KEPLERIAN, J2}
            pf =   PropagatorFactory(PropagatorType.KEPLERIAN, initialOrbit2);
            scen =   Scenario('test', startDate, endDate, utc, inertialFrame, pf, false, n_threads);
            scen.addCoverageDefinition(covDef);
            
            %command to run scenario
            scen.call();
            
            %save accesses with meaningful filename
            tmps = strrep(startDate.toString(), ':', '-');
            tmpe = strrep(endDatae.toString(), ':', '-');
            ScenarioIO.saveAccess(savepath, filename, scen, covDef);
        else
            scen = ScenarioIO.load(path, strcat(filename,'.ore'));
        end
        
        access_collection.add(scen.getMergedAccesses);
    end
    
    %compute metrics of the overall constellation
    cam = CoverageAccessMerger();
    mergedAccesses = cam.mergeCoverageDefinitionAccesses(access_collection,false);
    ca = CoverageAnalyzer(mergedAccesses);
    
    %%%%%%%%%%%%%%%%%%%%%%%
    % Collect access stats%
    %%%%%%%%%%%%%%%%%%%%%%%
    
    %collect global access metrics
    results{i}.accesscdf = sort(double(CAA.getAllStats.getCdf.getAllPercentiles));
    results{i}.accessstats.mean = ca.getMeanAccess;
    results{i}.accessstats.max = ca.getMaxAccess;
    results{i}.accessstats.min = ca.getMinAccess;
    results{i}.accessstats.variance = ca.getVarianceAccess;
    results{i}.accessstats.sumTotal = ca.getSumAccess;
    results{i}.accessstats.durations = ca.getSortedAccess;
    sorted_accesses = ca.getSortedAccesses;
    violating_accesses = sorted_accesses(sorted_access > access_threshold);
    chrc_access = sum(violating_accesses)/(tSimulation*double(ca.getCoveragePoints.size));
    results{i}.accessstats = setfield(results{i}.accessstats,sprintf('CHRC_%.5f',access_threshold), chrc_access);
    clear sorted_accesses violating_accesses %try to reduce memory footprint by removing memory intensive arrays
    
    %collect the access data by latitude
    latitudes = ca.getLatitudes;
    results{i}.accesscdfByLat = zeros(latitudes.size,100);
    results{i}.accessstatsByLat.mean = zeros(latitudes.size,1);
    results{i}.accessstatsByLat.max = zeros(latitudes.size,1);
    results{i}.accessstatsByLat.min = zeros(latitudes.size,1);
    results{i}.accessstatsByLat.stddev = zeros(latitudes.size,1);
    results{i}.accessstatsByLat.sumTotal = zeros(latitudes.size,1);
    results{i}.accessstatsByLat.latitudes = zeros(latitudes.size,1);
    results{i}.accessstatsByLat.CHRC = zeros(latitudes.size,1);
    results{i}.accessstatsByLat.numPts = zeros(latitudes.size,1);
    
    iter = latitudes.iterator;
    ind = 1;
    while(iter.hasNext)
        lat = iter.next;
        results{i}.accessstatsByLat.latitudes(ind)=lat;
        results{i}.accesscdfByLat(ind,:) = ca.getCDFAccess(lat);
        results{i}.accessstatsByLat.mean(ind) = ca.getMeanAccess(lat);
        results{i}.accessstatsByLat.max(ind) = ca.getMaxAccess(lat);
        results{i}.accessstatsByLat.min(ind) = ca.getMinAccess(lat);
        results{i}.accessstatsByLat.variance(ind) = ca.getVarianceAccess(lat);
        results{i}.accessstatsByLat.sumTotal(ind) = ca.getSumAccess(lat);
        results{i}.accessstatsByLat.durations{ind} = sort(double(CAA.getStatsByLat.get(lat).getAccessDuration));
        results{i}.accessstatsByLat.numPts(ind) = ca.getCoveragePoints(lat).size;
        
        sorted_accesses = ca.getSortedAccesses(lat);
        violating_accesses = sorted_accesses(sorted_access > access_threshold);
        chrc_access = sum(violating_accesses)/(tSimulation*double(ca.getCoveragePoints.size));
        results{i}.accessstats = setfield(results{i}.accessstats,sprintf('CHRC_%.5f',access_threshold), chrc_access);
        clear sorted_accesses violating_accesses %try to reduce memory footprint by removing memory intensive arrays
        
        ind = ind + 1;
    end
    
    %collect the access data by point
    pts = ca.getCoveragePoints;
    pts_data = zeros(pts.size,10);
    iter = pts.iterator;
    ind = 1;
    while(iter.hasNext)
        point = iter.next;
        pts_data(ind,1) = point.getPoint.getLatitude;
        pts_data(ind,2 )= point.getPoint.getLongitude;
        pts_data(ind,3) = ca.getMeanAccess(pt);
        pts_data(ind,4) = ca.getMaxAccess(pt);
        pts_data(ind,5) = ca.getMinAccess(pt);
        pts_data(ind,6) = ca.getPercentileAccess(pt,85);
        pts_data(ind,7) = ca.getPercentileAccess(pt,90);
        pts_data(ind,8) = ca.getPercentileAccess(pt,95);
        pts_data(ind,9) = ca.getSumAccess(pt);
        sorted_accesses = ca.getSortedAccesses(pt);
        violating_accesses = sorted_accesses(sorted_access > access_threshold);
        pts_data(ind,10) = sum(violating_accesses)/(tSimulation*double(ca.getCoveragePoints.size));
        clear sorted_accesses violating_accesses %try to reduce memory footprint by removing memory intensive arrays
        
        ind = ind + 1;
    end
    results{i}.accessstatsByPoints.metrics = pts_data;
    results{i}.accessstatsByPoints.header = {'Latitude','Longitude',...
        'Mean access', 'Max access', 'Min access', '85 pct access', '90 pct access', '95 pct access',...
        'Sum access', sprintf('CHRC_%.5f access',access_threshold)};
    
    %%%%%%%%%%%%%%%%%%%%
    % Collect gap stats%
    %%%%%%%%%%%%%%%%%%%%
    
    %collect global gap metrics
    results{i}.gapcdf = sort(double(CAA.getAllStats.getCdf.getAllPercentiles));
    results{i}.gapstats.mean = ca.getMeanGap;
    results{i}.gapstats.max = ca.getMaxGap;
    results{i}.gapstats.min = ca.getMinGap;
    results{i}.gapstats.variance = ca.getVarianceGap;
    results{i}.gapstats.sumTotal = ca.getSumGap;
    results{i}.gapstats.durations = ca.getSortedGap;
    sorted_gaps = ca.getSortedGapes;
    violating_gaps = sorted_gaps(sorted_gap > gap_threshold);
    chrc_gap = sum(violating_gaps)/(tSimulation*double(ca.getCoveragePoints.size));
    results{i}.gapstats = setfield(results{i}.gapstats,sprintf('CHRC_%.5f',gap_threshold), chrc_gap);
    clear sorted_gaps violating_gaps %try to reduce memory footprint by removing memory intensive arrays
    
    %collect the gap data by latitude
    latitudes = ca.getLatitudes;
    results{i}.gapcdfByLat = zeros(latitudes.size,100);
    results{i}.gapstatsByLat.mean = zeros(latitudes.size,1);
    results{i}.gapstatsByLat.max = zeros(latitudes.size,1);
    results{i}.gapstatsByLat.min = zeros(latitudes.size,1);
    results{i}.gapstatsByLat.stddev = zeros(latitudes.size,1);
    results{i}.gapstatsByLat.sumTotal = zeros(latitudes.size,1);
    results{i}.gapstatsByLat.latitudes = zeros(latitudes.size,1);
    results{i}.gapstatsByLat.CHRC = zeros(latitudes.size,1);
    results{i}.gapstatsByLat.numPts = zeros(latitudes.size,1);
    
    iter = latitudes.iterator;
    ind = 1;
    while(iter.hasNext)
        lat = iter.next;
        results{i}.gapstatsByLat.latitudes(ind)=lat;
        results{i}.gapcdfByLat(ind,:) = ca.getCDFGap(lat);
        results{i}.gapstatsByLat.mean(ind) = ca.getMeanGap(lat);
        results{i}.gapstatsByLat.max(ind) = ca.getMaxGap(lat);
        results{i}.gapstatsByLat.min(ind) = ca.getMinGap(lat);
        results{i}.gapstatsByLat.variance(ind) = ca.getVarianceGap(lat);
        results{i}.gapstatsByLat.sumTotal(ind) = ca.getSumGap(lat);
        results{i}.gapstatsByLat.durations{ind} = sort(double(CAA.getStatsByLat.get(lat).getGapDuration));
        results{i}.gapstatsByLat.numPts(ind) = ca.getCoveragePoints(lat).size;
        
        sorted_gaps = ca.getSortedGapes(lat);
        violating_gaps = sorted_gaps(sorted_gap > gap_threshold);
        chrc_gap = sum(violating_gaps)/(tSimulation*double(ca.getCoveragePoints.size));
        results{i}.gapstats = setfield(results{i}.gapstats,sprintf('CHRC_%.5f',gap_threshold), chrc_gap);
        clear sorted_gaps violating_gaps %try to reduce memory footprint by removing memory intensive arrays
        
        ind = ind + 1;
    end
    
    %collect the access data by point
    pts = ca.getCoveragePoints;
    pts_data = zeros(pts.size,10);
    iter = pts.iterator;
    ind = 1;
    while(iter.hasNext)
        point = iter.next;
        pts_data(ind,1) = point.getPoint.getLatitude;
        pts_data(ind,2 )= point.getPoint.getLongitude;
        pts_data(ind,3) = ca.getMeanGap(pt);
        pts_data(ind,4) = ca.getMaxGap(pt);
        pts_data(ind,5) = ca.getMinGap(pt);
        pts_data(ind,6) = ca.getPercentileGap(pt,85);
        pts_data(ind,7) = ca.getPercentileGap(pt,90);
        pts_data(ind,8) = ca.getPercentileGap(pt,95);
        pts_data(ind,9) = ca.getSumGap(pt);
        sorted_gaps = ca.getSortedGapes(pt);
        violating_gaps = sorted_gaps(sorted_gap > gap_threshold);
        pts_data(ind,10) = sum(violating_gaps)/(tSimulation*double(ca.getCoveragePoints.size));
        clear sorted_gaps violating_gaps %try to reduce memory footprint by removing memory intensive arrays
        
        ind = ind + 1;
    end
    results{i}.gapstatsByPoints.metrics = pts_data;
    results{i}.gapstatsByPoints.header = {'Latitude','Longitude',...
        'Mean gap', 'Max gap', 'Min gap', '85 pct gap', '90 pct gap', '95 pct gap',...
        'Sum gap', sprintf('CHRC_%.5f gap',gap_threshold)};
    
    %save inputs and outputs of scenarios
    results{exp_i}.a = a;
    results{exp_i}.e = e;
    results{exp_i}.inc = inc;
    results{exp_i}.argper = argper;
    results{exp_i}.raan = raan;
    results{exp_i}.m_anom = m_anom;
    results{exp_i}.coverage
    results{exp_i}.fov = rect_fov;
    results{exp_i}.n_orbits = n_orbits;
    results{exp_i}.n_sats_per_plane = n_sats_per_plane;
    results{exp_i}.cov_grid_bounds = cov_grid_bounds;
    results{exp_i}.cov_grid_granularity = coverageGridGranularity;
    results{exp_i}.metrics = metrics;
end

filesave = [savepath scenario_name];% '-' date '-' hour '-' min];
save(filesave,'results');

%% Remove orekit jar file from matlab dynamic path
orekit_end()

end