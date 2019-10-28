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
package de.cismet.cids.custom.utils.berechtigungspruefung;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungBaulastInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungFlurstueckInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungGruppeInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.katasterauszug.BerechtigungspruefungAlkisDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.katasterauszug.BerechtigungspruefungAlkisEinzelnachweisDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.katasterauszug.BerechtigungspruefungAlkisKarteDownloadInfo;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class DownloadInfoFactory {

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   product             DOCUMENT ME!
     * @param   stichtag            DOCUMENT ME!
     * @param   buchungsblattCodes  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungAlkisEinzelnachweisDownloadInfo createAlkisBuchungsblattnachweisDownloadInfo(
            final String product,
            final Date stichtag,
            final List<String> buchungsblattCodes) {
        final BerechtigungspruefungAlkisEinzelnachweisDownloadInfo downloadInfo =
            new BerechtigungspruefungAlkisEinzelnachweisDownloadInfo(
                BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp.BUCHUNGSBLAETTER,
                product,
                stichtag,
                buchungsblattCodes);
        return downloadInfo;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product             DOCUMENT ME!
     * @param   buchungsblattCodes  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungAlkisEinzelnachweisDownloadInfo createAlkisBuchungsblattachweisDownloadInfo(
            final String product,
            final List<String> buchungsblattCodes) {
        final BerechtigungspruefungAlkisEinzelnachweisDownloadInfo downloadInfo =
            new BerechtigungspruefungAlkisEinzelnachweisDownloadInfo(
                BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp.BUCHUNGSBLAETTER,
                product,
                buchungsblattCodes);
        return downloadInfo;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product     DOCUMENT ME!
     * @param   alkisCodes  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungAlkisKarteDownloadInfo createBerechtigungspruefungAlkisKarteDownloadInfo(
            final String product,
            final List<String> alkisCodes) {
        final BerechtigungspruefungAlkisKarteDownloadInfo downloadInfo =
            new BerechtigungspruefungAlkisKarteDownloadInfo(
                BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp.FLURSTUECKE,
                alkisCodes);
        return downloadInfo;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product     DOCUMENT ME!
     * @param   alkisCodes  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungAlkisEinzelnachweisDownloadInfo
    createBerechtigungspruefungAlkisEinzelnachweisDownloadInfo(final String product, final List<String> alkisCodes) {
        final BerechtigungspruefungAlkisEinzelnachweisDownloadInfo downloadInfo =
            new BerechtigungspruefungAlkisEinzelnachweisDownloadInfo(
                BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp.FLURSTUECKE,
                product,
                alkisCodes);
        return downloadInfo;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   alkisCodes  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungAlkisKarteDownloadInfo createBerechtigungspruefungAlkisKarteDownloadInfo(
            final List<String> alkisCodes) {
        final BerechtigungspruefungAlkisKarteDownloadInfo downloadInfo =
            new BerechtigungspruefungAlkisKarteDownloadInfo(
                BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp.FLURSTUECKE,
                alkisCodes);
        return downloadInfo;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   name                          DOCUMENT ME!
     * @param   flurstuecketoGrundstueckeMap  DOCUMENT ME!
     * @param   baulastenBeguenstigtBeans     DOCUMENT ME!
     * @param   baulastenBelastetBeans        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungBescheinigungGruppeInfo createBerechtigungspruefungBescheinigungGruppeInfo(
            final String name,
            final Map<CidsBean, Collection<String>> flurstuecketoGrundstueckeMap,
            final Collection<CidsBean> baulastenBeguenstigtBeans,
            final Collection<CidsBean> baulastenBelastetBeans) {
        final List<BerechtigungspruefungBescheinigungFlurstueckInfo> flurstueckeInfo = new ArrayList<>();
        for (final CidsBean flurstueck : flurstuecketoGrundstueckeMap.keySet()) {
            flurstueckeInfo.add(createBerechtigungspruefungBescheinigungFlurstueckInfo(
                    flurstueck,
                    flurstuecketoGrundstueckeMap.get(flurstueck)));
        }

        final List<BerechtigungspruefungBescheinigungBaulastInfo> baulastBeguenstigtInfos = new ArrayList<>();
        for (final CidsBean baulastBeguenstigt : baulastenBeguenstigtBeans) {
            final BerechtigungspruefungBescheinigungBaulastInfo baulastBeguenstigtInfo =
                createBerechtigungspruefungBescheinigungBaulastInfo(
                    baulastBeguenstigt);
            baulastBeguenstigtInfos.add(baulastBeguenstigtInfo);
        }

        final List<BerechtigungspruefungBescheinigungBaulastInfo> baulastBelastetInfos = new ArrayList<>();
        for (final CidsBean baulastBelastet : baulastenBelastetBeans) {
            final BerechtigungspruefungBescheinigungBaulastInfo baulastBelastetInfo =
                createBerechtigungspruefungBescheinigungBaulastInfo(
                    baulastBelastet);
            baulastBelastetInfos.add(baulastBelastetInfo);
        }

        Collections.sort(flurstueckeInfo, new Comparator<BerechtigungspruefungBescheinigungFlurstueckInfo>() {

                @Override
                public int compare(final BerechtigungspruefungBescheinigungFlurstueckInfo o1,
                        final BerechtigungspruefungBescheinigungFlurstueckInfo o2) {
                    final int compareGemarkung = compareString(o1.getGemarkung(), o2.getGemarkung());
                    if (compareGemarkung != 0) {
                        return compareGemarkung;
                    } else {
                        final int compareFlur = compareString(o1.getFlur(), o2.getFlur());
                        if (compareFlur != 0) {
                            return compareFlur;
                        } else {
                            final int compareNummer = compareString(o1.getNummer(), o2.getNummer());
                            if (compareNummer != 0) {
                                return compareNummer;
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            });

        final Comparator<BerechtigungspruefungBescheinigungBaulastInfo> baulastBeanComparator =
            new Comparator<BerechtigungspruefungBescheinigungBaulastInfo>() {

                @Override
                public int compare(final BerechtigungspruefungBescheinigungBaulastInfo o1,
                        final BerechtigungspruefungBescheinigungBaulastInfo o2) {
                    final int compareBlattnummer = compareString(o1.getBlattnummer(), o2.getBlattnummer());
                    if (compareBlattnummer != 0) {
                        return compareBlattnummer;
                    } else {
                        final Integer lfdN1 = Integer.parseInt((String)o1.getLaufende_nummer());
                        final int lfdN2 = Integer.parseInt((String)o2.getLaufende_nummer());
                        final int compareLaufendenummer = lfdN1.compareTo(lfdN2);

                        if (compareLaufendenummer != 0) {
                            return compareLaufendenummer;
                        } else {
                            return 0;
                        }
                    }
                }
            };

        Collections.sort(baulastBeguenstigtInfos, baulastBeanComparator);
        Collections.sort(baulastBelastetInfos, baulastBeanComparator);

        return new BerechtigungspruefungBescheinigungGruppeInfo(
                name,
                flurstueckeInfo,
                baulastBeguenstigtInfos,
                baulastBelastetInfos);
    }

    /**
     * Creates a new FlurstueckBean object.
     *
     * @param   flurstueck    DOCUMENT ME!
     * @param   grundstuecke  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungBescheinigungFlurstueckInfo
    createBerechtigungspruefungBescheinigungFlurstueckInfo(final CidsBean flurstueck,
            final Collection<String> grundstuecke) {
        final String alkisId = (String)flurstueck.getProperty("alkis_id");
        final String gemarkung = (String)flurstueck.getProperty("gemarkung");
        final String flur = (String)flurstueck.getProperty("flur");
        final String nenner = (String)flurstueck.getProperty("fstck_nenner");
        final String zaehler = (String)flurstueck.getProperty("fstck_zaehler");

        final String lage;
        final Collection<CidsBean> adressen = flurstueck.getBeanCollectionProperty("adressen");
        if (adressen.isEmpty()) {
            lage = "";
        } else {
            final Set<String> strassen = new HashSet<String>();
            final Map<String, Collection<String>> hausnummernMap = new HashMap<String, Collection<String>>();
            for (final CidsBean adresse : adressen) {
                final String strasse = (String)adresse.getProperty("strasse");
                final String hausnummer = (String)adresse.getProperty("nummer");
                strassen.add(strasse);
                if (hausnummer != null) {
                    if (!hausnummernMap.containsKey(strasse)) {
                        hausnummernMap.put(strasse, new ArrayList<String>());
                    }
                    final List<String> hausnummern = (List)hausnummernMap.get(strasse);
                    hausnummern.add(hausnummer);
                }
            }
            final String strasse = strassen.iterator().next();
            final StringBuffer sb = new StringBuffer(strasse);
            boolean first = true;
            final List<String> hausnummern = (List)hausnummernMap.get(strasse);
            if (hausnummern != null) {
                Collections.sort(hausnummern);
                sb.append(" ");
                for (final String hausnummer : hausnummern) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(hausnummer);
                    first = false;
                }
            }
            if (strassen.size() > 1) {
                sb.append(" u.a.");
            }
            lage = sb.toString();
        }

        return new BerechtigungspruefungBescheinigungFlurstueckInfo(
                alkisId,
                gemarkung,
                flur,
                zaehler,
                nenner,
                lage,
                grundstuecke);
    }

    /**
     * Creates a new BaulastBean object.
     *
     * @param   baulast  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungBescheinigungBaulastInfo createBerechtigungspruefungBescheinigungBaulastInfo(
            final CidsBean baulast) {
        final String blattnummer = (String)baulast.getProperty("blattnummer");
        final String laufende_nummer = (String)baulast.getProperty("laufende_nummer");

        final StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (final CidsBean art : baulast.getBeanCollectionProperty("art")) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(art.getProperty("baulast_art"));
        }
        final String arten = sb.toString();

        return new BerechtigungspruefungBescheinigungBaulastInfo(blattnummer, laufende_nummer, arten);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   s1  DOCUMENT ME!
     * @param   s2  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static int compareString(final String s1, final String s2) {
        if (s1 == null) {
            if (s2 == null) {
                return 0;
            } else {
                return -1;
            }
        } else if (s1.equals(s2)) {
            return 0;
        } else {
            return s1.compareTo(s2);
        }
    }
}
