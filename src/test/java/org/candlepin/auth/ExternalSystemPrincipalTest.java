/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.auth;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExternalSystemPrincipalTest {

    private ExternalSystemPrincipal principal;

    @BeforeEach
    public void init() {
        principal = new ExternalSystemPrincipal();
    }

    @Test
    public void name() {
        assertEquals("External System", principal.getPrincipalName());
    }

    @Test
    public void type() {
        assertEquals("system", principal.getType());
    }

    @Test
    public void fullAccess() {
        assertTrue(principal.hasFullAccess());
    }
}
