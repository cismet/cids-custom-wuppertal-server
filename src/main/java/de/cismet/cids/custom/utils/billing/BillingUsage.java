/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.billing;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */

public class BillingUsage {

    //~ Instance fields --------------------------------------------------------

    private String key;
    private String name;
    private String description;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Usage object.
     */
    public BillingUsage() {
    }

    /**
     * Creates a new Usage object.
     *
     * @param  key          DOCUMENT ME!
     * @param  name         DOCUMENT ME!
     * @param  description  DOCUMENT ME!
     */
    public BillingUsage(final String key, final String name, final String description) {
        this.key = key;
        this.name = name;
        this.description = description;
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
     * @param  description  DOCUMENT ME!
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getKey() {
        return key;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  key  DOCUMENT ME!
     */
    public void setKey(final String key) {
        this.key = key;
    }

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

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BillingUsage other = (BillingUsage)obj;
        if ((this.key == null) ? (other.key != null) : (!this.key.equals(other.key))) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : (!this.name.equals(other.name))) {
            return false;
        }
        if ((this.description == null) ? (other.description != null) : (!this.description.equals(other.description))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = (73 * hash) + ((this.key != null) ? this.key.hashCode() : 0);
        hash = (73 * hash) + ((this.name != null) ? this.name.hashCode() : 0);
        hash = (73 * hash) + ((this.description != null) ? this.description.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return name;
    }
}
