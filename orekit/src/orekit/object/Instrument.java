/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.object;

import java.io.Serializable;
import org.orekit.propagation.events.FieldOfView;

/**
 * The sensor or instrument to install aboard a satellite or at a ground station.
 * The sensor is defined with some field of view.
 * @author nozomihitomi
 */
public class Instrument implements OrekitObject, Serializable{
    private static final long serialVersionUID = -3579827074220214433L;
    
    private FieldOfView fov;
    private String name;

    public Instrument(String name, FieldOfView fov) {
        this.fov = fov;
        this.name = name;
    }

    public FieldOfView getFov() {
        return fov;
    }

    public String getName() {
        return name;
    }
    
    
}
