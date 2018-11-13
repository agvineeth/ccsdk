/**
 * (c) 2017 highstreet technologies GmbH
 */

package org.opendaylight.mwtn.devicemonitor.impl;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.mwtn.devicemanager.impl.listener.ODLEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Implementation of concept "Active monitoring" of a device.<br>
 *    <br>
 *  For each existing mountpoint a task runs with 120s cycle time. Every 120 seconds the check actions are performed.
 *  The request is handled by the NETCONF layer with a (default)configured time-out of 60 seconds.<br>
 *  Generated alarms, by the object/node "SDN-Controller" are (enum DeviceMonitorProblems):<br>
 *      - notConnected(InternalSeverity.Warning)<br>
 *      - noConnectionMediator(InternalSeverity.Minor)<br>
 *      - noConnectionNe(InternalSeverity.Critical)<br>
 *    <br>
 *  1. Mountpoint does not exist<br>
 *  If the mountpoint does not exists there are no related current alarms in the database.<br>
 *    <br>
 *  2. Created mountpoint with state "Connecting" or "UnableToConnect"<br>
 *  If the Mountpoint is created and connection status is "Connecting" or "UnableToConnect".<br>
 *  - After about 2..4 Minutes ... raise alarm "notConnected" with severity warning<br>
 *    <br>
 *  3. Created mountpoint with state "Connection"<br>
 *  There are two monitor activities.<br>
 *      3a. Check of Mediator connection by requesting (typical) cached data.<br>
 *          - After about 60 seconds raise alarm: connection-loss-mediator with severity minor<br>
 *          - Request from Mediator: network-element<br>
 *    <br>
 *      3b. Check connection to NEby requesting (typical) non-cached data.<br>
 *          - Only if AirInterface available. The first one is used.<br>
 *          - Requested are the currentAlarms<br>
 *          - After about 60 seconds raise alarm: connection-loss-network-element with severity critical<br>
 *    <br>
 * @author herbert
 */

public class DeviceMonitorImpl implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceMonitorImpl.class);

    private final ConcurrentHashMap<String, DeviceMonitorTask> queue;
    private final ScheduledExecutorService scheduler;
    private final ODLEventListener odlEventListener;
    @SuppressWarnings("unused")
    private final DataBroker dataBroker; //Future usage

    /*-------------------------------------------------------------
     * Construction/ destruction of service
     */

    /**
     * Basic implementation of devicemonitoring
     * @param odlEventListener as destination for problems
     */
    public DeviceMonitorImpl(DataBroker dataBroker, ODLEventListener odlEventListener) {
        LOG.info("Construct {}", this.getClass().getSimpleName());

        this.odlEventListener = odlEventListener;
        this.dataBroker = dataBroker;
        this.queue = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(10);
    }

    /**
     * Stop the service. Stop all running monitoring tasks.
     */
    @Override
    synchronized public void close() {
        LOG.info("Close {}", this.getClass().getSimpleName());

        Enumeration<String> e = queue.keys();
        while (e.hasMoreElements()) {
            deviceDisconnectIndication(e.nextElement());
        }

        scheduler.shutdown();
    }

    /*-------------------------------------------------------------
     * Start/ stop/ update service for Mountpoint
     */

    /**
     * Notify of device state changes to "connected" for slave nodes
     * @param mountPointNodeName name of mount point
     */
    synchronized public void deviceConnectSlaveIndication(String mountPointNodeName) {
        deviceConnectMasterIndication(mountPointNodeName, null);
    }

    /**
     * Notify of device state changes to "connected"
     * @param mountPointNodeName name of mount point
     * @param ne to monitor
     */
    synchronized public void deviceConnectMasterIndication(String mountPointNodeName, DeviceMonitoredNe ne) {

        LOG.debug("ne changes to connected state {}",mountPointNodeName);
        createMonitoringTask(mountPointNodeName);
        if (queue.containsKey(mountPointNodeName)) {
            DeviceMonitorTask task = queue.get(mountPointNodeName);
            task.deviceConnectIndication(ne);
        } else {
            LOG.warn("Monitoring task not in queue: {} {} {}", mountPointNodeName, mountPointNodeName.hashCode(), queue.size());
        }
    }

   /**
    * Notify of device state change to "disconnected"
    * Mount point supervision
    * @param mountPointNodeName to deregister
    */
    synchronized public void deviceDisconnectIndication(String mountPointNodeName) {

        LOG.debug("State changes to not connected state {}",mountPointNodeName);
        createMonitoringTask(mountPointNodeName);
        if (queue.containsKey(mountPointNodeName)) {
            DeviceMonitorTask task = queue.get(mountPointNodeName);
            task.deviceDisconnectIndication();
        } else {
            LOG.warn("Monitoring task not in queue: {} {} {}", mountPointNodeName, mountPointNodeName.hashCode(), queue.size());
        }
    }

    /**
     * removeMountpointIndication deregisters a mountpoint for registration services
     * @param mountPointNodeName to deregister
     */
    synchronized public void removeMountpointIndication(String mountPointNodeName) {

        if (queue.containsKey(mountPointNodeName)) {
            DeviceMonitorTask task = queue.get(mountPointNodeName);
            //Remove from here
            queue.remove(mountPointNodeName);

            //Clear all problems
            task.removeMountpointIndication();
            LOG.debug("Task stopped: {}", mountPointNodeName);
        } else {
            LOG.warn("Task not in queue: {}", mountPointNodeName);
        }
    }

    /**
     * Referesh database by raising all alarms again.
     */
    public void refreshAlarmsInDb() {
        synchronized(queue) {
            for (DeviceMonitorTask task : queue.values()) {
                task.refreshAlarms();
            }
        }
    }

    /*-------------------------------------------------------------
     * Private functions
     */

    /**
     * createMountpoint registers a new mountpoint monitoring service
     * @param mountPointNodeName name of mountpoint
     */
    synchronized private DeviceMonitorTask createMonitoringTask(String mountPointNodeName) {

        DeviceMonitorTask task;
        LOG.debug("Register for monitoring {} {}",mountPointNodeName, mountPointNodeName.hashCode());

        if (queue.containsKey(mountPointNodeName)) {
            LOG.info("Monitoring task exists");
            task = queue.get(mountPointNodeName);
        } else {
            LOG.info("Do start of DeviceMonitor task");
            //Runnable task = new PerformanceManagerTask(queue, databaseService);
            task = new DeviceMonitorTask(mountPointNodeName, this.odlEventListener);
            queue.put(mountPointNodeName, task);
            task.start(scheduler);
        }
        return task;
    }

}
