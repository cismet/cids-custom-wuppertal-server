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
package de.cismet.cids.custom.utils.vermessungsunterlagen.tasks;

import org.apache.commons.io.FileUtils;

import java.io.File;

import java.util.Collection;

import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservation;
import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservationRequest;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenAnfrageBean;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHandler;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTask;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException;
import de.cismet.cids.custom.wunda_blau.search.actions.PointNumberReserverationServerAction;

import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskPNR extends VermessungsunterlagenTask {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "PNR";

    //~ Instance fields --------------------------------------------------------

    private final String auftragsnummer;
    private final String vermessungsstelle;
    private final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean[] punktnummernreservierungBeans;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskRisseBilder object.
     *
     * @param  jobKey                         DOCUMENT ME!
     * @param  vermessungsstelle              DOCUMENT ME!
     * @param  auftragsnummer                 DOCUMENT ME!
     * @param  punktnummernreservierungBeans  DOCUMENT ME!
     */
    public VermUntTaskPNR(final String jobKey,
            final String vermessungsstelle,
            final String auftragsnummer,
            final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean[] punktnummernreservierungBeans) {
        super(
            TYPE,
            jobKey);

        this.auftragsnummer = auftragsnummer;
        this.vermessungsstelle = vermessungsstelle;
        this.punktnummernreservierungBeans = punktnummernreservierungBeans;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    protected void performTask() throws VermessungsunterlagenTaskException {
        if (vermessungsstelle == null) {
            final File src = new File(getProperties().getAbsPathPdfPnrVermstelle());
            final File dst = new File(getPath() + "/" + src.getName());
            if (!dst.exists()) {
                try {
                    FileUtils.copyFile(src, dst);
                } catch (final Exception ex) {
                    final String message =
                        "Beim Kopieren des PNR-Informations-PDFs kam es zu einem unerwarteten Fehler.";
                    throw new VermessungsunterlagenTaskException(getType(), message, ex);
                }
            }
        } else {
            if (punktnummernreservierungBeans != null) {
                final Collection reservations = getReservations();
                boolean first = (reservations == null) || reservations.isEmpty();
                for (final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean bean
                            : punktnummernreservierungBeans) {
                    if (bean.getAnzahlPunktnummern() > 0) {
                        try {
                            final PointNumberReservationRequest result = doReservation(bean, !first);

                            if (result != null) {
                                final String filebasename = getPath() + "/" + auftragsnummer + "_"
                                            + bean.getUtmKilometerQuadrat();

                                if (result.isSuccessfull()) {
                                    if (!isPointNumberBeanValid(result)) {
                                        throw new VermessungsunterlagenTaskException(
                                            getType(),
                                            "Ungültige Antwort des Punktnummernreservierungsdienstes.");
                                    }

                                    FileUtils.writeStringToFile(new File(filebasename + ".xml"),
                                        result.getRawResult(),
                                        "UTF-8");
                                    FileUtils.writeStringToFile(new File(filebasename + ".txt"),
                                        result.createTxtProtokoll(),
                                        "ISO-8859-1");
                                } else {
                                    FileUtils.writeStringToFile(new File(filebasename + ".txt"),
                                        result.getProtokoll(),
                                        "ISO-8859-1");
                                }
                            } else {
                                throw new VermessungsunterlagenTaskException(
                                    getType(),
                                    "Ungültige Antwort des Punktnummernreservierungsdienstes.");
                            }
                            first = false;
                        } catch (final Exception ex) {
                            final String message =
                                "Beim Herunterladen des Punktnummernreservierungsprotokolls kam es zu einem unerwarteten Fehler.";
                            throw new VermessungsunterlagenTaskException(getType(), message, ex);
                        }
                    }
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   content  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isPointNumberBeanValid(final PointNumberReservationRequest content) {
        if (content == null) {
            return false;
        }
        if ((content.getAntragsnummer() == null) || content.getAntragsnummer().isEmpty()) {
            return false;
        }
        if ((content.getPointNumbers() == null) || content.getPointNumbers().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    protected String getSubPath() {
        return "/PNR";
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<PointNumberReservation> getReservations() {
        final ServerActionParameter sapPrefix = new ServerActionParameter(
                PointNumberReserverationServerAction.Parameter.PREFIX.toString(),
                vermessungsstelle.substring(2));
        final ServerActionParameter sapAuftragsnummer = new ServerActionParameter(
                PointNumberReserverationServerAction.Parameter.AUFTRAG_NUMMER.toString(),
                auftragsnummer);
        final ServerActionParameter sapAction = new ServerActionParameter(
                PointNumberReserverationServerAction.Parameter.ACTION.toString(),
                PointNumberReserverationServerAction.Action.GET_POINT_NUMBERS);
        final PointNumberReserverationServerAction action = new PointNumberReserverationServerAction();
        action.setUser(getUser());
        action.setMetaService(getMetaService());
        final Collection<PointNumberReservation> request = (Collection)action.execute(
                null,
                sapAction,
                sapPrefix,
                sapAuftragsnummer);
        return request;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bean       DOCUMENT ME!
     * @param   ergaenzen  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected PointNumberReservationRequest doReservation(
            final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean bean,
            final boolean ergaenzen) {
        final ServerActionParameter sapAction;
        if (ergaenzen) {
            sapAction = new ServerActionParameter(
                    PointNumberReserverationServerAction.Parameter.ACTION.toString(),
                    PointNumberReserverationServerAction.Action.DO_ADDITION);
        } else {
            sapAction = new ServerActionParameter(
                    PointNumberReserverationServerAction.Parameter.ACTION.toString(),
                    PointNumberReserverationServerAction.Action.DO_RESERVATION);
        }
        final ServerActionParameter sapPrefix = new ServerActionParameter(
                PointNumberReserverationServerAction.Parameter.PREFIX.toString(),
                vermessungsstelle.substring(2));
        final ServerActionParameter sapAuftragsnummer = new ServerActionParameter(
                PointNumberReserverationServerAction.Parameter.AUFTRAG_NUMMER.toString(),
                auftragsnummer);
        final ServerActionParameter sapNummerierungsbezirk = new ServerActionParameter(
                PointNumberReserverationServerAction.Parameter.NBZ.toString(),
                bean.getUtmKilometerQuadrat());
        final ServerActionParameter sapAnzahl = new ServerActionParameter(
                PointNumberReserverationServerAction.Parameter.ANZAHL.toString(),
                bean.getAnzahlPunktnummern());
        final ServerActionParameter sapStartwert = new ServerActionParameter(
                PointNumberReserverationServerAction.Parameter.STARTWERT.toString(),
                0);

        final PointNumberReserverationServerAction action = new PointNumberReserverationServerAction();
        action.setUser(getUser());
        action.setMetaService(getMetaService());
        return (PointNumberReservationRequest)action.execute(
                null,
                sapAction,
                sapPrefix,
                sapAuftragsnummer,
                sapNummerierungsbezirk,
                sapAnzahl,
                sapStartwert);
    }
}
