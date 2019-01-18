package org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.wrapperc;

import java.util.List;

import javax.annotation.Nullable;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.xml.ProblemNotificationXml;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.core.model.rev170320.UniversalId;

public interface OnfInterfacePac {

    /**
     * Read problems of specific interfaces. TODO Goal for future implementation
     * without usage of explicit new. Key is generated by newInstance() function
     * here to verify this approach.
     *
     * @param uuId Universal index of Interfacepac
     * @return list of alarms
     */
    public List<ProblemNotificationXml> readTheFaults(UniversalId interfacePacUuid,  @Nullable List<ProblemNotificationXml> resultList);

}