%Main script that runs all the tropics scenarios

path = '/Users/paugarciabuzzi/Desktop/orekit/orekit/results';
alts=[400,600,800];
incs=[30,51.6,90];
t=[1,2,3,4,6,8,9,12,16];
createWalkerScenariosForTropics(path, alts, incs, t)