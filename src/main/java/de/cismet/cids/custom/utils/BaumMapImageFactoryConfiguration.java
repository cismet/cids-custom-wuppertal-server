/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils;

import Sirius.util.MapImageFactoryConfiguration;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author sandra
 */
public class BaumMapImageFactoryConfiguration extends MapImageFactoryConfiguration 
    implements Serializable{
    @Getter @Setter private Collection<ObjectIdentifier> mons = null;
    @Getter @Setter private Double buffer = 50.0;
    @Getter @Setter private Map<String, String> colorMap = new HashMap<>();
    @NoArgsConstructor @AllArgsConstructor public static class ObjectIdentifier implements Serializable{
        @Getter @Setter private Integer objectId;
        @Getter @Setter private Integer classId;
    }
}
