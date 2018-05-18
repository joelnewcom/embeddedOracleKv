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

package oracle.kv;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Username/password credentials.  This class provides the standard mechanism
 * for an application to authenticate as a particular user when accessing a
 * KVStore instance.  The object contains sensitive information and should be
 * kept private.  When no longer needed the user should call clear() to erase
 * the internal password information.
 *
 * @since 3.0
 */
public class PasswordCredentials implements LoginCredentials, Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private char[] password;

    /**
     * Creates a username/password credential set.   The password passed in is
     * copied internal to the object.  For maximum security, it is recommended
     * that you call the {@link #clear()} method when you are done with the
     * object to avoid have the password being present in the Java memory heap.
     *
     * @param username the name of the user
     * @param password the password of the user
     * @throws IllegalArgumentException if either username or password
     * have null values.
     */
    public PasswordCredentials(String username, char[] password)
        throws IllegalArgumentException {

        if (username == null) {
            throw new IllegalArgumentException(
                "The username argument must not be null");
        }
        if (password == null) {
            throw new IllegalArgumentException(
                "The password argument must not be null");
        }
        this.username = username;
        this.password = Arrays.copyOf(password, password.length);
    }

    /**
     * @see LoginCredentials#getUsername()
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password. This returns a copy of the password. The caller should
     * clear the returned memory when the value is no longer needed.
     *
     * @return The password for the user.
     */
    public char[] getPassword() {
        return Arrays.copyOf(password, password.length);
    }

    /**
     * Wipes out the password storage to ensure it does not hang around in
     * in the Java VM memory space.
     */
    public void clear() {
        Arrays.fill(password, ' ');
    }
}
