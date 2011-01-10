package net.sf.helpaddons.crosslinkmanager.tests;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Locale;

import net.sf.helpaddons.crosslinkmanager.CrossLinkManagerPlugin;
import net.sf.helpaddons.crosslinkmanager.HelpContentWithCrossLinks;
import net.sf.helpaddons.crosslinkmanager.IStaticHelpContent;

import org.eclipse.help.HelpSystem;
import org.junit.Before;
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
