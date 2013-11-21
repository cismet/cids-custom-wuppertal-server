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
package de.cismet.cids.custom.utils.pointnumberreservation;

import java.io.Serializable;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class VermessungsStellenSearchResult implements Serializable {

    //~ Instance fields --------------------------------------------------------

    private String name;
    private String zulassungsNummer;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsStellenSearchResult object.
     *
     * @param  name              DOCUMENT ME!
     * @param  zulassungsNummer  DOCUMENT ME!
     */
    public VermessungsStellenSearchResult(final String name, final String zulassungsNummer) {
        this.name = name;
        this.zulassungsNummer = zulassungsNummer;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getName() {
        return name;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  name  DOCUMENT ME!
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getZulassungsNummer() {
        return zulassungsNummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  zulassungsNummer  DOCUMENT ME!
     */
    public void setZulassungsNummer(final String zulassungsNummer) {
        this.zulassungsNummer = zulassungsNummer;
    }
}
