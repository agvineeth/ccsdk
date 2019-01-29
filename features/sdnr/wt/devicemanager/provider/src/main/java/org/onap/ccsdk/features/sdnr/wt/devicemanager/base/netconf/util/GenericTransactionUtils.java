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
package org.onap.ccsdk.features.sdnr.wt.devicemanager.base.netconf.util;

import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;

public final class GenericTransactionUtils {
    static final Logger LOG = LoggerFactory.getLogger(GenericTransactionUtils.class);

    public static <T extends DataObject> boolean writeData(DataBroker dataBroker,
            LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> iid, T dataObject, boolean isAdd) {
        Preconditions.checkNotNull(dataBroker);
        WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
        if (isAdd) {
            if (dataObject == null) {
                LOG.warn("Invalid attempt to add a non-existent object to path {}", iid);
                return false;
            }
            modification.merge(logicalDatastoreType, iid, dataObject, true /* createMissingParents */);
        } else {
            modification.delete(LogicalDatastoreType.CONFIGURATION, iid);
        }
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.checkedGet();
            LOG.debug("Transaction success for {} of object {}", isAdd ? "add" : "delete", dataObject);
            return true;
        } catch (Exception e) {
            LOG.warn("Transaction failed with error {} for {} of object {}", e.getMessage(), isAdd ? "add" : "delete",
                    dataObject);
            modification.cancel();
            return false;
        }
    }

    /**
     * Deliver the data back or null. Warning
     *
     * @param <T> SubType of the DataObject to be handled
     * @param dataBroker for accessing data
     * @param dataStoreType to address datastore
     * @param iid id to access data
     * @return null or object
     */
    @Nullable
    public static <T extends DataObject> T readData(DataBroker dataBroker, LogicalDatastoreType dataStoreType,
            InstanceIdentifier<T> iid) {

        AtomicBoolean noErrorIndication = new AtomicBoolean();
        AtomicReference<String> statusText = new AtomicReference<>();

        T obj = readDataOptionalWithStatus(dataBroker, dataStoreType, iid, noErrorIndication, statusText);

        if (!noErrorIndication.get()) {
            LOG.warn("Read transaction for identifier " + iid + " failed with status " + statusText.get());
        }

        return obj;
    }

    /**
     * Deliver the data back or null
     *
     * @param <T> SubType of the DataObject to be handled
     * @param dataBroker for accessing data
     * @param dataStoreType to address datastore
     * @param iid id to access data
     * @param noErrorIndication (Output) true if data could be read and are available and is not null
     * @param statusIndicator (Output) String with status indications during the read.
     * @return null or object
     */
    @Nullable
    public static <T extends DataObject> T readDataOptionalWithStatus(DataBroker dataBroker,
            LogicalDatastoreType dataStoreType, InstanceIdentifier<T> iid, AtomicBoolean noErrorIndication,
            AtomicReference<String> statusIndicator) {

        T data = null;
        noErrorIndication.set(false);

        statusIndicator.set("Preconditions");
        Preconditions.checkNotNull(dataBroker);

        int retry = 0;
        int retryDelayMilliseconds = 2000;
        int maxRetries = 5; // 0 no Retry

        do {
            if (retry > 0) {
                try {
                    LOG.debug("Sleep {}ms", retryDelayMilliseconds);
                    Thread.sleep(retryDelayMilliseconds);
                } catch (InterruptedException e) {
                    LOG.debug("Sleep interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }

            LOG.debug("Sending message with retry {} ", retry);
            statusIndicator.set("Create Read Transaction");

            try (ReadOnlyTransaction readTransaction = dataBroker.newReadOnlyTransaction();) {
                CheckedFuture<Optional<T>, ReadFailedException> od = readTransaction.read(dataStoreType, iid);
                statusIndicator.set("Read done");
                if (od != null) {
                    statusIndicator.set("Unwrap checkFuture done");
                    Optional<T> optionalData = od.get();
                    if (optionalData != null) {
                        statusIndicator.set("Unwrap optional done");
                        data = optionalData.orNull();
                        statusIndicator.set("Read transaction done");
                        noErrorIndication.set(true);
                    }
                }

                readTransaction.close();
            } catch (CancellationException | ExecutionException | InterruptedException | NoSuchElementException e) {
                statusIndicator.set(ExceptionUtils.getStackTrace(e));
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }

        } while (noErrorIndication.get() == false && retry++ < maxRetries);

        return data;
    }


}
