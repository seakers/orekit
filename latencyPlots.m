clc; close all; clear all;

data = csvread('latencyResults3.csv');
[n,m] = size(data);

path = "./orekit/src/main/java/seakers/orekit/exec/results/";

NEN_lat = cell(14,2);
AWS_lat = cell(22,2);

NEN_gap = cell(14,2);
AWS_gap = cell(22,2);

NEN_cost = cell(14,2);
AWS_cost = cell(22,2);

NEN_dur = cell(14,2);
AWS_dur = cell(22,2);

NEN_data3d = [];
NEN_data3d_cl = [];

AWS_data3d = [];
AWS_data3d_cl = [];

sat_gap_NEN = cell(6,2);
sat_gap_AWS = cell(6,2);

for i = 1:n
    n_gnd = 0;
    n_gnd_str = num2str(data(i,m-2));
    
    if(contains(n_gnd_str, "e"))
        n_gnd = str2double(n_gnd_str(20:21)) + 1;
    else
        for j = 1:length(n_gnd_str)
            n_gnd = n_gnd + 1;
        end
    end
    
    nen = data(i,m-1);
    crosslinks = data(i,m);
    
    if nen == 1
       if crosslinks == 1
            A = [ NEN_lat{n_gnd,2}, data(i,1), data(i,2), data(i,3), data(i,4), data(i,5) ];
            NEN_lat{n_gnd,2} = A;
            
            B = [ NEN_gap{n_gnd,2}, data(i,6), data(i,7), data(i,8), ... 
                                    data(i,18), data(i,19), data(i,20), ...
                                    data(i,30), data(i,31), data(i,32), ...
                                    data(i,42), data(i,43), data(i,44), ...
                                    data(i,54), data(i,55), data(i,56), ...
                                    data(i,66), data(i,67), data(i,68)];
            NEN_gap{n_gnd,2} = B;
            
            C = [ NEN_cost{n_gnd,2}, data(i,m-8), data(i,m-7), data(i,m-6), data(i,m-5), data(i,m-4) ];
            NEN_cost{n_gnd,2} = C;
            
            D = [NEN_dur{n_gnd,2},  data(i,76),data(i,77),data(i,78),data(i,79),data(i,80),data(i,81), ...
                                    data(i,82),data(i,83),data(i,84),data(i,85),data(i,86),data(i,87), ...
                                    data(i,88),data(i,89),data(i,90),data(i,91),data(i,92),data(i,93), ...
                                    data(i,94),data(i,95),data(i,96),data(i,97),data(i,98),data(i,99), ...
                                    data(i,100),data(i,101),data(i,102),data(i,103),data(i,104),data(i,105), ...
                                    data(i,106),data(i,107),data(i,108),data(i,109),data(i,110),data(i,111) ];
            NEN_dur{n_gnd,2} = D;
            
            NEN_data3d_cl = [NEN_data3d_cl; 
                             data(i,7)  data(i,m-7) n_gnd data(i,77) data(i,2);
                             data(i,19) data(i,m-7) n_gnd data(i,83) data(i,2);
                             data(i,31) data(i,m-7) n_gnd data(i,89) data(i,2);
                             data(i,43) data(i,m-7) n_gnd data(i,95) data(i,2);
                             data(i,55) data(i,m-7) n_gnd data(i,101) data(i,2);
                             data(i,67) data(i,m-7) n_gnd data(i,107) data(i,2)];
           
           sat_gap_NEN{1,2} = [sat_gap_NEN{1,2}, data(i,6), data(i,7), data(i,8)];
           sat_gap_NEN{2,2} = [sat_gap_NEN{2,2}, data(i,18), data(i,19), data(i,20)];
           sat_gap_NEN{3,2} = [sat_gap_NEN{3,2}, data(i,30), data(i,31), data(i,32)];
           sat_gap_NEN{4,2} = [sat_gap_NEN{4,2}, data(i,42), data(i,43), data(i,44)];
           sat_gap_NEN{5,2} = [sat_gap_NEN{5,2}, data(i,54), data(i,55), data(i,56)];
           sat_gap_NEN{6,2} = [sat_gap_NEN{6,2}, data(i,66), data(i,67), data(i,68)];
       else
            A = [ NEN_lat{n_gnd,1}, data(i,1), data(i,2), data(i,3), data(i,4), data(i,5) ];
            NEN_lat{n_gnd,1} = A;
            
            B = [ NEN_gap{n_gnd,1}, data(i,6), data(i,7), data(i,8), ... 
                                    data(i,18), data(i,19), data(i,20), ...
                                    data(i,30), data(i,31), data(i,32), ...
                                    data(i,42), data(i,43), data(i,44), ...
                                    data(i,54), data(i,55), data(i,56), ...
                                    data(i,66), data(i,67), data(i,68)];
            NEN_gap{n_gnd,1} = B;
            
            C = [ NEN_cost{n_gnd,1}, data(i,m-8), data(i,m-7), data(i,m-6), data(i,m-5), data(i,m-4) ];
            NEN_cost{n_gnd,1} = C;
            
            D = [NEN_dur{n_gnd,1},  data(i,76),data(i,77),data(i,78),data(i,79),data(i,80),data(i,81), ...
                                    data(i,82),data(i,83),data(i,84),data(i,85),data(i,86),data(i,87), ...
                                    data(i,88),data(i,89),data(i,90),data(i,91),data(i,92),data(i,93), ...
                                    data(i,94),data(i,95),data(i,96),data(i,97),data(i,98),data(i,99), ...
                                    data(i,100),data(i,101),data(i,102),data(i,103),data(i,104),data(i,105), ...
                                    data(i,106),data(i,107),data(i,108),data(i,109),data(i,110),data(i,111) ];
            NEN_dur{n_gnd,1} = D;
            
            NEN_data3d = [ NEN_data3d; 
                             data(i,7)  data(i,m-7) n_gnd data(i,77) data(i,2);
                             data(i,19) data(i,m-7) n_gnd data(i,83) data(i,2);
                             data(i,31) data(i,m-7) n_gnd data(i,89) data(i,2);
                             data(i,43) data(i,m-7) n_gnd data(i,95) data(i,2);
                             data(i,55) data(i,m-7) n_gnd data(i,101) data(i,2);
                             data(i,67) data(i,m-7) n_gnd data(i,107) data(i,2)];
                       
           sat_gap_NEN{1,1} = [sat_gap_NEN{1,2}, data(i,6), data(i,7), data(i,8)];
           sat_gap_NEN{2,1} = [sat_gap_NEN{2,2}, data(i,18), data(i,19), data(i,20)];
           sat_gap_NEN{3,1} = [sat_gap_NEN{3,2}, data(i,30), data(i,31), data(i,32)];
           sat_gap_NEN{4,1} = [sat_gap_NEN{4,2}, data(i,42), data(i,43), data(i,44)];
           sat_gap_NEN{5,1} = [sat_gap_NEN{5,2}, data(i,54), data(i,55), data(i,56)];
           sat_gap_NEN{6,1} = [sat_gap_NEN{6,2}, data(i,66), data(i,67), data(i,68)];
       end
    else
        if crosslinks == 1
            A = [ AWS_lat{n_gnd,2}, data(i,1), data(i,2), data(i,3), data(i,4), data(i,5) ];
            AWS_lat{n_gnd,2} = A;
            
            B = [ AWS_gap{n_gnd,2}, data(i,6), data(i,7), data(i,8), ... 
                                    data(i,18), data(i,19), data(i,20), ...
                                    data(i,30), data(i,31), data(i,32), ...
                                    data(i,42), data(i,43), data(i,44), ...
                                    data(i,54), data(i,55), data(i,56), ...
                                    data(i,66), data(i,67), data(i,68)];
            AWS_gap{n_gnd,2} = B;
            
            C = [ AWS_cost{n_gnd,2}, data(i,m-8), data(i,m-7), data(i,m-6), data(i,m-5), data(i,m-4) ];
            AWS_cost{n_gnd,2} = C;
            
            D = [AWS_dur{n_gnd,2},  data(i,76),data(i,77),data(i,78),data(i,79),data(i,80),data(i,81), ...
                                    data(i,82),data(i,83),data(i,84),data(i,85),data(i,86),data(i,87), ...
                                    data(i,88),data(i,89),data(i,90),data(i,91),data(i,92),data(i,93), ...
                                    data(i,94),data(i,95),data(i,96),data(i,97),data(i,98),data(i,99), ...
                                    data(i,100),data(i,101),data(i,102),data(i,103),data(i,104),data(i,105), ...
                                    data(i,106),data(i,107),data(i,108),data(i,109),data(i,110),data(i,111) ];
            AWS_dur{n_gnd,2} = D;
            
            AWS_data3d_cl = [ AWS_data3d_cl; 
                             data(i,7)  data(i,m-7) n_gnd data(i,77) data(i,2);
                             data(i,19) data(i,m-7) n_gnd data(i,83) data(i,2);
                             data(i,31) data(i,m-7) n_gnd data(i,89) data(i,2);
                             data(i,43) data(i,m-7) n_gnd data(i,95) data(i,2);
                             data(i,55) data(i,m-7) n_gnd data(i,101) data(i,2);
                             data(i,67) data(i,m-7) n_gnd data(i,107) data(i,2)];
                       
           sat_gap_AWS{1,2} = [sat_gap_NEN{1,2}, data(i,6), data(i,7), data(i,8)];
           sat_gap_AWS{2,2} = [sat_gap_NEN{2,2}, data(i,18), data(i,19), data(i,20)];
           sat_gap_AWS{3,2} = [sat_gap_NEN{3,2}, data(i,30), data(i,31), data(i,32)];
           sat_gap_AWS{4,2} = [sat_gap_NEN{4,2}, data(i,42), data(i,43), data(i,44)];
           sat_gap_AWS{5,2} = [sat_gap_NEN{5,2}, data(i,54), data(i,55), data(i,56)];
           sat_gap_AWS{6,2} = [sat_gap_NEN{6,2}, data(i,66), data(i,67), data(i,68)];
           
       else
            A = [ AWS_gap{n_gnd,1}, data(i,1), data(i,2), data(i,3), data(i,4), data(i,5) ];
            AWS_gap{n_gnd,1} = A;
            
            B = [ AWS_lat{n_gnd,1}, data(i,6), data(i,7), data(i,8), ... 
                                    data(i,18), data(i,19), data(i,20), ...
                                    data(i,30), data(i,31), data(i,32), ...
                                    data(i,42), data(i,43), data(i,44), ...
                                    data(i,54), data(i,55), data(i,56), ...
                                    data(i,66), data(i,67), data(i,68)];
            AWS_lat{n_gnd,1} = B;
            
            C = [ AWS_cost{n_gnd,1}, data(i,m-8), data(i,m-7), data(i,m-6), data(i,m-5), data(i,m-4) ];
            AWS_cost{n_gnd,1} = C;
            
            D = [AWS_dur{n_gnd,1},  data(i,76),data(i,77),data(i,78),data(i,79),data(i,80),data(i,81), ...
                                    data(i,82),data(i,83),data(i,84),data(i,85),data(i,86),data(i,87), ...
                                    data(i,88),data(i,89),data(i,90),data(i,91),data(i,92),data(i,93), ...
                                    data(i,94),data(i,95),data(i,96),data(i,97),data(i,98),data(i,99), ...
                                    data(i,100),data(i,101),data(i,102),data(i,103),data(i,104),data(i,105), ...
                                    data(i,106),data(i,107),data(i,108),data(i,109),data(i,110),data(i,111) ];
            AWS_dur{n_gnd,1} = D;
            
            AWS_data3d = [ AWS_data3d; 
                             data(i,7)  data(i,m-7) n_gnd data(i,77) data(i,2);
                             data(i,19) data(i,m-7) n_gnd data(i,83) data(i,2);
                             data(i,31) data(i,m-7) n_gnd data(i,89) data(i,2);
                             data(i,43) data(i,m-7) n_gnd data(i,95) data(i,2);
                             data(i,55) data(i,m-7) n_gnd data(i,101) data(i,2);
                             data(i,67) data(i,m-7) n_gnd data(i,107) data(i,2)];
                       
           sat_gap_AWS{1,1} = [sat_gap_NEN{1,2}, data(i,6), data(i,7), data(i,8)];
           sat_gap_AWS{2,1} = [sat_gap_NEN{2,2}, data(i,18), data(i,19), data(i,20)];
           sat_gap_AWS{3,1} = [sat_gap_NEN{3,2}, data(i,30), data(i,31), data(i,32)];
           sat_gap_AWS{4,1} = [sat_gap_NEN{4,2}, data(i,42), data(i,43), data(i,44)];
           sat_gap_AWS{5,1} = [sat_gap_NEN{5,2}, data(i,54), data(i,55), data(i,56)];
           sat_gap_AWS{6,1} = [sat_gap_NEN{6,2}, data(i,66), data(i,67), data(i,68)];
       end
    end
    
    x = 1;
