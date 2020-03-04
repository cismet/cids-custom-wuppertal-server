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
package de.cismet.cids.custom.tostringconverter.wunda_blau;

import de.cismet.cids.tools.CustomToStringConverter;

/**
 *
 * @author sandra
 */
public class PrbrBuslinieToStringConverter extends CustomToStringConverter{
    
    public static final String FIELD__NAME = "name";                                  
    @Override
      public String createString() {
         
        String myname;
        myname = String.valueOf(cidsBean.getProperty(FIELD__NAME));
        if ("null".equals(myname)) {
            myname = "Neue Buslinie anlegen";
        }

        return myname;
      }  
}
