/**
 *
 */
package org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.container;

import java.util.ArrayList;
import java.util.List;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.performancemanager.impl.database.types.EsHistoricalPerformance15Minutes;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.performancemanager.impl.database.types.EsHistoricalPerformance24Hours;

/**
 * @author herbert
 *
 */

public class AllPm {

    public static AllPm EMPTY = new AllPm();

    private final List<EsHistoricalPerformance15Minutes> pm15 = new ArrayList<>();
    private final List<EsHistoricalPerformance24Hours> pm24 = new ArrayList<>();

    public void add(EsHistoricalPerformance15Minutes pm) {
        pm15.add(pm);
    }

    public void add(EsHistoricalPerformance24Hours pm) {
        pm24.add(pm);
    }

    public List<EsHistoricalPerformance15Minutes> getPm15() {
        return pm15;
    }

    public List<EsHistoricalPerformance24Hours> getPm24() {
        return pm24;
    }

    public Object size() {
        return pm15.size()+pm24.size();
    }

}