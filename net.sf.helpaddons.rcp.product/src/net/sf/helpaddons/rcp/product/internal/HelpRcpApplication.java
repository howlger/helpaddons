/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Holger Voormann - initial API and implementation
 *******************************************************************************/
package net.sf.helpaddons.rcp.product.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBaseResources;
import org.eclipse.help.internal.base.HelpDisplay;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

/**
 * This is a modified copy of
 * {@link org.eclipse.help.internal.base.HelpApplication} (Eclipse version
 * 3.6.0). In contrast to {@code HelpApplication} this RCP application does not
 * only start the Eclipse Help system as Web application (like
 * {@code HelpApplication}) but also displays the help window and allows to run
 * more than one application instances in parallel (neither
 * {@code %workspace%/.metadata/.applicationlock} nor
 * {@code %workspace%/.metadata/.connection} will be created).
 */
public class HelpRcpApplication implements IApplication {

    /**
     * One of the states which are defined in
     * {@link org.eclipse.help.internal.base.HelpApplication}:
     * must be equal to {@code HelpApplication.STATE_EXITING}.
     */
    private static final int STATE_EXITING = 0;

    private static final String HELP_UI_PLUGIN_ID =
        "org.eclipse.help.ui"; //$NON-NLS-1$

    private static final String LOOP_CLASS_NAME =
        "org.eclipse.help.ui.internal.HelpUIEventLoop"; //$NON-NLS-1$

    private static final String HELP_APPLICATION_CLASS_NAME =
        "org.eclipse.help.internal.base.HelpApplication"; //$NON-NLS-1$

    /**
     * Copy of org.eclipse.help.internal.base.DisplayUtils (only not used
     * parts have been removed) which is not accessible from other plug-ins:
     *
     * Utility class to control SWT Display and event loop run in
     * org.eclipse.help.ui plug-in
     */
    private static class DisplayUtils {

        static void runUI() {
            invoke("run"); //$NON-NLS-1$
        }

        static void wakeupUI() {
            invoke("wakeup"); //$NON-NLS-1$
        }

        private static void invoke(String method) {
            try {
                Bundle bundle = Platform.getBundle(HELP_UI_PLUGIN_ID);
                if (bundle == null) {
                    return;
                }
                Class c = bundle.loadClass(LOOP_CLASS_NAME);
                Method m = c.getMethod(method, new Class[] {});
                m.invoke(null, new Object[] {});
            } catch (Exception e) {
            }
        }
    }

    public synchronized Object start(IApplicationContext context)
            throws Exception {
//        Display.setAppName(context.getBrandingName());

        // start Help Web Server
        BaseHelpSystem.setMode(BaseHelpSystem.MODE_STANDALONE);
        if (!BaseHelpSystem.ensureWebappRunning()) {
            System.out.println(NLS.bind(
                    HelpBaseResources.HelpApplication_couldNotStart, Platform
                            .getLogFileLocation().toOSString()));
            return EXIT_OK;
        }

        // as soon as the UI thread is started (see below)
        // display the help window
        Display display = Display.getCurrent();
        if (display == null)
            display = new Display();

        // use open file feature to open search results
        display.addListener(SWT.OpenDocument, new Listener(){
            public void handleEvent(final Event event) {
                if (event.text != null) {
                    Display.getCurrent().asyncExec(new Runnable() {
                        public void run() {
                            HelpDisplay helpDisplay = BaseHelpSystem.getHelpDisplay();
                            helpDisplay.displaySearch("searchWord=" + event.text, "", false);
                        };
                    });
                }
            }
        });

        display.asyncExec(new Runnable() {
            public void run() {
                InternalPlatform.getDefault().endSplash();

                // open help window
                HelpDisplay helpDisplay = BaseHelpSystem.getHelpDisplay();
                helpDisplay.displayHelp(false);

                // instead of helpDisplay.displayHelp(false) the following
                // can be used to display the window and run a query:
                // helpDisplay.displaySearch("searchWord=myQuery", "", false);

                // exiting the application by closing the help window
                Shell[] allShells = Display.getCurrent().getShells();
                if (allShells.length != 1) {
                    throw new RuntimeException("No or more than one shells found");
                }

                allShells[0].addListener(SWT.Close, new Listener() {
                    public void handleEvent(Event event) {
                        stopHelp();
                    }
                });

            }
        });

        // try running UI loop if possible
        DisplayUtils.runUI();

        return EXIT_OK;
    }

    public synchronized void stop() {
        stopHelp();

        // wait until start has finished
        synchronized(this) {};
   }

    /**
     * Causes help service to stop and exit
     */
    public static void stopHelp() {

        // in HelpApplication set the private static field
        // status = STATE_EXITING;
        // via reflection:
        Bundle bundle = Platform.getBundle(HELP_UI_PLUGIN_ID);
        if (bundle == null) {
            return;
        }
        try {
            Class c = bundle.loadClass(HELP_APPLICATION_CLASS_NAME);
            Field statusField = c.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.setInt(null, STATE_EXITING);
        } catch (SecurityException e) {
            RcpPlugin.log(e);
        } catch (IllegalArgumentException e) {
            RcpPlugin.log(e);
        } catch (ClassNotFoundException e) {
            RcpPlugin.log(e);
        } catch (NoSuchFieldException e) {
            RcpPlugin.log(e);
        } catch (IllegalAccessException e) {
            RcpPlugin.log(e);
        }

        // UI loop may be sleeping if no SWT browser is up
        DisplayUtils.wakeupUI();
    }

}
