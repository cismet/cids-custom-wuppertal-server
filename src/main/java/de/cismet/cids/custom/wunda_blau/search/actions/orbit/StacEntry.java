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
package de.cismet.cids.custom.wunda_blau.search.actions.orbit;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * DOCUMENT ME!
 *
 * @author   hell
 * @version  $Revision$, $Date$
 */
public class StacEntry {

    //~ Instance fields --------------------------------------------------------

    String hash;
    long timestamp;
    String base_login_name;
    String stac_options;
    String ipAddress;
    String socketChannelId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new StacEntry object.
     *
     * @param  stac             DOCUMENT ME!
     * @param  base_login_name  DOCUMENT ME!
     * @param  ipAddress        DOCUMENT ME!
     * @param  stacOptions      DOCUMENT ME!
     */
    public StacEntry(final String stac,
            final String base_login_name,
            final String ipAddress,
            final String stacOptions) {
        this.base_login_name = base_login_name;
        this.timestamp = System.currentTimeMillis();
        this.ipAddress = ipAddress;
        this.stac_options = stacOptions;
        this.hash = DigestUtils.md5Hex(stac);
        this.socketChannelId = DigestUtils.md5Hex(hash) + "." + DigestUtils.md5Hex(Long.toString(timestamp));
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getHash() {
        return hash;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getBase_login_name() {
        return base_login_name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getStac_options() {
        return stac_options;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSocketChannelId() {
        return socketChannelId;
    }
}
