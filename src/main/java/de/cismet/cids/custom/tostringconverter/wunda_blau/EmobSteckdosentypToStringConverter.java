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
public class EmobSteckdosentypToStringConverter extends CustomToStringConverter{
    
    public static final String FIELD__NAME = "name";                             // emob_steckdosentyp   
    public static final String FIELD__ID = "id";                                 // emob_steckdosentyp
    @Override
      public String createString() {
          final String myid = String.valueOf(cidsBean.getProperty(FIELD__ID));
          if ("-1".equals(myid)) {
              return "Neuen Steckdosentyp anlegen";
          } else {
              return String.valueOf(cidsBean.getProperty(FIELD__NAME));
          }
      }  
}
