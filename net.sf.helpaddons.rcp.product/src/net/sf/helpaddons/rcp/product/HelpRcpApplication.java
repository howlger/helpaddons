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
package net.sf.helpaddons.rcp.product;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Locale;

import net.sf.helpaddons.rcp.product.internal.RcpPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBaseResources;
import org.eclipse.help.internal.base.HelpDisplay;
import org.eclipse.help.internal.search.LocalSearchManager;
import org.eclipse.help.internal.search.SearchIndexWithIndexingProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Control;
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

    private static final String ARG_IDENTIFIER_A = "-openFile"; //$NON-NLS-1$
    private static final String ARG_IDENTIFIER_B = "--openFile"; //$NON-NLS-1$
    private static final String ARG_RESTART = "??restart"; //$NON-NLS-1$
    private static final String ARG_SEARCH = "??search="; //$NON-NLS-1$
    private static final String ARG_TOPIC = "??topic="; //$NON-NLS-1$
    private static final String ARG_CONTEXT_ID = "??contextId="; //$NON-NLS-1$

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

    private Integer exitCode = EXIT_OK;

    private volatile boolean openFileAtStartUpDone = false;

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

    public synchronized Object start(final IApplicationContext context)
            throws Exception {

        // use open file feature to open search results
        Listener openFileListener = new Listener() {
            public void handleEvent(final Event event) {
                if (event.text == null) return;
                if (!openFileAtStartUpDone) {
                    openFileAtStartUpDone = true;
                    if (computeOpenFileArg() != null) {
                        open(computeOpenFileArg());
                        return;
                    }
                }
                Display.getCurrent().asyncExec(new Runnable() {
                    public void run() {
                        open(event.text);
                    };
                });
            }
        };
        Display.setAppName(context.getBrandingName());
        Display display = Display.getCurrent();
        if (display == null) {
            display = new Display();
        }
        display.addListener(SWT.OpenDocument, openFileListener);

        // as soon as the UI thread is started (see below)
        // display the help window
        display.asyncExec(new Runnable() {
            public void run() {

                // open help window
                if (!openFileAtStartUpDone) {
                    openFileAtStartUpDone = true;
                    if (computeOpenFileArg() != null) {
                        open(computeOpenFileArg());
                    } else {
                        showStartPage();
                    }
                }

                // end splash (in the org.eclipse.core.runtime.applications
                // extension thread must be set to "main") etc.
                context.applicationRunning();

                // create or update search index on first start-up instead of
                // on first query
                Job ensureIndexUpdated = new Job("") {
                    protected IStatus run(IProgressMonitor monitor) {
                        LocalSearchManager searchManager =
                                BaseHelpSystem.getLocalSearchManager();
                        String locale = Platform.getNL();
                        if (locale == null) {
                            locale = Locale.getDefault().toString();
                        }
                        SearchIndexWithIndexingProgress index =
                                searchManager.getIndex(locale);
                        searchManager.ensureIndexUpdated(monitor, index);
                        return Status.OK_STATUS;
                    }
                };
                ensureIndexUpdated.setPriority(Job.LONG);
                ensureIndexUpdated.schedule();
            }
        });

        // try running UI loop if possible
        if (!ensureHelpWebServerRunning()) return EXIT_OK;
        DisplayUtils.runUI();

        return exitCode;
    }

    private boolean ensureHelpWebServerRunning() {
        BaseHelpSystem.setMode(BaseHelpSystem.MODE_STANDALONE);
        if (!BaseHelpSystem.ensureWebappRunning()) {
            System.out.println(NLS.bind(
                    HelpBaseResources.HelpApplication_couldNotStart, Platform
                    .getLogFileLocation().toOSString()));
            return false;
        }
        return true;
    }

    private void armHelpWindow() {
        if (isExternalBrowserMode()) return;

        // exiting the application by closing the help window
        Shell[] allShells = Display.getCurrent().getShells();
        if (allShells.length != 1) {
            throw new RuntimeException("No or more than one shell found");
        }
        allShells[0].addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
                stopHelp();
            }
        });

        // find dialog on Ctrl+F (if not Internet Explorer which
        // supports this feature out of the box)
        Browser browser = null;
        Control[] children = allShells[0].getChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Browser) {
                browser = (Browser) children[i];
                break;
            }
        }
        if (   browser != null
            && !"ie".equalsIgnoreCase(browser.getBrowserType())) {
            browser.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (    e.keyCode == 'f'
                        && (e.stateMask & SWT.CTRL) != 0
                        && e.widget instanceof Browser) {
                        Browser browser = (Browser) e.widget;
                        e.doit = !browser.execute(
                            "if(parent "
                          + "&& parent.HelpFrame "
                          + "&& parent.HelpFrame.ContentFrame "
                          + "&& parent.HelpFrame.ContentFrame.ContentViewFrame "
                          + "&& parent.HelpFrame.ContentFrame.ContentViewFrame.find){"
                          + "parent.HelpFrame.ContentFrame.ContentViewFrame.find()}"
                          + "else if(window.find){window.find()}");
                    }
                }
            });
        }

    }

    public synchronized void stop() {
        stopHelp();

        // wait until start has finished
        synchronized(this) {};
    }

    /**
     * Override this method if the web browser of the operating system should be
     * used instead of running the application in a separate window.
     *
     * @return {@code true} to use the system web browser;
     *         {@code false} to run as an application with its own window
     */
    protected boolean isExternalBrowserMode() {
        return false;
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

    /**
     * @param args search word to query, topic or context ID to open (search
     *             word and topic can be combined); Examples:
     *             <ul><li>??topic=/my.plugin/path/file.htm</li>
     *                 <li>??search=my query</li>
     *                 <li>??topic=/my.plugin/path/file.htm??search=query</li>
     *                 <li>???contextId=my.plugin.context_sensitive_help_123</li>
     */
    private void open(String args) {

        // restart?
        if (ARG_RESTART.equalsIgnoreCase(args)) {
            exitCode = EXIT_RESTART;
            stopHelp();
            return;
        }

        // context ID?
        if (args.startsWith(ARG_CONTEXT_ID)) {
            String contextId = args.substring(ARG_CONTEXT_ID.length());
            IContext context = HelpSystem.getContext(contextId);
            if (context == null) {
                showPageNotFoundPage();
                return;
            }

            IHelpResource[] topics = context.getRelatedTopics();
            if (topics.length == 0) {
                showPageNotFoundPage();
                return;
            }
            args = ARG_TOPIC + topics[0].getHref();
        }

        // topic to open?
        HelpDisplay help = BaseHelpSystem.getHelpDisplay();
        String topic = "";
        if (args.startsWith(ARG_TOPIC)) {
            int searchStart = args.indexOf(ARG_SEARCH, ARG_TOPIC.length());
            topic = searchStart < 0
                    ? args.substring(ARG_TOPIC.length()).trim()
                    : args.substring(ARG_TOPIC.length(), searchStart).trim();

            // without query?
            if (searchStart < 0) {
                ensureHelpWebServerRunning();
                help.displayHelpResource(
                        "topic=" + encode(args.substring(ARG_TOPIC.length())),
                        isExternalBrowserMode());
                armHelpWindow();
                return;
            }

            args = args.substring(searchStart + ARG_SEARCH.length()).trim();
        }

        // with query
        if (args.startsWith(ARG_SEARCH)) {
            args = args.substring(ARG_SEARCH.length());
        }
        ensureHelpWebServerRunning();
        help.displaySearch("searchWord=" + encode(args),
                           topic,
                           isExternalBrowserMode());
        armHelpWindow();
    }


    private void showStartPage() {
        ensureHelpWebServerRunning();
        BaseHelpSystem.getHelpDisplay().displayHelp(isExternalBrowserMode());
        armHelpWindow();
    }

    private void showPageNotFoundPage() {
        ensureHelpWebServerRunning();
        HelpDisplay helpDisplay = BaseHelpSystem.getHelpDisplay();
        String href =
            Platform.getPreferencesService().getString("org.eclipse.help.base",
                                                       "page_not_found",
                                                       null,
                                                       null);
        helpDisplay.displayHelpResource(href, isExternalBrowserMode());
        armHelpWindow();
    }

    private static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 encoding is always supported, if not:
            e.printStackTrace();
            return string;
        }
    }

    private static String computeOpenFileArg() {
        String[] args = Platform.getCommandLineArgs();
        if (args == null) return null;

        for (int i = 0; i < args.length; i++) {

            // without identifier
            if (   args[i].startsWith(ARG_RESTART)
                || args[i].startsWith(ARG_SEARCH)
                || args[i].startsWith(ARG_TOPIC)
                || args[i].startsWith(ARG_CONTEXT_ID))
                return args[i];

            // with identifier "--openFile"
            if (   i + 1 < args.length
                && (   args[i].equalsIgnoreCase(ARG_IDENTIFIER_A)
                    || args[i].equalsIgnoreCase(ARG_IDENTIFIER_B)))
                return args[i + 1];
        }
        return null;
    }

}
