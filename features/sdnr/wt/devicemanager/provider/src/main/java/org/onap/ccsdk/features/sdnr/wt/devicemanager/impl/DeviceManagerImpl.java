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
package org.onap.ccsdk.features.sdnr.wt.devicemanager.impl;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.aaiconnector.impl.AaiProviderClient;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.archiveservice.ArchiveCleanService;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.database.HtDatabaseNode;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.ONFCoreNetworkElementFactory;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.ONFCoreNetworkElementRepresentation;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.toggleAlarmFilter.NotificationDelayService;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.config.HtDevicemanagerConfiguration;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.config.impl.AkkaConfig;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.config.impl.EsConfig;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.config.impl.GeoConfig;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.config.impl.PmConfig;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.dcaeconnector.impl.DcaeProviderClient;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.devicemonitor.impl.DeviceMonitor;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.devicemonitor.impl.DeviceMonitorEmptyImpl;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.devicemonitor.impl.DeviceMonitorImpl;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.database.service.HtDatabaseEventsService;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.listener.NetconfChangeListener;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.listener.ODLEventListener;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.xml.ProblemNotificationXml;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.xml.WebSocketServiceClient;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.xml.WebSocketServiceClientDummyImpl;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.xml.WebSocketServiceClientImpl2;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.index.impl.IndexConfigService;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.index.impl.IndexMwtnService;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.index.impl.IndexUpdateService;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.maintenance.impl.MaintenanceServiceImpl;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.performancemanager.impl.PerformanceManagerImpl;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.performancemanager.impl.database.service.MicrowaveHistoricalPerformanceWriterService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.ClusteredConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;

