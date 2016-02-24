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
package de.cismet.cids.custom.utils.motd;

import java.util.EventObject;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class MotdRetrieverListenerEvent extends EventObject {

    //~ Static fields/initializers ---------------------------------------------

    public static final int TYPE_MOTD_CHANGED = 1;
    public static final int TYPE_TOTD_CHANGED = 2;

    //~ Instance fields --------------------------------------------------------

    private final int type;
    private final String content;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MotdRetrieverListenerEvent object.
     *
     * @param  type     DOCUMENT ME!
     * @param  content  DOCUMENT ME!
     * @param  source   DOCUMENT ME!
     */
    public MotdRetrieverListenerEvent(final int type, final String content, final MotdRetriever source) {
        super(source);

        this.type = type;
        this.content = content;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public MotdRetriever getSource() {
        return (MotdRetriever)super.getSource();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getContent() {
        return content;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getType() {
        return type;
    }
}
