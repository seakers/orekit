path = '/Users/nozomihitomi/Dropbox/OREKIT/orekit/results/';
orbit1 = csvread(strcat(path,'sat0.vec'),1,0);
orbit2 = csvread(strcat(path,'sat1.vec'),1,0);
orbit3 = csvread(strcat(path,'sat2.vec'),1,0);
% grid = csvread('gridtest.txt');
% pt1data = csvread('pt_0_1745lat_0_6283lon_Data.txt');
% pt2data = csvread('pt_-50lat_-60lon_Data.txt');
npts = 104;
step_size = 1;
        
mins = min([orbit1;orbit2;orbit3]);
maxs = max([orbit1;orbit2;orbit3]);


tail_length = 100;
time = 0 + tail_length * step_size;

re = 6378137;
alt = 800000;

[X,Y,Z] = sphere(20);

for i = tail_length+1:length(orbit1)
cla;
hold on
scatter3(orbit1(i,2), orbit1(i,3), orbit1(i,4),'r','filled');
scatter3(orbit2(i,2), orbit2(i,3), orbit2(i,4),'b','filled');
scatter3(orbit3(i,2), orbit3(i,3), orbit3(i,4),'g','filled');

plot3(orbit1(i-tail_length:i,2),orbit1(i-tail_length:i,3),orbit1(i-tail_length:i,4),'--r')
plot3(orbit2(i-tail_length:i,2),orbit2(i-tail_length:i,3),orbit2(i-tail_length:i,4),'--b')
plot3(orbit3(i-tail_length:i,2),orbit3(i-tail_length:i,3),orbit3(i-tail_length:i,4),'--g')

% scatter3(pt1data(i,1), pt1data(i,2), pt1data(i,3),100,'c')
% plot3(pt1data(i-tail_length:i,1),pt1data(i-tail_length:i,2),pt1data(i-tail_length:i,3),'c')

% scatter3(grid(i*104:(i+1)*104,1),grid(i*104:(i+1)*104,2),grid(i*104:(i+1)*104,3),'k')

surf(X.*re,Y.*re,Z.*re)

hold off
axis([-(re+alt),re+alt,-(re+alt),re+alt,-(re+alt),re+alt])
axis square
title(sprintf('Elapsed time (s): %.2f',time));
view(3)
shg
% pause(0.1)
pause
time = time + step_size;
end