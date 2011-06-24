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

import static org.junit.Assert.*;
import net.sf.helpaddons.crosslinkmanager.HelpContentWithCrossLinks;

import org.junit.Test;

public class HelpContentWithCrossLinksTest {

    @Test
    public void testIsHtml() {
        assertTrue(HelpContentWithCrossLinks.hasHtmlFileExtension("test.htm"));
        assertTrue(HelpContentWithCrossLinks.hasHtmlFileExtension("test.html"));
        assertTrue(HelpContentWithCrossLinks.hasHtmlFileExtension("test.xhtml"));
        assertFalse(HelpContentWithCrossLinks.hasHtmlFileExtension("xhtml.gif"));

        // query and anchor
        assertTrue(HelpContentWithCrossLinks.hasHtmlFileExtension("test.htm?query=test"));
        assertTrue(HelpContentWithCrossLinks.hasHtmlFileExtension("test.htm#anchor"));
        assertTrue(HelpContentWithCrossLinks.hasHtmlFileExtension("test.htm?query=test#anchor"));
    }

}
