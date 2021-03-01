/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import seakers.orekit.util.Orbits;
import seakers.orekit.util.OrekitConfig;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;

/**
 *
 * @author paugarciabuzzi
 */
public class test_LTAN2RAAN {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));
        OrekitConfig.init();
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);
        long start2 = System.nanoTime();
        double raan=Orbits.LTAN2RAAN(600000,23.55,25,10,2018);
        long end2 = System.nanoTime();
        Logger.getGlobal().finest(String.format("Took %.4f sec", (end2 - start2) / Math.pow(10, 9)));
        Logger.getGlobal().finest(String.format("ANGLE=%.4f", FastMath.toDegrees(raan)));
        
    }
    
}
