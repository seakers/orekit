clc; close all; clear all;

save = false;

rawData = csvread('latencyResults.csv');
[n,m] = size(rawData);
n_metrics = 7;

path = "./orekit/src/main/java/seakers/orekit/exec/results/";

data = zeros(22,n_metrics*5,2,2,2);

for i = 1:n
    n_gnd = 0;
    n_gnd_str = num2str(rawData(i,m-3));
    
    if(contains(n_gnd_str, "e"))
        n_gnd = str2double(n_gnd_str(20:21)) + 1;
    else
        for j = 1:length(n_gnd_str)
            n_gnd = n_gnd + 1;
        end
    end  
  
    
    nen = rawData(i,m-2) + 1;
    strategy = rawData(i,m-1) + 1;
    crosslinks = rawData(i,m) + 1;
    
%     fprintf("NEN: %d, STRAT: %d, CL: %d\n", nen-1, strategy-1, crosslinks-1);
    
    for j = 0:n_metrics-1
        for k = 1:5
            data(n_gnd,(j*5)+k,nen,strategy,crosslinks) = rawData(i,(j*5)+k);  
        end
    end
end
conversions = [60 60 60 24*3600/100 1 1e6 1e3 1e3];

%% Req Print
req = [1, 2, 6, 12]*60;
for strategy = 1:2
    if strategy == 1
        strategy_label = "Conservative";
    else
        strategy_label = "Optimistic";
    end

    fprintf("\n" + strategy_label + " DL Strategy\n")
    
    costPrint = zeros(4,4);
    gsPrint = zeros(4,4);
    for nen = 1:2
        if nen == 1
            nen_label = "AWS";
        else
            nen_label = "NEN";
        end
        
        for crosslinks = 1:2
            if crosslinks == 1
                crosslinks_label = ":\t";
            else
                crosslinks_label = " w/CL:";
            end
            
            fprintf(nen_label + crosslinks_label) 
            
            gapData = data(:,((2-1)*5)+2,nen,strategy,crosslinks)'/conversions(2);
            [~, n] = size(gapData);
            
            for j = 1:length(req)
                i_req = -1;
                for i = 1:n
                    if(gapData(i) <= req(j))
                        i_req = i;
                        break;
                    end
                end
                cost_req = data(i_req,((7-1)*5)+2,nen,strategy,crosslinks)'/conversions(7);
                
                costPrint((2*(nen-1))+crosslinks,j) = cost_req;
                gsPrint((2*(nen-1))+crosslinks,j) = i_req;
                
                fprintf("\t%d GS at $%.2fK/day",i_req, cost_req);
            end
            fprintf("\n")
        end
    end
end

     
%% Plots
labels = cell(n_metrics+1,1);
labels{1} = ["Measurement Latency [mins]", "lat"];
labels{2} = ["Gap Time [mins]", "gap"];
labels{3} = ["Access Time [mins]", "access"];
labels{4} = ["Dailly Access [%day]", "dur"];
labels{5} = ["Daily Number of Accesses [-]", "passes"];
labels{6} = ["Data Captured [Mbits]", "data"];
labels{7} = ["Daily Cost [USD$k]", "opCost"];
labels{8} = ["Sat Cost [USD$k]", "satCost"];

limsX = [1 22; 1 14];
limsY = [-1 1;
         -25 750;
         0 10;
         -0.5 20
         -25 250;
         -1 1;
         -0.5 35];
