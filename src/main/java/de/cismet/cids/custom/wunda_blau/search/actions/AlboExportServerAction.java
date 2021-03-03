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

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AlboExportServerAction implements ConnectionContextStore, UserAwareServerAction, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final Logger LOG = Logger.getLogger(AlboExportServerAction.class);
    public static final String TASK_NAME = "alboExport";
    private static final String QUERY = "SELECT "
                + "  fisalbo_nr, "
                + "  artflaeche, "
                + "  unterart, "
                + "  verw_kennz, "
                + "  statusfl, "
                + "  ortsbez, "
                + "  bergaufs, "
                + "  teilflae, "
                + "  schwerp_e, "
                + "  schwerp_n, "
                + "  flaeche_gr, "
                + "  gemkennz, "
                + "  strasse, "
                + "  anmerkung, "
                + "  bearbstand, "
                + "  bbst_ga_tf, "
                + "  bearbdate, "
                + "  sub.branche_[ 1] AS branche_01, "
                + "  sub.branche_[ 2] AS branche_02, "
                + "  sub.branche_[ 3] AS branche_03, "
                + "  sub.branche_[ 4] AS branche_04, "
                + "  sub.branche_[ 5] AS branche_05, "
                + "  sub.branche_[ 6] AS branche_06, "
                + "  sub.branche_[ 7] AS branche_07, "
                + "  sub.branche_[ 8] AS branche_08, "
                + "  sub.branche_[ 9] AS branche_09, "
                + "  sub.branche_[10] AS branche_10, "
                + "  sub.branche_[11] AS branche_11, "
                + "  sub.branche_[12] AS branche_12, "
                + "  sub.branche_[13] AS branche_13, "
                + "  sub.branche_[14] AS branche_14, "
                + "  sub.branche_[15] AS branche_15, "
                + "  br_massgeb, "
                + "  aa_unbek, "
                + "  aa_siedl, "
                + "  aa_ind_gew, "
                + "  aa_berge, "
                + "  aa_aschsch, "
                + "  aa_bau_erd, "
                + "  aa_klaer, "
                + "  aa_schlaem, "
                + "  aa_ueberw, "
                + "  ga_bo_men, "
                + "  ga_bo_pfl, "
                + "  ga_bo_was, "
                + "  ga_sonst, "
                + "  ueberw_msn, "
                + "  schbesch_m, "
                + "  dm_ah_depo, "
                + "  dm_ah_bobe, "
                + "  dm_bobe_o, "
                + "  dm_pneu, "
                + "  dm_pu_tre, "
                + "  dm_in_situ, "
                + "  sm_sichbau, "
                + "  sm_abdich, "
                + "  sm_abdeck, "
                + "  sm_ver_abd, "
                + "  sm_immo, "
                + "  sm_pneu, "
                + "  sm_pu_tre, "
                + "  sm_in_situ, "
                + "  sm_sonst, "
                + "  shape, "
                + "  shape_format "
                + "FROM ( "
                + "SELECT "
                + "  CASE WHEN geodaten_id NOT LIKE '' THEN geodaten_id::int ELSE -1 END AS fisalbo_nr, "
                + "  albo_flaechenart.name AS artflaeche, "
                + "  '?' AS unterart, "
                + "  albo_flaeche.erhebungsnummer AS verw_kennz, "
                + "  albo_flaechenstatus.schluessel AS statusfl, "
                + "  ortsuebliche_bezeichnung AS ortsbez, "
                + "  FALSE AS bergaufs,  "
                + "  CASE WHEN 'teilflaeche' = albo_flaechentyp.schluessel THEN TRUE ELSE FALSE END AS teilflae, "
                + "  NULL AS schwerp_e, "
                + "  NULL AS schwerp_n, "
                + "  st_area(geom.geo_field)::TEXT AS flaeche_gr, "
                + "  '05124000' AS gemkennz, "
                + "  CASE WHEN albo_flaeche.hausnummer IS NOT NULL THEN str_adr_strasse.name || ' ' || albo_flaeche.hausnummer ELSE str_adr_strasse.name END AS strasse, "
                + "  NULL AS anmerkung, "
                + "  NULL AS bearbstand, "
                + "  NULL AS bbst_ga_tf, "
                + "  to_char(now(), 'dd.MM.yyyy') AS bearbdate, "
                + "  wz.array AS branche_, "
                + "  '?' AS br_massgeb, "
                + "  ah.*, "
                + "  (SELECT COUNT(*) > 0 FROM albo_altablagerung, albo_altablagerung_abfallherkunft, albo_abfallherkunft WHERE albo_altablagerung_abfallherkunft.fk_altablagerung = albo_altablagerung.id AND albo_altablagerung.id = albo_flaeche.fk_altablagerung AND albo_abfallherkunft.id = albo_altablagerung_abfallherkunft.fk_abfallherkunft "
                + "    AND albo_altablagerung_abfallherkunft.ueberwiegend IS TRUE) AS aa_ueberw, "
                + "  albo_massnahmen.ga_boden_mensch AS ga_bo_men, "
                + "  albo_massnahmen.ga_boden_pflanze AS ga_bo_pfl, "
                + "  albo_massnahmen.ga_boden_wasser AS ga_bo_was, "
                + "  albo_massnahmen.ga_sonstiges AS ga_sonst, "
                + "  albo_massnahmen.ueberwachungs_massnahmen AS ueberw_msn, "
                + "  albo_massnahmen.schutz_beschr_massnahmen AS schbesch_m, "
                + "  albo_massnahmen.dm_aushub_deponierung AS dm_ah_depo, "
                + "  albo_massnahmen.dm_aushub_bodenbehandlung AS dm_ah_bobe, "
                + "  albo_massnahmen.dm_bodenbeh_ohne_aushub AS dm_bobe_o, "
                + "  albo_massnahmen.dm_pneumatisch AS dm_pneu, "
                + "  albo_massnahmen.dm_pump_treat AS dm_pu_tre, "
                + "  albo_massnahmen.dm_in_situ_behandlung AS dm_in_situ, "
                + "  albo_massnahmen.sm_sicherungsbauwerk AS sm_sichbau, "
                + "  (albo_massnahmen.ea_oberfl_abdicht OR albo_massnahmen.ea_versiegelung) AS sm_abdich, "
                + "  albo_massnahmen.sm_oberflaechenabdeckung AS sm_abdeck, "
                + "  albo_massnahmen.sm_vertikale_abdichtung AS sm_ver_abd, "
                + "  albo_massnahmen.sm_immobilisierung AS sm_immo, "
                + "  albo_massnahmen.sm_pneumatisch AS sm_pneu, "
                + "  albo_massnahmen.sm_pump_treat AS sm_pu_tre, "
                + "  albo_massnahmen.sm_in_situ_behandlung AS sm_in_situ, "
                + "  albo_massnahmen.sm_sonstige AS sm_sonst, "
                + "  ST_ASTEXT(geom.geo_field) AS shape, "
                + "  'WKT' AS shape_format, "
                + "  albo_flaeche.id "
                + "FROM albo_flaeche "
                + "LEFT JOIN albo_flaechenart ON albo_flaechenart.id = albo_flaeche.fk_art "
                + "LEFT JOIN albo_flaechenstatus ON albo_flaechenstatus.id = albo_flaeche.fk_status "
                + "LEFT JOIN albo_flaechentyp ON albo_flaechentyp.id = albo_flaeche.fk_typ "
                + "LEFT JOIN albo_massnahmen ON albo_massnahmen.id = albo_flaeche.fk_massnahmen "
                + "LEFT JOIN str_adr_strasse ON str_adr_strasse.id = albo_flaeche.fk_strasse "
                + "LEFT JOIN geom ON geom.id = albo_flaeche.fk_geom "
                + "LEFT JOIN ( "
                + "  SELECT "
                + "    COUNT(albo_abfallherkunft.schluessel = '1' OR NULL) > 0 AS aa_unbek, "
                + "    COUNT(albo_abfallherkunft.schluessel = '2' OR NULL) > 0 AS aa_siedl, "
                + "    COUNT(albo_abfallherkunft.schluessel = '3' OR NULL) > 0 AS aa_ind_gew, "
                + "    COUNT(albo_abfallherkunft.schluessel = '4' OR NULL) > 0 AS aa_berge, "
                + "    COUNT(albo_abfallherkunft.schluessel = '5' OR NULL) > 0 AS aa_aschsch, "
                + "    COUNT(albo_abfallherkunft.schluessel = '6' OR NULL) > 0 AS aa_bau_erd, "
                + "    COUNT(albo_abfallherkunft.schluessel = '7' OR NULL) > 0 AS aa_klaer, "
                + "    COUNT(albo_abfallherkunft.schluessel = '8' OR NULL) > 0 AS aa_schlaem, "
                + "    albo_altablagerung.id AS altablagerung_id "
                + "  FROM "
                + "    albo_altablagerung, "
                + "    albo_altablagerung_abfallherkunft, "
                + "    albo_abfallherkunft "
                + "  WHERE "
                + "    albo_altablagerung_abfallherkunft.fk_altablagerung = albo_altablagerung.id "
                + "    AND albo_abfallherkunft.id = albo_altablagerung_abfallherkunft.fk_abfallherkunft "
                + "  GROUP BY albo_altablagerung.id "
                + ") AS ah ON ah.altablagerung_id = albo_flaeche.fk_altablagerung "
                + "LEFT JOIN ( "
                + "  SELECT "
                + "    ARRAY_AGG('2003_' || albo_wirtschaftszweig.schluessel) AS array, "
                + "    albo_standort.fk_flaeche AS flaeche_id "
                + "  FROM "
                + "    albo_standort, "
                + "    albo_standort_wirtschaftszweig, "
                + "    albo_wirtschaftszweig "
                + "  WHERE "
                + "    albo_standort_wirtschaftszweig.standort_reference = albo_standort.arr_wirtschaftszweige "
                + "    AND albo_wirtschaftszweig.id = albo_standort_wirtschaftszweig.fk_wirtschaftszweig "
                + "  GROUP BY albo_standort.fk_flaeche "
                + ") AS wz ON wz.flaeche_id = albo_flaeche.id "
                + "WHERE loeschen IS NOT TRUE "
                + ") AS sub;";

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();
    private User user;
    private MetaService metaService;

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... saps) {
        try {
            final List<String> fields = Arrays.asList(
                    new String[] {
                        "fisalbo_nr",
                        "artflaeche",
                        "unterart",
                        "verw_kennz",
                        "statusfl",
                        "ortsbez",
                        "bergaufs",
                        "teilflae",
                        "schwerp_e",
                        "schwerp_n",
                        "flaeche_gr",
                        "gemkennz",
                        "strasse",
                        "anmerkung",
                        "bearbstand",
                        "bbst_ga_tf",
                        "bearbdate",
                        "branche_01",
                        "branche_02",
                        "branche_03",
                        "branche_04",
                        "branche_05",
                        "branche_06",
                        "branche_07",
                        "branche_08",
                        "branche_09",
                        "branche_10",
                        "branche_11",
                        "branche_12",
                        "branche_13",
                        "branche_14",
                        "branche_15",
                        "br_massgeb",
                        "aa_unbek",
                        "aa_siedl",
                        "aa_ind_gew",
                        "aa_berge",
                        "aa_aschsch",
                        "aa_bau_erd",
                        "aa_klaer",
                        "aa_schlaem",
                        "aa_ueberw",
                        "ga_bo_men",
                        "ga_bo_pfl",
                        "ga_bo_was",
                        "ga_sonst",
                        "ueberw_msn",
                        "schbesch_m",
                        "dm_ah_depo",
                        "dm_ah_bobe",
                        "dm_bobe_o",
                        "dm_pneu",
                        "dm_pu_tre",
                        "dm_in_situ",
                        "sm_sichbau",
                        "sm_abdich",
                        "sm_abdeck",
                        "sm_ver_abd",
                        "sm_immo",
                        "sm_pneu",
                        "sm_pu_tre",
                        "sm_in_situ",
                        "sm_sonst",
                        "shape",
                        "shape_format"
                    });
            final List<String> csvs = new ArrayList();
            csvs.add(String.join(";", fields));
            final ArrayList<ArrayList> rows = getMetaService().performCustomSearch(QUERY, getConnectionContext());
            if (rows != null) {
                for (final ArrayList cols : rows) {
                    final List<String> values = new ArrayList<>();
                    for (final Object value : cols) {
                        if (value instanceof Boolean) {
                            values.add((boolean)value ? "1" : "0");
                        } else if (value instanceof String) {
                            values.add(String.format("\"%s\"", (String)value));
                        } else if (value instanceof Integer) {
                            values.add(String.format("%d", (Integer)value));
                        } else if (value == null) {
                            values.add("");
                        } else {
                            values.add("#######" + value);
                        }
                    }
                    csvs.add(String.join(";", values));
                }
            }
            final String csv = String.join("\n", csvs);
            // LOG.info(csv);
            return csv.getBytes();
        } catch (final Exception ex) {
            return ex;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }
}
