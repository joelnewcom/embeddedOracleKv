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

package oracle.kv.impl.api.table;

import java.util.List;
import java.util.Map;

/**
 * A TableChange to add an index.
 */
class AddIndex extends TableChange {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String description;
    private final String tableName;
    private final List<String> fields;
    private final Map<String,String> annotations;
    private final Map<String,String> properties;

    AddIndex(IndexImpl index, int seqNum) {
        super(seqNum);
        name = index.getName();
        description = index.getDescription();
        tableName = index.getTable().getFullName();
        fields = index.getFieldsInternal();
        annotations = index.getAnnotationsInternal();
        properties = index.getProperties();
    }

    @Override
    boolean apply(TableMetadata md) {
        if (annotations == null) {
            md.insertIndex(name, tableName, fields, description);
        } else {
            md.insertTextIndex
                (name, tableName, fields, annotations, properties, description);
        }
        return true;
    }
}
