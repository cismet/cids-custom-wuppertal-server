/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

import java.util.Map;

import de.cismet.cids.utils.serverresources.DefaultServerResourceJsonHandler;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PotenzialflaechenMapsJson extends DefaultServerResourceJsonHandler implements Serializable {

    //~ Static fields/initializers ---------------------------------------------

    public static final int DEFAULT_MAP_DPI = 300;
    public static final int DEFAULT_BUFFER = 50;
    public static final int DEFAULT_MAP_WIDTH = 300;
    public static final int DEFAULT_MAP_HEIGHT = 200;

    //~ Instance fields --------------------------------------------------------

    private Map<String, PotenzialflaechenMapsJson.MapProperties> mapProperties;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   identifier  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MapProperties getMapProperties(final String identifier) {
        return getMapProperties().get(identifier);
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MapProperties implements Serializable {

        //~ Instance fields ----------------------------------------------------

        private String wmsUrl;
        private int width = DEFAULT_MAP_WIDTH;
        private int height = DEFAULT_MAP_HEIGHT;
        private int dpi = DEFAULT_MAP_DPI;
        private boolean showGeom = true;
        private int buffer = DEFAULT_BUFFER;
    }
}
