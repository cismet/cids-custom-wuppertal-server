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

import Sirius.server.middleware.types.MetaObjectNode;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.ByteArrayOutputStream;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class AbstractPotenzialflaecheReportCreator implements PotenzialflaecheReportCreator {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            PotenzialflaecheReportCreatorImpl.class);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy");

    //~ Instance fields --------------------------------------------------------

    private CidsBean flaecheBean;
    private CidsBean templateBean;
    private ReportConfiguration reportConfiguration;
    private MapConfiguration mapConfiguration;

    //~ Methods ----------------------------------------------------------------

    @Override
    public CidsBean getFlaecheBean() {
        return flaecheBean;
    }

    @Override
    public CidsBean getTemplateBean() {
        return templateBean;
    }

    @Override
    public byte[] createReport(final ReportConfiguration reportConfiguration) throws Exception {
        this.reportConfiguration = reportConfiguration;

        final Integer flaecheId = reportConfiguration.getId();
        final MetaObjectNode flaecheMon = (flaecheId != null)
            ? new MetaObjectNode(
                "WUNDA_BLAU",
                flaecheId,
                CidsBean.getMetaClassFromTableName("WUNDA_BLAU", "pf_potenzialflaeche", getConnectionContext())
                            .getId()) : null;
        flaecheBean = (flaecheMon != null) ? getMetaObject(flaecheMon).getBean() : null;

        final Integer templateId = getReportConfiguration().getTemplateId();
        final MetaObjectNode templateMon = (templateId != null)
            ? new MetaObjectNode(
                "WUNDA_BLAU",
                templateId,
                CidsBean.getMetaClassFromTableName("WUNDA_BLAU", "pf_steckbrieftemplate", getConnectionContext())
                            .getId()) : null;
        templateBean = (templateMon != null) ? getMetaObject(templateMon).getBean() : null;

        final CidsBean kampagne = (CidsBean)flaecheBean.getProperty("kampagne");
        CidsBean selectedTemplateBean = null;
        final CidsBean flaecheBean = getFlaecheBean();
        final CidsBean templateBean = getTemplateBean();
        if (templateBean != null) {
            selectedTemplateBean = templateBean;
        } else {
            if (kampagne != null) {
                final Collection<CidsBean> templateBeans = kampagne.getBeanCollectionProperty(
                        "n_steckbrieftemplates");
                selectedTemplateBean = ((templateBeans != null) && !templateBeans.isEmpty())
                    ? templateBeans.iterator().next() : null;
                final Integer mainSteckbriefId = (Integer)kampagne.getProperty("haupt_steckbrieftemplate_id");
                if (mainSteckbriefId != null) {
                    for (final CidsBean templateSubBean : templateBeans) {
                        if ((templateSubBean != null)
                                    && (mainSteckbriefId == templateSubBean.getMetaObject().getId())) {
                            selectedTemplateBean = templateSubBean;
                            break;
                        }
                    }
                }
            }
        }

        if (selectedTemplateBean == null) {
            throw new Exception("no template found");
        }

        final String confAttr = (String)selectedTemplateBean.getProperty("conf_attr");
        if ((confAttr != null) && !confAttr.trim().isEmpty()
                    && (getConfAttr(confAttr) == null)) {
            throw new Exception("kein Recht an Konfigurationsattribut " + confAttr);
        }

        initMap();

        final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(
                    flaecheBean));
        final Map<String, Object> parameters = generateParams(flaecheBean);
        parameters.put("SUBREPORT_DIR", reportConfiguration.getSubreportDirectory());

        final JasperPrint print = JasperFillManager.fillReport(getJasperReport(selectedTemplateBean),
                parameters,
                dataSource);

        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, os);
            final byte[] bytes = os.toByteArray();
            return bytes;
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flaecheBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public Map generateParams(final CidsBean flaecheBean) throws Exception {
        final ConcurrentHashMap params = new ConcurrentHashMap();
        final ExecutorService taskExecutor = Executors.newFixedThreadPool(4);

        final PotenzialflaecheReportCreator creator = this;

        for (final PotenzialflaecheReportServerAction.Property property
                    : PotenzialflaecheReportServerAction.Property.values()) {
            final String parameterName = property.name();
            final PotenzialflaecheReportServerAction.ReportProperty reportProperty = property.getValue();

            taskExecutor.execute(new SwingWorker() {

                    @Override
                    protected Void doInBackground() throws Exception {
                        Object object = null;
                        if (reportProperty instanceof PotenzialflaecheReportServerAction.VirtualReportProperty) {
                            object = ((PotenzialflaecheReportServerAction.VirtualReportProperty)reportProperty)
                                        .calculateProperty(
                                            creator);
                        } else if (reportProperty
                                    instanceof PotenzialflaecheReportServerAction.MultiKeytableReportProperty) {
                            final PotenzialflaecheReportServerAction.MultiKeytableReportProperty multiFieldReportProperty =
                                (PotenzialflaecheReportServerAction.MultiKeytableReportProperty)reportProperty;
                            final Collection beans = flaecheBean.getBeanCollectionProperty(
                                    multiFieldReportProperty.getPath());
                            if (beans != null) {
                                final Collection<String> strings = new ArrayList<>();
                                for (final Object bean : (Collection)beans) {
                                    if (bean != null) {
                                        strings.add(String.valueOf(bean));
                                    }
                                }
                                object = String.join(", ", strings);
                            }
                        } else if (reportProperty instanceof PotenzialflaecheReportServerAction.PathReportProperty) {
                            final PotenzialflaecheReportServerAction.PathReportProperty fieldReportProperty =
                                (PotenzialflaecheReportServerAction.PathReportProperty)reportProperty;
                            final Object value = flaecheBean.getProperty(fieldReportProperty.getPath());
                            if (value == null) {
                                object = null;
                            } else if (value instanceof Date) {
                                object = SDF.format((Date)value);
                            } else {
                                object = value.toString();
                            }
                        } else if (reportProperty
                                    instanceof PotenzialflaecheReportServerAction.MonSearchReportProperty) {
                            object = "UNBEKANNTE PROPERTY";
                        }
                        params.put(parameterName, object);
                        return null;
                    }
                });
        }

        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            throw new Exception("PARAMS generation took too long", ex);
        }

        return params;
    }

    @Override
    public ReportConfiguration getReportConfiguration() {
        return reportConfiguration;
    }

    @Override
    public MapConfiguration getMapConfiguration(final Type type) {
        return mapConfiguration;
    }
}
