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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Deque;
import java.util.LinkedList;

// TODO not for UTF-16
public class CrossLinksResolvedInputStream extends InputStream {

    /** The String ' class=' as bytes. */
    private static final byte[] CLASS_IS = " class=".getBytes(); //$NON-NLS-1$

    private boolean endOfIn = false;
    private boolean awaitStartOfElement = true;

    private enum ReaderStatus {AWAIT_ATTRIBUTE,
                               READING_HREF,
                               READING_CLASS,
                               READING_MISC,
                               AWAIT_EQUALS_SIGN,
                               AWAIT_VALUE }

    private Deque<Byte> buffer = new LinkedList<Byte>();

    /** Wrapped in(put stream) to transform. */
    private final InputStream in;

    /** Resolver to transform 'href' attribute values. */
    private final IHrefResolver hrefResolver;

    private final byte[] notFoundClassName;

    public CrossLinksResolvedInputStream(InputStream in, IHrefResolver hrefResolver) {
        this.in = in;
        this.hrefResolver = hrefResolver;
        this.notFoundClassName = computeClassName(hrefResolver);
    }

    private static byte[] computeClassName(IHrefResolver hrefResolver) {
         try {
             return hrefResolver.getNotFoundClassName().getBytes("UTF-8");
         } catch (UnsupportedEncodingException e) {
             e.printStackTrace();
             return hrefResolver.getNotFoundClassName().getBytes();
         }
    }

    @Override
    public int read() throws IOException {

        // flush buffer?
        if (!awaitStartOfElement) {
            if (!buffer.isEmpty()) {
                return buffer.pollFirst().byteValue();
            }
            awaitStartOfElement = true;
        }

        // reading from wrapped in finished?
        if (endOfIn) return -1;

        // read
        int read = in.read();

        // end of in?
        if (read < 0) {
            endOfIn = true;
            awaitStartOfElement = false;
            return -1;
        }

        // start?
        if (read == '<') {
            tryReadAndTransformElement();
            awaitStartOfElement = false;
            return '<';
        }

        // bypass read
        return read;
    }

    // TODO max length
    private void tryReadAndTransformElement() throws IOException {

        // read <a> tag name?
        int read = in.read();
        if (read != 'a' && read != 'A') {
            if (read < 0) {
                endOfIn = true;
            } else {
                buffer(read);
            }
            return;
        }
        buffer(read);
        if ((read = in.read()) < 0) {
            endOfIn = true;
            return;
        }
        buffer(read);
        if (!Character.isWhitespace((char)read)) return;

        // read <a> tag content
        ReaderStatus status = ReaderStatus.AWAIT_ATTRIBUTE;
        int start = buffer.size();
        int hrefValueStart = -1;
        int hrefValueEnd = -1;
        int classValueStart = -1;
        int classValueEnd = -1;
        boolean hrefRead = false;
        while ((read = in.read()) != '>') {
            if (read < 0) {
                endOfIn = true;
                return;
            }

            // quoted?
            if (read == '\"' || read == '\'') {
                buffer(read);
                if (status == ReaderStatus.AWAIT_VALUE) {
                    if (hrefRead) {
                        hrefValueStart = buffer.size();
                    } else {
                        classValueStart = buffer.size();
                    }
                }
                int quoteChar = read;
                while ((read = in.read()) >= 0) {
                    if (read == quoteChar) break;

                    buffer(read);

                    // skip href which contains ':', e.g. mailto:...; http:...
                    if (hrefRead && read == ':') return;
                }
                if (read < 0) {
                    endOfIn = true;
                    return;
                }
                if (status == ReaderStatus.AWAIT_VALUE) {
                    if (hrefRead) {
                        hrefValueEnd = buffer.size();
                    } else {
                        classValueEnd = buffer.size();
                    }
                    status = ReaderStatus.AWAIT_ATTRIBUTE;
                }
            }
            buffer(read);
            if (Character.isLetter((char)read)) {

                int attributeNameIndex = buffer.size() - start - 1;

                // href
                if (attributeNameIndex == 0 && read == 'h') {
                    status = ReaderStatus.READING_HREF;
                } else if (attributeNameIndex == 1 && read == 'r' && status == ReaderStatus.READING_HREF) {
                    //
                } else if (attributeNameIndex == 2 && read == 'e' && status == ReaderStatus.READING_HREF) {
                    //
                } else if (attributeNameIndex == 3 && read == 'f' && status == ReaderStatus.READING_HREF) {
                    hrefRead = true;
                    status = ReaderStatus.AWAIT_EQUALS_SIGN;

                // class
                } else if (attributeNameIndex == 0 && read == 'c') {
                    status = ReaderStatus.READING_CLASS;
                } else if (attributeNameIndex == 1 && read == 'l' && status == ReaderStatus.READING_CLASS) {
                    //
                } else if (attributeNameIndex == 2 && read == 'a' && status == ReaderStatus.READING_CLASS) {
                    //
                } else if (attributeNameIndex == 3 && read == 's' && status == ReaderStatus.READING_CLASS) {
                    //
                } else if (attributeNameIndex == 4 && read == 's' && status == ReaderStatus.READING_CLASS) {
                    hrefRead = false;
                    status = ReaderStatus.AWAIT_EQUALS_SIGN;

                // other
                } else {
                    status = ReaderStatus.READING_MISC;
                }

            } else {

                if (status == ReaderStatus.AWAIT_EQUALS_SIGN && read == '=') {
                    status = ReaderStatus.AWAIT_VALUE;
                } else if (Character.isWhitespace((char)read)) {
                    start = buffer.size();
                    if (   status == ReaderStatus.READING_MISC
                        || status == ReaderStatus.READING_HREF) {
                        status = ReaderStatus.AWAIT_ATTRIBUTE;
                    }

                }

            }
        }
        buffer(read); // add '>'

        // replace
        if (hrefValueEnd > hrefValueStart) {
            replace(hrefValueStart, hrefValueEnd, classValueStart, classValueEnd);
        }

    }

