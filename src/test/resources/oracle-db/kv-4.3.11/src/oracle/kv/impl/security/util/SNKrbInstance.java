/*-
 * Copyright (C) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This file was distributed by Oracle as part of a version of Oracle NoSQL
 * Database made available at:
 *
 * http://www.oracle.com/technetwork/database/database-technologies/nosqldb/downloads/index.html
 *
 * Please see the LICENSE file included in the top-level directory of the
 * appropriate version of Oracle NoSQL Database for a copy of the license and
 * additional information.
 */

package oracle.kv.impl.security.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import oracle.kv.impl.util.FastExternalizable;

/**
 * Kerberos principal instance name of a storage node.
 */
public class SNKrbInstance implements Serializable, FastExternalizable {

    private static final long serialVersionUID = 1L;

    private final String instanceName;
    private final int snId;

    public SNKrbInstance(String instanceName, int storageNodeId) {
        this.instanceName = instanceName;
        this.snId = storageNodeId;
    }

    /**
     * FastExternalizable constructor.
     */
    public SNKrbInstance(DataInput in,
                         @SuppressWarnings("unused") short serialVersion)
        throws IOException {

        this.instanceName = in.readUTF();
        this.snId = in.readInt();
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    public int getStorageNodeId() {
        return snId;
    }

    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        out.writeUTF(instanceName);
        out.writeInt(snId);
    }
}
