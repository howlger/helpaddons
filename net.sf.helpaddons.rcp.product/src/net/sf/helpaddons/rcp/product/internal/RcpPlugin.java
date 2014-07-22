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
package net.sf.helpaddons.rcp.product.internal;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

/**
 * General function for this plug-in: its ID and logging.
 */
public class RcpPlugin {

    /** The ID of this plug-in/bundle. */
    public static final String ID =
        "net.sf.helpaddons.rcp.product"; //$NON-NLS-1$

    /**
     * Logs the given exception. It will be associated with this plug-in.
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    public static void log(Throwable throwable) {
        log(throwable.getMessage(), throwable);
    }

    /**
     * Logs the given message and exception. It will be associated with this
     * plug-in.
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    public static void log(String message, Throwable throwable) {
        ILog log = Platform.getLog(Platform.getBundle(ID));
        log.log(new Status(IStatus.ERROR, ID, message, throwable));
    }

}
