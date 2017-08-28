function orekit_end2()
%removes the jar file from javaclasspath

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%remove the java class path for the orekit jar file
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
directory = cd;
jarFile1 = ['..',filesep,'orekit',filesep,'target',filesep,'orekit-1.0-SNAPSHOT.jar'];
tmp = javaclasspath;
javaclasspathadded1 = false;

%search through current dynamics paths to see if jar file is in
%dynamic path (could occur if scenario_builder script throws an error
%before the path is removed at the end). Attempt to remove only if it
%already exists in path
for i=1:length(tmp)
    if ~isempty(strfind(tmp{i},jarFile1))
        javaclasspathadded1 = true;
    end
end

if javaclasspathadded1
    javarmpath(['..',filesep,'orekit',filesep,'target',filesep,'orekit-1.0-SNAPSHOT.jar']);
end