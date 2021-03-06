function results = compareAccessGaps()

%Compares global acccesses and gaps from STK vs orekit.
%assumes file input is a csv with the first column containing start times
%and the second column containing end times

% path = 'C:\Users\SEAK1\Nozomi\OREKIT\Documentation\Comparing STK vs orekit\';
path = '/Users/nozomihitomi/Dropbox/OREKIT/Documentation/Comparing STK vs orekit/';

filename = '1x600km_45degINC_45degConical_Kepler_20160101-20160201';
stkdata = read_accesses(strcat(path,filename,'.cvaa'));
orekitdata = read_accesses(strcat(path,filename,'.cva'));

%global statistic
figure(1)
subplot(3,2,1)
[f,x] = ecdf(stkdata(:,4)-stkdata(:,3));
plot(x,f,'b')
hold on
[f,x] = ecdf(orekitdata(:,4)-orekitdata(:,3));
plot(x,f,'r')
set(gca,'fontsize',14)
xlabel('access time','fontsize',14)
ylabel('cdf','fontsize',14)
hold off

%to get global gaps, need to find the access times at each individual point
stk_uniq_lat = unique(stkdata(:,1));
stk_uniq_lon = unique(stkdata(:,2));
stk_points = fullfact([length(stk_uniq_lat),length(stk_uniq_lon)]);
stk_gaps = zeros(length(stkdata),1);
stk_accesses_lat = zeros(length(stk_uniq_lat),4); %mean, min, max, 90th percentile
stk_gaps_lat = zeros(length(stk_uniq_lat),4); %mean, min, max, 90th percentile
%iterate through the unique points to obtain the times at the points
ngaps = 0;
for i=1:length(stk_points)
    lat = stk_uniq_lat(stk_points(i,1));
    lon = stk_uniq_lon(stk_points(i,2));
    ind = and(stkdata(:,1)==lat, stkdata(:,2)==lon);
    times = stkdata(ind,3:4);
    if(isempty(times))
        continue
    end
    stk_gaps(ngaps+1:ngaps+length(times)-1) = times(2:end,2)-times(1:end-1,1);
    ngaps = ngaps + length(times)-1;
end
stk_gaps(ngaps+1:end)=[];
for i=1:length(stk_uniq_lat)
    ind = stkdata(:,1)==stk_uniq_lat(i);
    times = stkdata(ind,3:4);
    if(isempty(times))
        stk_gaps_lat(i,:) = NaN;
        continue;
    end
    stk_accesses_lat(i,1)=mean(times(:,2)-times(:,1));
    stk_accesses_lat(i,2)=min(times(:,2)-times(:,1));
    stk_accesses_lat(i,3)=max(times(:,2)-times(:,1));
    stk_accesses_lat(i,4)=prctile(times(:,2)-times(:,1),90);
    
    stk_gaps_lat(i,1)=mean(times(2:end,2)-times(1:end-1,1));
    stk_gaps_lat(i,2)=min(times(2:end,2)-times(1:end-1,1));
    stk_gaps_lat(i,3)=max(times(2:end,2)-times(1:end-1,1));
    stk_gaps_lat(i,4)=prctile((times(2:end,2)-times(1:end-1,1)),90);
end

%do the same for the orekit data
orekit_uniq_lat = unique(orekitdata(:,1));
orekit_uniq_lon = unique(orekitdata(:,2));
orekit_points = fullfact([length(orekit_uniq_lat),length(orekit_uniq_lon)]);
orekit_gaps = zeros(length(orekitdata),1);
orekit_accesses_lat = zeros(length(orekit_uniq_lat),4); %mean, min, max, 90th percentile
orekit_gaps_lat = zeros(length(orekit_uniq_lat),4); %mean, min, max, 90th percentile
%iterate through the unique points to obtain the times at the points
ngaps = 0;
for i=1:length(orekit_points)
    lat = orekit_uniq_lat(orekit_points(i,1));
    lon = orekit_uniq_lon(orekit_points(i,2));
    ind = and(orekitdata(:,1)==lat,orekitdata(:,2)==lon);
    times = orekitdata(ind,3:4);
    if(size(times,1) < 2)
        continue
    end
    orekit_gaps(ngaps+1:ngaps+length(times)-1) = times(2:end,2)-times(1:end-1,1);
    ngaps = ngaps + length(times)-1;