% % Cross-Link Plots
% figs = [];
% for i = 1:n_metrics    
%     for nen = 1:2
%         if nen == 1
%             nen_label = "AWS";
%         else
%             nen_label = "NEN";
%         end
%                 
%         for strategy = 1:2
%             if strategy == 1
%                 strategy_label = "Conservative";
%             else
%                 strategy_label = "Optimistic";
%             end
%                 
%             fig_strat = figure;
%             for crosslinks = 1:2
%                 if crosslinks == 1
%                     crosslinks_label = "";
%                 else
%                     crosslinks_label = " + CL";
%                 end
%                 subplot(1,2,crosslinks)
%                 boxplot(data(:,((i-1)*5)+1:((i-1)*5)+5,nen,strategy,crosslinks)'/conversions(i))
%                 grid on
%                 title([labels{i}(1);
%                         "vs Number of Ground Stations";
%                         "("+nen_label+crosslinks_label+" w/"+strategy_label+" DL Strategy)"]);
%                 xlabel("Number of Ground Stations");
%                 ylabel(labels{i}(1));
%                 xlim([limsX(nen,1) limsX(nen,2)]);
%                 ylim([limsY(i,1) limsY(i,2)]);
%             end  
%             figs = [figs, fig_strat];
%         end 
%     end
% end
% 
% if(save)
%     [~, n_figs] = size(figs);
%     for j = 1:n_metrics
%         if(j == 1 || j == 6) 
%             continue;
%         end
%         saveas(figs((j-1)*4+1),  path+"\cl_"+labels{j}(2)+"_AWS_cons"+".png",'png'); 
%         saveas(figs((j-1)*4+2),  path+"\cl_"+labels{j}(2)+"_AWS_opt"+".png",'png'); 
%         saveas(figs((j-1)*4+3),  path+"\cl_"+labels{j}(2)+"_NEN_cons"+".png",'png'); 
%         saveas(figs((j-1)*4+4),  path+"\cl_"+labels{j}(2)+"_NEN_opt"+".png",'png'); 
%     end
% end
% close all;
% 
% % Strategy Plots
% figs = [];
% for i = 1:n_metrics
%     for nen = 1:2
%         if nen == 1
%             nen_label = "AWS";
%         else
%             nen_label = "NEN";
%         end
%         
%         for crosslinks = 1:2
%             if crosslinks == 1
%                 crosslinks_label = "";
%             else
%                 crosslinks_label = " + CL";
%             end
%         
% 
%             fig_strat = figure;    
%             for strategy = 1:2
%                 if strategy == 1
%                     strategy_label = "Conservative";
%                 else
%                     strategy_label = "Optimistic";
%                 end
%                 subplot(1,2,strategy)
%                 boxplot(data(:,((i-1)*5)+1:((i-1)*5)+5,nen,strategy,crosslinks)'/conversions(i))
%                 grid on
%                 title([labels{i}(1);
%                         "vs Number of Ground Stations";
%                         "("+nen_label+crosslinks_label+" w/"+strategy_label+" DL Strategy)"]);
%                 xlabel("Number of Ground Stations");
%                 ylabel(labels{i}(1));
%                 xlim([limsX(nen,1) limsX(nen,2)]);
%                 ylim([limsY(i,1) limsY(i,2)]);
%             end    
%             figs = [figs, fig_strat];
%         end 
%     end
% end
% 
% if(save)
%     [~, n_figs] = size(figs);
%     for j = 1:n_metrics
%         if(j == 1 || j == 6) 
%             continue;
%         end
%         saveas(figs((j-1)*4+1),  path+"\strat_"+labels{j}(2)+"_AWS"+".png",'png'); 
%         saveas(figs((j-1)*4+2),  path+"\strat_"+labels{j}(2)+"_AWS_cl"+".png",'png'); 
%         saveas(figs((j-1)*4+3),  path+"\strat_"+labels{j}(2)+"_NEN"+".png",'png'); 
%         saveas(figs((j-1)*4+4),  path+"\strat_"+labels{j}(2)+"_NEN_cl"+".png",'png'); 
%     end
% end
% close all;
    

% Pareto Front
figs = [];

i_metrics = [2, 7];
limsY = [0 525;
         0 35;
         0 100];
for strategy = 1:2
    if strategy == 1
        strategy_label = "Conservative";
    else
        strategy_label = "Optimistic";
    end
    fig3 = figure('position', [1000, 1000, 700, 850]);
   
    for j = 1:2
        subplot(3,1,j)
        i = i_metrics(j);
        for nen = 1:2
            for crosslinks = 1:2
                if(nen == 1)
                    scatter(1:1:22, data(:,((i-1)*5)+2,nen,strategy,crosslinks)'/conversions(i),'*');
                else
                    scatter(1:1:22, data(:,((i-1)*5)+2,nen,strategy,crosslinks)'/conversions(i),'filled');
                end
            
                hold on;
                grid on;
                xlabel("Number of Ground Stations");
                ylabel(labels{i}(1));
                xlim([limsX(1,1) limsX(1,2)]);
                ylim(limsY(j,:));
                xticks([1:1:22])
            end
        end
        if j == 1
            plot(1:1:22, 1*60*ones(1,22),'--','Color',[17 17 17]/255)
            legend("AWS","AWS + CL","NEN","NEN + CL","1 hr",'Location','Best')
        end
        title(labels{i}(1)+" vs Number of Ground Stations");
    end
    
    subplot(3,1,3)
    for nen = 1:2
        for crosslinks = 1:2
            if(nen == 1)
                scatter3(data(:,((7-1)*5)+2,nen,strategy,crosslinks)'/conversions(7), ...
                        data(:,((2-1)*5)+2,nen,strategy,crosslinks)'/conversions(2),...
                        1:1:22,'*');
            else
                scatter3(data(1:14,((7-1)*5)+2,nen,strategy,crosslinks)'/conversions(7),...
                    data(1:14,((2-1)*5)+2,nen,strategy,crosslinks)'/conversions(2),...
                    1:1:14,'filled');
            end
            hold on;
            grid on;
            xlabel(labels{7}(1));
            ylabel(labels{2}(1));
            zlabel("Number of Ground Stations");
            xlim(limsY(2,:));
            ylim(limsY(1,:));
            xticks(0:2.5:35)
            yticks(0:50:550);
        end
    end
    view(0,90)
    sgtitle(strategy_label+" Down-Link Strategy")
    figs = [figs, fig3];
end 

if(save)
    saveas(figs(1),  path+"\paret_"+labels{j}(2)+"_cons"+".png",'png'); 
    saveas(figs(2),  path+"\paret_"+labels{j}(2)+"_opt"+".png",'png');
end

fprintf("DONE\n")