end

NEN_lat_box = [];
NEN_lat_box_cl = [];

NEN_gap_box = [];
NEN_gap_box_cl = [];

NEN_cost_box = [];
NEN_cost_box_cl = [];

NEN_dur_box = [];
NEN_dur_box_cl = [];

for i = 1:14
    NEN_lat_box(:,i) = NEN_lat{i,1}';
    NEN_lat_box_cl(:,i) = NEN_lat{i,2}';
    
    NEN_gap_box(:,i) = NEN_gap{i,1}';
    NEN_gap_box_cl(:,i) = NEN_gap{i,2}';
    
    NEN_cost_box(:,i) = NEN_cost{i,1}';
    NEN_cost_box_cl(:,i) = NEN_cost{i,2}';
    
    NEN_dur_box(:,i) = NEN_dur{i,1}';
    NEN_dur_box_cl(:,i) = NEN_dur{i,2}';
end

AWS_lat_box = [];
AWS_lat_box_cl = [];

AWS_gap_box = [];
AWS_gap_box_cl = [];

AWS_cost_box = [];
AWS_cost_box_cl = [];

AWS_dur_box = [];
AWS_dur_box_cl = [];

for i = 1:22
    AWS_lat_box(:,i) = AWS_lat{i,1}';
    AWS_lat_box_cl(:,i) = AWS_lat{i,2}';
    
    AWS_gap_box(:,i) = AWS_gap{i,1}';
    AWS_gap_box_cl(:,i) = AWS_gap{i,2}';
    
    AWS_cost_box(:,i) = AWS_cost{i,1}';
    AWS_cost_box_cl(:,i) = AWS_cost{i,2}';
    
    AWS_dur_box(:,i) = AWS_dur{i,1}';
    AWS_dur_box_cl(:,i) = AWS_dur{i,2}';
