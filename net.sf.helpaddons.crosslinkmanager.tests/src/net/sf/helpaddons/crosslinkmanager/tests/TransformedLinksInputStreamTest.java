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

import java.io.IOException;
import java.io.InputStream;

import net.sf.helpaddons.crosslinkmanager.IHrefResolver;
import net.sf.helpaddons.crosslinkmanager.TransformedLinksInputStream;

import org.junit.Test;

public class TransformedLinksInputStreamTest {

    private static String NOT_FOUND = "this_target_does_not_exist";

    private static class StringInputStream extends InputStream {

        private byte[] bytes;
        private int readPosition = 0;

        public StringInputStream(String string) {
            super();
            bytes = string.getBytes();
        }

        @Override
        public int read() throws IOException {
            if (readPosition >= bytes.length) return -1;

            return bytes[readPosition++];
        }

    }

    @Test
    public void testRead() throws Exception {

        // basics
        assertTransformedEquals("<a href='-TEST-'>", "<a href='test'>");
        assertTransformedEquals("<a href=\"-TEST-\">", "<a href=\"test\">");
        assertTransformedEquals("<a\nhref =\t'-TEST-'>", "<a\nhref =\t'test'>");
        assertTransformedEquals("<a id='c' href='-TEST-' class='c'>",
                                "<a id='c' href='test' class='c'>");
        assertTransformedEquals("... <a href='-A-'> ... <a href=\"-B-\">",
                                "... <a href='a'> ... <a href=\"b\">");

        // should not be transformed
        assertTransformedUnchanged("... <b>without</b> link ...");
        assertTransformedUnchanged("... <a/> ...");
        assertTransformedUnchanged("... <a name='test'> ...");
        assertTransformedUnchanged("... <a hre='test'> ...");
        assertTransformedUnchanged("... <a hreff='test'> ...");
        assertTransformedUnchanged("... <a hr='test' ef='test'> ...");
        assertTransformedUnchanged("... <aa href='test'> ...");
        assertTransformedUnchanged("... <a href> ...");
        assertTransformedUnchanged("... <a href=> ...");
        assertTransformedUnchanged("... <a href = > ...");
        assertTransformedUnchanged("... < a href='test'> ...");
        assertTransformedUnchanged("... <a href='http://www...'> ...");
        assertTransformedUnchanged("... <a href='file://...'> ...");
        assertTransformedUnchanged("... <a href='mailto:me'> ...");

        // " vs '
        assertTransformedUnchanged("... <a name='te\"st'> ...");
        assertTransformedUnchanged("... <a name=\"te'st\"> ...");
        assertTransformedEquals("<a href='-\"TEST\"-'>", "<a href='\"test\"'>");
        assertTransformedEquals("<a href=\"-'TEST'-\">", "<a href=\"'test'\">");

        // 404: file not found
        assertTransformedEquals("<a href='error404.htm' class='error404'>",
                                "<a href='" + NOT_FOUND + "'>");
        assertTransformedEquals("<a class='error404' href='error404.htm'>",
                                "<a class='myClass' href='" + NOT_FOUND + "'>");
        assertTransformedEquals("<a href='error404.htm' class='error404'>",
                                "<a href='" + NOT_FOUND + "' class='myClass'>");

        // TODO # e.g. #anchor

    }

    private static void assertTransformedUnchanged(String beforeAndAfter) throws Exception {
        assertTransformedEquals(beforeAndAfter, beforeAndAfter);
    }

    private static void assertTransformedEquals(String expected, String beforeTransformed) throws Exception {
        StringInputStream in = new StringInputStream(beforeTransformed);

        IHrefResolver dummyResolver = new IHrefResolver() {
            @Override
            public String resolve(String href) {
                if (href.equals(NOT_FOUND)) return null;

                return "-" + href.toUpperCase() + "-";
            }
        };

        TransformedLinksInputStream transformed =
            new TransformedLinksInputStream(in, dummyResolver);
        String out = null;
        byte[] bytes = new byte[1000];
        for (int i = 0; i < bytes.length; i++) {
            int b = transformed.read();
            if (b < 0) {
                out = new String(bytes, 0, i);
                break;
            }
            bytes[i] = (byte) b;
        }

        assertEquals(expected, out);
    }

}