end
orekit_gaps(ngaps+1:end)=[];
for i=1:length(orekit_uniq_lat)
    ind = orekitdata(:,1)==orekit_uniq_lat(i);
    times = orekitdata(ind,3:4);
    if(isempty(times))
        stk_gaps_lat(i,:) = NaN;
        continue;
    end
    orekit_accesses_lat(i,1)=mean(times(:,2)-times(:,1));
    orekit_accesses_lat(i,2)=min(times(:,2)-times(:,1));
    orekit_accesses_lat(i,3)=max(times(:,2)-times(:,1));
    orekit_accesses_lat(i,4)=prctile(times(:,2)-times(:,1),90);
    
    orekit_gaps_lat(i,1)=mean(times(2:end,2)-times(1:end-1,1));
    orekit_gaps_lat(i,2)=min(times(2:end,2)-times(1:end-1,1));
    orekit_gaps_lat(i,3)=max(times(2:end,2)-times(1:end-1,1));
    orekit_gaps_lat(i,4)=prctile((times(2:end,2)-times(1:end-1,1)),90);
end
%global gap statistic
subplot(3,2,2)
[f,x] = ecdf(stk_gaps);
plot(x,f,'b')
hold on
[f,x] = ecdf(orekit_gaps);
plot(x,f,'r')
set(gca,'fontsize',14)
xlabel('gap time','fontsize',14)
ylabel('cdf','fontsize',14)
legend('stk','orekit','Location','SouthEast')
hold off

%per latitude access statistic
subplot(3,2,3)
plot(stk_uniq_lat,stk_accesses_lat(:,1),'b',orekit_uniq_lat,orekit_accesses_lat(:,1),'r');
set(gca,'fontsize',14)
xlabel('Latitude','fontsize',14)
ylabel('Mean access time (s)','fontsize',14)
subplot(3,2,4)
plot(stk_uniq_lat,stk_accesses_lat(:,4),'b',orekit_uniq_lat,orekit_accesses_lat(:,4),'r');
set(gca,'fontsize',14)
xlabel('Latitude','fontsize',14)
ylabel('90th percentile access (s)','fontsize',14)

%per latitude gap statistic
subplot(3,2,5)
plot(stk_uniq_lat,stk_gaps_lat(:,1),'b',orekit_uniq_lat,orekit_gaps_lat(:,1),'r');
set(gca,'fontsize',14)
xlabel('Latitude','fontsize',14)
ylabel('Mean gap time (s)','fontsize',14)
subplot(3,2,6)
plot(stk_uniq_lat,stk_gaps_lat(:,4),'b',orekit_uniq_lat,orekit_gaps_lat(:,4),'r');
set(gca,'fontsize',14)
xlabel('Latitude','fontsize',14)
ylabel('90th percentile gap (s)','fontsize',14)
end

%method to read in stk-like cvaa files
function results = read_accesses(filename)
fid = fopen(filename,'r');
max_rows = 1000000;
results = zeros(max_rows,4);
i = 1;
pt_i = -1;
n_pts = 1148;
h = waitbar(0,strcat('Points accessed (%s) ...',filename));
while(~feof(fid))  
    tline = fgetl(fid);
    % point update
    latlon_update = regexp(tline,'Lat:\s+(?<lat>[-+]*\d+\.\d+[eE][-+]*\d+)','tokens');
    if ~isempty(latlon_update)
        lat = str2double(latlon_update{1});
        tline = fgetl(fid);
        latlon_update = regexp(tline,'Lon:\s+(?<lon>[-+]*\d+\.\d+[eE][-+]*\d+)','tokens');
        lon = str2double(latlon_update{1});
        pt_i = pt_i + 1;
        waitbar( pt_i/n_pts,h);
        continue;
    end
    % new point
    gap = regexp(tline,'(?<t0>-*\d+\.\d+[eE][-+]*\d+)\s+(?<t1>-*\d+\.\d+[eE][-+]*\d+)','names');
    if ~isempty(gap)
        results(i,:) = [lat lon str2double(gap.t0) str2double(gap.t1)];
        i=i+1;
    end
end
fclose(fid);
close(h);
results(i:end,:) = [];
end