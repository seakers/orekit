/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.orekit.parallel;

import java.util.concurrent.Callable;

/**
 * A subroutine that is executed by ParallelRoutine
 * @author nozomihitomi
 */
public interface SubRoutine extends Callable<SubRoutine>{
    
}