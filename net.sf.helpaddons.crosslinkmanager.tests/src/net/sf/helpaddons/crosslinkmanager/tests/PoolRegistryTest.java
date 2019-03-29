/*******************************************************************************
 * Copyright (c) 2010, 2011 Holger Voormann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Holger Voormann - initial API and implementation
 *******************************************************************************/
package net.sf.helpaddons.crosslinkmanager.tests;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.helpaddons.crosslinkmanager.IHrefResolver;
import net.sf.helpaddons.crosslinkmanager.IStaticHelpContent;
import net.sf.helpaddons.crosslinkmanager.PoolRegistry;

public class PoolRegistryTest {

    private static class PoolExtensionBuilder {

        private final Set<IExtension> extensions = new HashSet<IExtension>();

        private String bundle;

        private final List<PoolElement> pools = new ArrayList<PoolElement>();
        private final List<ErrorPageElement> errorPages = new ArrayList<ErrorPageElement>();

        public PoolExtensionBuilder bundle(String bundleSymbolicName) {
            addPools();
            bundle = bundleSymbolicName;
            return this;
        }

        public PoolExtensionBuilder pool(String id) {
            return pool(id, null);
        }

        public PoolExtensionBuilder pool(String id, String bundlesToPrefer) {
            if (bundle == null)
                throw new IllegalStateException("pool() must follow bundle()");
            pools.add(new PoolElement(id,
                                      bundlesToPrefer));
            return this;
        }

        public PoolExtensionBuilder errorPage(String href) {
            return errorPage(null, href);
        }

        public PoolExtensionBuilder errorPage(String prefix, String href) {
            if (bundle == null)
                throw new IllegalStateException("errorPage() must follow bundle()");
            errorPages.add(new ErrorPageElement(prefix, href));
            return this;
        }

        public Set<IExtension> build() {
            addPools();
            bundle = null;
            return Collections.unmodifiableSet(extensions);
        }

        private void addPools() {
            if (bundle != null) {
                extensions.add(
                    new PoolExtension(bundle,
                                      new ArrayList<PoolElement>(pools),
                                      new ArrayList<ErrorPageElement>(errorPages)));
                pools.clear();
                errorPages.clear();
            }
        }
    }

    private static class PoolExtension implements IExtension {

        private final String contributor;
        private final List<PoolElement> pools;
        private final List<ErrorPageElement> errorPages;

        public PoolExtension(String contributor,
                             List<PoolElement> pools,
                             List<ErrorPageElement> errorPages) {
            this.contributor = contributor;
            this.pools = pools;
            this.errorPages = errorPages;
        }

        @Override
        public IConfigurationElement[] getConfigurationElements() {
            List<IConfigurationElement> result =
                new ArrayList<IConfigurationElement>();
            result.addAll(pools);
            result.addAll(errorPages);
            return result.toArray(new IConfigurationElement[result.size()]);
        }

        @Override
        public String getNamespace() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getNamespaceIdentifier() {
            return contributor;
        }

        @Override
        public IContributor getContributor() {
            return new IContributor() {

                @Override
                public String getName() {
                    return contributor;
                }
            };
        }

        @Override
        public String getExtensionPointUniqueIdentifier() {
            return "net.sf.helpaddons.crosslinkmanager.contentPools";
        }

        @Override
        public String getLabel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getLabel(String locale) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSimpleIdentifier() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getUniqueIdentifier() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValid() {
            return true;
        }

    }

    private static abstract class AbstractElement implements IConfigurationElement {

        @Override
        public Object createExecutableExtension(String propertyName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAttribute(String attrName, String locale) {
            return getAttribute(attrName);
        }

        @Override
        public String getAttributeAsIs(String name) {
            return getAttribute(name);
        }

        @Override
        public IConfigurationElement[] getChildren() {
            return new IConfigurationElement[0];
        }

        @Override
        public IConfigurationElement[] getChildren(String name) {
            return new IConfigurationElement[0];
        }

        @Override
        public IExtension getDeclaringExtension() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getParent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getValue(String locale) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getValueAsIs() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getNamespace() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getNamespaceIdentifier() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IContributor getContributor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValid() {
            return true;
        }

    }

    private static class PoolElement extends AbstractElement {

        private final String id;
        private final String bundlesToPrefer;

        public PoolElement(String id,
                           String bundlesToPrefer) {
            if (id == null) throw new IllegalArgumentException("id must not be null");
            this.id = id;
            this.bundlesToPrefer = bundlesToPrefer;
        }

        @Override
        public String getAttribute(String name) {
            if (name.equals("id")) return id;
            if (name.equals("bundlesToPrefer")) return bundlesToPrefer;
            return null;
        }

        @Override
        public String[] getAttributeNames() {
            return bundlesToPrefer == null
                   ? new String[] {"id"}
                   : new String[] {"id", "bundlesToPrefer"};
        }

        @Override
        public String getName() {
            return "pool";
        }

		@Override
		public int getHandleId() {
			return 0;
		}

    }

    private static class ErrorPageElement extends AbstractElement {

        private final String prefix;

        private final String href;

        public ErrorPageElement(String prefix, String href) {
            if (href == null)
                throw new IllegalArgumentException("href must not be null");
            this.prefix = prefix;
            this.href = href;
        }

        @Override
        public String getAttribute(String name) {
            if (name.equals("prefix")) return prefix;
            if (name.equals("href")) return href;
            return null;
        }

        @Override
        public String[] getAttributeNames() {
            return prefix == null
                   ? new String[] {"href"}
                   : new String[] {"prefix", "href"};
        }

        @Override
        public String getName() {
            return "errorPage";
        }

		@Override
		public int getHandleId() {
			return 0;
		}

    }

    private PoolExtensionBuilder builder;