end

NEN_sat_gap_box = [];
NEN_sat_gap_box_cl = [];

AWS_sat_gap_box = [];
AWS_sat_gap_box_cl = [];

for i = 1:6
    NEN_sat_gap_box(:,i) = sat_gap_NEN{i,1};
    NEN_sat_gap_box_cl(:,i) = sat_gap_NEN{i,2}; 
    
    AWS_sat_gap_box(:,i) = sat_gap_AWS{i,1};
    AWS_sat_gap_box_cl(:,i) = sat_gap_AWS{i,2}; 
end

%% PLOTS

% LATENCY PLOTS
lat_fig_nen = figure;
subplot(1,2,1)
boxplot(NEN_lat_box/60)
grid on
title(["Measurement Latency vs";"Number of Ground Stations";"(NEN)"])
xlabel("Number of Ground Stations")
ylabel("Measurement Latency [min]")
ylim([-50 800])

subplot(1,2,2)
boxplot(NEN_lat_box_cl/60)
grid on
title(["Measurement Latency vs";"Number of Ground Stations";"(NEN + CL)"])
xlabel("Number of Ground Stations")
ylabel("Measurement Latency [min]")
ylim([-50 800])

lat_fig_aws = figure;
subplot(1,2,1)
boxplot(AWS_lat_box/60)
grid on
title(["Measurement Latency vs";"Number of Ground Stations";"(AWS)"])
xlabel("Number of Ground Stations")
ylabel("Measurement Latency [min]")
ylim([-50 800])

