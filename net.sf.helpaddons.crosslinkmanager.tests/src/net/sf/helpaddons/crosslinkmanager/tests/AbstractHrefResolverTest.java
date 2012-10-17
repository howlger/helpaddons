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
import net.sf.helpaddons.crosslinkmanager.AbstractHrefResolver;
import net.sf.helpaddons.crosslinkmanager.IHrefResolver;

import org.junit.Test;

public class AbstractHrefResolverTest {

    private static class HrefResolverDummy extends AbstractHrefResolver {

        public HrefResolverDummy(String sourceHref) {
            super(sourceHref);
        }

        @Override
        protected boolean computeExistsInSourceBundle(String href) {
            return href.endsWith("inSource.htm");
        }

        @Override
        protected String computeTargetBundle(String href) {
            if (href.endsWith("nirvana.htm")) return null;

            return "pool";
        }

        @Override
        protected String getNotFoundHtmlFile(String hrefPrefix) {
            return "errors/404.htm";
        }

        @Override
        public String getNotFoundClassName() {
            return "error404";
        }

    }

    @Test
    public void testResolve() throws Exception {
        assertResolved("inSource.htm", "root.htm", "inSource.htm");
        assertResolved("../pool/inTarget.htm", "root.htm", "inTarget.htm");
        assertResolved(null, "root.htm", "nirvana.htm");

        // page which contains the link or link target in subdir
        assertResolved("../inSource.htm", "dir/page.htm", "../inSource.htm");
        assertResolved("../../pool/dir/target.htm", "dir/page.htm", "target.htm");
        assertResolved("../pool/dir/target.htm", "root.htm", "dir/target.htm");
        assertResolved("../../../pool/sub/dir/target.htm", "sub/dir/root.htm", "target.htm");
        assertResolved("../../../pool/sub/dir2/target.htm", "sub/dir1/page.htm", "../dir2/target.htm");
        assertResolved(null, "dir/page.htm", "../nirvana.htm");

        // TODO check if fragments can be included in help content producer calls?
    }

    @Test
    public void testExplicitCrossLinks() throws Exception {
        assertResolved("/help/topic/other.plugin/topic.htm",
                       "dir/page.htm",
                       "/help/topic/other.plugin/topic.htm");
        assertResolved("../other.plugin/topic.htm",
                       "page.htm",
                       "../other.plugin/topic.htm");
        assertResolved("../../other.plugin/topic.htm",
                       "dir/page.htm",
                       "../../other.plugin/topic.htm");

        // '\' can be used instead of '/'
        assertResolved("\\help\\topic\\other.plugin\\topic.htm",
                       "dir/page.htm",
                       "\\help\\topic\\other.plugin\\topic.htm");
        assertResolved("..\\other.plugin\\topic.htm",
                       "page.htm",
                       "..\\other.plugin\\topic.htm");
        assertResolved("/help\\topic/other.plugin\\topic.htm",
                       "dir/page.htm",
                       "/help\\topic/other.plugin\\topic.htm");
        assertResolved("\\help/topic\\other.plugin/topic.htm",
                       "dir/page.htm",
                       "\\help/topic\\other.plugin/topic.htm");
    }

    @Test
    public void testPoolLinks() throws Exception {

        // in same plug-in
        assertResolved("inSource.htm", "page.htm", "/pool/inSource.htm");
        assertResolved("../inSource.htm", "dir/page.htm", "/pool/inSource.htm");
        assertResolved("../../inSource.htm",
                       "sub/dir/page.htm",
                       "/pool/inSource.htm");
        assertResolved("dir/inSource.htm",
                       "page.htm",
                       "/pool/dir/inSource.htm");
        assertResolved("../../sub/dir/inSource.htm",
                       "sub/dir/page.htm",
                       "/pool/sub/dir/inSource.htm");

        // in other plug-in
        assertResolved("../pool/target.htm", "page.htm", "/pool/target.htm");
        assertResolved("../pool/dir/target.htm",
                       "page.htm",
                       "/pool/dir/target.htm");
        assertResolved("../pool/sub/dir/target.htm",
                       "page.htm",
                       "/pool/sub/dir/target.htm");
        assertResolved("../../pool/target.htm",
                       "dir/page.htm",
                       "/pool/target.htm");
        assertResolved("../../../pool/sub/dir/target.htm",
                       "dir/sub/page.htm",
                       "/pool/sub/dir/target.htm");

        // target doesn't exist
        assertResolved(null, "page.htm", "/pool/nirvana.htm");
        assertResolved(null, "dir/page.htm", "/pool/dir/nirvana.htm");
    }

    @Test
    public void testGetNotFoundHref() throws Exception {
        assertErrorHrefResolved("errors/404.htm", "nirvana.htm");
        assertErrorHrefResolved("../errors/404.htm", "dir/nirvana.htm");
    }

    private static void assertResolved(String expectedHref,
                                       String sourceHref,
                                       String targetHref) {
        IHrefResolver dummy = new HrefResolverDummy(sourceHref);
        assertEquals(expectedHref, dummy.resolve(targetHref));
    }

    private static void assertErrorHrefResolved(String expectedHref,
                                                String sourceHref) {
        IHrefResolver dummy = new HrefResolverDummy(sourceHref);
        assertEquals(expectedHref, dummy.getNotFoundHref(null));
    }

}
