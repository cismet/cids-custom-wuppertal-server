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

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public class StempelfeldInfo {

    //~ Instance fields --------------------------------------------------------

    float fromX;
    float fromY;
    float toX;
    float toY;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new StempelfeldInfo object.
     *
     * @param  fromX  DOCUMENT ME!
     * @param  fromY  DOCUMENT ME!
     * @param  toX    DOCUMENT ME!
     * @param  toY    DOCUMENT ME!
     */
    public StempelfeldInfo(final float fromX, final float fromY, final float toX, final float toY) {
        if (fromX < toX) {
            this.fromX = fromX;
            this.toX = toX;
        } else {
            this.fromX = toX;
            this.toX = fromX;
        }
        if (fromY < toY) {
            this.fromY = fromY;
            this.toY = toY;
        } else {
            this.fromY = toY;
            this.toY = fromY;
        }
        this.fromX = transformToInnerBounds(this.fromX);
        this.fromY = transformToInnerBounds(this.fromY);
        this.toX = transformToInnerBounds(this.toX);
        this.toY = transformToInnerBounds(this.toY);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   f  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private float transformToInnerBounds(final float f) {
        if ((f > 0f) && (f < 1f)) {
            return f;
        } else if (f <= 0f) {
            return 0.0001f;
        } else {
            return 0.9999f;
        }
    }
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public float getFromX() {
        return fromX;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public float getFromY() {
        return fromY;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public float getToX() {
        return toX;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public float getToY() {
        return toY;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = (23 * hash) + Float.floatToIntBits(this.fromX);
        hash = (23 * hash) + Float.floatToIntBits(this.fromY);
        hash = (23 * hash) + Float.floatToIntBits(this.toX);
        hash = (23 * hash) + Float.floatToIntBits(this.toY);
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
        final StempelfeldInfo other = (StempelfeldInfo)obj;
        if (Float.floatToIntBits(this.fromX) != Float.floatToIntBits(other.fromX)) {
            return false;
        }
        if (Float.floatToIntBits(this.fromY) != Float.floatToIntBits(other.fromY)) {
            return false;
        }
        if (Float.floatToIntBits(this.toX) != Float.floatToIntBits(other.toX)) {
            return false;
        }
        if (Float.floatToIntBits(this.toY) != Float.floatToIntBits(other.toY)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "StempelfeldInfo{" + "fromX=" + fromX + ", fromY=" + fromY + ", toX=" + toX + ", toY=" + toY + '}';
    }
}
