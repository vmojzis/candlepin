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
package org.candlepin.config;

import static org.candlepin.config.ConfigurationPrefixes.JPA_CONFIG_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Properties;



public class EncryptedConfigurationTest {
    // generated with katello-secure-passphrase and katell-passwd
    // we are going to try to be compatible with this if we can
    private String passphrase = "QwGhDv4FSnyTbFJf8O6gvWIsmQX7PZtE64ALMCXx4DcS48s5Sum7RkVcefD0vMe5";
    private String plainPassword = "testpassword";
    private String encPasswordAsStored = "$1$8dg00oV+ZhN74tvxG+kAhw==";

    private Properties props;
    private final String key1 = JPA_CONFIG_PREFIX + "hibernate.connection.password";
    private final String key2 = JPA_CONFIG_PREFIX + "x";

    @BeforeEach
    public void init() {
        props = new Properties();
        props.setProperty(key1, encPasswordAsStored);
        props.setProperty(key2, "y");
    }

    private EncryptedConfiguration loadConfig(Properties properties) throws ConfigurationException {
        EncryptedConfiguration config = new EncryptedConfiguration();
        properties.forEach((key, value) -> config.setProperty((String) key, (String) value));

        return config;
    }

    @Test
    public void testDecrypt(@TempDir Path temp) throws Exception {
        File passphraseFile = temp.resolve("passphrase.txt").toFile();
        Writer w = new FileWriter(passphraseFile);
        w.write(passphrase);
        w.close();

        props.setProperty("passphrase_file", passphraseFile.getAbsolutePath());

        EncryptedConfiguration c = this.loadConfig(props)
            .use("passphrase_file")
            .toDecrypt(key1, key2);

        assertEquals(plainPassword, c.getString(key1));
        assertEquals("y", c.getString(key2));
    }

    @Test
    public void testDecryptWithEmptyPassphraseFile() throws Exception {
        props.setProperty("passphrase_file", "");

        EncryptedConfiguration c = this.loadConfig(props)
            .use("passphrase_file")
            .toDecrypt(key1, key2);

        assertEquals(encPasswordAsStored, c.getString(key1));
        assertEquals("y", c.getString(key2));
    }

    @Test
    public void testDecryptWithNoPassphraseFile() throws Exception {
        EncryptedConfiguration c = this.loadConfig(props)
            .use("passphrase_file")
            .toDecrypt(key1, key2);

        assertEquals(encPasswordAsStored, c.getString(key1));
        assertEquals("y", c.getString(key2));
    }

    @Test
    public void testDecryptWithBadPassphraseFile() throws Exception {
        props.setProperty("passphrase_file", "/does/not/exist");

        EncryptedConfiguration c = this.loadConfig(props);

        Throwable t = assertThrows(ConfigurationException.class, () -> c.use("passphrase_file"));
        assertThat(t.getCause(), IsInstanceOf.instanceOf(FileNotFoundException.class));
    }

    @Test
    public void testDecryptWithWrongPassphrase(@TempDir Path temp) throws Exception {
        File passphraseFile = temp.resolve("passphrase.txt").toFile();
        Writer w = new FileWriter(passphraseFile);
        w.write("wrong");
        w.close();

        props.setProperty("passphrase_file", passphraseFile.getAbsolutePath());

        EncryptedConfiguration c = this.loadConfig(props)
            .use("passphrase_file");

        assertThrows(ConfigurationException.class, () -> c.toDecrypt(key1, key2));

        // Note: if we care about being backend-agnostic here, we can't guarantee a specific exception
        // will be thrown, as which exception is used varies between crypto providers.
    }

    @Test
    public void testUnusualPassword(@TempDir Path temp) throws Exception {
        File passphraseFile = temp.resolve("passphrase.txt").toFile();
        Writer w = new FileWriter(passphraseFile);
        String expected = "Hello\nWorld\n";
        w.write(expected);
        w.close();

        props.setProperty("passphrase_file", passphraseFile.getAbsolutePath());
        EncryptedConfiguration c = this.loadConfig(props);
        assertEquals(expected, c.readPassphrase("passphrase_file"));
    }
}
