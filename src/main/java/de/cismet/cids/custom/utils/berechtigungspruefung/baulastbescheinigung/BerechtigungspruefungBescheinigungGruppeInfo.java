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
package de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class BerechtigungspruefungBescheinigungGruppeInfo {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final List<BerechtigungspruefungBescheinigungFlurstueckInfo> flurstuecke;
    @JsonProperty private final List<BerechtigungspruefungBescheinigungBaulastInfo> baulastenBeguenstigt;
    @JsonProperty private final List<BerechtigungspruefungBescheinigungBaulastInfo> baulastenBelastet;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaulastenBescheinigungsGruppeInfo object.
     *
     * @param  flurstuecke           DOCUMENT ME!
     * @param  baulastenBeguenstigt  DOCUMENT ME!
     * @param  baulastenBelastet     DOCUMENT ME!
     */
    public BerechtigungspruefungBescheinigungGruppeInfo(
            @JsonProperty("flurstuecke") final List<BerechtigungspruefungBescheinigungFlurstueckInfo> flurstuecke,
            @JsonProperty("baulastenBeguenstigt") final List<BerechtigungspruefungBescheinigungBaulastInfo> baulastenBeguenstigt,
            @JsonProperty("baulastenBelastet") final List<BerechtigungspruefungBescheinigungBaulastInfo> baulastenBelastet) {
        this.flurstuecke = flurstuecke;
        this.baulastenBeguenstigt = baulastenBeguenstigt;
        this.baulastenBelastet = baulastenBelastet;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        final List<BerechtigungspruefungBescheinigungBaulastInfo> sortedBeguenstigt =
            new ArrayList<BerechtigungspruefungBescheinigungBaulastInfo>(
                baulastenBeguenstigt);
        Collections.sort(sortedBeguenstigt, new BaulastBeanComparator());

        boolean first = true;
        for (final BerechtigungspruefungBescheinigungBaulastInfo baulast : sortedBeguenstigt) {
            if (!first) {
                sb.append(", ");
                first = false;
            }
            sb.append(baulast.toString());
        }

        sb.append("|");

        final List<BerechtigungspruefungBescheinigungBaulastInfo> sortedBelastet =
            new ArrayList<BerechtigungspruefungBescheinigungBaulastInfo>(
                baulastenBelastet);
        Collections.sort(sortedBelastet, new BaulastBeanComparator());

        first = true;
        for (final BerechtigungspruefungBescheinigungBaulastInfo baulast : sortedBelastet) {
            if (!first) {
                sb.append(";");
                first = false;
            }
            sb.append(baulast.toString());
        }

        return sb.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof BerechtigungspruefungBescheinigungGruppeInfo) {
            return toString().equals(other.toString());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    static class BaulastBeanComparator implements Comparator<BerechtigungspruefungBescheinigungBaulastInfo> {

        //~ Methods ------------------------------------------------------------

        @Override
        public int compare(final BerechtigungspruefungBescheinigungBaulastInfo o1,
                final BerechtigungspruefungBescheinigungBaulastInfo o2) {
            final String s1 = (o1 == null) ? "" : o1.toString(); // NOI18N
            final String s2 = (o2 == null) ? "" : o2.toString(); // NOI18N

            return (s1).compareToIgnoreCase(s2);
        }
    }
}
