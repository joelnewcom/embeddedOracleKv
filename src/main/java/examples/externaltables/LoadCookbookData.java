/*-
 * Copyright (C) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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

package externaltables;

import java.util.Iterator;

import oracle.kv.Direction;
import oracle.kv.FaultException;
import oracle.kv.KVStore;
import oracle.kv.KVStoreConfig;
import oracle.kv.KVStoreFactory;
import oracle.kv.Key;

/**
 * A class used in the External Tables Cookbook example to create sample
 * records in the NoSQL Database.
 */
public final class LoadCookbookData {

    private final KVStore store;

    private long nOps = 10;

    private boolean deleteExisting = false;

    static final String USER_OBJECT_TYPE = "user";

    public static void main(final String[] args) {
        try {
            final LoadCookbookData loadData = new LoadCookbookData(args);
            loadData.run();
        } catch (FaultException e) {
        	e.printStackTrace();
        	System.out.println("Please make sure a store is running.");
            System.out.println("The error could be caused by a security " +
                    "mismatch. If the store is configured secure, you " +
                    "should specify a user login file " +
                    "with system property oracle.kv.security. " +
                    "For example, \n" +
                    "\tjava -Doracle.kv.security=<user security login file> " +
                    "externaltables.LoadCookbookData\n" +
                    "KVLite generates the security file in " +
                    "$KVHOME/kvroot/security/user.security ");
        } catch (Exception e) {
            e.printStackTrace();
            
        }
    }

    /**
     * Parses command line args and opens the KVStore.
     */
    private LoadCookbookData(final String[] argv) {

        String storeName = "";
        String hostName = "";
        String hostPort = "";

        final int nArgs = argv.length;
        int argc = 0;

        if (nArgs == 0) {
            usage(null);
        }

        while (argc < nArgs) {
            final String thisArg = argv[argc++];

            if ("-store".equals(thisArg)) {
                if (argc < nArgs) {
                    storeName = argv[argc++];
                } else {
                    usage("-store requires an argument");
                }
            } else if ("-host".equals(thisArg)) {
                if (argc < nArgs) {
                    hostName = argv[argc++];
                } else {
                    usage("-host requires an argument");
                }
            } else if ("-port".equals(thisArg)) {
                if (argc < nArgs) {
                    hostPort = argv[argc++];
                } else {
                    usage("-port requires an argument");
                }
            } else if ("-nops".equals(thisArg)) {
                if (argc < nArgs) {
                    nOps = Long.parseLong(argv[argc++]);
                } else {
                    usage("-nops requires an argument");
                }
            } else if ("-delete".equals(thisArg)) {
                deleteExisting = true;
            } else {
                usage("Unknown argument: " + thisArg);
            }
        }

        store = KVStoreFactory.getStore
            (new KVStoreConfig(storeName, hostName + ":" + hostPort));
    }

    private void usage(final String message) {
        if (message != null) {
            System.out.println("\n" + message + "\n");
        }

        System.out.println("usage: " + getClass().getName());
        System.out.println
            ("\t-store <instance name>\n" +
             "\t-host <host name>\n" +
             "\t-port <port number>\n" +
             "\t-nops <total records to create>\n" +
             "\t-delete (default: false) [delete all existing data]\n");
        System.exit(1);
    }

    private void run() {
        if (deleteExisting) {
            deleteExistingData();
        }

        doLoad();
    }

    private void doLoad() {
        for (long i = 0; i < nOps; i++) {
            addUser(i);
        }
        store.close();
    }

    private void addUser(final long i) {
        final String email = "user" + i + "@example.com";

        final UserInfo userInfo = new UserInfo(email);
        final String gender = (i % 2 == 0) ? "F" : "M";
        final int mod = (int) (i % 10);

        /* Pad the number for nicer column alignment. */
        String iStr = String.format("%03d", i);
        userInfo.setGender(gender);
        userInfo.setName((("F".equals(gender)) ? "Ms." : "Mr.") +
                         " Number-" + iStr);
        userInfo.setAddress(iStr + " Example St, Example Town, AZ");
        userInfo.setPhone("000.000.0000".replace('0', (char) ('0' + mod)));

        store.putIfAbsent(userInfo.getStoreKey(), userInfo.getStoreValue());
    }

    private void deleteExistingData() {

        /*
         * The simple Key "user" is a prefix for all user Keys and can be used
         * as the parentKey for querying all user Key/Value pairs.
         */
        final Key userTypeKey = Key.createKey(USER_OBJECT_TYPE);

        /*
         * Create an iterator over all user Keys.  The direction parameter is
         * Direction.UNORDERED because ordering is not necessary and in fact
         * not currently supported by the storeKeysIterator method.  The
         * batchSize is zero to use a default iterator batch size.  The
         * subRange is null to select all children Keys.  The depth is null to
         * select all descendant Keys.
         */
        final Iterator<Key> iter = store.storeKeysIterator
            (Direction.UNORDERED, 0 /*batchSize*/, userTypeKey,
             null /*subRange*/, null /*depth*/);

        long cnt = 0;
        while (iter.hasNext()) {
            final Key key = iter.next();
            store.delete(key);
            cnt++;
        }

        System.out.println(cnt + " records deleted");
    }
}
