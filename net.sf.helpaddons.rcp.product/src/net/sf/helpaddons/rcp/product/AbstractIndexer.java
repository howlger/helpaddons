/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Holger Voormann - initial API and implementation
 *******************************************************************************/
package net.sf.helpaddons.rcp.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.LocalSearchManager;
import org.eclipse.help.internal.search.SearchIndexWithIndexingProgress;

/**
 * This application creates or update the search index depending on
 * {@link #indexWebLocales()} either for the current locale and for all by the
 * "-locales" parameter specified locales or for the current locale only.
 */
public abstract class AbstractIndexer implements IApplication {

    public synchronized Object start(IApplicationContext context) throws Exception {

        // default locale
        String defaultLocale = Platform.getNL();
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault().toString();
        }
        index(defaultLocale);

        // default locale only (RCP application)
        // or all locale (Web application)?
        if (!indexWebLocales()) return EXIT_OK;

        // web locales
        String[] locales = computeLocales();
        for (int i = 0; i < locales.length; i++) {
            if (defaultLocale.equals(locales[i])) continue;
            index(locales[i]);
        }

        // done
        return EXIT_OK;
    }

    public synchronized void stop() {

        // wait until start has finished
        synchronized(this) {};

    }

    abstract protected boolean indexWebLocales();

    private void index(String locale) {
        LocalSearchManager searchManager =
                BaseHelpSystem.getLocalSearchManager();
        SearchIndexWithIndexingProgress index = searchManager.getIndex(locale);
        searchManager.ensureIndexUpdated(new NullProgressMonitor(), index);
    }

    /**
     * Copied from
     * {@link org.eclipse.help.internal.webapp.utils.Utils#initializeLocales()}:
     * First part unchanged, rest edited (see comment "CODE CHANGED BELOW").
     */
    private static String[] computeLocales() {

        // locale strings as passed in command line or in preferences
        final List infocenterLocales= new ArrayList();

        // first check if locales passed as command line arguments
        String[] args = Platform.getCommandLineArgs();
        boolean localeOption = false;
        for (int i = 0; i < args.length; i++) {
            if ("-locales".equalsIgnoreCase(args[i])) { //$NON-NLS-1$
                localeOption = true;
                continue;
            } else if (args[i].startsWith("-")) { //$NON-NLS-1$
                localeOption = false;
                continue;
            }
            if (localeOption) {
                infocenterLocales.add(args[i]);
            }
        }

        // if no locales from command line, get them from preferences
        if (infocenterLocales.isEmpty()) {
            String preferredLocales = Platform.getPreferencesService().getString
                ("org.eclipse.help.base", ("locales"), "", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            StringTokenizer tokenizer = new StringTokenizer(preferredLocales,
                    " ,\t"); //$NON-NLS-1$
            while (tokenizer.hasMoreTokens()) {
                infocenterLocales.add(tokenizer.nextToken());
            }
        }

        // CODE CHANGED BELOW

        // format locales
        String[] result = new String[infocenterLocales.size()];
        for (int i = 0; i < infocenterLocales.size(); i++) {
            String locale = infocenterLocales.get(i).toString();
            if (locale.length() >= 5) {
                result[i] =   locale.substring(0, 2).toLowerCase(Locale.ENGLISH)
                            + "_" //$NON-NLS-1$
                            + locale.substring(3, 5).toUpperCase(Locale.ENGLISH);
            } else if (locale.length() >= 2) {
                result[i] = locale.substring(0, 2).toLowerCase(Locale.ENGLISH);
            }
        }
        return result;
    }

}
