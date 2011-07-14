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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import net.sf.helpaddons.crosslinkmanager.CrossLinksResolvedInputStream;
import net.sf.helpaddons.crosslinkmanager.IHrefResolver;

import org.junit.Test;


public class CrossLinksResolvedInputStreamTest {

    private static String NOT_FOUND = "this_target_does_not_exist";


    @Test
    public void testRead1() throws Exception {

        assertTransformedEquals("...< abc-->...", "...< abc-->...");
        assertTransformedEquals("...<! abc-->...", "...<! abc-->...");
        assertTransformedEquals("...<!- abc-->...", "...<!- abc-->...");
        assertTransformedEquals("...<!-- abc-->...", "...<!-- abc-->...");
        assertTransformedEquals("...<!--a abc-->...", "...<!--a abc-->...");
        assertTransformedEquals("...<!--a ab->c-->...", "...<!--a ab->c-->...");
        assertTransformedEquals("...<!--a abc-->...<!--/a-->#", "...<!--a abc-->...<!--/a-->#");

        assertTransformedEquals("...<a href=\"-TARGET.HTM-\">...", "...<!--a href=\"target.htm\"-->...");
        assertTransformedEquals("...<a href=\"-TARGET.HTM-\">...</a>...", "...<!--a href=\"target.htm\"-->...<!--/a-->...");
        assertTransformedEquals("...<a href=\"-TARGET.HTM-\">text<!--comment-->text</a>...",
                                "...<!--a href=\"target.htm\"-->text<!--comment-->text<!--/a-->...");
        assertTransformedEquals("...<a href=\"-TARGET.HTM-\">text<!--a comment-->text</a>...",
                                "...<!--a href=\"target.htm\"-->text<!--a comment-->text<!--/a-->...");
        assertTransformedEquals("...<a href=\"-TARGET.HTM-\">text<!--a comment-->text</a>...<!--/a-->...",
                                "...<!--a href=\"target.htm\"-->text<!--a comment-->text<!--/a-->...<!--/a-->...");

        assertTransformedEquals("...<a href=\"error404.htm\" class=\"error404\">...",
                                "...<!--a href=\"" + NOT_FOUND + "\"-->...");

        assertTransformedEquals("...<a class=\"error404\" href=\"error404.htm\">...",
                                "...<!--a class=\"dummy\" href=\"" + NOT_FOUND + "\"-->...");
        assertTransformedEquals("...<a class='error404' href='error404.htm'>...",
                                "...<!--a class='dummy' href='" + NOT_FOUND + "'-->...");
        assertTransformedEquals("...<a href='error404.htm' class='error404'>...",
                                "...<!--a href='" + NOT_FOUND + "' class='dummy'-->...");

        assertTransformedEquals(
                "<a href='error404.htm' class='error404'>",
                "<!--a href='" + NOT_FOUND + "'-->");
        assertTransformedEquals(
                "<a href='error404.htm' class='error404'>",
                "<!--a href='" + NOT_FOUND + "' class='dummy'-->");
        assertTransformedEquals(
                "<a class='error404' href='error404.htm'>",
                "<!--a class='dummy' href='" + NOT_FOUND + "'-->");
    }

    @Test
    public void testRead() throws Exception {

        // basics
        assertTransformedEquals("<a href='-TEST-'>", "<!--a href='test'-->");
        assertTransformedEquals("<a href=\"-TEST-\">", "<!--a href=\"test\"-->");
        assertTransformedEquals("<a href =\t'-TEST-'>", "<!--a href =\t'test'-->");
        assertTransformedEquals("<a id='c' href='-TEST-' class='c'>",
                                "<!--a id='c' href='test' class='c'-->");
        assertTransformedEquals("<a href='-A-'>..</a>..<a href=\"-B-\">",
                                "<!--a href='a'-->..<!--/a-->..<!--a href=\"b\"-->");

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
        assertTransformedEquals("<a href='-\"TEST\"-'>", "<!--a href='\"test\"'-->");
        assertTransformedEquals("<a href=\"-'TEST'-\">", "<!--a href=\"'test'\"-->");

        // 404: file not found
        assertTransformedEquals("<a href='error404.htm' class='error404'>",
                                "<!--a href='" + NOT_FOUND + "'-->");
        assertTransformedEquals("<a class='error404' href='error404.htm'>",
                                "<!--a class='myClass' href='" + NOT_FOUND + "'-->");
        assertTransformedEquals("<a href='error404.htm' class='error404'>",
                                "<!--a href='" + NOT_FOUND + "' class='myClass'-->");

        // TODO # e.g. #anchor

    }

    @Test
    public void testTargetSpecificErrorPages() throws Exception {
        assertTransformedEquals(
                "<a href='-T-' class='abc'>text</a>",
                "<!--a href='prefix<t' class='abc'-->text<!--/a-->");
        assertTransformedEquals(
                "<a class='abc' href='-T-'>text</a>",
                "<!--a class='abc' href='prefix<t'-->text<!--/a-->");
        assertTransformedEquals(
                "<a href='myError404.htm' class='error404'>text</a>",
                "<!--a href='my<" + NOT_FOUND + "' class='abc'-->text<!--/a-->");
        assertTransformedEquals(
                "<a class='error404' href='myError404.htm'>text</a>",
                "<!--a class='abc' href='my<" + NOT_FOUND + "'-->text<!--/a-->");
    }

    @Test
    public void testBufferOverflow() throws Exception {
        char[] chars9000 = new char[9000];
        Arrays.fill(chars9000, 'o');
        String o1000 = String.valueOf(chars9000);

        assertTransformedEquals(
              "...<!--a alt='too lo'" + o1000 + "ng' href='target.htm'-->"
            + "text1<!--/a-->..."
            + "<a href='-TARGET.HTM-'>text2</a>",
              "...<!--a alt='too lo'" + o1000 + "ng' href='target.htm'-->"
            + "text1<!--/a-->..."
            + "<!--a href='target.htm'-->text2<!--/a-->");
    }

    private static void assertTransformedUnchanged(String beforeAndAfter) throws Exception {
        assertTransformedEquals(beforeAndAfter, beforeAndAfter);
    }

    private static void assertTransformedEquals(String expected,
                                                String actual) throws Exception {
        IHrefResolver dummyResolver = new IHrefResolver() {

            @Override
            public String resolve(String href) {
                if (href.equals(NOT_FOUND)) return null;

                return "-" + href.toUpperCase() + "-";
            }

            @Override
            public String getNotFoundHref(String targetId) {
                return "my".equals(targetId)
                        ? "myError404.htm"
                        : "error404.htm";
            }

            @Override
            public String getNotFoundClassName() {
                return "error404";
            }

        };

        InputStream transformed =
            new CrossLinksResolvedInputStream(new StringInputStream(actual),
                                              dummyResolver);

        String out = null;
        byte[] bytes = new byte[10000];
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


}
