/*******************************************************************************
 * ============LICENSE_START========================================================================
 * ONAP : ccsdk feature sdnr wt
 * =================================================================================================
 * Copyright (C) 2019 highstreet technologies GmbH Intellectual Property. All rights reserved.
 * =================================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * ============LICENSE_END==========================================================================
 ******************************************************************************/
package org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.internalTypes.InternalDateAndTime;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.internalTypes.InternalSeverity;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.container.AllPm;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.container.Capabilities;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.container.ONFLayerProtocolName;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.util.GenericTransactionUtils;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.wrapperc.OnfMicrowaveModel;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.wrapperc.WrapperMicrowaveModelRev170324;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.wrapperc.WrapperMicrowaveModelRev180907;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.wrapperc.WrapperMicrowaveModelRev181010;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.toggleAlarmFilter.NotificationDelayService;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.ProviderClient;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.database.service.HtDatabaseEventsService;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.listener.MicrowaveEventListener12;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.xml.AttributeValueChangedNotificationXml;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.xml.ProblemNotificationXml;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.xml.WebSocketServiceClient;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.maintenance.MaintenanceService;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.performancemanager.impl.database.types.EsHistoricalPerformance15Minutes;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.performancemanager.impl.database.types.EsHistoricalPerformance24Hours;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.core.model.rev170320.NetworkElement;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.core.model.rev170320.UniversalId;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.core.model.rev170320.extension.g.Extension;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.core.model.rev170320.logical.termination.point.g.Lp;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.core.model.rev170320.network.element.Ltp;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.g._874._1.model.rev170320.GranularityPeriodType;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.g._874._1.model.rev170320.OtnHistoryDataG;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.onf.core.model.conditional.packages.rev170402.NetworkElementPac;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.onf.core.model.conditional.packages.rev170402.network.element.pac.NetworkElementCurrentProblems;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

/**
 * Get information over NETCONF device according to ONF Coremodel. Read networkelement and
 * conditional packages.
 *
 * Get conditional packages from Networkelement Possible interfaces are: MWPS, LTP(MWPS-TTP),
 * MWAirInterfacePac, MicrowaveModel-ObjectClasses-AirInterface ETH-CTP,LTP(Client),
 * MW_EthernetContainer_Pac MWS, LTP(MWS-CTP-xD), MWAirInterfaceDiversityPac,
 * MicrowaveModel-ObjectClasses-AirInterfaceDiversity MWS, LTP(MWS-TTP),
 * ,MicrowaveModel-ObjectClasses-HybridMwStructure MWS, LTP(MWS-TTP),
 * ,MicrowaveModel-ObjectClasses-PureEthernetStructure
 *
 * @author herbert
 *
 */
