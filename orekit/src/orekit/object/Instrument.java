/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object;

import java.io.Serializable;
import orekit.object.fieldofview.FieldOfViewDefinition;

/**
 * The sensor or instrument to install aboard a satellite or at a ground
 * station. The sensor is defined with some field of view. There are two
 * constructors for simple conical field of view or a rectangular field of view
 *
 * @author nozomihitomi
 */
public class Instrument implements OrekitObject, Serializable {

    private static final long serialVersionUID = -3579827074220214433L;

    private final String name;
    private final FieldOfViewDefinition fov;

    /**
     * This constructor is for creating a custom shape field of view.
     *
     * @param name
     * @param fov
     */
    public Instrument(String name, FieldOfViewDefinition fov) {
        this.name = name;
        this.fov = fov;
    }

    /**
     * Returns the field of view object
     *
     * @return
     */
    public FieldOfViewDefinition getFOV() {
        return fov;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Instrument{" + "Name=" + name + fov.toString();
    }

}
