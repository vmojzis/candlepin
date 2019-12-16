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
package org.candlepin.audit;

import org.candlepin.service.SubscriptionServiceAdapter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * ActivationListener
 */
public class ActivationListener implements EventListener {
    private static Logger log = LoggerFactory.getLogger(ActivationListener.class);
    private SubscriptionServiceAdapter subscriptionService;
    private ObjectMapper mapper;

    @Inject
    public ActivationListener(SubscriptionServiceAdapter subService,
        @Named("ActivationListenerObjectMapper") ObjectMapper objectMapper) {
        this.mapper = objectMapper;
        this.subscriptionService = subService;
    }

    @Override
    public void onEvent(Event e) {
        if (e.getType().equals(Event.Type.CREATED) &&
            e.getTarget().equals(Event.Target.POOL)) {
            try {
                String subscriptionId = mapper.readTree(e.getEventData()).get("subscriptionId").asText();
                subscriptionService.sendActivationEmail(subscriptionId);
            }
            catch (JsonMappingException ex) {
                logError(e);
            }
            catch (JsonParseException ex) {
                logError(e);
            }
            catch (IOException ex) {
                logError(e);
            }
        }
    }

    @Override
    public boolean requiresQpid() {
        return false;
    }

    private void logError(Event e) {
        log.debug("Invalid JSON for pool : " + e.getEntityId());
    }
}
