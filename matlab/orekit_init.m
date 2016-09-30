function orekit_init()
%imports the jar file so that orekit can be used to propogate orbits and
%compute coverage metrics

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%Add the java class path for the orekit jar file
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
jarFile1 = ['.',filesep,'orekit',filesep,'dist',filesep,'orekit.jar'];
jarFile2 = ['.',filesep,'orekit',filesep,'dist',filesep,'lib',filesep,'orekit-8.0.jar'];
jarFile3 = ['.',filesep,'orekit',filesep,'dist',filesep,'lib',filesep,'hipparchus-geometry-1.0.jar'];
tmp = javaclasspath;
javaclasspathadded1 = false;
javaclasspathadded2 = false;
javaclasspathadded3 = false;

%search through current dynamics paths to see if jar file is already in
%dynamic path (could occur if scenario_builder script throws an error
%before the path is removed at the end)
for i=1:length(tmp)
    if ~isempty(strfind(tmp{i},jarFile1))
        javaclasspathadded1 = true;
    end
    if ~isempty(strfind(tmp{i},jarFile2))
        javaclasspathadded2 = true;
    end
    if ~isempty(strfind(tmp{i},jarFile3))
        javaclasspathadded3 = true;
    end
end

if ~javaclasspathadded1
    javaaddpath(['.',filesep,'orekit',filesep,'dist',filesep,'orekit.jar']);
end
if ~javaclasspathadded2
    javaaddpath(['.',filesep,'orekit',filesep,'dist',filesep,'lib',filesep,'orekit-8.0.jar']);
end
if ~javaclasspathadded3
    javaaddpath(['.',filesep,'orekit',filesep,'dist',filesep,'lib',filesep,'hipparchus-geometry-1.0.jar']);
end