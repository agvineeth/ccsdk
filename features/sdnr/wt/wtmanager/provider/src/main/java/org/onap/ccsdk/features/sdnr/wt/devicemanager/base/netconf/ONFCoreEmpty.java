/**
 *
 */
package org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf;

import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.internalTypes.InventoryInformation;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.container.AllPm;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;

/**
 * @author herbert
 *
 */
public class ONFCoreEmpty implements ONFCoreNetworkElementRepresentation {

    private String mountPointNodeName = "";


    ONFCoreEmpty(String mountPointNodeName) {
        this.mountPointNodeName = mountPointNodeName;
    }

    @Override
    public void initialReadFromNetworkElement() {
    }

    @Override
    public String getMountPointNodeName() {
        return mountPointNodeName;
    }

    @Override
    public void resetPMIterator() {
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public void next() {
    }

    @Override
    public AllPm getHistoricalPM() {
        return AllPm.EMPTY;
    }

    @Override
    public String pmStatusToString() {
        return "ONFCoreEmpty";
    }

    @Override
    public int removeAllCurrentProblemsOfNode() {
        return 0;
    }

    @Override
    public void doRegisterMicrowaveEventListener(MountPoint mointPoint) {
        //Do nothing
    }

    @Override
    public void prepareCheck() {
        //Do nothing here
    }

    @Override
    public boolean checkIfConnectionToMediatorIsOk() {
        return false;
    }

    @Override
    public boolean checkIfConnectionToNeIsOk() {
        return false;
    }

    @Override
    public void initSynchronizationExtension() {
    }

	@Override
	public InventoryInformation getInventoryInformation() {
		return InventoryInformation.DEFAULT;
	}

	@Override
	public InventoryInformation getInventoryInformation(String layerProtocolFilter) {
		return InventoryInformation.DEFAULT;
	}

}