@SuppressWarnings("deprecation")
public class ONFCoreNetworkElement12 extends ONFCoreNetworkElement12Base
        implements ONFCoreNetworkElementCallback, NotificationActor<AttributeValueChangedNotificationXml> {

    private static final Logger LOG = LoggerFactory.getLogger(ONFCoreNetworkElement12.class);

    /*-----------------------------------------------------------------------------
     * Class members
     */
    private final @Nonnull MicrowaveEventListener12 microwaveEventListener;
    private final @Nonnull OnfMicrowaveModel microwaveModel;
    private final NotificationWorker<AttributeValueChangedNotificationXml> notificationQueue;

    private ListenerRegistration<NotificationListener> listenerRegistrationresult = null;

    /*-----------------------------------------------------------------------------
     * Construction
     */

    /**
     * Constructor
     *
     * @param mountPointNodeName as String
     * @param capabilities of the specific network element
     * @param netconfNodeDataBroker for the network element specific data
     * @param webSocketService to forward event notifications
     * @param databaseService to access the database
     * @param dcaeProvider to forward problem / change notifications
     */
    private ONFCoreNetworkElement12(String mountPointNodeName, Capabilities capabilities,
            DataBroker netconfNodeDataBroker, WebSocketServiceClient webSocketService,
            HtDatabaseEventsService databaseService, ProviderClient dcaeProvider, @Nullable ProviderClient aotsmClient,
            MaintenanceService maintenanceService,
            NotificationDelayService<ProblemNotificationXml> notificationDelayService,
            OnfMicrowaveModel onfMicrowaveModel) {

        super(mountPointNodeName, netconfNodeDataBroker, capabilities);

        this.microwaveModel = onfMicrowaveModel;
        this.microwaveModel.setCoreData(this);

        // Create MicrowaveService here
        this.microwaveEventListener = new MicrowaveEventListener12(mountPointNodeName, webSocketService,
                databaseService, dcaeProvider, aotsmClient, maintenanceService, notificationDelayService, this);
        this.microwaveModel.setOnfMicrowaveModelListener(microwaveEventListener);

        this.notificationQueue = new NotificationWorker<>(1, 100, this);

        // ->Below shifted to super class
        // this.isNetworkElementCurrentProblemsSupporting12 =
        // capabilities.isSupportingNamespaceAndRevision(NetworkElementPac.QNAME);
        // LOG.debug("support necurrent-problem-list=" + this.isNetworkElementCurrentProblemsSupporting12);
        // LOG.info("Create NE instance {}", InstanceList.QNAME.getLocalName());

    }

    /**
     * Check capabilities are matching the this specific implementation and create network element
     * representation if so.
     *
     * @param mountPointNodeName as String
     * @param capabilities of the specific network element
     * @param netconfNodeDataBroker for the network element specific data
     * @param webSocketService to forward event notifications
     * @param databaseService to access the database
     * @param dcaeProvider to forward problem / change notifications
     * @return created Object if conditions are OK or null if not.
     */
    public static @Nullable ONFCoreNetworkElement12 build(String mountPointNodeName, Capabilities capabilities,
            DataBroker netconfNodeDataBroker, WebSocketServiceClient webSocketService,
            HtDatabaseEventsService databaseService, ProviderClient dcaeProvider, @Nullable ProviderClient aotsmClient,
            MaintenanceService maintenanceService,
            NotificationDelayService<ProblemNotificationXml> notificationDelayService) {

        if (capabilities.isSupportingNamespaceAndRevision(NetworkElement.QNAME)) {
            OnfMicrowaveModel onfMicrowaveModel = null;

            if (capabilities.isSupportingNamespaceAndRevision(WrapperMicrowaveModelRev170324.QNAME)) {
                onfMicrowaveModel = new WrapperMicrowaveModelRev170324();
            } else if (capabilities.isSupportingNamespaceAndRevision(WrapperMicrowaveModelRev180907.QNAME)) {
                onfMicrowaveModel = new WrapperMicrowaveModelRev180907();
            } else if (capabilities.isSupportingNamespaceAndRevision(WrapperMicrowaveModelRev181010.QNAME)) {
                onfMicrowaveModel = new WrapperMicrowaveModelRev181010();
            }

            if (onfMicrowaveModel != null) {
                return new ONFCoreNetworkElement12(mountPointNodeName, capabilities, netconfNodeDataBroker,
                        webSocketService, databaseService, dcaeProvider, aotsmClient, maintenanceService,
                        notificationDelayService, onfMicrowaveModel);
            }
        }
        return null;

    }

    /*-----------------------------------------------------------------------------
     * Functions
     */

    /**
     * DeviceMonitor Prepare check by updating NE state and reading all interfaces.
     */
    @Override
    public void prepareCheck() {
        synchronized (dmLock) {
            boolean change = readNetworkElementAndInterfaces();
            if (change) {
                int problems = microwaveEventListener.removeAllCurrentProblemsOfNode();
                List<ProblemNotificationXml> resultList = readAllCurrentProblemsOfNode();
                microwaveEventListener.initCurrentProblemStatus(resultList);
                LOG.info("Resync mountpoint {} for device {}. Removed {}. Current problems: {}", getMountPointNodeName(),
                        getUuId(), problems, resultList.size());
            }
        }
    }

    // public boolean checkIfConnectionToMediatorIsOk() -> Shifted to super class
    // public boolean checkIfConnectionToNeIsOk() -> Shifted to super class

    /*-----------------------------------------------------------------------------
     * Synchronization
     */

    // public void initSynchronizationExtension() -> Shifted to super class
    // private InstanceList readPTPClockInstances() -> Shifted to super class

    /*-----------------------------------------------------------------------------
     * Services for NE/Device synchronization
     */

    /**
     * Handling of specific Notifications from NE, indicating changes and need for synchronization.
     *
     * <attribute-value-changed-notification xmlns="urn:onf:params:xml:ns:yang:microwave-model">
     * <attribute-name>/equipment-pac/equipment-current-problems</attribute-name>
     * <object-id-ref>CARD-1.1.1.0</object-id-ref> <new-value></new-value>
     * </attribute-value-changed-notification>
     * <attribute-value-changed-notification xmlns="urn:onf:params:xml:ns:yang:microwave-model">
     * <attribute-name>/network-element/extension[value-name="top-level-equipment"]/value</attribute-name>
     * <object-id-ref>Hybrid-Z</object-id-ref>
     * <new-value>SHELF-1.1.0.0,IDU-1.55.0.0,ODU-1.56.0.0,IDU-1.65.0.0</new-value>
     * </attribute-value-changed-notification>
     */


    @Override
    public void notificationFromNeListener(AttributeValueChangedNotificationXml notificationXml) {
        notificationQueue.put(notificationXml);
    }

    @Override
    public void notificationActor(AttributeValueChangedNotificationXml notificationXml) {

        LOG.debug("Enter change notification listener");
        if (LOG.isTraceEnabled()) {
            LOG.trace("Notification: {}", notificationXml);
        }
        if (notificationXml.getAttributeName().equals("/equipment-pac/equipment-current-problems")) {
            syncEquipmentPac(notificationXml.getObjectId());
        } else if (notificationXml.getAttributeName()
                .equals("/network-element/extension[value-name=\"top-level-equipment\"]/value")) {
            initialReadFromNetworkElement();
        }
        LOG.debug("Leave change notification listener");
    }

    /**
     * Synchronize problems for a specific equipment-pac
     *
     * @param uuidString of the equipment-pac
     */
    private synchronized void syncEquipmentPac(String uuidString) {

        int problems = microwaveEventListener.removeObjectsCurrentProblemsOfNode(uuidString);
        LOG.debug("Removed {} problems for uuid {}", problems, uuidString);

        List<ProblemNotificationXml> resultList = equipment.addProblemsofNodeObject(uuidString);
        microwaveEventListener.initCurrentProblemStatus(resultList);
        LOG.debug("Added {} problems for uuid {}", resultList.size(), uuidString);

    }


    /*-----------------------------------------------------------------------------
     * Problem/Fault related functions
     */

    /**
     * Read during startup all relevant structure and status parameters from device
     */
    @Override
    public synchronized void initialReadFromNetworkElement() {
        // optionalNe.getLtp().get(0).getLp();
        LOG.debug("Get info about {}", getMountPointNodeName());

        int problems = microwaveEventListener.removeAllCurrentProblemsOfNode();
        LOG.debug("Removed all {} problems from database at registration", problems);

        // Step 2.1: access data broker within this mount point
        LOG.debug("DBRead start");

        // Step 2.2: read ne from data store
        readNetworkElementAndInterfaces();
        equipment.readNetworkElementEquipment();

        // Step 2.3: read the existing faults and add to DB
        List<ProblemNotificationXml> resultList = readAllCurrentProblemsOfNode();
        equipment.addProblemsofNode(resultList);

        microwaveEventListener.initCurrentProblemStatus(resultList);

        microwaveEventListener.writeEquipment(equipment);

        LOG.info("Found info at {} for device {} number of problems: {}", getMountPointNodeName(), getUuId(),
                resultList.size());
    }

    /**
     * LOG the newly added problems of the interface pac
     *
     * @param idxStart
     * @param uuid
     * @param resultList
     */
    private void debugResultList(String uuid, List<ProblemNotificationXml> resultList, int idxStart) {
        if (LOG.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            int idx = 0;
            for (int t = idxStart; t < resultList.size(); t++) {
                sb.append(idx++);
                sb.append(":{");
                sb.append(resultList.get(t));
                sb.append('}');
            }
            LOG.debug("Found problems {} {}", uuid, sb.toString());
        }
    }

    /**
     * Read from NetworkElement and verify LTPs have changed. If the NE has changed, update to the new
     * structure. From initial state it changes also.
     */
    private synchronized boolean readNetworkElementAndInterfaces() {

        LOG.debug("Update mountpoint if changed {}", getMountPointNodeName());

        optionalNe = GenericTransactionUtils.readData(getNetconfNodeDataBroker(), LogicalDatastoreType.OPERATIONAL,
                NETWORKELEMENT_IID);;
        synchronized (pmLock) {
            boolean change = false;

            if (optionalNe == null) {
                LOG.debug("Unable to read NE data for mountpoint {}", getMountPointNodeName());
                if (!interfaceList.isEmpty()) {
                    interfaceList.clear();
                    interfaceListIterator = null;
                    change = true;
                }

            } else {
                LOG.debug("Mountpoint '{}' NE-Name '{}'", getMountPointNodeName(), optionalNe.getName());
                List<Lp> actualInterfaceList = getLtpList(optionalNe);
                if (!interfaceList.equals(actualInterfaceList)) {
                    LOG.debug("Mountpoint '{}' Update LTP List. Elements {}", getMountPointNodeName(),
                            actualInterfaceList.size());
                    interfaceList.clear();
                    interfaceList.addAll(actualInterfaceList);
                    interfaceListIterator = null;
                    change = true;
                }
            }
            return change;
        }
    }

    /**
     * Read current problems of AirInterfaces and EthernetContainer according to NE status into DB
     *
     * @return List with all problems
     */
    private List<ProblemNotificationXml> readAllCurrentProblemsOfNode() {

        // Step 2.3: read the existing faults and add to DB
        List<ProblemNotificationXml> resultList = new ArrayList<>();
        int idxStart; // Start index for debug messages
        UniversalId uuid;

        synchronized (pmLock) {
            for (Lp lp : interfaceList) {

                idxStart = resultList.size();
                uuid = lp.getUuid();
                Class<?> lpClass = getLpExtension(lp);

                ONFLayerProtocolName lpName = ONFLayerProtocolName.valueOf(lp.getLayerProtocolName());

                microwaveModel.readTheFaultsOfMicrowaveModel(lpName, lpClass, uuid, resultList);

                debugResultList(uuid.getValue(), resultList, idxStart);

            }
        }

        // Step 2.4: Read other problems from mountpoint
        if (isNetworkElementCurrentProblemsSupporting12) {
            idxStart = resultList.size();
            readNetworkElementCurrentProblems12(resultList);
            debugResultList("CurrentProblems12", resultList, idxStart);
        }

        return resultList;

    }

    /**
     * Get uuid of Optional NE.
     *
     * @return Uuid or EMPTY String if optionNE is not available
     */
    private String getUuId() {
        String uuid = EMPTY;

        try {
            uuid = optionalNe != null ? optionalNe.getUuid() != null ? optionalNe.getUuid().getValue() : EMPTY : EMPTY;
        } catch (NullPointerException e) {
            // Unfortunately throws null pointer if not definied
        }
        return uuid;
    }

    /**
     * Read the NetworkElement part from database.
     *
     * @return Optional with NetworkElement or empty
     */
    @Nullable
    private NetworkElement readNetworkElement() {
        // Step 2.2: construct data and the relative iid
        // The schema path to identify an instance is
        // <i>CoreModel-CoreNetworkModule-ObjectClasses/NetworkElement</i>
        // Read to the config data store
        return GenericTransactionUtils.readData(getNetconfNodeDataBroker(), LogicalDatastoreType.OPERATIONAL,
                NETWORKELEMENT_IID);
    }

    /**
     * Get from LayerProtocolExtensions the related generated ONF Interface PAC class which represents it.
     *
     * @param lp logical termination point
     * @return Class of InterfacePac
     */
    @Nullable
    private Class<?> getLpExtension(@Nullable Lp lp) {

        String capability = EMPTY;
        String revision = EMPTY;
        String conditionalPackage = EMPTY;
        Class<?> res = null;

        if (lp != null) {
            for (Extension e : getExtensionList(lp)) {
                if (e.getValueName().contentEquals("capability")) {
                    capability = e.getValue();
                    int idx = capability.indexOf("?");
                    if (idx != -1) {
                        capability = capability.substring(0, idx);
                    }
                }
                if (e.getValueName().contentEquals("revision")) {
                    revision = e.getValue();
                }
                if (e.getValueName().contentEquals("conditional-package")) {
                    conditionalPackage = e.getValue();
                }
            }
        }
        // QName qName =
        // org.opendaylight.yangtools.yang.common.QName.create("urn:onf:params:xml:ns:yang:microwave-model",
        // "2017-03-24", "mw-air-interface-pac").intern();
        LOG.info("LpExtension capability={} revision={} conditionalPackage={}", capability, revision,
                conditionalPackage);
        if (!capability.isEmpty() && !revision.isEmpty() && !conditionalPackage.isEmpty()) {
            try {
                QName qName = QName.create(capability, revision, conditionalPackage);
                res = this.microwaveModel.getClassForLtpExtension(qName);
            } catch (IllegalArgumentException e) {
                LOG.debug("Can not create QName from ({}{}{}): {}", capability, revision, conditionalPackage,
                        e.getMessage());
            }
        }
        return res;
    }

    /**
     * Read element from class that could be not available
     *
     * @param ltp layer termination point
     * @return List with extension parameters or empty list
     */
    @Nonnull
    private static List<Extension> getExtensionList(@Nullable Lp ltp) {
        if (ltp != null && ltp.getExtension() != null) {
            return ltp.getExtension();
        } else {
            return EMPTYLTPEXTENSIONLIST;
        }
    }

    /**
     * Get List of UUIDs for conditional packages from Networkelement<br>
     * Possible interfaces are:<br>
     * MWPS, LTP(MWPS-TTP), MWAirInterfacePac, MicrowaveModel-ObjectClasses-AirInterface<br>
     * ETH-CTP,LTP(Client), MW_EthernetContainer_Pac<br>
     * MWS, LTP(MWS-CTP-xD), MWAirInterfaceDiversityPac,
     * MicrowaveModel-ObjectClasses-AirInterfaceDiversity<br>
     * MWS, LTP(MWS-TTP), ,MicrowaveModel-ObjectClasses-HybridMwStructure<br>
     * MWS, LTP(MWS-TTP), ,MicrowaveModel-ObjectClasses-PureEthernetStructure<br>
     *
     * @param ne Networkelement
     * @return Id List, never null.
     */
    private static List<Lp> getLtpList(@Nullable NetworkElement ne) {

        List<Lp> res = Collections.synchronizedList(new ArrayList<Lp>());

        if (ne != null) {
            List<Ltp> ltpRefList = ne.getLtp();
            if (ltpRefList == null) {
                LOG.debug("DBRead NE-Interfaces: null");
            } else {
                for (Ltp ltRefListE : ltpRefList) {
                    List<Lp> lpList = ltRefListE.getLp();
                    if (lpList == null) {
                        LOG.debug("DBRead NE-Interfaces Reference List: null");
                    } else {
                        for (Lp ltp : lpList) {
                            //// LayerProtocolName layerProtocolName = lpListE.getLayerProtocolName();
                            // UniversalId uuId = lpListE.getUuid();
                            res.add(ltp);
                        }
                    }
                }
            }
        } else {
            LOG.debug("DBRead NE: null");
        }

        // ---- Debug
        if (LOG.isDebugEnabled()) {
            StringBuffer strBuf = new StringBuffer();
            for (Lp ltp : res) {
                if (strBuf.length() > 0) {
                    strBuf.append(", ");
                }
                strBuf.append(ltp.getLayerProtocolName().getValue());
                strBuf.append(':');
                strBuf.append(ltp.getUuid().getValue());
            }
            LOG.debug("DBRead NE-Interfaces: {}", strBuf.toString());
        }
        // ---- Debug end

        return res;
    }


    @Nonnull
    private List<? extends OtnHistoryDataG> readTheHistoricalPerformanceData(Lp lp) {
        ONFLayerProtocolName lpName = ONFLayerProtocolName.valueOf(lp.getLayerProtocolName());

        return this.microwaveModel.readTheHistoricalPerformanceData(lpName, lp);
        /*
         * switch (lpName) { case MWAirInterface: return
         * readTheHistoricalPerformanceDataOfMwAirInterfacePac(lp);
         *
         * case EthernetContainer12: return readTheHistoricalPerformanceDataOfEthernetContainer(lp);
         *
         * case EthernetContainer10: case EthernetPhysical: case Ethernet: case TDMContainer: case
         * Structure: case Unknown: LOG.debug("Do not read HistoricalPM data for {} {}", lpName,
         * lp.getUuid().getValue()); break; } return new ArrayList<>();
         */
    }

    @Override
    public AllPm getHistoricalPM() {

        synchronized (pmLock) {
            if (pmLp != null) {
                LOG.debug("Enter query PM");
                AllPm allPm = new AllPm();
                Lp lp = pmLp;

                List<? extends OtnHistoryDataG> resultList = readTheHistoricalPerformanceData(lp);
                LOG.debug("Got records: {}", resultList.size());
                // org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.g._874._1.model.rev170320.GranularityPeriodType
                GranularityPeriodType granularityPeriod;
                for (OtnHistoryDataG perf : resultList) {

                    granularityPeriod = perf.getGranularityPeriod();
                    if (granularityPeriod == null) {
                        granularityPeriod = GranularityPeriodType.Unknown;
                    }

                    switch (granularityPeriod) {
                        case Period15Min: {
                            EsHistoricalPerformance15Minutes pm =
                                    new EsHistoricalPerformance15Minutes(getMountPointNodeName(), lp)
                                            .setHistoricalRecord15Minutes(perf);
                            allPm.add(pm);
                        }
                            break;

                        case Period24Hours: {
                            EsHistoricalPerformance24Hours pm =
                                    new EsHistoricalPerformance24Hours(getMountPointNodeName(), lp)
                                            .setHistoricalRecord24Hours(perf);
                            LOG.debug("Write 24h write to DB");
                            allPm.add(pm);
                        }
                            break;

                        default:
                            LOG.warn("Unknown granularity {}", perf.getGranularityPeriod());
                            break;

                    }
                }
                LOG.debug("Deliver normalized records: {}", allPm.size());
                return allPm;
            } else {
                LOG.debug("Deliver empty, no LTP");
                return AllPm.getEmpty();
            }
        }
    }

    @Override
    public void resetPMIterator() {
        synchronized (pmLock) {
            interfaceListIterator = interfaceList.iterator();
        }
        LOG.debug("PM reset iterator");
    }

    @Override
    public boolean hasNext() {
        boolean res;
        synchronized (pmLock) {
            res = interfaceListIterator != null ? interfaceListIterator.hasNext() : false;
        }
        LOG.debug("PM hasNext LTP {}", res);
        return res;
    }

    @Override
    public void next() {
        synchronized (pmLock) {
            if (interfaceListIterator == null) {
                pmLp = null;
                LOG.debug("PM next LTP null");
            } else {
                pmLp = interfaceListIterator.next();
                LOG.debug("PM next LTP {}", pmLp.getLayerProtocolName().getValue());
            }
        }
    }

    @Override
    public String pmStatusToString() {
        StringBuffer res = new StringBuffer();
        synchronized (pmLock) {
            res.append(pmLp == null ? "no interface" : pmLp.getLayerProtocolName().getValue());
            for (Lp lp : interfaceList) {
                res.append("IF:");
                res.append(lp.getLayerProtocolName().getValue());
                res.append(" ");
            }
        }
        return res.toString();
    }

    /**
     * Remove all entries from list
     */
    @Override
    public int removeAllCurrentProblemsOfNode() {
        return microwaveEventListener.removeAllCurrentProblemsOfNode();
    }

    /**
     * Register the listener
     */
    @Override
    public void doRegisterMicrowaveEventListener(MountPoint mountPoint) {
        LOG.info("End registration listener for Mountpoint {}", mountPoint.getIdentifier().toString());
        final Optional<NotificationService> optionalNotificationService =
                mountPoint.getService(NotificationService.class);
        final NotificationService notificationService = optionalNotificationService.get();
        // notificationService.registerNotificationListener(microwaveEventListener);
        listenerRegistrationresult =
                notificationService.registerNotificationListener(microwaveModel.getNotificationListener());
        LOG.info("End registration listener for Mountpoint {} Listener: {} Result: {}",
                mountPoint.getIdentifier().toString(), optionalNotificationService, listenerRegistrationresult);
    }


    /*------------------------------------------------------------
     * private function to access database
     */

    /*-----------------------------------------------------------------------------
     * Reading problems for the networkElement V1.2
     */

    private List<ProblemNotificationXml> readNetworkElementCurrentProblems12(List<ProblemNotificationXml> resultList) {

        LOG.info("DBRead Get {} NetworkElementCurrentProblems12", getMountPointNodeName());

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.onf.core.model.conditional.packages.rev170402.NetworkElementPac> networkElementCurrentProblemsIID =
                InstanceIdentifier.builder(
                        org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.onf.core.model.conditional.packages.rev170402.NetworkElementPac.class)
                        .build();

        // Step 2.3: read to the config data store
        NetworkElementPac problemPac;
        NetworkElementCurrentProblems problems;
        try {
            problemPac = GenericTransactionUtils.readData(getNetconfNodeDataBroker(), LogicalDatastoreType.OPERATIONAL,
                    networkElementCurrentProblemsIID);
            problems = problemPac.getNetworkElementCurrentProblems();
            if (problems == null) {
                LOG.debug("DBRead no NetworkElementCurrentProblems12");
            } else if (problems.getCurrentProblemList() == null) {
                LOG.debug("DBRead empty CurrentProblemList12");
            } else {
                for (org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.onf.core.model.conditional.packages.rev170402.network.element.current.problems.g.CurrentProblemList problem : problems
                        .getCurrentProblemList()) {
                    resultList.add(new ProblemNotificationXml(getMountPointNodeName(), problem.getObjectReference(),
                            problem.getProblemName(), InternalSeverity.valueOf(problem.getProblemSeverity()),
                            problem.getSequenceNumber().toString(),
                            InternalDateAndTime.valueOf(problem.getTimeStamp())));
                }
            }
        } catch (Exception e) {
            LOG.warn("DBRead {} NetworkElementCurrentProblems12 not supported. Message '{}' ", getMountPointNodeName(),
                    e.getMessage());
        }
        return resultList;

    }

    @Override
    public void close() throws Exception {
        super.close();
        if (listenerRegistrationresult != null) {
            listenerRegistrationresult.close();
        }
    }




}
