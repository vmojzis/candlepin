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
package org.candlepin.pki;

import org.candlepin.config.ConfigProperties;
import org.candlepin.config.Configuration;
import org.candlepin.config.MapConfiguration;
import org.candlepin.pki.impl.JSSPrivateKeyReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;


public class CertificateReaderTest {

    @Test
    public void readKey() throws Exception {
        final ClassLoader loader = getClass().getClassLoader();
        final String caCert = loader.getResource("certs/test.crt").toURI().getPath();
        final String caCertUpstream = loader.getResource("certs/upstream").toURI().getPath();
        final String caKey = loader.getResource("keys/pkcs1-des-encrypted.pem").toURI().getPath();
        final Configuration config = new MapConfiguration(
            new HashMap<>() {
                {
                    put(ConfigProperties.CA_CERT, caCert);
                    put(ConfigProperties.CA_CERT_UPSTREAM, caCertUpstream);
                    put(ConfigProperties.CA_KEY, caKey);
                    put(ConfigProperties.CA_KEY_PASSWORD, "password");
                }
            });

        Assertions.assertDoesNotThrow(() -> new CertificateReader(config, new JSSPrivateKeyReader()));
    }
}
