%Compares orbital elements from STK vs orekit. csv file is assumed to
%contain data in the order of "Semi-major Axis (m)","Eccentricity","Inclination (deg)","RAAN (deg)","Arg of Perigee (deg)","Mean Anomaly (deg)"
%assumes that the rows are taken at the same time during the simulation
path = 'C:\Users\SEAK1\Nozomi\OREKIT\Documentation\Comparing STK vs orekit\';
stkdata = csvread(strcat(path,'1x600km_30degINC_HPOP_20160101-20160301.csv'),1,1);
orekitdata = csvread(strcat(path,'1x600km_30degINC_J2_20160101-20160301.eph'),1,0);

subplot(2,3,1)
plot(orekitdata(:,1),orekitdata(:,2),orekitdata(:,1),stkdata(:,1)*1e3)
xlabel('Epoch time (s)','fontsize',14)
ylabel('Semi-major Axis (m)','fontsize',14)
legend('orekit','stk')
set(gca,'fontsize',14)

subplot(2,3,2)
plot(orekitdata(:,1),orekitdata(:,3),orekitdata(:,1),stkdata(:,2))
xlabel('Epoch time (s)','fontsize',14)
ylabel('Eccentricity','fontsize',14)

subplot(2,3,3)
plot(orekitdata(:,1),orekitdata(:,4),orekitdata(:,1),stkdata(:,3))
xlabel('Epoch time (s)','fontsize',14)
ylabel('Inclination (deg)','fontsize',14)
set(gca,'fontsize',14)

subplot(2,3,4)
plot(orekitdata(:,1),mod(orekitdata(:,5),360),orekitdata(:,1),stkdata(:,4))
xlabel('Epoch time (s)','fontsize',14)
ylabel('RAAN (deg)','fontsize',14)
set(gca,'fontsize',14)

subplot(2,3,5)
plot(orekitdata(:,1),mod(orekitdata(:,6),360),orekitdata(:,1),stkdata(:,5))
xlabel('Epoch time (s)','fontsize',14)
ylabel('Arg. Per. (deg)','fontsize',14)
set(gca,'fontsize',14)

subplot(2,3,6)
plot(orekitdata(:,1),mod(orekitdata(:,7),360),orekitdata(:,1),stkdata(:,6))
xlabel('Epoch time (s)','fontsize',14)
ylabel('Mean Anomaly (deg)','fontsize',14)
set(gca,'fontsize',14)
shg