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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CrossLinkManagerPlugin implements BundleActivator, IRegistryChangeListener {

    /** The ID of this plug-in/bundle. */
    private static final String ID =
        "net.sf.helpaddons.crosslinkmanager"; //$NON-NLS-1$

    private static final String CONTENT_POOLS_EXTENSION_POINT_ID =
        "contentPools";  //$NON-NLS-1$

    /** The singleton instance of this class representing plug-in/bundle. */
    private static CrossLinkManagerPlugin plugin;

    private final Set<IExtension> contentPoolsExtensions =
        new HashSet<IExtension>();

    private final PoolRegistry poolRegistry = new PoolRegistry();

    public void start(BundleContext bundleContext) throws Exception {
        if (plugin != null) {
            throw new RuntimeException("Bundle must be singleton"); //$NON-NLS-1$
        }
        plugin = this;
        IExtensionRegistry reg = Platform.getExtensionRegistry();
        IExtensionPoint pt =
            reg.getExtensionPoint(ID,
                                  CONTENT_POOLS_EXTENSION_POINT_ID);
        IExtension[] allExtensions = pt.getExtensions();
        for (IExtension ext : allExtensions) {
            contentPoolsExtensions.add(ext);
        }
        reg.addRegistryChangeListener(this);
        poolRegistry.changed(contentPoolsExtensions);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        plugin = null;
        contentPoolsExtensions.clear();
        IExtensionRegistry reg = Platform.getExtensionRegistry();
        reg.removeRegistryChangeListener(this);
    }

    public void registryChanged(IRegistryChangeEvent event) {
        IExtensionDelta[] deltas =
            event.getExtensionDeltas(ID, CONTENT_POOLS_EXTENSION_POINT_ID);
        for (int i = 0; i < deltas.length; i++) {
            if (deltas[i].getKind() == IExtensionDelta.ADDED) {
                contentPoolsExtensions.add(deltas[i].getExtension());
            } else {
                contentPoolsExtensions.remove(deltas[i].getExtension());
            }
        }
        poolRegistry.changed(contentPoolsExtensions);
    }

    /**
     * @param sourceBundle source bundle symbolic name,
     *                     e.g. {@code com.domain.mybundle}
     * @param sourceHref path (HTML reference) of the source HTML file,
     *                   e.g. {@code dir/file.htm}
     * @param locale the (current) local to use
     *               (required if the help content is localized)
     * @return an instance of {@link IHrefResolver} associated with the source
     *         HTML file
     */
    public static IHrefResolver createHrefResolver(String sourceBundle,
                                                   String sourceHref,
                                                   Locale locale) {
        if (plugin == null) return IHrefResolver.NULL; // may happen on shutdown
        return plugin.poolRegistry.createHrefResolver(sourceBundle,
                                                      sourceHref,
                                                      locale);
    }

    public static boolean isPoolBundle(String bundleSymbolicName) {
        if (plugin == null) return false; // may happen on shutdown
        return plugin.poolRegistry.isPoolBundle(bundleSymbolicName);
    }

}
