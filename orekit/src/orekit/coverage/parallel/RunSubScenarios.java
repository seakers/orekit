/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.coverage.parallel;

import java.io.File;
import orekit.util.OrekitConfig;

/**
 * This class will run all the subscenarios in the specified path. The main
 * method takes in two arguments: 
 * 1) the path to the directory where the unrun
 * scenarios reside. 
 * 2) the number of threads to use to run all scenarios
 *
 * @author nozomihitomi
 */
public class RunSubScenarios {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
         args = new String[]{"/Users/nozomihitomi/Dropbox/OREKIT/testing","1"};
        
        OrekitConfig.init();
        
        ParallelCoverage parCov = new ParallelCoverage();
        File path = new File(args[0]);
        parCov.loadRunAndSave(path.toPath(), Integer.parseInt(args[1]));
    }

}
