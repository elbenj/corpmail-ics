/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elbenj.emailcommon.provider;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Unit tests for the HostAuth inner class.
 * These tests must be locally complete - no server(s) required.
 */
@SmallTest
public class HostAuthTests extends AndroidTestCase {

    /**
     * Test user name and password are set correctly
     */
    public void testSetLogin() {
        HostAuth ha = new HostAuth();
        ha.setLogin("user:password");
        assertEquals("user", ha.mLogin);
        assertEquals("password", ha.mPassword);

        // special characters are not removed during insertion
        ha.setLogin("%20us%20er%20:password");
        assertEquals("%20us%20er%20", ha.mLogin);
        assertEquals("password", ha.mPassword);

        // special characters are not removed during insertion
        ha.setLogin("user:%20pass%20word%20");
        assertEquals("user", ha.mLogin);
        assertEquals("%20pass%20word%20", ha.mPassword);

        ha.setLogin("user:");
        assertEquals("user", ha.mLogin);
        assertEquals("", ha.mPassword);

        ha.setLogin(":password");
        assertEquals("", ha.mLogin);
        assertEquals("password", ha.mPassword);

        ha.setLogin("");
        assertNull(ha.mLogin);
        assertNull(ha.mPassword);

        ha.setLogin(null);
        assertNull(ha.mLogin);
        assertNull(ha.mPassword);

        ha.setLogin("userpassword");
        assertEquals("userpassword", ha.mLogin);
        assertNull(ha.mPassword);
    }

    /**
     * Test the authentication flag is set correctly when setting user name and password
     */
    public void testSetLoginAuthenticate() {
        HostAuth ha = new HostAuth();

        ha.mFlags = 0x00000000;
        ha.setLogin("user", "password");
        assertEquals(HostAuth.FLAG_AUTHENTICATE, ha.mFlags);

        ha.mFlags = 0x00000000;
        ha.setLogin("user", "");
        assertEquals(HostAuth.FLAG_AUTHENTICATE, ha.mFlags);

        ha.mFlags = 0x00000000;
        ha.setLogin("", "password");
        assertEquals(HostAuth.FLAG_AUTHENTICATE, ha.mFlags);

        ha.mFlags = 0x00000000;
        ha.setLogin("user", null);
        assertEquals(HostAuth.FLAG_AUTHENTICATE, ha.mFlags);

        ha.mFlags = 0xffffffff;
        ha.setLogin(null, "password");
        assertEquals(~HostAuth.FLAG_AUTHENTICATE, ha.mFlags);

        ha.mFlags = 0xffffffff;
        ha.setLogin(null, null);
        assertEquals(~HostAuth.FLAG_AUTHENTICATE, ha.mFlags);
    }

    /**
     * Test setting the connection using a protocol and flags
     */
    public void testSetConnectionFlags() {
        HostAuth ha = new HostAuth();

        // Different port types don't affect flags
        ha.setConnection("imap", "server", 123, 0);
        assertEquals(0, ha.mFlags);
        ha.setConnection("imap", "server", -1, 0);
        assertEquals(0, ha.mFlags);

        // Different protocol types don't affect flags
        ha.setConnection("pop3", "server", 123, 0);
        assertEquals(0, ha.mFlags);
        ha.setConnection("pop3", "server", -1, 0);
        assertEquals(0, ha.mFlags);
        ha.setConnection("eas", "server", 123, 0);
        assertEquals(0, ha.mFlags);
        ha.setConnection("eas", "server", -1, 0);
        assertEquals(0, ha.mFlags);
        ha.setConnection("smtp", "server", 123, 0);
        assertEquals(0, ha.mFlags);
        ha.setConnection("smtp", "server", -1, 0);
        assertEquals(0, ha.mFlags);

        // Sets SSL flag
        ha.setConnection("imap", "server", HostAuth.PORT_UNKNOWN, HostAuth.FLAG_SSL);
        assertEquals(HostAuth.FLAG_SSL, ha.mFlags);

        // Sets SSL+Trusted flags
        ha.setConnection("imap", "server", HostAuth.PORT_UNKNOWN,
                HostAuth.FLAG_SSL | HostAuth.FLAG_TRUST_ALL);
        assertEquals(HostAuth.FLAG_SSL | HostAuth.FLAG_TRUST_ALL, ha.mFlags);

        // Sets TLS flag
        ha.setConnection("imap", "server", HostAuth.PORT_UNKNOWN, HostAuth.FLAG_TLS);
        assertEquals(HostAuth.FLAG_TLS, ha.mFlags);

        // Sets TLS+Trusted flags
        ha.setConnection("imap", "server", HostAuth.PORT_UNKNOWN,
                HostAuth.FLAG_TLS | HostAuth.FLAG_TRUST_ALL);
        assertEquals(HostAuth.FLAG_TLS | HostAuth.FLAG_TRUST_ALL, ha.mFlags);

        // Test other defined flags; should not affect mFlags
        ha.setConnection("imap", "server", HostAuth.PORT_UNKNOWN, HostAuth.FLAG_AUTHENTICATE);
        assertEquals(0, ha.mFlags);

        // Test every other bit; should not affect mFlags
        ha.setConnection("imap", "server", HostAuth.PORT_UNKNOWN, 0xfffffff4);
        assertEquals(0, ha.mFlags);
    }

    public void testSetConnectionWithCerts() {
        HostAuth ha = new HostAuth();

        ha.setConnection("eas", "server", HostAuth.PORT_UNKNOWN, HostAuth.FLAG_SSL, "client-cert");
        assertEquals(HostAuth.FLAG_SSL, ha.mFlags);
        assertEquals("client-cert", ha.mClientCertAlias);

        ha.setConnection("eas", "server", HostAuth.PORT_UNKNOWN, HostAuth.FLAG_TLS, "client-cert");
        assertEquals(HostAuth.FLAG_TLS, ha.mFlags);
        assertEquals("client-cert", ha.mClientCertAlias);

        // Note that we can still trust all server certificates, even if we present a client
        // user certificate.
        ha.setConnection("eas", "server", HostAuth.PORT_UNKNOWN,
                HostAuth.FLAG_SSL | HostAuth.FLAG_TRUST_ALL, "client-cert");
        assertEquals(HostAuth.FLAG_SSL | HostAuth.FLAG_TRUST_ALL, ha.mFlags);
        assertEquals("client-cert", ha.mClientCertAlias);

        try {
            ha.setConnection(
                    "eas", "server", HostAuth.PORT_UNKNOWN, 0 /* no flags */, "client-cert");
            fail("Shouldn't be able to set a client certificate on an unsecure connection");
        } catch (IllegalArgumentException expected) {
        }
    }
}

