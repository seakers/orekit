clear all; close all; clc;
format long
data1=csvread('/Users/paugarciabuzzi/Dropbox/orekit/orekit/results/SCOUT(modified 135deg respect bus)/analysis_135_SP1_1_angle_velocity_sun_sat1.vecang',1);
data2=csvread('/Users/paugarciabuzzi/Dropbox/orekit/orekit/results/SCOUT(modified 135deg respect bus)/analysis_135_SP2_1_angle_velocity_sun_sat1.vecang',1);

datadeg1(:,2)=rad2deg(data1(:,2));
datadeg2(:,2)=rad2deg(data2(:,2));

figure
plot(data1(:,1)/60/60/24/365,datadeg1(:,2))
xlim([0 1.01])
x=xlabel('Time [years]');
y=ylabel('Angle between norma SP1 and Sat-Sun vectors [deg]');
set(x,'FontSize',16)
set(y,'FontSize',16)
figure
plot(data2(:,1)/60/60/24/365,datadeg2(:,2))
xlim([0 1.01])
x=xlabel('Time [years]');
y=ylabel('Angle between normal SP2 and Sat-Sun vectors [deg]');
set(x,'FontSize',16)
set(y,'FontSize',16)

%ENERGY
dataEclipse=csvread('/Users/paugarciabuzzi/Dropbox/orekit/orekit/results/SCOUT(modified 135deg respect bus)/135_analysis_test1.ecl',7);
RiseSetEclipse=sort([dataEclipse(:,1);dataEclipse(:,2)]);
x=[0:60:31622400]';
SunlightFlags=zeros(length(x),1);
for i=1:length(x)
    sortV=sort([RiseSetEclipse;x(i)]);
    [row,col,v] = find(sortV==x(i));
    if find(mod(row,2)==1)
        SunlightFlags(i)=1;
    else
        SunlightFlags(i)=0;
    end
end

figure
EnergySP1=1366*0.29*0.12*cos(data1(:,2));
EnergySP2=1366*0.29*0.12*cos(data2(:,2));
%Energy1=abs(Energy1)+abs(Energy2);%if it was double sided
Energy1=EnergySP1.*((1+sign(EnergySP1))/2)+EnergySP2.*((1+sign(EnergySP2))/2); %one sided
Energy2=Energy1.*SunlightFlags;
plot(data1(:,1)/60/60/24/365,Energy2);
xlim([0 1.01])
x=xlabel('Time [years]');
y=ylabel('Power [W]');
set(x,'FontSize',16)
set(y,'FontSize',16)

eclipseTime= sum(dataEclipse(:,2)-dataEclipse(:,1));
simulationTime = data1(end,1)-data1(1,1);
percentageEclipse = eclipseTime/simulationTime
EnergyNotCosideringEclipse = trapz(data1(:,1),Energy1)/3600000 %kWh
EnergyCosideringEclipse = trapz(data1(:,1),Energy2)/3600000 %kWh
AveragePower = trapz(data1(:,1),Energy2)/simulationTime