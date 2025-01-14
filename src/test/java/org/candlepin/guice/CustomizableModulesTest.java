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
package org.candlepin.guice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.candlepin.config.Configuration;
import org.candlepin.config.ConfigurationException;
import org.candlepin.config.PropertiesFileConfiguration;

import com.google.inject.Module;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.Set;

public class CustomizableModulesTest {

    private Configuration loadConfig(String filename) throws URISyntaxException, ConfigurationException {
        PropertiesFileConfiguration config = new PropertiesFileConfiguration();

        String path = this.getClass().getResource(filename).toURI().getPath();
        config.load(path);

        return config;
    }

    @Test
    public void shouldLoadAndParseConfigurationFile() throws Exception {
        Configuration config = this.loadConfig("customizable_modules_test.conf");
        Set<Module> loaded = new CustomizableModules().load(config);

        assertEquals(1, loaded.size());
        assertTrue(loaded.iterator().next() instanceof DummyModuleForTesting);
    }

    @Test
    public void shouldFailWhenConfigurationContainsMissingClass() throws Exception {
        Configuration config = this.loadConfig("customizable_modules_with_missing_class.conf");

        // TODO: We should probably be more specific...
        assertThrows(RuntimeException.class, () -> new CustomizableModules().load(config));
    }

}
