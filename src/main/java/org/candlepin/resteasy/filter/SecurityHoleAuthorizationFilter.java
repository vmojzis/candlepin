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
package org.candlepin.resteasy.filter;

import org.candlepin.auth.ActivationKeyPrincipal;
import org.candlepin.auth.Principal;
import org.candlepin.auth.SecurityHole;
import org.candlepin.resteasy.AnnotationLocator;

import org.jboss.resteasy.core.ResteasyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;

import java.lang.reflect.Method;
import java.util.Objects;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;

/**
 * SecurityHoleAuthorizationFilter is a no-op JAX-RS 2.0 Filter that is applied
 * to methods that have the SecurityHole annotation applied to them.  The
 * AuthorizationFeature class is what determines whether to register this filter to
 * a method.
 */
@Priority(Priorities.AUTHORIZATION)
public class SecurityHoleAuthorizationFilter extends AbstractAuthorizationFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityHoleAuthorizationFilter.class);

    private final AnnotationLocator annotationLocator;

    @Inject
    public SecurityHoleAuthorizationFilter(
        Provider<I18n> i18nProvider,
        AnnotationLocator annotationLocator) {
        this.i18nProvider = i18nProvider;
        this.annotationLocator = Objects.requireNonNull(annotationLocator);
    }

    @Override
    void runFilter(ContainerRequestContext requestContext) {
        log.debug("NO authorization check for {}", requestContext.getUriInfo().getPath());

        Principal principal = (Principal) requestContext.getSecurityContext().getUserPrincipal();

        if (principal instanceof ActivationKeyPrincipal) {
            ResourceInfo resourceInfo = ResteasyContext.getContextData(ResourceInfo.class);
            Method method = resourceInfo.getResourceMethod();
            SecurityHole securityHole = annotationLocator.getAnnotation(method, SecurityHole.class);
            if (!securityHole.activationKey()) {
                denyAccess(principal, method);
            }
        }
    }
}
