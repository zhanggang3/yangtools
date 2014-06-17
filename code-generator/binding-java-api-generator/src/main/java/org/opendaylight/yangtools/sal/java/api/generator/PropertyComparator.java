/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.sal.java.api.generator;

import java.util.Comparator;

import org.opendaylight.yangtools.sal.binding.model.api.GeneratedProperty;

public class PropertyComparator implements Comparator<GeneratedProperty> {

    @Override
    public int compare(GeneratedProperty p1, GeneratedProperty p2) {
        return p1.getName().compareTo(p2.getName());
    }

}