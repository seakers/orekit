function orekit_init2()
%imports the jar file so that orekit can be used to propogate orbits and
%compute coverage metrics

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%Add the java class path for the orekit jar file
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
jarFile1 = ['..',filesep,'orekit',filesep,'target',filesep,'orekit-1.0-SNAPSHOT.jar'];
tmp = javaclasspath;
javaclasspathadded1 = false;

%search through current dynamics paths to see if jar file is already in
%dynamic path (could occur if scenario_builder script throws an error
%before the path is removed at the end)
for i=1:length(tmp)
    if ~isempty(strfind(tmp{i},jarFile1))
        javaclasspathadded1 = true;
    end
end

if ~javaclasspathadded1
    javaaddpath(['..',filesep,'orekit',filesep,'target',filesep,'orekit-1.0-SNAPSHOT.jar']);
end