@SuppressWarnings("deprecation")
public class DeviceManagerImpl implements DeviceManagerService, AutoCloseable, ResyncNetworkElementsListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceManagerImpl.class);
    private static final String APPLICATION_NAME = "DeviceManager";
    private static final String MYDBKEYNAMEBASE = "SDN-Controller";

    // http://sendateodl:8181/restconf/operational/network-topology:network-topology/topology/topology-netconf
    private static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                    new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));
    @SuppressWarnings("unused")
    private static final String STARTUPLOG_FILENAME = "etc/devicemanager.startup.log";
    // private static final String STARTUPLOG_FILENAME2 = "data/cache/devicemanager.startup.log";

    private DataBroker dataBroker = null;
    private MountPointService mountPointService = null;
    private RpcProviderRegistry rpcProviderRegistry = null;
    @SuppressWarnings("unused")
    private NotificationPublishService notificationPublishService = null;
    private ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    private final ConcurrentHashMap<String, ONFCoreNetworkElementRepresentation> networkElementRepresentations =
            new ConcurrentHashMap<>();
    private final ONFCoreNetworkElementRepresentation networkelementLock = ONFCoreNetworkElementFactory.getEmpty("NE-LOCK");
    private WebSocketServiceClient webSocketService;
    private HtDatabaseEventsService databaseClientEvents;
    private ODLEventListener odlEventListener;
    private NetconfChangeListener netconfChangeListener;
    private DeviceManagerApiServiceImpl rpcApiService;
    private @Nullable PerformanceManagerImpl performanceManager = null;
    private ProviderClient dcaeProviderClient;
    private ProviderClient aotsMProvider;
    private @Nullable AaiProviderClient aaiProviderClient = null;
    private @Nullable DeviceMonitor deviceMonitor = new DeviceMonitorEmptyImpl();
    private IndexUpdateService updateService;
    private IndexConfigService configService;
    private IndexMwtnService mwtnService;
    private HtDatabaseNode htDatabase;
    private Boolean devicemanagerInitializationOk = false;
    private MaintenanceServiceImpl maintenanceService;
    private NotificationDelayService<ProblemNotificationXml> notificationDelayService;
    private Thread threadDoClearCurrentFaultByNodename = null;
    private int refreshCounter = 0;
    private AkkaConfig akkaConfig;
    private ArchiveCleanService archiveCleanService;
    @SuppressWarnings("unused")
	private ClusterSingletonServiceRegistration cssRegistration;

    // Blueprint 1
    public DeviceManagerImpl() {
        LOG.info("Creating provider for {}", APPLICATION_NAME);
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setRpcProviderRegistry(RpcProviderRegistry rpcProviderRegistry) {
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    public void setNotificationPublishService(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    public void setMountPointService(MountPointService mountPointService) {
        this.mountPointService = mountPointService;
    }
    public void setClusterSingletonService(ClusterSingletonServiceProvider clusterSingletonService) {
    	this.clusterSingletonServiceProvider = clusterSingletonService;
    }
    public void init() {

        LOG.info("Session Initiated start {}", APPLICATION_NAME);

        // Start RPC Service
        this.rpcApiService = new DeviceManagerApiServiceImpl(rpcProviderRegistry);
        // Get configuration
        HtDevicemanagerConfiguration config = HtDevicemanagerConfiguration.getConfiguration();
        try {
            this.akkaConfig = AkkaConfig.load();
            LOG.debug("akka.conf loaded: " + akkaConfig.toString());
        } catch (Exception e1) {
            this.akkaConfig = null;
            LOG.warn("problem loading akka.conf: " + e1.getMessage());
        }
        GeoConfig geoConfig = null;
        if (akkaConfig != null && akkaConfig.isCluster()) {
            LOG.info("cluster mode detected");
            if (GeoConfig.fileExists()) {
                try {
                    LOG.debug("try to load geoconfig");
                    geoConfig = GeoConfig.load();
                } catch (Exception err) {
                    LOG.warn("problem loading geoconfig: " + err.getMessage());
                }
            } else {
                LOG.debug("no geoconfig file found");
            }
        } else {
            LOG.info("single node mode detected");
        }

        this.notificationDelayService = new NotificationDelayService<>(config);

        EsConfig dbConfig = config.getEs();
        LOG.debug("esConfig=" + dbConfig.toString());
        // Start database
        htDatabase = HtDatabaseNode.start(dbConfig, akkaConfig, geoConfig);
        if (htDatabase == null) {
            LOG.error("Can only run with local database. Stop initialization of devicemanager.");
        } else {
            // init Database Values only if singleNode or clusterMember=1
            if (akkaConfig == null || akkaConfig.isClusterAndFirstNode()) {
                // Create DB index if not existing and if database is running
                try {
                    this.configService = new IndexConfigService(htDatabase);
                    this.mwtnService = new IndexMwtnService(htDatabase);
                } catch (Exception e) {
                    LOG.warn("Can not start ES access clients to provide database index config, mwtn. ", e);
                }
            }
            // start service for device maintenance service
            this.maintenanceService = new MaintenanceServiceImpl(htDatabase);

            // Websockets
            try {
                this.webSocketService = new WebSocketServiceClientImpl2(rpcProviderRegistry);
            } catch (Exception e) {
                LOG.error("Can not start websocket service. Loading mock class.", e);
                this.webSocketService = new WebSocketServiceClientDummyImpl();
            }
            // DCAE
            this.dcaeProviderClient = new DcaeProviderClient(config, dbConfig.getCluster(), this);

            this.aaiProviderClient = new AaiProviderClient(config, this);
            // EM
            EsConfig emConfig = dbConfig.cloneWithIndex("sdnevents");

            if (emConfig == null) {
                LOG.warn("No configuration available. Don't start event manager");
            } else {
                this.databaseClientEvents = new HtDatabaseEventsService(htDatabase);

                String myDbKeyNameExtended = MYDBKEYNAMEBASE + "-" + dbConfig.getCluster();

                this.odlEventListener = new ODLEventListener(myDbKeyNameExtended, webSocketService,
                        databaseClientEvents, dcaeProviderClient, aotsMProvider, maintenanceService);
            }
            this.archiveCleanService = new ArchiveCleanService(config, databaseClientEvents, mwtnService);
            this.cssRegistration = this.clusterSingletonServiceProvider.registerClusterSingletonService(this.archiveCleanService);
            // PM
            PmConfig configurationPM = config.getPm();
            LOG.info("Performance manager configuration: {}", configurationPM);
            if (!configurationPM.isPerformanceManagerEnabled()) {

                LOG.info("No configuration available. Don't start performance manager");
            } else {
                @Nullable
                MicrowaveHistoricalPerformanceWriterService databaseClientHistoricalPerformance;
                databaseClientHistoricalPerformance = new MicrowaveHistoricalPerformanceWriterService(htDatabase);
                this.performanceManager = new PerformanceManagerImpl(60, databaseClientHistoricalPerformance);
            }

            // DUS (Database update service)
            LOG.debug("start db update service");
            this.updateService =
                    new IndexUpdateService(htDatabase, dbConfig.getHost(), dbConfig.getCluster(), dbConfig.getNode());
            this.updateService.start();

            // RPC Service for specific services
            this.rpcApiService.setMaintenanceService(this.maintenanceService);
            this.rpcApiService.setResyncListener(this);
            // DM
            // DeviceMonitor has to be available before netconfSubscriptionManager is
            // configured
            LOG.debug("start DeviceMonitor Service");
            this.deviceMonitor = new DeviceMonitorImpl(dataBroker, odlEventListener, config);

            // netconfSubscriptionManager should be the last one because this is a callback
            // service
            LOG.debug("start NetconfSubscriptionManager Service");
            // this.netconfSubscriptionManager = new
            // NetconfSubscriptionManagerOfDeviceManager(this, dataBroker);
            // this.netconfSubscriptionManager.register();
            this.netconfChangeListener = new NetconfChangeListener(this, dataBroker);
            this.netconfChangeListener.register();

            this.devicemanagerInitializationOk = true;
        }
        LOG.info("Session Initiated end. Initialization done {}", devicemanagerInitializationOk);
    }

    @Override
    public void close() throws Exception {
        LOG.info("DeviceManagerImpl closing ...");

        close(performanceManager);
        close(dcaeProviderClient);
        close(aaiProviderClient);
        close(aotsMProvider);
        close(deviceMonitor);
        close(updateService, configService, mwtnService);
        close(htDatabase);
        close(netconfChangeListener);
        close(maintenanceService);
        close(rpcApiService);
        close(notificationDelayService);
        close(archiveCleanService);
        LOG.info("DeviceManagerImpl closing done");
    }


    /**
     * Used to close all Services, that should support AutoCloseable Pattern
     *
     * @param toClose
     * @throws Exception
     */
    private void close(AutoCloseable... toCloseList) throws Exception {
        for (AutoCloseable element : toCloseList) {
            if (element != null) {
                element.close();
            }
        }
    }

    /*-------------------------------------------------------------------------------------------
     * Functions for interface DeviceManagerService
     */

    /**
     * For each mounted device a mountpoint is created and this listener is called.
     * Mountpoint was created or existing. Managed device is now fully connected to node/mountpoint.
     * @param action provide action
     * @param nNodeId id of the mountpoint
     * @param nNode mountpoint contents
     */
    public void startListenerOnNodeForConnectedState(Action action, NodeId nNodeId, NetconfNode nNode) {

        String mountPointNodeName = nNodeId.getValue();
        LOG.info("Starting Event listener on Netconf for mountpoint {} Action {}", mountPointNodeName, action);

        boolean preConditionMissing = false;
        if (mountPointService == null) {
            preConditionMissing = true;
            LOG.warn("No mountservice available.");
        }
        if (!devicemanagerInitializationOk) {
            preConditionMissing = true;
            LOG.warn("Devicemanager initialization still pending.");
        }
        if (preConditionMissing) {
            return;
        }

        if (!isNetconfNodeMaster(nNode)) {
            // Change Devicemonitor-status to connected ... for non master mountpoints.
            deviceMonitor.deviceConnectSlaveIndication(mountPointNodeName);
		} else {

			InstanceIdentifier<Node> instanceIdentifier = NETCONF_TOPO_IID.child(Node.class,
					new NodeKey(new NodeId(mountPointNodeName)));

			Optional<MountPoint> optionalMountPoint = null;
			int timeout = 10000;
			while (!(optionalMountPoint = mountPointService.getMountPoint(instanceIdentifier)).isPresent()
					&& timeout > 0) {
				LOG.info("Event listener waiting for mount point for Netconf device :: Name : {}", mountPointNodeName);
				sleepMs(1000);
				timeout -= 1000;
			}

			if (!optionalMountPoint.isPresent()) {
				LOG.warn("Event listener timeout while waiting for mount point for Netconf device :: Name : {} ",
						mountPointNodeName);
			} else {
				// Mountpoint is present for sure
				MountPoint mountPoint = optionalMountPoint.get();
				// BindingDOMDataBrokerAdapter.BUILDER_FACTORY;
				LOG.info("Mountpoint with id: {} class {} toString {}", mountPoint.getIdentifier(),
						mountPoint.getClass().getName(), mountPoint);
				Optional<DataBroker> optionalNetconfNodeDatabroker = mountPoint.getService(DataBroker.class);

				if (!optionalNetconfNodeDatabroker.isPresent()) {
					LOG.info("Slave mountpoint {} without databroker", mountPointNodeName);
				} else {

					// It is master for mountpoint and all data are available.
					// Make sure that specific mountPointNodeName is handled only once.
					// be aware that startListenerOnNodeForConnectedState could be called multiple
					// times for same mountPointNodeName.
					// networkElementRepresentations contains handled NEs at master node.

					synchronized (networkelementLock) {
						if (networkElementRepresentations.containsKey(mountPointNodeName)) {
							LOG.warn("Mountpoint {} already registered. Leave startup procedure.", mountPointNodeName);
							return;
						} else {
							ONFCoreNetworkElementRepresentation result = networkElementRepresentations.put(mountPointNodeName,
									networkelementLock);
							if (result != null) {
								LOG.info("Expected null value was not provided, but {}", result.getMountPointNodeName());
							}
						}
					}

					DataBroker netconfNodeDataBroker = optionalNetconfNodeDatabroker.get();
					LOG.info("Master mountpoint {}", mountPointNodeName);
					// getNodeInfoTest(dataBroker);

					// create automatic empty maintenance entry into db before reading and listening
					// for problems
					this.maintenanceService.createIfNotExists(mountPointNodeName);

					// Setup microwaveEventListener for Notificationservice

					// MicrowaveEventListener microwaveEventListener = new
					// MicrowaveEventListener(mountPointNodeName, websocketmanagerService,
					// xmlMapper, databaseClientEvents);
					ONFCoreNetworkElementRepresentation ne = ONFCoreNetworkElementFactory.create(mountPointNodeName,
							dataBroker, webSocketService, databaseClientEvents, instanceIdentifier,
							netconfNodeDataBroker, dcaeProviderClient, aotsMProvider, maintenanceService,
							notificationDelayService);

					synchronized (networkelementLock) {
						ONFCoreNetworkElementRepresentation result = networkElementRepresentations
								.put(mountPointNodeName, ne);
						if (result != networkelementLock) {
							LOG.info("NE list does not provide lock as epxected, but {}.",
									result.getMountPointNodeName());
						}
					}
					ne.doRegisterMicrowaveEventListener(mountPoint);

					// Register netconf stream
					registerNotificationStream(mountPointNodeName, mountPoint, "NETCONF");

					// -- Read data from NE
					ne.initialReadFromNetworkElement();
					ne.initSynchronizationExtension();

					sendUpdateNotification(mountPointNodeName, nNode.getConnectionStatus());

					if (aaiProviderClient != null) {
						aaiProviderClient.onDeviceRegistered(mountPointNodeName);
					}
					// -- Register NE to performance manager
					if (performanceManager != null) {
						performanceManager.registration(mountPointNodeName, ne);
					}

					deviceMonitor.deviceConnectMasterIndication(mountPointNodeName, ne);

					LOG.info("Starting Event listener on Netconf device :: Name : {} finished", mountPointNodeName);
				}
			}
		}
	}

    /**
     * Mountpoint created or existing. Managed device is actually disconnected from node/ mountpoint.
     * Origin state: Connecting, Connected
     * Target state: are UnableToConnect or Connecting
     * @param action create or update
     * @param nNodeId id of the mountpoint
     * @param nNode mountpoint contents
     */
    public void enterNonConnectedState(Action action, NodeId nNodeId, NetconfNode nNode) {
        String mountPointNodeName = nNodeId.getValue();
        ConnectionStatus csts = nNode.getConnectionStatus();
        if (isNetconfNodeMaster(nNode)) {
        	sendUpdateNotification(mountPointNodeName, csts);
        }

        // Handling if mountpoint exist. connected -> connecting/UnableToConnect
        stopListenerOnNodeForConnectedState(mountPointNodeName);

        deviceMonitor.deviceDisconnectIndication(mountPointNodeName);

    }

    /**
     * Mountpoint removed indication.
     * @param nNodeId id of the mountpoint
     */
    public void removeMountpointState(NodeId nNodeId) {
        String mountPointNodeName = nNodeId.getValue();
        LOG.info("mountpointNodeRemoved {}", nNodeId.getValue());

        stopListenerOnNodeForConnectedState(mountPointNodeName);
        deviceMonitor.removeMountpointIndication(mountPointNodeName);
        if (odlEventListener != null) {
            odlEventListener.deRegistration(mountPointNodeName);
        }
    }

    /**
     * Do all tasks necessary to move from mountpoint state connected -> connecting
     * @param mountPointNodeName provided
     * @param ne representing the device connected to mountpoint
     */
    private void stopListenerOnNodeForConnectedState( String mountPointNodeName) {
        ONFCoreNetworkElementRepresentation ne = networkElementRepresentations.remove(mountPointNodeName);
        if (ne != null) {
            this.maintenanceService.deleteIfNotRequired(mountPointNodeName);
            int problems = ne.removeAllCurrentProblemsOfNode();
            LOG.debug("Removed all {} problems from database at deregistration for {}", problems, mountPointNodeName);
            if (performanceManager != null) {
                performanceManager.deRegistration(mountPointNodeName);
            }
            if (aaiProviderClient != null) {
                aaiProviderClient.onDeviceUnregistered(mountPointNodeName);
            }
        }
    }

    private void sendUpdateNotification(String mountPointNodeName, ConnectionStatus csts) {
        LOG.info("enter Non ConnectedState for device :: Name : {} ConnectionStatus {}", mountPointNodeName, csts);
        if (odlEventListener != null) {
        	odlEventListener.updateRegistration(mountPointNodeName, ConnectionStatus.class.getSimpleName(), csts != null ? csts.getName() : "null");
        }
    }

    /**
     * Handle netconf/mountpoint changes
     */
    @Override
    public void netconfChangeHandler(Action action, @Nullable ConnectionStatus csts, NodeId nodeId, NetconfNode nNode) {

		ClusteredConnectionStatus ccsts = nNode.getClusteredConnectionStatus();
		String nodeIdString = nodeId.getValue();
		if (action == Action.CREATE) {
	        if (odlEventListener != null) {
	        	odlEventListener.registration(nodeIdString);
	        }
		}
		boolean isCluster = akkaConfig == null && akkaConfig.isCluster();
		if (isCluster && ccsts == null) {
			LOG.debug("NETCONF Node {} {} does not provide cluster status. Stop execution.", nodeIdString, action);
		} else {
			switch (action) {
			case REMOVE:
				removeMountpointState(nodeId); // Stop Monitor
				break;

			case UPDATE:
			case CREATE:
				if (csts != null) {
					switch (csts) {
					case Connected: {
						startListenerOnNodeForConnectedState(action, nodeId, nNode);
						break;
					}
					case UnableToConnect:
					case Connecting: {
						enterNonConnectedState(action, nodeId, nNode);
						break;
					}
					}
				} else {
					LOG.debug("NETCONF Node handled with null status for action", action);
				}
				break;
			}
		}
    }

    /*-------------------------------------------------------------------------------------------
     * Functions
     */

    public ArchiveCleanService getArchiveCleanService() {
        return this.archiveCleanService;
    }

    public HtDatabaseEventsService getDatabaseClientEvents() {
        return databaseClientEvents;
    }

    public IndexMwtnService getMwtnService() {
        return mwtnService;
    }

    /**
     * Async RPC Interface implementation
     */
    @Override
    public @Nonnull List<String> doClearCurrentFaultByNodename(@Nullable List<String> nodeNamesInput)
            throws IllegalStateException {

        if (this.databaseClientEvents == null) {
            throw new IllegalStateException("dbEvents service not instantiated");
        }

        if (threadDoClearCurrentFaultByNodename != null && threadDoClearCurrentFaultByNodename.isAlive()) {
            throw new IllegalStateException("A clear task is already active");
        } else {

            // Create list of mountpoints if input is empty, using the content in ES
            if (nodeNamesInput == null || nodeNamesInput.size() <= 0) {
                nodeNamesInput = this.databaseClientEvents.getAllNodesWithCurrentAlarms();
            }

            // Filter all mountpoints from input that were found and are known to this Cluster-node instance of
            // DeviceManager
            final List<String> nodeNamesHandled = new ArrayList<>();
            for (String mountpointName : nodeNamesInput) {
                LOG.info("Work with mountpoint {}", mountpointName);

                if (odlEventListener != null && mountpointName.equals(odlEventListener.getOwnKeyName())) {

                    // SDN Controller related alarms
                    // -- can not be recreated on all nodes in connected state
                    // -- would result in a DCAE/AAI Notification
                    // Conclusion for 1810 Delivery ... not covered by RPC function (See issue #43)
                    LOG.info("Ignore SDN Controller related alarms for {}", mountpointName);
                    // this.databaseClientEvents.clearFaultsCurrentOfNode(mountpointName);
                    // nodeNamesHandled.add(mountpointName);

                } else {

                    if (mountPointService != null) {
                        InstanceIdentifier<Node> instanceIdentifier =
                                NETCONF_TOPO_IID.child(Node.class, new NodeKey(new NodeId(mountpointName)));
                        Optional<MountPoint> optionalMountPoint = mountPointService.getMountPoint(instanceIdentifier);

                        if (!optionalMountPoint.isPresent()) {
                            LOG.info("Remove Alarms for unknown mountpoint {}", mountpointName);
                            this.databaseClientEvents.clearFaultsCurrentOfNode(mountpointName);
                            nodeNamesHandled.add(mountpointName);
                        } else {
                            if (networkElementRepresentations.containsKey(mountpointName)) {
                                LOG.info("At node known mountpoint {}", mountpointName);
                                nodeNamesHandled.add(mountpointName);
                            } else {
                                LOG.info("At node unknown mountpoint {}", mountpointName);
                            }
                        }
                    }
                }
            }

            // Force a sync
            this.deviceMonitor.refreshAlarmsInDb();

            threadDoClearCurrentFaultByNodename = new Thread(() -> {
                refreshCounter++;
                LOG.info("Start refresh mountpoint task {}", refreshCounter);
                // for(String nodeName:nodeNamesOutput) {
                for (String nodeName : nodeNamesHandled) {
                    ONFCoreNetworkElementRepresentation ne = networkElementRepresentations.get(nodeName);
                    if (ne != null) {
                        LOG.info("Refresh mountpoint {}", nodeName);
                        ne.initialReadFromNetworkElement();
                    } else {
                        LOG.info("Unhandled mountpoint {}", nodeName);
                    }
                }
                LOG.info("End refresh mountpoint task {}", refreshCounter);
            });
            threadDoClearCurrentFaultByNodename.start();
            return nodeNamesHandled;
        }
    };

    /**
     * Indication if init() of devicemanager successfully done.
     *
     * @return true if init() was sucessfull. False if not done or not successfull.
     */
    public boolean isDevicemanagerInitializationOk() {
        return this.devicemanagerInitializationOk;
    }

    /**
     * Get initialization status of database.
     *
     * @return true if fully initialized false if not
     */
    public boolean isDatabaseInitializationFinished() {
        return htDatabase == null ? false : htDatabase.getInitialized();
    }

    /*---------------------------------------------------------------------
     * Private funtions
     */

    /**
     * Do the stream creation for the device.
     *
     * @param mountPointNodeName
     * @param mountPoint
     */
    private void registerNotificationStream(String mountPointNodeName, MountPoint mountPoint, String streamName) {

        final Optional<RpcConsumerRegistry> optionalRpcConsumerService =
                mountPoint.getService(RpcConsumerRegistry.class);
        if (optionalRpcConsumerService.isPresent()) {
            final RpcConsumerRegistry rpcConsumerRegitry = optionalRpcConsumerService.get();
            final NotificationsService rpcService = rpcConsumerRegitry.getRpcService(NotificationsService.class);
            if (rpcService == null) {
                LOG.warn("rpcService is null for mountpoint {}", mountPointNodeName);
            } else {
                final CreateSubscriptionInputBuilder createSubscriptionInputBuilder =
                        new CreateSubscriptionInputBuilder();
                createSubscriptionInputBuilder.setStream(new StreamNameType(streamName));
                LOG.info("Event listener triggering notification stream {} for node {}", streamName,
                        mountPointNodeName);
                try {
                    CreateSubscriptionInput createSubscriptionInput = createSubscriptionInputBuilder.build();
                    if (createSubscriptionInput == null) {
                        LOG.warn("createSubscriptionInput is null for mountpoint {}", mountPointNodeName);
                    } else {
                        rpcService.createSubscription(createSubscriptionInput);
                    }
                } catch (NullPointerException e) {
                    LOG.warn("createSubscription failed");
                }
            }
        } else {
            LOG.warn("No RpcConsumerRegistry avaialble.");
        }

    }

    /**
     * Get NE object
     *
     * @param mountpoint mount point name
     * @return null or NE specific data
     */
    public @Nullable ONFCoreNetworkElementRepresentation getNeByMountpoint(String mountpoint) {

        return networkElementRepresentations.get(mountpoint);

    }

    /* -- LOG related functions -- */


    private boolean isInClusterMode() {
        return this.akkaConfig == null ? false : this.akkaConfig.isCluster();
    }

    private String getClusterNetconfNodeName() {
        return this.akkaConfig == null ? "" : this.akkaConfig.getClusterConfig().getClusterSeedNodeName("abc");
    }

    private boolean isNetconfNodeMaster(NetconfNode nnode) {
        if (isInClusterMode()) {
            LOG.debug("check if me is responsible for node");
            String masterNodeName = nnode.getClusteredConnectionStatus() == null ? "null"
                    : nnode.getClusteredConnectionStatus().getNetconfMasterNode();
            String myNodeName = getClusterNetconfNodeName();
            LOG.debug("sdnMasterNode=" + masterNodeName + " and sdnMyNode=" + myNodeName);
            if (!masterNodeName.equals(myNodeName)) {
                LOG.debug("netconf change but me is not master for this node");
                return false;
            }
        }
        return true;
    }


    private void sleepMs(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            LOG.debug("Interrupted sleep");
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }

}