    private PoolRegistry poolRegistry;

    private IHrefResolver resolver;

    @Before
    public void setUp() {
        builder = new PoolExtensionBuilder();
        poolRegistry = new PoolRegistry();
        poolRegistry.setHelpContentDelegate(new IStaticHelpContent() {

            @Override
            public InputStream getInputStream(String plugin, String href, Locale locale) {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream getInputStream(String plugin, String href, String locale) {
            	throw new UnsupportedOperationException();
            }

            @Override
            public boolean checkExists(String pluginId, String href, Locale locale) {
                return href.contains(pluginId);
            }

			@Override
			public boolean checkExists(String pluginId, String href, String locale) {
				return href.contains(pluginId);
			}
        });
    }

    @After
    public void tearDown() throws Exception {
        builder = null;
        poolRegistry = null;
        resolver = null;
    }

    @Test
    public void testTwoPools() {
        builder.bundle("doc.a").pool("z")
               .bundle("doc.b").pool("z");

        initResolverAndRegistry("doc.a", "topic.htm");
        assertResolvedEquals("in_doc.a", "in_doc.a");
        assertResolvedEquals("../doc.b/in_doc.b", "in_doc.b");
        assertResolvedEquals("in_doc.a_and_doc.b", "in_doc.a_and_doc.b");
        assertResolvedEquals(null, "nowhere");

        initResolverAndRegistry("doc.a", "dir/topic.htm");
        assertResolvedEquals("../in_doc.a", "../in_doc.a");
        assertResolvedEquals("../../doc.b/dir/in_doc.b", "in_doc.b");
        assertResolvedEquals("../../doc.b/in_doc.b", "../in_doc.b");
        assertResolvedEquals("in_doc.a_and_doc.b", "in_doc.a_and_doc.b");
        assertResolvedEquals(null, "../nowhere");
    }

    @Test
    public void testFourePools() {
        builder.bundle("doc.a").pool("one")
               .bundle("doc.b").pool("one").pool("two")
               .bundle("doc.c").pool("two").pool("three")
               .bundle("doc.a").pool("three")
               .bundle("doc.d").pool("two");

        initResolverAndRegistry("doc.a", "topic.htm");
        assertResolvedEquals("in_doc.a_doc.c", "in_doc.a_doc.c");
        assertResolvedEquals("../doc.b/in_doc.b", "in_doc.b");
        assertResolvedEquals("../doc.c/in_doc.c", "in_doc.c");
        assertResolvedEquals(null, "in_doc.d");
    }

    @Test
    public void testPreferredBunldes() {
        builder.bundle("doc.a").pool("z", "d42.z");
        StringBuilder inAllBundlesBuilder = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            String bundle = "d" + i + ".z";
            inAllBundlesBuilder.append(bundle).append('_');
            builder.bundle(bundle).pool("z");
        }
        String inAllBundles = inAllBundlesBuilder.toString();

        // preferred: "d42.z"
        initResolverAndRegistry("doc.a", "topic.htm");
        assertResolvedEquals("../d42.z/" + inAllBundles, inAllBundles);
        assertResolvedEquals("../d7.z/in_d7.z", "in_d7.z");
        assertResolvedEquals(null, "nowhere");

        // preferred: "d47.z, d11.z, d43.z"
        builder.bundle("doc.b").pool("z", "d47.z,d11.z\t\n\r ,\t\n\r d43.z");
        initResolverAndRegistry("doc.b", "topic.htm");
        assertResolvedEquals("../d47.z/" + inAllBundles, inAllBundles);
        String inAllWithoutD47 =
              inAllBundles.substring(0, inAllBundles.indexOf("d47.z"))
            + inAllBundles.substring(inAllBundles.indexOf("d47.z") + 5);
        assertResolvedEquals("../d11.z/" + inAllWithoutD47, inAllWithoutD47);
        String inAllWithoutD47andD11 =
              inAllWithoutD47.substring(0, inAllWithoutD47.indexOf("d11.z"))
            + inAllWithoutD47.substring(inAllWithoutD47.indexOf("d11.z") + 5);
        assertResolvedEquals("../d43.z/" + inAllWithoutD47andD11,
                             inAllWithoutD47andD11);
        assertResolvedEquals("../d42.z/in_d42.z", "in_d42.z");
        assertResolvedEquals(null, "nowhere");
    }

    @Test
    public void testDefaultErrorPage() {

        // default
        builder.bundle("doc.a").pool("z");
        initResolverAndRegistry("doc.a", "topic.htm");
        assertEquals("error404.htm", resolver.getNotFoundHref(null));

        // set default error page
        builder.bundle("doc.a").pool("z").errorPage("path/to/myErrorPage.htm");
        initResolverAndRegistry("doc.a", "topic.htm");
        assertEquals("path/to/myErrorPage.htm", resolver.getNotFoundHref(null));

    }

    @Test
    public void testSpecificErrorPage() {
        builder.bundle("doc.a").pool("z").errorPage("error.htm")
                               .errorPage("my", "myError.htm")
                               .errorPage("my2", "myError2.htm")
                               ;
        initResolverAndRegistry("doc.a", "topic.htm");
        assertEquals("error.htm", resolver.getNotFoundHref(null));
        assertEquals("myError.htm", resolver.getNotFoundHref("my"));
    assertEquals("myError2.htm", resolver.getNotFoundHref("my2"));
    }

    private void initResolverAndRegistry(String sourceBundle,
                                         String sourceHref) {
        poolRegistry.changed(builder.build());
        resolver = poolRegistry.createHrefResolver(sourceBundle,
                                                   sourceHref,
                                                   null);
    }

    private void assertResolvedEquals(String expected, String hrefToResolve) {
        assertEquals(expected, resolver.resolve(hrefToResolve));
    }

}
