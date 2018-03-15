clear all; close all; clc;
format long
data=csvread('/Users/paugarciabuzzi/Dropbox/orekit/orekit/results/SCOUT/analysis_SCOUT_angle_velocity_sun_sat1.vecang',1);
% data2=csvread('/Users/paugarciabuzzi/Desktop/stkfileshypmas/Satellite1sunvelocity.csv',1);
data3=csvread('/Users/paugarciabuzzi/Dropbox/orekit/orekit/results/SCOUT/analysis_lifetime_eph_sat1_SCOUT.eph',1);
% data4=csvread('/Users/paugarciabuzzi/Desktop/stkfileshypmas/Satellite1 Classical Orbit Elements.csv',1);
datadeg(:,2)=rad2deg(data(:,2));

figure
plot(data(:,1)/60/60/24/365,datadeg(:,2))
xlim([0 1.01])
x=xlabel('Time [years]');
y=ylabel('Angle between Velocity and Sat-Sun vectors [deg]');
set(x,'FontSize',16)
set(y,'FontSize',16)

% figure
% plot(data2(:,1)/60/60/24/365,data2(:,2))
% xlim([0 max(data(:,1)/60/60/24/365)])
% xlabel('years')
% ylabel('degrees')
 
semimajoraxis=data3(:,2)/1000;
altitude=semimajoraxis-6378.14;
figure
plot(data3(:,1)/60/60/24/365,altitude);
x=xlabel('Time [years]');
y=ylabel('Altitude [km]');
set(x,'FontSize',16)
set(y,'FontSize',16)
xlim([0 1.01])

% semimajoraxis=data4(:,2);
% altitude=semimajoraxis-6378.14;
% figure
% plot(data4(:,1)/60/60/24/365,altitude);
% xlim([0 max(data3(:,1)/60/60/24/365)])
% xlabel('years')
% ylabel('altitude')



%ENERGY
dataEclipse=csvread('/Users/paugarciabuzzi/Dropbox/orekit/orekit/results/SCOUT/analysis_test1_SCOUT.ecl',7);
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
Energy=1366*0.29*0.12*cos(data(:,2));
%Energy1=abs(Energy);%if it was double sided
Energy1=Energy.*((1+sign(Energy))/2); %one sided
Energy2=Energy1.*SunlightFlags;
plot(data(:,1)/60/60/24/365,Energy2);
xlim([0 1.01])
x=xlabel('Time [years]');
y=ylabel('Power [W]');
set(x,'FontSize',16)
set(y,'FontSize',16)


eclipseTime= sum(dataEclipse(:,2)-dataEclipse(:,1));
simulationTime = data(end,1)-data(1,1);
percentageEclipse = eclipseTime/simulationTime
EnergyNotCosideringEclipse = trapz(data(:,1),Energy1)/3600000 %kWh
EnergyCosideringEclipse = trapz(data(:,1),Energy2)/3600000 %kWh
AveragePower = trapz(data(:,1),Energy2)/simulationTime

figure
Period=92.56*60;%in seconds
ntrocets=floor(simulationTime/Period);
En=zeros(ntrocets,1);
samplesEachPeriod=length(0:60:Period)-1;
TotalSamples=ntrocets*samplesEachPeriod;
Energy2=Energy2(1:TotalSamples);
En(1)=trapz(data(1:samplesEachPeriod,1),Energy2(1:samplesEachPeriod))/3600000;
for i=1:ntrocets-1 
    En(i+1)=trapz(data(i*samplesEachPeriod:(i+1)*samplesEachPeriod,1),Energy2(i*samplesEachPeriod:(i+1)*samplesEachPeriod))/3600000;
end
plot(1:ntrocets,En)
x=xlabel('n_orbit');
y=ylabel('Energy [kWh]');
set(x,'FontSize',16)
set(y,'FontSize',16)