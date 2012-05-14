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

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpApplication;
import org.eclipse.help.internal.standalone.EclipseController;

/**
 * This application starts or stops Eclipse Help as web application. In contrast
 * to {@link org.eclipse.help.standalone.Infocenter} no "eclipse" executable
 * file is required.
 */
public class HelpWebApplication implements IApplication {

    private IApplication help;

    public Object start(IApplicationContext context) throws Exception {

        // shutdown?
        if (isShutdownRequested()) {
            String workspace = Platform.getLocation().toFile().getAbsolutePath();
            String[] data = new String[] {"-data", workspace};
            new EclipseController("", data).shutdown();
            return EXIT_OK;
        }

        // create meta data area
        File metadata = new File(Platform.getLocation().toFile(), ".metadata/");
        metadata.mkdirs();

        // original
        help = new HelpApplication();

        // overwrite mode after(!) creating Help application
        // (otherwise "Bookmarks" tab will be shown)
        BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);

        // delegate to original
        return help.start(context);
    }

    private static boolean isShutdownRequested() {
        String[] args = Platform.getCommandLineArgs();
        for (int i = 0; i < args.length; i++) {
            if (   "-command".equalsIgnoreCase(args[i])
                && i + 1 < args.length
                && "shutdown".equalsIgnoreCase(args[i + 1])) return true;
        }
        return false;
    }

    public void stop() {
        help.stop();
        help = null;
    }

}
