/**
 * Copyright (c) 2009 - 2022 Red Hat, Inc.
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

package org.candlepin.spec.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.candlepin.spec.bootstrap.assertions.StatusCodeAssertions.assertUnauthorized;

import org.candlepin.dto.api.client.v1.ConsumerDTO;
import org.candlepin.dto.api.client.v1.OwnerDTO;
import org.candlepin.dto.api.client.v1.UserDTO;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.client.SpecTest;
import org.candlepin.spec.bootstrap.data.builder.Consumers;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import org.junit.jupiter.api.Test;

@SpecTest
class BasicAuthSpecTest {

    @Test
    void shouldRejectInvalidCredentials() {
        ApiClient client = ApiClients.basic("random", "not valid");

        assertUnauthorized(client.consumerTypes()::getConsumerTypes);
    }

    @Test
    void shouldPassToUnprotectedEndpointForBadCredentials() {
        ApiClient client = ApiClients.basic("random", "not valid");

        assertThatCode(client.root()::getRootResources)
            .doesNotThrowAnyException();
    }

    @Test
    void shouldPassBasicAuthForValidUser() {
        ApiClient admin = ApiClients.admin();
        OwnerDTO owner = admin.owners().createOwner(Owners.random());
        UserDTO user = UserUtil.createUser(admin, owner);
        ApiClient client = ApiClients.basic(user.getUsername(), user.getPassword());

        ConsumerDTO consumer = client.consumers().createConsumer(Consumers.random(owner));

        assertThat(consumer).isNotNull();
    }

}
