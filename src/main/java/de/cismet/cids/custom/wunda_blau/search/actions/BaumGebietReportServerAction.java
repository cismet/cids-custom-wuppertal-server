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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.types.MetaObjectNode;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.ByteArrayInputStream;

import java.sql.Date;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import de.cismet.cids.custom.utils.BaumMeldungReportScriptlet;
import de.cismet.cids.custom.utils.StampedJasperReportServerAction;
import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.server.BaumChildLightweightSearch;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class BaumGebietReportServerAction extends StampedJasperReportServerAction implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final String CHILD_TOSTRING_TEMPLATE = "%s";
    private static final String TABLE_MELDUNG = "baum_meldung";
    private static final String[] CHILD_TOSTRING_FIELDS = { "id" };
    private static final String FK_GEBIET = "fk_gebiet";
    public static final String TASK_NAME = "baumGebietReport";
    public static final String FIELD__BEZEICHNUNG = "name";              // baum_gebiet
    public static final String FIELD__AZ = "aktenzeichen";               // baum_gebiet
    public static final String FIELD__STRASSE_NAME = "fk_strasse.name";  // strasse
    public static final String FIELD__WV = "erneut";                     // baum_gebiet
    public static final String FIELD__ADR_HNR = "fk_adresse.hausnummer"; // adresse
    public static final String FIELD__BEMERKUNG = "bemerkung";           // baum_gebiet
    public static final String FIELD__ID = "id";                         // baum_gebiet
    public static final String FIELD__GEOREFERENZ = "fk_geom";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        MAP_IMAGE_BYTES
    }

    //~ Instance fields --------------------------------------------------------

    private final BaumChildLightweightSearch searchChild = new BaumChildLightweightSearch(
            CHILD_TOSTRING_TEMPLATE,
            CHILD_TOSTRING_FIELDS,
            TABLE_MELDUNG,
            FK_GEBIET);

    //~ Methods ----------------------------------------------------------------

    @Override
    protected JasperReport getJasperReport() throws Exception {
        return ServerResourcesLoader.getInstance()
                    .loadJasperReport(WundaBlauServerResources.BAUMGEBIET_JASPER.getValue());
    }

    @Override
    public Object execute(final Object o, final ServerActionParameter... params) {
        try {
            byte[] imageBytes = null;
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(Parameter.MAP_IMAGE_BYTES.toString())) {
                        imageBytes = (byte[])sap.getValue();
                    }
                }
            }

            final MetaObjectNode gebietMon = (MetaObjectNode)o;

            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("SUBREPORT_DIR", DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "");
            parameters.put(FIELD__AZ, gebietMon.getObject().getBean().getProperty(FIELD__AZ).toString());
            parameters.put(FIELD__ID, gebietMon.getObject().getBean().getProperty(FIELD__ID).toString());
            parameters.put("bezeichnung", gebietMon.getObject().getBean().getProperty(FIELD__BEZEICHNUNG).toString());
            parameters.put("strasse", gebietMon.getObject().getBean().getProperty(FIELD__STRASSE_NAME).toString());
            parameters.put("hnr", getAttribute(gebietMon, FIELD__ADR_HNR));
            parameters.put(FIELD__BEMERKUNG, getAttribute(gebietMon, FIELD__BEMERKUNG));
            parameters.put("wiedervorlage", getDateAttribute(gebietMon, FIELD__WV));

            parameters.put("mon", gebietMon);

            if (imageBytes != null) {
                try(final ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                    parameters.put("MAP_IMAGE", ImageIO.read(bis));
                }
            }

            final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(
                        getMetaService().getMetaObject(
                            getUser(),
                            gebietMon.getObjectId(),
                            gebietMon.getClassId(),
                            getConnectionContext()).getBean()));

            searchChild.setParentId(gebietMon.getObject().getBean().getPrimaryKeyValue());
            searchChild.setFkField(FK_GEBIET);
            searchChild.setTable(TABLE_MELDUNG);
            searchChild.setRepresentationFields(CHILD_TOSTRING_FIELDS);
            final Collection<MetaObjectNode> mons;

            final BaumMeldungReportScriptlet scriptlet = new BaumMeldungReportScriptlet(
                    getMetaService(),
                    getUser(),
                    getConnectionContext());
            parameters.put("REPORT_SCRIPTLET", scriptlet);
            return generateReport(parameters, dataSource);
        } catch (final Exception ex) {
            LOG.error( "Parameter für Gebiet-Report nicht erzeugt.", ex);
            return ex;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mon        DOCUMENT ME!
     * @param   attribute  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getAttribute(final MetaObjectNode mon, final String attribute) {
        if (mon.getObject().getBean().getProperty(attribute) == null) {
            return "";
        } else {
            final String result;
            result = (String)mon.getObject().getBean().getProperty(attribute);
            return result;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mon        DOCUMENT ME!
     * @param   attribute  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getDateAttribute(final MetaObjectNode mon, final String attribute) {
        if (mon.getObject().getBean().getProperty(attribute) == null) {
            return "";
        } else {
            final String result;
            final Date datum;
            final SimpleDateFormat formatTag = new SimpleDateFormat("dd.MM.yy");
            datum = (Date)mon.getObject().getBean().getProperty(attribute);
            result = formatTag.format(datum);
            return result;
        }
    }
}
