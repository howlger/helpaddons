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
            return href.equals("inSource.htm");
        }

        @Override
        protected String computeTargetBundle(String href) {
            if (href.equals("nirvana.htm")) return null;

            return "pool";
        }

        @Override
        protected String getNotFoundHtmlFile() {
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

        // TODO if source in subdir -> ../404.htm


        // TODO check if fragments can be included in help content producer calls?
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
        assertEquals(expectedHref, dummy.getNotFoundHref());
    }

}
