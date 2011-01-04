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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public abstract class AbstractHrefResolver implements IHrefResolver {

    /** The source directory of the HTML file which contains the links to
     *  resolve. */
    private final IPath sourceDir;

    /**
     * @param sourceHref the path of the HTML file which contains the links to
     *                   resolve
     */
    public AbstractHrefResolver(String sourceHref) {
        sourceDir = new Path(sourceHref).removeLastSegments(1);
    }

    final public String resolve(String targetHref) {
        IPath absolutePath = sourceDir.append(targetHref);
        String absolute = absolutePath.toString();
        if (computeExistsInSourceBundle(absolute)) return targetHref;

        String targetBundle = computeTargetBundle(absolute);
        if (targetBundle == null) return null;

        IPath target = new Path(targetBundle).append(absolutePath);
        IPath source = new Path("_").append(sourceDir);
        return target.makeRelativeTo(source).toString();
    }

    final public String getNotFoundHref() {
        IPath absolute = new Path(getNotFoundHtmlFile());
        return absolute.makeRelativeTo(sourceDir).toString();
    }

    /**
     * @param href the relative path of the HTML file to check (HTML reference)
     * @return {@code true} if and only if the specified HTML file exists in the
     *         source bundle/plug-in
     */
    protected abstract boolean computeExistsInSourceBundle(String href);

    /**
     * @param href the HTML reference (path) of the HTML file to look for
     * @return the bundle symbolic name of the bundle which contains the
     *         specified help resource or {@code null} if no the resource can
     *         not be found in the pool
     */
    protected abstract String computeTargetBundle(String href);

    /**
     * @return the absolute link to the HTML error page which is used as link
     *         target instead of the original target if the target HTML file
     *         does not exist in the pool, e.g. {@code "errors/404.htm"}
     * @see IHrefResolver#getNotFoundHref()
     */
    protected abstract String getNotFoundHtmlFile();

}
