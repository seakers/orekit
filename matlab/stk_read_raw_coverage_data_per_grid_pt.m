%% stk_read_raw_coverage_data2.m
%this function reads in the cvaa coverage access files prodced in stk.
%The input to this function is the filename *.cvaa
%The output is a cell of cells. One cell for each grid point and each cell
%within contains the lat | lon | access start | access stop | access duration 
function [latitudes, longitudes, accesses] = stk_read_raw_coverage_data_per_grid_pt(filename)
fid = fopen(filename,'r');
MAX_POINTS = 10000;
latitudes = zeros(MAX_POINTS,1);
longitudes = zeros(MAX_POINTS,1);
accesses = cell(MAX_POINTS,1);
np = 0;
while(~feof(fid))  
    tline = fgetl(fid);
    %point update
    point_update = regexp(tline,'PointNumber:\s+(?<nr>\d+)','tokens');
    if ~isempty(point_update)
        if np>0
            accesses{np}(na:end,:) = [];
        end
        np = np + 1;
        accesses{np} = zeros(MAX_POINTS,3);
        na = 1;
        continue;
    end
    
    % point update
    latlon_update = regexp(tline,'Lat:\s+(?<lat>[-+]*\d+\.\d+[eE][-+]*\d+)','tokens');
    if ~isempty(latlon_update)
        lat = str2double(latlon_update{1});
        tline = fgetl(fid);
        latlon_update = regexp(tline,'Lon:\s+(?<lon>[-+]*\d+\.\d+[eE][-+]*\d+)','tokens');
        lon = str2double(latlon_update{1});
        latitudes(np) = lat;
        longitudes(np) = lon;
        continue;
    end
    % new point
    access = regexp(tline,'(?<i>\d+)\s+(?<t0>-*\d+\.\d+[eE][-+]*\d+)\s+(?<t1>-*\d+\.\d+[eE][-+]*\d+)','names');
    if ~isempty(access)
        accesses{np}(na,:) = [str2double(access.t0) str2double(access.t1) str2double(access.t1)-str2double(access.t0)];
        na = na + 1;
    end
end
accesses{np}(na:end,:) = [];
fclose(fid);

accesses(np+1:end,:) = [];
latitudes(np+1:end,:) = [];
longitudes(np+1:end,:) = [];

end
