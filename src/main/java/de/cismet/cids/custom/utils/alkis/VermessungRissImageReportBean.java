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
package de.cismet.cids.custom.utils.alkis;

import de.cismet.commons.utils.MultiPagePictureReader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungRissImageReportBean {

    //~ Instance fields --------------------------------------------------------

    private final String description;
    private final String host;
    private final String schluessel;
    private final Integer gemarkung;
    private final String flur;
    private final String blatt;
    private final Integer page;
    private final MultiPagePictureReader reader;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungRissImageReportBean object.
     *
     * @param  description  DOCUMENT ME!
     * @param  host         DOCUMENT ME!
     * @param  schluessel   DOCUMENT ME!
     * @param  gemarkung    DOCUMENT ME!
     * @param  flur         DOCUMENT ME!
     * @param  blatt        DOCUMENT ME!
     * @param  page         DOCUMENT ME!
     * @param  reader       DOCUMENT ME!
     */
    public VermessungRissImageReportBean(final String description,
            final String host,
            final String schluessel,
            final Integer gemarkung,
            final String flur,
            final String blatt,
            final Integer page,
            final MultiPagePictureReader reader) {
        this.description = description;
        this.host = host;
        this.schluessel = schluessel;
        this.gemarkung = gemarkung;
        this.flur = flur;
        this.blatt = blatt;
        this.page = page;
        this.reader = reader;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getDescription() {
        return description;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getBlatt() {
        return blatt;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getFlur() {
        return flur;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Integer getGemarkung() {
        return gemarkung;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getHost() {
        return host;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Integer getPage() {
        return page;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSchluessel() {
        return schluessel;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MultiPagePictureReader getReader() {
        return reader;
    }
}
