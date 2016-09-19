orbit1 = csvread('test.txt');
% orbit2 = csvread('orbit2Data.txt');
grid = csvread('gridtest.txt');
pt1data = csvread('testpt.txt');
% pt2data = csvread('pt_-50lat_-60lon_Data.txt');
npts = 104;
step_size = 60;

% color1 = [  1, 1, 1;
%             .9, 1, 1;
%             .8, 1, 1;
%             .7, 1, 1;
%             .6, 1, 1;
%             .5, 1, 1;
%             .4, 1, 1;
%             .3, 1, 1;
%             .2, 1, 1;
%             .1, 1, 1];
%         
% color2 = [  1, 1, 1;
%             1, .9, 1;
%             1, .8, 1;
%             1, .7, 1;
%             1, .6, 1;
%             1, .5, 1;
%             1, .4, 1;
%             1, .3, 1;
%             1, .2, 1;
%             1, .1, 1];
        
mins = min([grid;orbit1;pt1data]);
maxs = max([grid;orbit1;pt1data]);


tail_length = 0;
time = 0 + tail_length * step_size;
for i = tail_length+1:length(data)
scatter3(orbit1(i,1), orbit1(i,2), orbit1(i,3),'r');
hold on
plot3(orbit1(i-tail_length:i,1),orbit1(i-tail_length:i,2),orbit1(i-tail_length:i,3),'--r')
% scatter3(orbit2(i,1), orbit2(i,2), orbit2(i,3),'b');
% plot3(orbit2(i-dt:i,1),orbit2(i-dt:i,2),orbit2(i-dt:i,3),'--b')
scatter3(pt1data(i,1), pt1data(i,2), pt1data(i,3),100,'c')
plot3(pt1data(i-tail_length:i,1),pt1data(i-tail_length:i,2),pt1data(i-tail_length:i,3),'c')
% scatter3(pt2data(i,1), pt2data(i,2), pt2data(i,3),'k')
% plot3(pt2data(i-dt:i,1),pt2data(i-dt:i,2),pt2data(i-dt:i,3),'k')
scatter3(grid(i*104:(i+1)*104,1),grid(i*104:(i+1)*104,2),grid(i*104:(i+1)*104,3),'k')

hold off
axis([mins(1),maxs(1),mins(2),maxs(2), mins(3), maxs(3)])
axis square
title(sprintf('Elapsed time (s): %.2f',time));
view(3)
shg
pause(0.1)
time = time + step_size;
end