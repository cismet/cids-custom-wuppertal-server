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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Collection;
import java.util.GregorianCalendar;

import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservation;
import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservationRequest;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenAnfrageBean;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTask;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenException;
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
            final File src = new File(VermessungsunterlagenHelper.getInstance().getProperties()
                            .getAbsPathPdfPnrVermstelle());
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
                                final String protokoll = getProtokoll(result);

                                final String filename = getPath() + "/" + auftragsnummer + "_"
                                            + bean.getUtmKilometerQuadrat() + ".txt";
                                FileUtils.writeStringToFile(new File(filename), protokoll, "ISO-8859-1");
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
     *
     * @throws  VermessungsunterlagenException      DOCUMENT ME!
     * @throws  VermessungsunterlagenTaskException  DOCUMENT ME!
     */
    private String getProtokoll(final PointNumberReservationRequest content) throws VermessungsunterlagenException {
        boolean isFreigabeMode = false;
        if ((content != null) && content.isSuccessfull() && (content.getPointNumbers() != null)) {
            for (final PointNumberReservation pnr : content.getPointNumbers()) {
                if ((pnr.getAblaufDatum() == null) || pnr.getAblaufDatum().isEmpty()) {
                    isFreigabeMode = true;
                    break;
                }
            }
        }

        final StringBuffer contentBuilder = new StringBuffer();
        if ((content == null) || content.isSuccessfull()) {
            if (!isPointNumberBeanValid(content)) {
                throw new VermessungsunterlagenTaskException(
                    getType(),
                    "Ungültige Antwort des Punktnummernreservierungsdienstes.");
            }
            String header = "Antragsnummer: " + content.getAntragsnummer() + " erstellt am: ";
            final GregorianCalendar cal = new GregorianCalendar();
            header += new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            header += " Anzahl ";
            if (isFreigabeMode) {
                header += "freigegebener";
            } else {
                header += "reservierter";
            }

            header += " Punktnummern: " + content.getPointNumbers().size();
            contentBuilder.append(header).append("\r\n");
            if (isFreigabeMode) {
                contentBuilder.append("freigegebene Punktnummern").append("\r\n");
            } else {
                contentBuilder.append("reservierte Punktnummern (gültig bis)").append("\r\n");
            }
            contentBuilder.append("\r\n");

            for (final PointNumberReservation pnr : content.getPointNumbers()) {
                contentBuilder.append(pnr.getPunktnummer());
                if (!isFreigabeMode) {
                    contentBuilder.append(" (");
                    try {
                        contentBuilder.append(
                            new SimpleDateFormat("dd-MM-yyyy").format(
                                new SimpleDateFormat("yyyy-MM-dd").parse(pnr.getAblaufDatum())));
                    } catch (final ParseException ex) {
                        LOG.info(
                            "Could not parse the expiration date of a reservation. Using the string representation return by server");
                        contentBuilder.append(pnr.getAblaufDatum());
                    }
                    contentBuilder.append(")");
                }
                contentBuilder.append("\r\n");
            }

            return contentBuilder.toString();
        } else {
            return content.getProtokoll();
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
        action.setUser(VermessungsunterlagenHelper.getInstance().getUser());
        action.setMetaService(VermessungsunterlagenHelper.getInstance().getMetaService());
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
        action.setUser(VermessungsunterlagenHelper.getInstance().getUser());
        action.setMetaService(VermessungsunterlagenHelper.getInstance().getMetaService());
        final PointNumberReservationRequest request = (PointNumberReservationRequest)action.execute(
                null,
                sapAction,
                sapPrefix,
                sapAuftragsnummer,
                sapNummerierungsbezirk,
                sapAnzahl,
                sapStartwert);
        return request;
    }
}
