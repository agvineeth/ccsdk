/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.database.types.equipment;

import org.onap.ccsdk.features.sdnr.wt.database.base.EsObject;

/**
 * @author hanif
 *
 */
public class EsBaseRequireNetworkElement extends EsObject {
    public static final String ESDATATYPENAME = "required-networkelement";

    public EsBaseRequireNetworkElement() {

    }

    public EsBaseRequireNetworkElement(String mountpountName) {
        super();
        this.setEsId(mountpountName);
    }
}