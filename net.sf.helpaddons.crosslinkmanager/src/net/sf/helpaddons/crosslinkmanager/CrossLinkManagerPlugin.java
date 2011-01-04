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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
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

    private final HashSet<IExtension> extensions = new HashSet<IExtension>();

    private final Map<String, Set<String>> bundlesOfPool =
        new HashMap<String, Set<String>>();

    private final Map<String, Set<String>> poolsOfBundle =
        new HashMap<String, Set<String>>();

    private static CrossLinkManagerPlugin plugin;

    /**
     * @return the singleton instance of this class representing plug-in
     */
    public static CrossLinkManagerPlugin getDefault() {
        return plugin;
    }

    public void start(BundleContext bundleContext) throws Exception {
        if (plugin != null) {
            throw new RuntimeException("Bundle must be singleton");
        }
        plugin = this;
        IExtensionRegistry reg = Platform.getExtensionRegistry();
        IExtensionPoint pt = reg.getExtensionPoint(ID + "." + CONTENT_POOLS_EXTENSION_POINT_ID);
        IExtension[] allExtensions = pt.getExtensions();
        for (IExtension ext : allExtensions) {
            extensions.add(ext);
        }
        reg.addRegistryChangeListener(this);
        registryChanged();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        plugin = null;
        extensions.clear();
        IExtensionRegistry reg = Platform.getExtensionRegistry();
        reg.removeRegistryChangeListener(this);
    }

    public void registryChanged(IRegistryChangeEvent event) {
        IExtensionDelta[] deltas =
            event.getExtensionDeltas(ID, CONTENT_POOLS_EXTENSION_POINT_ID);
        for (int i = 0; i < deltas.length; i++) {
            if (deltas[i].getKind() == IExtensionDelta.ADDED) {
                extensions.add(deltas[i].getExtension());
            } else {
                extensions.remove(deltas[i].getExtension());
            }
        }
        registryChanged();
    }

    private synchronized void registryChanged() {
        bundlesOfPool.clear();
        poolsOfBundle.clear();

        for (IExtension ext : extensions) {
            String bundleSymbolicName = ext.getNamespaceIdentifier();
            IConfigurationElement[] poolElements = ext.getConfigurationElements();
            for (IConfigurationElement element : poolElements) {
                if (!"pool".equals(element.getName())) continue;

                String pool = element.getAttribute("id");

                // pools of bundle
                Set<String> poolSet = poolsOfBundle.get(bundleSymbolicName);
                if (poolSet == null) {
                    poolSet = new LinkedHashSet<String>();
                    poolsOfBundle.put(bundleSymbolicName, poolSet);
                }
                poolSet.add(pool);


                // bundles of pool
                Set<String> bundlesSet = bundlesOfPool.get(pool);
                if (bundlesSet == null) {
                    bundlesSet = new LinkedHashSet<String>();
                    bundlesOfPool.put(pool, bundlesSet);
                }
                bundlesSet.add(bundleSymbolicName);
            }

        }

    }

    public static IHrefResolver createHrefResolver(String sourceBundleSymbolicName, String sourceHref, Locale locale) {
        return getDefault().privateCreateHrefResolver(sourceBundleSymbolicName, sourceHref, locale);
    }

    private IHrefResolver privateCreateHrefResolver(String sourceBundleSymbolicName, String sourceHref, Locale locale) {
        return new MyHrefResolver(sourceBundleSymbolicName, sourceHref, locale);
    }

    private class MyHrefResolver extends AbstractHrefResolver {

        private final String sourceBundle;
        private final Locale locale;
        private final Set<String> pools;

        public MyHrefResolver(String sourceBundleSymbolicName,
                              String sourceHref,
                              Locale locale) {
            super(sourceHref);
            this.sourceBundle = sourceBundleSymbolicName;
            this.locale = locale;
            pools = poolsOfBundle.get(sourceBundleSymbolicName);
        }

        @Override
        protected boolean computeExistsInSourceBundle(String href) {
            return UntransformedHelpContent.checkExists(sourceBundle,
                                                        href,
                                                        locale);
        }

        @Override
        protected String computeTargetBundle(String href) {
            if (pools == null) return null;
            for (String pool : pools) {
                for (String bundle : bundlesOfPool.get(pool)) {

                    if (UntransformedHelpContent.checkExists(bundle, href, locale)) {
                        return bundle;
                    }

                }

            }
            return null;
        }

        @Override
        protected String getNotFoundHtmlFile() {
            return "error404.htm";
        }

        public String getNotFoundClassName() {
            return "error404";
        }

    }

}
