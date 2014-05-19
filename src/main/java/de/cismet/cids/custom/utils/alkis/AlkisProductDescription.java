/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils.alkis;

/**
 * DOCUMENT ME!
 *
 * @author   stefan
 * @version  $Revision$, $Date$
 */
public class AlkisProductDescription {

    //~ Instance fields --------------------------------------------------------

    int width;
    int height;
    private final String clazz;
    private final String type;
    private final String code;
    private final String dinFormat;
    private final String fileFormat;
    private final String massstab;
    private final String massstabMin;
    private final String massstabMax;
    private boolean defaultProduct = false;
    private StempelfeldInfo stempelfeldInfo = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisProduct object.
     *
     * @param  clazz            DOCUMENT ME!
     * @param  type             DOCUMENT ME!
     * @param  code             DOCUMENT ME!
     * @param  dinFormat        DOCUMENT ME!
     * @param  massstab         DOCUMENT ME!
     * @param  massstabMin      DOCUMENT ME!
     * @param  massstabMax      DOCUMENT ME!
     * @param  fileFormat       DOCUMENT ME!
     * @param  width            DOCUMENT ME!
     * @param  height           DOCUMENT ME!
     * @param  defaultProduct   DOCUMENT ME!
     * @param  stempelfeldInfo  DOCUMENT ME!
     */
    public AlkisProductDescription(final String clazz,
            final String type,
            final String code,
            final String dinFormat,
            final String massstab,
            final String massstabMin,
            final String massstabMax,
            final String fileFormat,
            final int width,
            final int height,
            final boolean defaultProduct,
            final StempelfeldInfo stempelfeldInfo) {
        this.clazz = clazz;
        this.type = type;
        this.code = code;
        this.dinFormat = dinFormat;
        this.massstab = massstab;
        this.massstabMin = massstabMin;
        this.massstabMax = massstabMax;
        this.fileFormat = fileFormat;
        this.width = width;
        this.height = height;
        this.defaultProduct = defaultProduct;
        this.stempelfeldInfo = stempelfeldInfo;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  the clazz
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the type
     */
    public String getType() {
        return type;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the code
     */
    public String getCode() {
        return code;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the format
     */
    public String getDinFormat() {
        return dinFormat;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getWidth() {
        return width;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getHeight() {
        return height;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the massstab
     */
    public String getMassstab() {
        return massstab;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getFileFormat() {
        return fileFormat;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getMassstabMin() {
        return massstabMin;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getMassstabMax() {
        return massstabMax;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isDefaultProduct() {
        return defaultProduct;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public StempelfeldInfo getStempelfeldInfo() {
        return stempelfeldInfo;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = (11 * hash) + this.width;
        hash = (11 * hash) + this.height;
        hash = (11 * hash) + ((this.clazz != null) ? this.clazz.hashCode() : 0);
        hash = (11 * hash) + ((this.type != null) ? this.type.hashCode() : 0);
        hash = (11 * hash) + ((this.code != null) ? this.code.hashCode() : 0);
        hash = (11 * hash) + ((this.dinFormat != null) ? this.dinFormat.hashCode() : 0);
        hash = (11 * hash) + ((this.fileFormat != null) ? this.fileFormat.hashCode() : 0);
        hash = (11 * hash) + ((this.massstab != null) ? this.massstab.hashCode() : 0);
        hash = (11 * hash) + ((this.massstabMin != null) ? this.massstabMin.hashCode() : 0);
        hash = (11 * hash) + ((this.massstabMax != null) ? this.massstabMax.hashCode() : 0);
        hash = (11 * hash) + (this.defaultProduct ? 1 : 0);
        hash = (11 * hash) + ((this.stempelfeldInfo != null) ? this.stempelfeldInfo.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AlkisProductDescription other = (AlkisProductDescription)obj;
        if (this.width != other.width) {
            return false;
        }
        if (this.height != other.height) {
            return false;
        }
        if ((this.clazz == null) ? (other.clazz != null) : (!this.clazz.equals(other.clazz))) {
            return false;
        }
        if ((this.type == null) ? (other.type != null) : (!this.type.equals(other.type))) {
            return false;
        }
        if ((this.code == null) ? (other.code != null) : (!this.code.equals(other.code))) {
            return false;
        }
        if ((this.dinFormat == null) ? (other.dinFormat != null) : (!this.dinFormat.equals(other.dinFormat))) {
            return false;
        }
        if ((this.fileFormat == null) ? (other.fileFormat != null) : (!this.fileFormat.equals(other.fileFormat))) {
            return false;
        }
        if ((this.massstab == null) ? (other.massstab != null) : (!this.massstab.equals(other.massstab))) {
            return false;
        }
        if ((this.massstabMin == null) ? (other.massstabMin != null) : (!this.massstabMin.equals(other.massstabMin))) {
            return false;
        }
        if ((this.massstabMax == null) ? (other.massstabMax != null) : (!this.massstabMax.equals(other.massstabMax))) {
            return false;
        }
        if (this.defaultProduct != other.defaultProduct) {
            return false;
        }
        if ((this.stempelfeldInfo != other.stempelfeldInfo)
                    && ((this.stempelfeldInfo == null) || !this.stempelfeldInfo.equals(other.stempelfeldInfo))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AlkisProductDescription{" + "width=" + width + ", height=" + height + ", clazz=" + clazz + ", type="
                    + type + ", code=" + code + ", dinFormat=" + dinFormat + ", fileFormat=" + fileFormat
                    + ", massstab=" + massstab + ", massstabMin=" + massstabMin + ", massstabMax=" + massstabMax
                    + ", defaultProduct=" + defaultProduct + ", stempelfeldInfo=" + stempelfeldInfo + '}';
    }
}
