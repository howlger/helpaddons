/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Holger Voormann - parts of the
 *                       org.eclipse.help.internal.util.ResourceLocator class
 *                       copied to this new class; minor changes
 *******************************************************************************/
package net.sf.helpaddons.crosslinkmanager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Utility class to find and open a help content resource (e.g. HTML file or GIF
 * image) which are contributed by files (unzipped, in JAR, or in doc.zip) but
 * not from a producer ({@code IHelpContentProducer}).
 *
 * The code is based on the the
 * {@link org.eclipse.help.internal.protocols.HelpURLConnection#getLocalHelp(Bundle)}
 * method and the org.eclipse.help.internal.util.ResourceLocator class.
 */
public interface IStaticHelpContent {

    /**
     * Opens a internal help content resource as {@link InputStream}.
     *
     * @param plugin the bundle symbolic name of the plug-in which contains
     *               the requested file to open
     * @param href the hyperlink reference of the file to open
     * @param locale the locale to use (to find the file in the corresponding
     *               subfolders)
     * @return help content resource as {@link InputStream} or {@code null} if
     *         requested resource does not exist
     *
     * @see org.eclipse.help.internal.protocols.HelpURLConnection#getLocalHelp(Bundle)
     */
    InputStream getInputStream(String plugin, String href, Locale locale);
    InputStream getInputStream(String plugin, String href, String locale);


    /**
     * Tests if the specified static help resource exists and can be opened.
     *
     * @param pluginId the id of the plug-in which contains HTML file to open
     * @param href the hyperlink reference of the HTML file to open
     * @param locale the locale to use (to retrieve static HTML file)
     * @return static help resource as {@link InputStream}
     */
    boolean checkExists(String pluginId, String href, Locale locale);
    boolean checkExists(String pluginId, String href, String locale);

    public static final IStaticHelpContent DEFAULT =
        new IStaticHelpContent() {

          public InputStream getInputStream(String plugin,
                                            String href,
                                            Locale locale) {
              return getInputStream(plugin,
                                    href,
                                    locale == null ? null : locale.toString());
          }


           public InputStream getInputStream(String plugin,
                                             String href,
                                             String locale) {
               if (Platform.getBundle(plugin) == null) return null;

               // href without query
               int queryDelimiterIndex = href.indexOf('?');
               String hrefWithoutQuery = queryDelimiterIndex < 0
                                         ? href
                       : href.substring(0, queryDelimiterIndex);

               Bundle bundle = Platform.getBundle(plugin);

               // 1. first try to find the file inside "doc.zip"...
               InputStream in = openFromZip(bundle,
                                            "doc.zip", //$NON-NLS-1$
                                            hrefWithoutQuery,
                                            locale == null ? null : locale.toString());
               if (in != null) return in;

               // 2. ... and then try the file system
               return openFromPlugin(bundle,
                                     hrefWithoutQuery,
                                     locale == null ? null : locale.toString());

           }

           public boolean checkExists(String pluginId,
                                      String href,
                                      Locale locale) {
               return checkExists(pluginId,
                                  href,
                                  locale == null ? null : locale.toString());
           }

           public boolean checkExists(String pluginId,
                                      String href,
                                      String locale) {
// TODO replace with find: "!/" for ZIP
               InputStream resource = null;
               try {
                   resource = getInputStream(pluginId, href, locale);
                   return resource != null;
               } finally {
                   if (resource != null) {
                       try {
                           resource.close();
                       } catch (IOException e) {
// TODO log
                       }
                   }
               }
           }


            // -----------------------------------------------------------------
            // The following is a partial copy of the internal
            // org.eclipse.help.internal.util.ResourceLocator
            // "public" -> "private"; Generic added; "static" removed

            private Map<String, Object> zipCache = new Hashtable<String, Object>();

            private final Object ZIP_NOT_FOUND = new Object();

            /**
             * Opens an input stream to a file contained in a zip in a plugin. This includes OS, WS and NL
             * lookup.
             *
             * @param pluginDesc
             *            the plugin description of the plugin that contains the file you are trying to find
             * @param file
             *            the relative path of the file to find
             * @param locale
             *            the locale used as an override or <code>null</code> to use the default locale
             *
             * @return an InputStream to the file or <code>null</code> if the file wasn't found
             */
            private InputStream openFromZip(Bundle pluginDesc, String zip, String file, String locale) {

                String pluginID = pluginDesc.getSymbolicName();
                Map<String, Object> cache = zipCache;
                List<String> pathPrefix = getPathPrefix(locale);

                for (int i = 0; i < pathPrefix.size(); i++) {

                    // finds the zip file by either using a cached location, or
                    // calling Platform.find - the result is cached for future use.
                    Object cached = cache.get(pluginID + '/' + pathPrefix.get(i) + zip);
                    if (cached == null) {
                        try {
                            URL url = FileLocator.find(pluginDesc, new Path(pathPrefix.get(i) + zip), null);
                            if (url != null) {
                                URL realZipURL = FileLocator.toFileURL(FileLocator.resolve(url));
                                cached = realZipURL.toExternalForm();
                            } else {
                                cached = ZIP_NOT_FOUND;
                            }
                        } catch (IOException ioe) {
                            cached = ZIP_NOT_FOUND;
                        }
                        // cache it
                        cache.put(pluginID + '/' + pathPrefix.get(i) + zip, cached);
                    }

                    if (cached == ZIP_NOT_FOUND || cached.toString().startsWith("jar:")) //$NON-NLS-1$
                        continue;

                    // cached should be a zip file that is actually on the filesystem
                    // now check if the file is in this zip
                    try {
                        URL jurl = new URL("jar", "", (String) cached + "!/" + file); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        URLConnection jconnection = jurl.openConnection();
                        jconnection.setDefaultUseCaches(false);
                        jconnection.setUseCaches(false);
                        return jconnection.getInputStream();
                    } catch (IOException ioe) {
                        // a file not found exception is an io exception
                        continue;
                    }

                } // end for loop

                // we didn't find the file in any zip
                return null;
            }

            /**
             * Opens an input stream to a file contained in a plugin. This includes includes OS, WS and NL
             * lookup.
             *
             * @param pluginDesc
             *            the plugin description of the plugin that contains the file you are trying to find
             * @param file
             *            the relative path of the file to find
             * @param locale
             *            the locale used as an override or <code>null</code> to use the default locale
             *
             * @return an InputStream to the file or <code>null</code> if the file wasn't found
             */
            private InputStream openFromPlugin(Bundle pluginDesc, String file, String locale) {

                List<String> pathPrefix = getPathPrefix(locale);
                URL flatFileURL = find(pluginDesc, new Path(file), pathPrefix);
                if (flatFileURL != null)
                    try {
                        return flatFileURL.openStream();
                    } catch (IOException e) {
                        return null;
                    }
                return null;
            }

            /**
             * Search the ws, os then nl for a resource. Platform.find can't be used directly with $nl$,
             * $os$ or $ws$ becuase the root directory will be searched too early.
             */
            private URL find(Bundle pluginDesc, IPath flatFilePath, List<String> pathPrefix) {

                // try to find the actual file.
                for (int i = 0; i < pathPrefix.size(); i++) {
                    URL url = FileLocator.find(pluginDesc, new Path((String) pathPrefix.get(i) + flatFilePath), null);
                    if (url != null)
                        return url;
                }
                return null;
            }

            /**
             * Gets an ArrayList that has the path prefixes to search.
             *
             * @param locale the locale used as an override or <code>null</code> to use the default locale
             * @return an ArrayList that has path prefixes that need to be search. The returned ArrayList
             * will have an entry for the root of the plugin.
             */
            private List<String> getPathPrefix(String locale) {
                List<String> pathPrefix = new LinkedList<String>();
                // TODO add override for ws and os similar to how it's done with locale
                // now
                String ws = Platform.getWS();
                String os = Platform.getOS();
                if (locale == null)
                    locale = Platform.getNL();

                if (ws != null)
                    pathPrefix.add("ws/" + ws + '/'); //$NON-NLS-1$

                if (os != null && !os.equals("OS_UNKNOWN")) //$NON-NLS-1$
                    pathPrefix.add("os/" + os + '/'); //$NON-NLS-1$

                if (locale != null && locale.length() >= 5)
                    pathPrefix.add("nl/" + locale.substring(0, 2) + '/' + locale.substring(3, 5) + '/'); //$NON-NLS-1$

                if (locale != null && locale.length() >= 2)
                    pathPrefix.add("nl/" + locale.substring(0, 2) + '/'); //$NON-NLS-1$

                // the plugin root
                pathPrefix.add(""); //$NON-NLS-1$

                return pathPrefix;
            }

    };

}
