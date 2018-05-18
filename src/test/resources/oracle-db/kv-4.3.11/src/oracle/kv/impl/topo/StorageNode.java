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

package oracle.kv.impl.topo;

import oracle.kv.impl.topo.ResourceId.ResourceType;
import oracle.kv.impl.topo.Topology.Component;

import com.sleepycat.persist.model.Persistent;

/**
 * The SN topology component as created by the GAT and maintained by it.
 */
@Persistent
public class StorageNode extends Topology.Component<StorageNodeId>
    implements Comparable<StorageNode> {

    private static final long serialVersionUID = 1L;

    /**
     * The data center that hosts this SN.
     */
    private DatacenterId datacenterId;

    /**
     * The hostname/interface used for KVS communications.
     */
    private String hostname;

    /**
     * The registry port used by the RMI registry on this SN
     */
    private int registryPort;

    public StorageNode(Datacenter datacenter,
                       String hostname,
                       int registryPort) {
        this.datacenterId = datacenter.getResourceId();
        this.hostname = hostname;
        this.registryPort = registryPort;
    }

    private StorageNode(StorageNode storageNode) {
        super(storageNode);
        datacenterId = storageNode.datacenterId;
        hostname = storageNode.hostname;
        registryPort = storageNode.registryPort;
    }

    @SuppressWarnings("unused")
    private StorageNode() {
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.Topology.Component#getResourceType()
     */
    @Override
    public ResourceType getResourceType() {
        return ResourceType.STORAGE_NODE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result +
            ((datacenterId == null) ? 0 : datacenterId.hashCode());
        result = prime * result +
            ((hostname == null) ? 0 : hostname.hashCode());
        result = prime * result + registryPort;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        return propertiesEquals((StorageNode) obj);
    }

    /**
     * @return true if this storage node already exists in the topology,
     * excluding any comparison of resource id and sequence number.
     */
    public boolean propertiesEquals(StorageNode other) {
        if (datacenterId == null) {
            if (other.datacenterId != null) {
                return false;
            }
        } else if (!datacenterId.equals(other.datacenterId)) {
            return false;
        }

        if (hostname == null) {
            if (other.hostname != null) {
                return false;
            }
        } else if (!hostname.equals(other.hostname)) {
            return false;
        }
        if (registryPort != other.registryPort) {
            return false;
        }
        return true;
    }

    /**
     * Returns the datacenter id for the datacenter hosting the storage node.
     */
    public DatacenterId getDatacenterId() {
        return datacenterId;
    }

    /**
     * Returns the hostname associated with the StorageNode.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Returns the registry port associated with the SN.
     */
    public int getRegistryPort() {
        return registryPort;
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.Topology.Component#clone()
     */
    @Override
    public Component<?> clone() {
        return new StorageNode(this);
    }

    @Override
    public StorageNodeId getStorageNodeId() {
        return getResourceId();
    }

    @Override
    public boolean isMonitorEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "[" + getResourceId() + "] " +
            DatacenterId.DATACENTER_PREFIX + ":[id=" + datacenterId +
            " name=" + getTopology().get(datacenterId).getName() + "] " +
            hostname + ":" + registryPort;
    }

    @Override
    public int compareTo(StorageNode other) {
        return getStorageNodeId().compareTo(other.getStorageNodeId());
    }
}
