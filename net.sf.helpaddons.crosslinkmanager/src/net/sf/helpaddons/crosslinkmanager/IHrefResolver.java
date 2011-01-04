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
     * @return the the maybe redirected link target or {@code null} if the link
     *         can not resolved (error 404: file not found)
     */
    String resolve(String href);

    /**
     * @return the relative link to the HTML error page which is used as link
     *         target instead of the original target if the target HTML file
     *         does not exist in the pool, e.g. {@code "../errors/404.htm"}
     */
    String getNotFoundHref();

    /**
     * @return the HTML class name of links without an existing link target,
     *         e.g. {@code "error404"}
     */
    String getNotFoundClassName();

}