    private void replace(int hrefValueStart,
                         int hrefValueEnd,
                         int classValueStart,
                         int classValueEnd) throws UnsupportedEncodingException {

        // TODO
        // -1 < hrefValueStart < hrefValueEnd < classValueStart < classValueEnd
        // or...


        // rest...
        byte[] rest = new byte[   buffer.size()
                               - (hrefValueEnd > classValueEnd
                                  ? hrefValueEnd
                                  : classValueEnd)];
        for (int i = rest.length - 1; i >= 0; i--) {
            rest[i] = buffer.pollLast();
        }

        // ...href, clazz, midsection
        byte[] href = new byte[0];
        byte[] clazz = new byte[0];
        byte[] midsection = new byte[0];

        // last value (href or class)
        if (hrefValueEnd > classValueEnd) {
            href = new byte[hrefValueEnd - hrefValueStart];
            for (int i = href.length - 1; i >= 0; i--) {
                href[i] = buffer.pollLast();
            }
        } else if (classValueEnd >= 0) {
            clazz = new byte[classValueEnd - classValueStart];
            for (int i = clazz.length - 1; i >= 0; i--) {
                clazz[i] = buffer.pollLast();
            }
        }

        // class value exists?
        boolean classValueExists = classValueEnd > classValueStart;
        if (classValueExists) {

            // midsection
            midsection = new byte[hrefValueStart > classValueEnd
                                  ? hrefValueStart - classValueEnd
                                  : classValueStart - hrefValueEnd];
            for (int i = midsection.length - 1; i >= 0; i--) {
                midsection[i] = buffer.pollLast();
            }

            // last value
            if (hrefValueEnd > classValueEnd) {
                clazz = new byte[classValueEnd - classValueStart];
                for (int i = clazz.length - 1; i >= 0; i--) {
                    clazz[i] = buffer.pollLast();
                }
            } else if (classValueEnd >= 0) {
                href = new byte[hrefValueEnd - hrefValueStart];
                for (int i = href.length - 1; i >= 0; i--) {
                    href[i] = buffer.pollLast();
                }
            }

        }

        String hrefResolved = hrefResolver.resolve(new String(href));
        byte[] hrefReplacement = hrefResolved == null
                                 ? hrefResolver.getNotFoundHref().getBytes("UTF-8")
                                 : hrefResolved.getBytes("UTF-8");

        // first value
        if (!classValueExists || hrefValueEnd < classValueEnd) {
            for (byte b : hrefReplacement) {
                buffer(b);
            }
        } else if (classValueExists && hrefValueEnd > classValueEnd) {
            for (byte b : hrefResolved == null ? notFoundClassName : clazz) {
                buffer(b);
            }
        }

        // two values: href and class
        if (classValueExists) {

            // midsection
            for (byte b : midsection) {
                buffer(b);
            }

            // second value
            if (hrefValueEnd > classValueEnd) {
                for (byte b : hrefReplacement) {
                    buffer(b);
                }
            } else {
                for (byte b : hrefResolved == null ? notFoundClassName : clazz) {
                    buffer(b);
                }
            }

        }

        // add class="error404"?
        if (!classValueExists && hrefResolved == null) {
            buffer(rest[0]); // 'href' end quote
            for (byte b : CLASS_IS) {
                buffer(b);
            }
            buffer(rest[0]); // 'href' end quote as 'class' start quote
            for (byte b : notFoundClassName) {
                buffer(b);
            }
            // the end quote is the first byte of rest
        }

        // rest (starts with ending quote
        for (byte b : rest) {
            buffer(b);
        }
    }

    private void buffer(int toBuffer) {
        buffer.add(Byte.valueOf((byte) toBuffer));
    }

}
