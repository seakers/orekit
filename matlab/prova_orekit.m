%imports the jar file so that orekit can be used to propogate orbits and
%compute coverage metrics

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%Add the java class path for the orekit jar file
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
orekit_init2()
%code
%import seak.orekit.*;
%import java.*;
%path=java.nio.file.Paths.get('/Users/paugarciabuzzi/Desktop/orekit/orekit/results','');
path = java.io.File('/Users/paugarciabuzzi/Desktop/orekit/orekit/results');
path2=path.toPath;
filename='tropics_600000_30.0_1_1_0.obj';
ea=seak.orekit.scenario.ScenarioIO.loadGroundEventAnalyzerObject(path2, filename);
latBounds=[Math.toRadians(-30), Math.toRadians(30)];
lonBounds=[-Math.PI, Math.PI];
accessStats = ea.getStatistics(AnalysisMetric.DURATION, true,latBounds,lonBounds);
gapStats = ea.getStatistics(AnalysisMetric.DURATION, false,latBounds,lonBounds);
meanTime = ea.getStatistics(AnalysisMetric.MEAN_TIME_TO_T, false,latBounds,lonBounds);
timeAverage = ea.getStatistics(AnalysisMetric.TIME_AVERAGE, false,latBounds,lonBounds);
percentTime = ea.getStatistics(AnalysisMetric.PERCENT_TIME, true,latBounds,lonBounds);

percentTime.getMean()
meanTime.getMean()
gapStats.getMean()
accessStats.getMean()

%end
%removes the jar file from javaclasspath
orekit_end2()
