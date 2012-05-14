/*******************************************************************************
 * Copyright (c) 2012 Holger Voormann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Holger Voormann - initial API and implementation
 *******************************************************************************/
package net.sf.helpaddons.rcp.product;

/**
 * This application creates or update the search index which is used by the
 * {@link HelpWebApplication}.
 */
public class WebIndexer extends AbstractIndexer {

    protected boolean indexWebLocales() {
        return true;
    }

}
