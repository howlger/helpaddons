/*******************************************************************************
 * Copyright (c) 2010 Holger Voormann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Holger Voormann - initial API and implementation
 *******************************************************************************/
package net.sf.helpaddons.crosslinkmanager;

import java.io.InputStream;
import java.util.Locale;

import org.eclipse.help.IHelpContentProducer;

public class HelpContentWithCrossLinks implements IHelpContentProducer {

    public InputStream getInputStream(String pluginID,
                                      String href,
                                      Locale locale) {

        InputStream original =
            IStaticHelpContent.DEFAULT.getInputStream(pluginID,
                                                      href,
                                                      locale);

        // transform (X)HTML files only...
        if (hasHtmlFileExtension(href)) {
            IHrefResolver hrefResolver =
                CrossLinkManagerPlugin.createHrefResolver(pluginID,
                                                          href,
                                                          locale);
            return new CrossLinksResolvedInputStream(original, hrefResolver);
        }

        // ... otherwise return the stream untransformed
        return original;
    }

    /**
     * @param href the HTML reference to check (may contain a query or anchor
     *             part)
     * @return {@code true} if and only if the given HTML reference is a file
     *         with a (X)HTML file extension.
     */
    public static boolean hasHtmlFileExtension(String href) {

        // truncate query
        if (href.indexOf('?') >= 0) {
            href = href.substring(0, href.indexOf('?'));
        }

        // truncate anchor
        if (href.indexOf('#') >= 0) {
            href = href.substring(0, href.indexOf('#'));
        }

        return    href.endsWith(".htm")
               || href.endsWith(".html")
               || href.endsWith(".xhtml");
    }

}
