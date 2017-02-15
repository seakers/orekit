function revisitMap(path,filename)
%this function will read a scenario that has already been run and plot the
%revisit times on a heat map
% ../..
%parameters
%path: the directory containing the scneario file
%filename: .scen file that has already been run

try
    orekit_init;
    orekit.util.OrekitConfig.init([pwd,filesep,'orekit']);
   
    % load the scenario
    scen = orekit.scenario.ScenarioIO.load(java.io.File(path).toPath, filename);
    
    cdefTimeMap = scen.getFinalAccesses;
    cdefIter = cdefTimeMap.keySet.iterator;
    while(cdefIter.hasNext)
        figure
        cdef = cdefIter.next;
        ptTimeMap = cdefTimeMap.get(cdef);
        ca = orekit.coverage.analysis.CoverageAnalyzer(ptTimeMap);
        
        latitudes = ca.getLatitudes; %gets the sorted list of latitudes
        nlon = ca.getCoveragePoints(latitudes.get(0)).size;
        ptLat = zeros(latitudes.size, nlon);
        ptLon = zeros(size(ptLat,1), size(ptLat,2));
        vals = zeros(size(ptLat,1), size(ptLat,2));
        for i = 0 : latitudes.size-1
            lat = latitudes.get(i);
            ptLat(i+1, :) = lat;
            
            points = ca.getCoveragePoints(lat);
            for j = 0 : points.size-1
                ptLon(i+1, j+1) = points.get(j).getPoint.getLongitude;
                %choose metric here:
                vals(i+1, j+1) = ca.getMeanGap(points.get(j));
            end
            
            %sort longitudes (and corresponding vals) in ascending order
            [ptLon(i+1, :), sortingInd] = sort(ptLon(i+1, :));
            vals(i+1,sortingInd) =  vals(i+1, :);
        end
        
        surf(ptLat*180/pi,ptLon*180/pi + 180,vals, 'EdgeColor', 'None', 'facecolor', 'interp');
        
        xlabel('Latitude [deg]')
        ylabel('Longitude [deg]')
        title(sprintf('%s',char(cdef.toString)))
        colormap jet
        h = colorbar;
        h.Label.String = 'Mean Revisit Time [s]';
        axis([-90,90,0,360])
        view([90,-90])
    end
    
catch ME
    orekit_end();
    rethrow(ME)
end