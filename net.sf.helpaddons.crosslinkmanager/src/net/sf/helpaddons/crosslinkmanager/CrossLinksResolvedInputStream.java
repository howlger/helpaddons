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
package net.sf.helpaddons.crosslinkmanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * 8-bit encoded only (UTF-8 and other codepages but not UTF-16);
 *
 * case insensitive: tag and attribute names; "a" must be followed by a blank
 * (not by a tab or new line);
 *
 * attribute values could be enclosed by double or single quotes:
 * {@code href="..."} or {@code href='...'}.
 *
 * Length of link start tag {@code <a href="..." ... >} must be less or equal
 * {@link #BUFFER_SIZE}.
 *
 * Examples:
 * <ul>
 *   <li><code>&lt;!--a href="target"-->...&lt;!--/a--></code> becomes
 *       <code>&lt;a href="resolved-target">...&lt;/a></code></li>
 *   <li><code>&lt;!--a href="not-available"-->...&lt;!--/a--></code> becomes
 *       <code>&lt;a href="error404.htm" class="error404">...&lt;/a></code></li>
 * </ul>
 */
public class CrossLinksResolvedInputStream extends InputStream {

    private static final String ENCODING = "UTF-8";

    private static final int BUFFER_SIZE = 4096;

    private static final int CHAR_LT    = (int)'<';
    private static final int CHAR_DASH  = (int)'-';
    private static final int CHAR_SLASH = (int)'/';
    private static final int CHAR_BLANK = (int)' ';
    private static final int CHAR_GT    = (int)'>';
    private static final int CHAR_EQUAL = (int)'=';
    private static final int CHAR_QUOTE = (int)'"';
    private static final int CHAR_SINGLEQUOTE = (int)'\'';
    private static final int CHAR_EXCLAMATION_MARK = (int)'!';

    private static final int CHAR_A = (int)'a';
    private static final int CHAR_H = (int)'h';
    private static final int CHAR_R = (int)'r';
    private static final int CHAR_E = (int)'e';
    private static final int CHAR_F = (int)'f';
    private static final int CHAR_C = (int)'c';
    private static final int CHAR_L = (int)'l';
    private static final int CHAR_S = (int)'s';

    /** Wrapped in(put stream) to transform. */
    private final InputStream in;

    /** Resolver to transform 'href' attribute values. */
    private final IHrefResolver hrefResolver;

    private final byte[] notFoundClassName;
    private final int[] ringBuffer = new int[BUFFER_SIZE];
    private int ringBufferStart = 0;
    private int ringBufferEnd = 0;
    private boolean awaitEndTag = false;

    public CrossLinksResolvedInputStream(InputStream in,
                                         IHrefResolver hrefResolver) {
        this.in = in;
        this.hrefResolver = hrefResolver;
        this.notFoundClassName = computeClassName(hrefResolver);
    }

    private static byte[] computeClassName(IHrefResolver hrefResolver) {
        try {
            return hrefResolver.getNotFoundClassName().getBytes(ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return hrefResolver.getNotFoundClassName().getBytes();
        }
   }

    public int read() throws IOException {

        if (ringBufferStart == ringBufferEnd) {

            // next char: '<'?
            int read;
            if ((read = in.read()) != CHAR_LT) return read;

            // look ahead:

            // next char: '!'?
            read = in.read();
            ringBuffer[ringBufferEnd] = read;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            if (read != CHAR_EXCLAMATION_MARK) return CHAR_LT;

            // next char: '-'?
            read = in.read();
            ringBuffer[ringBufferEnd] = read;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            if (read != CHAR_DASH) return CHAR_LT;

            // next char: '-'?
            read = in.read();
            ringBuffer[ringBufferEnd] = read;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            if (read != CHAR_DASH) return CHAR_LT;

            // await end tag: <!--/a-->
            if (awaitEndTag) {

                // next char: '/'?
                read = in.read();
                ringBuffer[ringBufferEnd] = read;
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                if (read != CHAR_SLASH) return CHAR_LT;

                // next char: 'a'?
                read = in.read();
                ringBuffer[ringBufferEnd] = read;
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                if (read != CHAR_A) return CHAR_LT;

                // next char: '-'?
                read = in.read();
                ringBuffer[ringBufferEnd] = read;
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                if (read != CHAR_DASH) return CHAR_DASH;

                // next char: '-'?
                read = in.read();
                ringBuffer[ringBufferEnd] = read;
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                if (read != CHAR_DASH) return CHAR_DASH;

                // next char: '>'?
                read = in.read();
                ringBuffer[ringBufferEnd] = read;
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                if (read != CHAR_GT) return CHAR_GT;

                // replace with </a>
                ringBuffer[ringBufferStart] = CHAR_SLASH;
                ringBuffer[(ringBufferStart + 1) % BUFFER_SIZE] = CHAR_A;
                ringBuffer[(ringBufferStart + 2) % BUFFER_SIZE] = CHAR_GT;
                ringBufferEnd = (ringBufferStart + 3) % BUFFER_SIZE;
                awaitEndTag = false;
                return CHAR_LT;

            }

            // next char: 'a'?
            read = in.read();
            ringBuffer[ringBufferEnd] = read;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            if (read != CHAR_A) return CHAR_LT;

            // next char: ' '?
            read = in.read();
            ringBuffer[ringBufferEnd] = read;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            if (read != CHAR_BLANK) return CHAR_LT;

            // fill buffer until "-->"
            int bufferLimit = (ringBufferStart + BUFFER_SIZE - 1) % BUFFER_SIZE;
            while (true) {

                // "-"
                read = in.read();
                ringBuffer[ringBufferEnd] = read;
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                if (   read < 0
                    || ringBufferEnd == bufferLimit) return CHAR_LT;
                if (read != CHAR_DASH) continue;

                // "--"
                read = in.read();
                ringBuffer[ringBufferEnd] = read;
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                if (   read < 0
                    || ringBufferEnd == bufferLimit) return CHAR_LT;
                if (read != CHAR_DASH) continue;

                // "-->"
                read = in.read();
                ringBuffer[ringBufferEnd] = read;
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                if (   read < 0
                    || ringBufferEnd == bufferLimit) return CHAR_LT;
                if (read == CHAR_GT) break;
            }

            // morph
            final int size = ringBufferEnd >= ringBufferStart
                             ? ringBufferEnd - ringBufferStart
                             : ringBufferEnd + BUFFER_SIZE - ringBufferStart;

            int hrefStart = -6;
            int hrefLength = -1;
            int classStart = -7;
            int classLength = -1;
            int classPrefixLength = 0;
            boolean doubleQuote = true;
            byte[] bytes = new byte[size];
            for (int i = 0; i < size; i++) {
                int current = ringBuffer[(ringBufferStart + i) % BUFFER_SIZE];

                if (   (hrefStart == -6 || hrefLength >= 0)
                    && (classStart == -7 || classLength >= 0)) {
                    if (hrefStart == -6) {
                        hrefStart = current == CHAR_H ? -5 : -6;
                    }
                    if (classStart == -7) {
                        classStart = current == CHAR_C ? -6 : -7;
                    }
                } else if (hrefLength >= 0 && classLength >= 0) {
                    // NOP (performance: skip other if tests)
                } else if (   hrefStart > 0
                           && hrefLength < 0
                           && (   (doubleQuote && current == CHAR_QUOTE)
                               || (!doubleQuote && current == CHAR_SINGLEQUOTE))) {
                    hrefLength = i - hrefStart;
                } else if (   classStart > 0
                           && classLength < 0
                           && (   (doubleQuote && current == CHAR_QUOTE)
                               || (!doubleQuote && current == CHAR_SINGLEQUOTE))) {
                    classLength = i - classStart;
                } else if (   classStart > 0
                           && classLength < 0
                           && current == CHAR_LT) {
                    classPrefixLength = i - classStart;
                } else if (hrefStart == -5) {
                    hrefStart = current == CHAR_R ? -4 : -6;
                } else if (hrefStart == -4) {
                    hrefStart = current == CHAR_E ? -3 : -6;
                } else if (hrefStart == -3) {
                    hrefStart = current == CHAR_F ? -2 : -6;
                } else if (hrefStart == -2) {
                    if (current == CHAR_EQUAL) {
                        hrefStart = -1;
                    }
                } else if (hrefStart == -1) {
                    doubleQuote = current == CHAR_QUOTE;
                    if (doubleQuote || current == CHAR_SINGLEQUOTE) {
                        hrefStart = i + 1;
                    }
                } else if (classStart == -6) {
                    classStart = current == CHAR_L ? -5 : -7;
                } else if (classStart == -5) {
                    classStart = current == CHAR_A ? -4 : -7;
                } else if (classStart == -4) {
                    classStart = current == CHAR_S ? -3 : -7;
                } else if (classStart == -3) {
                    classStart = current == CHAR_S ? -2 : -7;
                } else if (classStart == -2) {
                    if (current == CHAR_EQUAL) {
                        classStart = -1;
                    }
                } else if (classStart == -1) {
                    doubleQuote = current == CHAR_QUOTE;
                    if (doubleQuote || current == CHAR_SINGLEQUOTE) {
                        classStart = i + 1;
                    }
                }

                bytes[i] = (byte)current;
            }
            awaitEndTag = resolve(bytes,
                                  hrefStart,
                                  hrefLength,
                                  classStart,
                                  classLength,
                                  classPrefixLength);
            return CHAR_LT;
        }

        int result = ringBuffer[ringBufferStart];
        ringBufferStart = (ringBufferStart + 1) % BUFFER_SIZE;
        return result;

    }

    private boolean resolve(byte[] bytes,
                            int hrefStart,
                            int hrefLength,
                            int classStart,
                            int classLength,
                            int classPrefixLength) throws IOException {

        // required: "href" attribute
        if (hrefLength <= 0) return false;

        String href = new String(bytes, hrefStart, hrefLength, ENCODING);
        String hrefResolved = hrefResolver.resolve(href);
        String linkPrefix = classPrefixLength <= 0
                            ? null
                            : new String(bytes, classStart, classPrefixLength, ENCODING);
        byte[] newHref = hrefResolved == null
                         ? hrefResolver.getNotFoundHref(linkPrefix).getBytes(ENCODING)
                         : hrefResolved.getBytes(ENCODING);

        // cross-link target not found and "class=..." exists?
        if (hrefResolved == null && classLength >= 0) {

            // class=... href=...
            if (classStart < hrefStart) {

                // beginning without "!--"
                ringBufferEnd = ringBufferStart;
                for (int i = 3; i < classStart; i++) {
                    ringBuffer[ringBufferEnd] = bytes[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }

                // "class"
                for (int i = 0; i < notFoundClassName.length; i++) {
                    ringBuffer[ringBufferEnd] = notFoundClassName[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }

                // middle (part between class=... and href=...)
                for (int i = classStart + classLength; i < hrefStart; i++) {
                    ringBuffer[ringBufferEnd] = bytes[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }

                // "href"
                for (int i = 0; i < newHref.length; i++) {
                    ringBuffer[ringBufferEnd] = newHref[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }

                // rest
                for (int i = hrefStart + hrefLength; i < bytes.length - 3; i++) {
                    ringBuffer[ringBufferEnd] = bytes[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }
                ringBuffer[ringBufferEnd] = bytes[bytes.length - 1];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;

            // href=... class=...
            } else {

                // beginning without "!--"
                ringBufferEnd = ringBufferStart;
                for (int i = 3; i < hrefStart; i++) {
                    ringBuffer[ringBufferEnd] = bytes[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }

                // "href"
                for (int i = 0; i < newHref.length; i++) {
                    ringBuffer[ringBufferEnd] = newHref[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }

                // middle (part between href=... and class=...)
                for (int i = hrefStart + hrefLength; i < classStart; i++) {
                    ringBuffer[ringBufferEnd] = bytes[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }

                // "class"
                for (int i = 0; i < notFoundClassName.length; i++) {
                    ringBuffer[ringBufferEnd] = notFoundClassName[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }

                // rest
                for (int i = classStart + classLength; i < bytes.length - 3; i++) {
                    ringBuffer[ringBufferEnd] = bytes[i];
                    ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
                }
                ringBuffer[ringBufferEnd] = bytes[bytes.length - 1];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;

            }

            return true;
        }

        // beginning without "!--"
        ringBufferEnd = ringBufferStart;
        if (classPrefixLength > 0 && classStart < hrefStart) {
            for (int i = 3; i < classStart; i++) {
                ringBuffer[ringBufferEnd] = bytes[i];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            }
            for (int i = classPrefixLength + 1; i < classLength; i++) {
                ringBuffer[ringBufferEnd] = bytes[classStart + i];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            }
            for (int i = classStart + classLength; i < hrefStart; i++) {
                ringBuffer[ringBufferEnd] = bytes[i];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            }
        } else {
            for (int i = 3; i < hrefStart; i++) {
                ringBuffer[ringBufferEnd] = bytes[i];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            }
        }


        // replace "href" value
        for (int i = 0; i < newHref.length; i++) {
            ringBuffer[ringBufferEnd] = newHref[i];
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
        }

        // end quote char
        ringBuffer[ringBufferEnd] = bytes[hrefStart + hrefLength];
        ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;

        // add: class="..."?
        if (hrefResolved == null) {
            ringBuffer[ringBufferEnd] = CHAR_BLANK;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            ringBuffer[ringBufferEnd] = CHAR_C;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            ringBuffer[ringBufferEnd] = CHAR_L;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            ringBuffer[ringBufferEnd] = CHAR_A;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            ringBuffer[ringBufferEnd] = CHAR_S;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            ringBuffer[ringBufferEnd] = CHAR_S;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            ringBuffer[ringBufferEnd] = CHAR_EQUAL;
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            ringBuffer[ringBufferEnd] = bytes[hrefStart - 1]; // quote
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            for (int i = 0; i < notFoundClassName.length; i++) {
                ringBuffer[ringBufferEnd] = notFoundClassName[i];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            }
            ringBuffer[ringBufferEnd] = bytes[hrefStart - 1]; // quote
            ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
        }

        // replace rest
        if (classPrefixLength > 0 && classStart > hrefStart) {
            for (int i = hrefStart + hrefLength + 1; i < classStart; i++) {
                ringBuffer[ringBufferEnd] = bytes[i];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            }
            for (int i = classPrefixLength + 1; i < classLength; i++) {
                ringBuffer[ringBufferEnd] = bytes[classStart + i];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            }
            for (int i = classStart + classLength; i < bytes.length - 3; i++) {
                ringBuffer[ringBufferEnd] = bytes[i];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            }
        } else {
            for (int i = hrefStart + hrefLength + 1; i < bytes.length - 3; i++) {
                ringBuffer[ringBufferEnd] = bytes[i];
                ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;
            }
        }

        // '>'
        ringBuffer[ringBufferEnd] = bytes[bytes.length - 1];
        ringBufferEnd = (ringBufferEnd + 1) % BUFFER_SIZE;

        return true;

    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } finally {
            super.close();
        }
    }

}