subplot(1,2,2)
boxplot(AWS_lat_box_cl/60)
grid on
title(["Measurement Latency vs";"Number of Ground Stations";"(AWS + CL)"])
xlabel("Number of Ground Stations")
ylabel("Measurement Latency [min]")
ylim([-50 800])

% GAP TIME 
gap_fig_nen = figure;
subplot(1,2,1)
boxplot(NEN_gap_box/60)
grid on
title(["GS Gap Time vs";"Number of Ground Stations";"(NEN)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Gap Time [min]")
ylim([-50 800])

subplot(1,2,2)
boxplot(NEN_gap_box_cl/60)
grid on
title(["GS Gap Time vs";"Number of Ground Stations";"(NEN + CL)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Gap Time [min]")
ylim([-50 800])

gap_fig_aws = figure;
subplot(1,2,1)
boxplot(AWS_gap_box/60)
grid on
title(["GS Gap Time vs";"Number of Ground Stations";"(AWS)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Gap Time [min]")
ylim([-50 800])

subplot(1,2,2)
boxplot(AWS_gap_box_cl/60)
grid on
title(["GS Gap Time vs";"Number of Ground Stations";"(AWS + CL)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Gap Time [min]")
ylim([-50 800])

% ACCESS DURATION
dur_fig_nen = figure;
subplot(1,2,1)
boxplot(NEN_dur_box/3600    )
grid on
title(["GS Daily Access Time Duration vs";"Number of Ground Stations";"(NEN)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Access Time Duration [hrs]")
ylim([0 24])

subplot(1,2,2)
boxplot(NEN_dur_box_cl/3600)
grid on
title(["GS Daily Access Time Duration vs";"Number of Ground Stations";"(NEN + CL)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Access Time Duration [hrs]")
ylim([0 24])

dur_fig_aws = figure;
subplot(1,2,1)
boxplot(AWS_dur_box/3600)
grid on
title(["GS Daily Access Time Duration vs";"Number of Ground Stations";"(AWS)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Access Time Duration [hrs]")
ylim([0 24])

subplot(1,2,2)
boxplot(AWS_dur_box_cl/3600)
grid on
title(["GS Daily Access Time Duration vs";"Number of Ground Stations";"(AWS + CL)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Access Time Duration [hrs]")
ylim([0 24])

% GAP TIME PER SAT
gap_sat_fig_nen = figure;
subplot(1,2,1)
boxplot(NEN_sat_gap_box/60)
grid on
title(["GS Gap Time per";"Satellite (NEN)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Gap Time [min]")
ylim([-50 800])

subplot(1,2,2)
boxplot(NEN_sat_gap_box_cl/60)
grid on
title(["GS Gap Time per";"Satellite (NEN + CL)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Gap Time [min]")
ylim([-50 800])

gap_sat_fig_aws = figure;
subplot(1,2,1)
boxplot(AWS_sat_gap_box/60)
grid on
title(["GS Gap Time per";"Satellite (AWS)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Gap Time [min]")
ylim([-50 800])

subplot(1,2,2)
boxplot(AWS_sat_gap_box_cl/60)
grid on
title(["GS Gap Time per";"Satellite (AWS + CL)"])
xlabel("Number of Ground Stations")
ylabel("Ground Station Gap Time [min]")
ylim([-50 800])


% COST
cost_fig_nen = figure;
subplot(1,2,1)
boxplot(NEN_cost_box)
grid on
title(["Daily Operating Costs vs";"Number of Ground Stations";"(NEN)"])
xlabel("Number of Ground Stations")
ylabel("Daily Operating Costs $USD")
ylim([-50 20E4])

subplot(1,2,2)
boxplot(NEN_cost_box_cl)
grid on
title(["Daily Operating Costs vs";"Number of Ground Stations";"(NEN + CL)"])
xlabel("Number of Ground Stations")
ylabel("Daily Operating Costs $USD")
ylim([-50 20E4])

cost_fig_aws = figure;
subplot(1,2,1)
boxplot(AWS_cost_box)
grid on
title(["Daily Operating Costs vs";"Number of Ground Stations";"(AWS)"])
xlabel("Number of Ground Stations")
ylabel("Daily Operating Costs $USD")
ylim([-50 20E4])

subplot(1,2,2)
boxplot(AWS_cost_box_cl)
grid on
title(["Daily Operating Costs vs";"Number of Ground Stations";"(AWS + CL)"])
xlabel("Number of Ground Stations")
ylabel("Daily Operating Costs $USD")
ylim([-50 20E4])

% 3D PLOTS
figure
scatter3(NEN_data3d(:,3), NEN_data3d(:,2),NEN_data3d(:,1)/3600, 'filled')
hold on
grid on
scatter3(NEN_data3d_cl(:,3), NEN_data3d_cl(:,2),NEN_data3d_cl(:,1)/3600, 'filled')
scatter3(AWS_data3d(:,3), AWS_data3d(:,2),AWS_data3d(:,1)/3600, 'filled')
scatter3(AWS_data3d_cl(:,3), AWS_data3d_cl(:,2),AWS_data3d_cl(:,1)/3600, 'filled')
xlabel("Number of Ground Stations")
ylabel("Daily Cost $")
zlabel("GS Gap Time [hrs]")
legend("NEN", "NEN + Crosslinks","AWS", "AWS + Crosslinks")
title("AWS vs NEN")

%% 2D PLOTS
ymin = ones(23,2);
ymin(:,1) = [0:1:22];
ymin(:,2) = 60;

triple_fig = figure('position', [1000, 1000, 700, 850]);
subplot(3,1,1)
scatter(NEN_data3d(:,3),NEN_data3d(:,1)/60);
hold on
grid on
scatter(NEN_data3d_cl(:,3),NEN_data3d_cl(:,1)/60);
scatter(AWS_data3d(:,3),AWS_data3d(:,1)/60,'*');
scatter(AWS_data3d_cl(:,3),AWS_data3d_cl(:,1)/60,'*');
plot(ymin(:,1),ymin(:,2),'--r')
xlabel("Number of Ground Stations")
ylabel("GS Gap Time [min]")
legend("NEN", "NEN + Crosslinks","AWS", "AWS + Crosslinks", '1 hr')
title("Gap Time vs Number Satellites")
xticks([1:1:22])
yticks([0:60:600])

subplot(3,1,2)
scatter(NEN_data3d(:,3),NEN_data3d(:,2)/1e3);
hold on
grid on
scatter(NEN_data3d_cl(:,3),NEN_data3d_cl(:,2)/1e3);
scatter(AWS_data3d(:,3),AWS_data3d(:,2)/1e3,'*');
scatter(AWS_data3d_cl(:,3),AWS_data3d_cl(:,2)/1e3,'*');
xlabel("Number of Ground Stations")
ylabel("Daily Cost $K")
legend("NEN", "NEN + Crosslinks","AWS", "AWS + Crosslinks")
title("Gap Time vs Number Satellites")
xticks([1:1:22])
yticks([0:25:200])

ymin = ones(23,2);
ymin(:,1) = [0:1:22];
ymin(:,2) = 1/24;

subplot(3,1,3)
scatter(NEN_data3d(:,3),NEN_data3d(:,4)/(24*3600));
hold on
grid on
scatter(NEN_data3d_cl(:,3),NEN_data3d_cl(:,4)/(24*3600));
scatter(AWS_data3d(:,3),AWS_data3d(:,4)/(24*3600),'*');
scatter(AWS_data3d_cl(:,3),AWS_data3d_cl(:,4)/(24*3600),'*');
% plot(ymin(:,1),ymin(:,2),'--r')
xlabel("Number of Ground Stations")
ylabel("Percentage of Day in GS Access [%]")
legend("NEN", "NEN + Crosslinks","AWS", "AWS + Crosslinks", '1 hr')
title("Percentage of Day in GS Access vs Number Satellites")
xticks([1:1:22])
ylim([0 1])
yticks([0:0.1:1])

% Dif Plots



%% Save Plots

saveas(lat_fig_nen,  path+"\lat_fig_nen.png",'png');
saveas(lat_fig_aws,  path+".\lat_fig_aws.png",'png');
saveas(gap_sat_fig_nen,  path+".\gap_sat_fig_nen.png",'png');
saveas(gap_sat_fig_aws,  path+".\gap_sat_fig_aws.png",'png');
saveas(gap_fig_nen,  path+".\gap_fig_nen.png",'png');
saveas(gap_fig_aws,  path+".\gap_fig_aws.png",'png');
saveas(dur_fig_nen,  path+".\dur_fig_nen.png",'png');
saveas(dur_fig_aws,  path+".\dur_fig_aws.png",'png');
saveas(cost_fig_nen, path+".\cost_fig_nen.png",'png');
saveas(cost_fig_aws, path+".\cost_fig_aws.png",'png');
saveas(triple_fig, path+".\triple_fig.png",'png');

disp DONE