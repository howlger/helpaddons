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

}
