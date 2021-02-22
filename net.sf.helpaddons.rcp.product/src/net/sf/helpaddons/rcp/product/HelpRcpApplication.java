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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpApplication;
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
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Bundle;

import net.sf.helpaddons.rcp.product.internal.RcpPlugin;

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
    private static final String ARG_RESTART = "/restart"; //$NON-NLS-1$
    private static final String ARG_SHUTDOWN = "/shutdown"; //$NON-NLS-1$
    private static final String ARG_SEARCH = "/search"; //$NON-NLS-1$
    private static final String ARG_TOPIC = "/topic"; //$NON-NLS-1$
    private static final String ARG_CONTEXT_ID = "/csh"; //$NON-NLS-1$

    // last
    private static final int LAST_TIMEOUT = 3000;

    private volatile String lastTopic;
    private volatile long lastTopicTime;
    private volatile String lastSearch;
    private volatile long lastSearchTime;

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

    private static final Object STARTED_INSTANCE_LOCK = new Object();
    private static volatile HelpRcpApplication startedInstance;
    private static volatile String openFile;

    private Integer exitCode = EXIT_OK;

    private static final Listener OPEN_FILE_LISTENER = new Listener() {
        public void handleEvent(final Event event) {
            if (event.text == null) return;

            synchronized (STARTED_INSTANCE_LOCK) {

                // started?
                if (startedInstance == null) {
                    openFile = event.text.trim();
                    return;
                }

                // if started show
                display().asyncExec(new Runnable() {
                    public void run() {
                        startedInstance.open(event.text);
                    };
                });

            }
        }
    };

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

        static void waitForDisplay() {
            invoke("waitFor"); //$NON-NLS-1$
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
                RcpPlugin.log(e);
            }
        }
    }

    private static synchronized Display display() {
        Display display = Display.getCurrent();
        if (display != null) return display;

        display = Display.getDefault();
        if (display != null) return display;

        return new Display();
    }


    public synchronized Object start(final IApplicationContext context)
            throws Exception {

        Display.setAppName(context.getBrandingName());
        display().addListener(SWT.OpenDocument, OPEN_FILE_LISTENER);
        HelpApplication.setShutdownOnClose(true);

        // restart on init?
        if (System.getProperty("restartOnInit") != null) {
            ScopedPreferenceStore store =
                    new ScopedPreferenceStore(ConfigurationScope.INSTANCE,
                            "net.sf.helpaddons.rcp.product");
            boolean initDone = store.getBoolean("INIT");
            if (!initDone) {
                store.setValue("INIT", true);
                store.save();
                return EXIT_RESTART;
            }
        }

        // restart always? (except last restart was less than 10 Minutes ago)
        if (System.getProperty("restartAlways") != null) {
            ScopedPreferenceStore store =
                    new ScopedPreferenceStore(ConfigurationScope.INSTANCE,
                            "net.sf.helpaddons.rcp.product");
            int lastRestart = store.getInt("LAST_RESTART");
            int currentTimeInMinutes =
                    (int)(System.currentTimeMillis() / 1000 / 60);
            if (currentTimeInMinutes > lastRestart + 10) {
                store.setValue("LAST_RESTART", currentTimeInMinutes);
                store.save();
                return EXIT_RESTART;
            }
        }

        // start web server
        BaseHelpSystem.setMode(BaseHelpSystem.MODE_STANDALONE);
        if (!BaseHelpSystem.ensureWebappRunning()) {
            System.out.println(NLS.bind(
                    HelpBaseResources.HelpApplication_couldNotStart, Platform
                    .getLogFileLocation().toOSString()));
            return EXIT_OK;
        }

        // as soon as the UI thread is started (see below)
        // display the help window
        display().asyncExec(new Runnable() {
            public void run() {

                // end splash (in the org.eclipse.core.runtime.applications
                // extension thread must be set to "main") etc.
                context.applicationRunning();

                // create or update search index on first start-up
                // (instead of on first query)
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

                // open help window
                display().asyncExec(new Runnable() {
                    public void run() {
                        synchronized (STARTED_INSTANCE_LOCK) {
                            startedInstance = HelpRcpApplication.this;
                            List openFileArgs = computeOpenFileCommandLineArg();
                            if (openFileArgs.size() == 0) {
                                 open(openFile);
                            }
                            for (int i = 0; i < openFileArgs.size(); i++) {
                                open(openFileArgs.get(i).toString());
                            }
                        }
                    }
                });
            }
        });

        // try running UI loop if possible
        DisplayUtils.runUI();

        return exitCode;
    }

    private void armHelpWindow() {
        if (isExternalBrowserMode()) return;

        // exiting the application by closing the help window
        Shell[] allShells = display().getShells();
        if (allShells.length != 1) {
            throw new RuntimeException("No or more than one shell found.");
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
     *             <ul><li>&quot;/topic /my.plugin/path/file.html&quot;</li>
     *                 <li>&quot;/search my query&quot;</li>
     *                 <li>&quot;/topic /my.plugin/path/file.htm&quot; &quot;/search my query&quot;</li>
     *                 <li>&quot;/csh my.plugin.context_sensitive_help_id123&quot;</li>
     */
    private void open(String args) {

        // null -> start page
        if (args == null) {
            BaseHelpSystem.getHelpDisplay().displayHelp(isExternalBrowserMode());
            if (!HelpApplication.isShutdownOnClose()) {
                armHelpWindow();
            }
            return;
        }
        String argsTrimmed = args.trim();

        // shutdown?
        if (ARG_SHUTDOWN.equalsIgnoreCase(argsTrimmed)) {
            stopHelp();
            return;
        }

        // restart?
        if (ARG_RESTART.equalsIgnoreCase(argsTrimmed)) {
            exitCode = EXIT_RESTART;
            stopHelp();
            return;
        }

        // context ID?
        if (hasPrefix(argsTrimmed, ARG_CONTEXT_ID)) {
            String contextId = argsTrimmed.substring(ARG_CONTEXT_ID.length() + 1);
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
            argsTrimmed = ARG_TOPIC + " " + topics[0].getHref();
        }

        // topic to open?
        HelpDisplay help = BaseHelpSystem.getHelpDisplay();
        String topic = null;
        if (hasPrefix(argsTrimmed, ARG_TOPIC)) {
            topic = argsTrimmed.substring(ARG_TOPIC.length() + 1).trim();
            synchronized (this) {
                lastTopic = topic;
                lastTopicTime = System.currentTimeMillis();
            }

            // without query?
            if (   lastSearch == null
                || lastSearchTime < System.currentTimeMillis() - 3000) {
                help.displayHelpResource(
                        "topic=" + encode(topic),
                        isExternalBrowserMode());
                if (!HelpApplication.isShutdownOnClose()) {
                    armHelpWindow();
                }
                return;
            }
        }

        // with query
        if (hasPrefix(argsTrimmed, ARG_SEARCH)) {
            argsTrimmed = argsTrimmed.substring(ARG_SEARCH.length() + 1).trim();
        }
        if (topic == null) {
            synchronized (this) {
                lastSearch = argsTrimmed;
                lastSearchTime = System.currentTimeMillis();
            }
            if (    lastTopic != null
                 && lastTopicTime > System.currentTimeMillis() - LAST_TIMEOUT) {
                topic = lastTopic;
            }
        } else {
            if (   lastSearch != null
                && lastSearchTime > System.currentTimeMillis() - LAST_TIMEOUT) {
                argsTrimmed = lastSearch;
            }
        }
        help.displaySearch("searchWord=" + encode(argsTrimmed),
                           topic == null ? "" : topic,
                           isExternalBrowserMode());
        if (!HelpApplication.isShutdownOnClose()) {
            armHelpWindow();
        }
    }

    private boolean hasPrefix(String args, String suffix) {
        return    args != null
               && args.length() > suffix.length()
               && Character.isWhitespace(args.charAt(suffix.length()));
    }

    private void showPageNotFoundPage() {
        HelpDisplay helpDisplay = BaseHelpSystem.getHelpDisplay();
        String href =
            Platform.getPreferencesService().getString("org.eclipse.help.base",
                                                       "page_not_found",
                                                       null,
                                                       null);
        helpDisplay.displayHelpResource(href, isExternalBrowserMode());
        if (!HelpApplication.isShutdownOnClose()) {
            armHelpWindow();
        }
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

    private static List computeOpenFileCommandLineArg() {
        String[] args = Platform.getCommandLineArgs();
        if (args == null) return Collections.EMPTY_LIST;

        List result = new ArrayList();
        for (int i = 0; i < args.length; i++) {

            // without identifier
            if (   args[i].startsWith(ARG_RESTART)
                || args[i].startsWith(ARG_SEARCH)
                || args[i].startsWith(ARG_TOPIC)
                || args[i].startsWith(ARG_CONTEXT_ID))
                result.add(args[i]);

            // with identifier "--openFile"
            if (   i + 1 < args.length
                && (   args[i].equalsIgnoreCase(ARG_IDENTIFIER_A)
                    || args[i].equalsIgnoreCase(ARG_IDENTIFIER_B)))
                result.add(args[i + 1]);
        }
        return result;
    }

}
