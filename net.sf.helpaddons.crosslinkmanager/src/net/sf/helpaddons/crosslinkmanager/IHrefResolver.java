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

public interface IHrefResolver {

    /**
     * @param href the link target to resolve
     * @return the (maybe redirected) link target or {@code null} if the link
     *         can not be resolved (error 404: file not found)
     */
    String resolve(String href);

    /**
     * @param hrefPrefix the HTML cross-link "href" attribute value prefix
     *                   (separated by '<' from the rest, e.g.
     *                   {@code <!--a ... href='prefix<dir/topic.htm' ... -->})
     *                   or {@code null} if HTML cross-link doesn't contain
     *                   such a prefix
     * @return the relative link to the HTML error page which is used as link
     *         target instead of the original target if the target HTML file
     *         does not exist in the pool, e.g. {@code "../errors/404.htm"}
     */
    String getNotFoundHref(String hrefPrefix);

    /**
     * @return the HTML class name of links without an existing link target,
     *         e.g. {@code "error404"}
     */
    String getNotFoundClassName();

}
