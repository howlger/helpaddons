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

    public InputStream getInputStream(String pluginID, String href,
            Locale locale) {

        String hrefWithoutQuery = href.indexOf('?')< 0
                                  ? href
                                  : href.substring(0, href.indexOf('?'));

        InputStream original = UntransformedHelpContent.getInputStream(pluginID, href, locale);

        if (   hrefWithoutQuery.endsWith(".htm")
            || hrefWithoutQuery.endsWith(".html")
            || hrefWithoutQuery.endsWith(".xhtml")) {
            IHrefResolver hrefResolver =
                CrossLinkManagerPlugin.createHrefResolver(pluginID, locale);
            return new TransformedLinksInputStream(original, hrefResolver);
        }

        return original;
    }

}
