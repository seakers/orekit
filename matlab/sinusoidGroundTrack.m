function sinusoidGroundTrack(incs, raans, anoms)

%approximate ground tracks with sinusoid based solely on raan and mean
%anomaly. TODO try to incorproate inclination and maybe altitude as well
%approximation for cirucluar orbits only!
%
%incs is a vector of inclination values [deg] for each orbit to visualize
%raans is a vector of raan values [deg] for each orbit to visualize
%anoms is a vector of mean anomaly values [deg] for each orbit to visualize

%check that both vectors are the same length
if length(raans)~=length(anoms)
    error('Expected raans and anoms to be same length vectors. Found %d for raans and %d for anoms',length(raans), length(anoms))
end

if max(incs)>180 || min(incs)<0
    error('Inclinations must be in interval [0,180]')
end

%plot out 1000 points along the longitudes
lon = linspace(0,360,1000);
lons = repmat(lon,length(raans),1);

colors = colormap(jet);
colors = colors(1: ceil(length(colors)/length(raans)) : length(colors), :);
labels = cell(length(raans),1);
handles = zeros(length(raans),3);

figure(1)
clf
hold on
for i=1:length(raans)
    %check prograde or retrograde
    isPrograde = true;
    if incs(i) <= 90
        maxLat = incs(i);
        handles(i,:) = plot(lons,maxLat*sin((lon-raans(i))*pi/180),'Color',colors(i,:));
        xlocation = mod(anoms(i)+raans(i),360);
        ylocation = maxLat*sin((xlocation-raans(i))*pi/180);
        scatter(xlocation,ylocation,50,colors(i,:),'filled');
        
        slope = pi/180*maxLat*cos((xlocation-raans(i))*pi/180);
        quiver(xlocation,ylocation,1/norm([1,slope]),slope/norm([1,slope]),20,'Color',colors(i,:),'LineWidth',3.0,'MaxHeadSize',5);
    else
        maxLat = 180-incs(i);
        handles(i,:) = plot(lons,maxLat*cos((lon-raans(i))*pi/180),'Color',colors(i,:));
        xlocation = mod(anoms(i)+raans(i),360);
        ylocation = maxLat*cos((xlocation-raans(i))*pi/180);
        scatter(xlocation,ylocation,50,colors(i,:),'filled');
        
        slope = -pi/180*maxLat*sin((xlocation-raans(i))*pi/180);
        quiver(xlocation,ylocation,-1/norm([1,slope]),-slope/norm([1,slope]),20,'Color',colors(i,:),'LineWidth',3.0,'MaxHeadSize',5);
    end
    
    labels{i} = strcat('sat_',num2str(i));
end
hold off

axis([0,360,-90,90])
xlabel('longitude [deg]')
ylabel('latitude [deg]')
legend(handles(:,1),labels)

