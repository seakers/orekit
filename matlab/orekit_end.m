function orekit_end()
%removes the jar file from javaclasspath

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%remove the java class path for the orekit jar file
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
jarFile1 = ['.',filesep,'orekit',filesep,'dist',filesep,'orekit.jar'];
jarFile2 = ['.',filesep,'orekit',filesep,'dist',filesep,'lib',filesep,'orekit-8.0.jar'];
jarFile3 = ['.',filesep,'orekit',filesep,'dist',filesep,'lib',filesep,'hipparchus-geometry-1.0'];
tmp = javaclasspath;
javaclasspathadded1 = false;
javaclasspathadded2 = false;
javaclasspathadded3 = false;

%search through current dynamics paths to see if jar file is in
%dynamic path (could occur if scenario_builder script throws an error
%before the path is removed at the end). Attempt to remove only if it
%already exists in path
for i=1:length(tmp)
    if ~isempty(strfind(tmp{i},jarFile1))
        javaclasspathadded1 = true;
    end
    if ~isempty(strfind(tmp{i},jarFile2))
        javaclasspathadded2 = true;
    end
    if ~isempty(strfind(tmp{i},jarFile3))
        javaclasspathadded2 = true;
    end
end

if javaclasspathadded1
    javaarmpath(['.',filesep,'orekit',filesep,'dist',filesep,'orekit.jar']);
end
if javaclasspathadded2
    javaarmpath(['.',filesep,'orekit',filesep,'dist',filesep,'lib',filesep,'orekit-8.0.jar']);
end
if javaclasspathadded3
    javaarmpath(['.',filesep,'orekit',filesep,'dist',filesep,'lib',filesep,'orekit-8.0.jar']);
end