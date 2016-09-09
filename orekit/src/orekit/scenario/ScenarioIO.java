/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.scenario;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves and loads scenarios
 * @author nozomihitomi
 */
public class ScenarioIO {
    
    /**
     * Saves the scenario in a desired directory
     * @param path to the directory to save the file
     * @param filename name of the file without the extension
     * @param scenario Scenario object to save
     * @return 
     */
    public static boolean save(Path path, String filename, Scenario scenario){
        File file = new File(path.toFile(), filename + ".ore");
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));) {
            os.writeObject(scenario);
            os.close();
        } catch (IOException ex) {
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        System.out.println("Saved scenario in " + file.toString());
        return true;
    }
    
    /**
     * Loads the Scenario instance saved by using save() from the given filename.
     * @param path to the directory to save the file
     * @param filename the file name (xtension included)
     * @return the Scenario instance saved by using save()
     */
    public static Scenario loadHistory(Path path, String filename) {
        Scenario hist = null;
        File file = new File(path.toFile(), filename); 
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))) {
            hist = (Scenario) is.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Successfully loaded scenario: " + file.toString());
        return hist;
    }
    
    public static boolean saveReadMe(Path path, String filename, Scenario scenario){
        File file = new File(path.toFile(), filename + ".ore");
        
        try (FileWriter fw = new FileWriter(file)) {
            fw.append(scenario.toString()); //write scenario header
            fw.append(scenario.ppDate());
            fw.append(scenario.ppFrame());
            fw.append(scenario.ppPropgator());
            fw.append(scenario.ppConstellations());
            fw.append(scenario.ppCoverageDefinition());
            fw.flush();
        } catch (IOException ex) {
            Logger.getLogger(ScenarioIO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        System.out.println("Saved readme in " + file.toString());
        return true;
    }
}
